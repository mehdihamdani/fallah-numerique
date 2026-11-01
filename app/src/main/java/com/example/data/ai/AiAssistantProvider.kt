package com.example.data.ai

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class AiProviderMessage(
  val role: String, // "user" or "model"
  val text: String,
)

data class AiProviderPromptContext(
  val systemInstructionText: String,
  val history: List<AiProviderMessage>,
  val userPromptWithSandboxedData: String,
  val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
) {
  companion object {
    const val DEFAULT_MAX_OUTPUT_TOKENS = 4096
  }
}

/**
 * Provider-agnostic AI interface so the underlying LLM provider can be swapped
 * or mocked in tests without rewriting application or UI logic.
 */
interface AiAssistantProvider {
  val providerId: String
  val modelName: String
  val isConfigured: Boolean

  suspend fun generateReply(context: AiProviderPromptContext): String

  fun streamReply(context: AiProviderPromptContext): Flow<String>
}

/**
 * Internal transport gateway over Firebase AI Logic so multi-stage streaming,
 * partial-stream recovery, and unary fallback behavior can be deterministically unit-tested.
 */
internal interface GeminiModelGateway {
  fun isEnvironmentConfigured(): Boolean

  fun streamChatChunks(targetModelName: String, context: AiProviderPromptContext): Flow<String>

  suspend fun sendUnaryChat(targetModelName: String, context: AiProviderPromptContext): String
}

private class DefaultFirebaseGeminiGateway(
  private val appContext: Context,
) : GeminiModelGateway {

  // Lazy initialization as required by Firebase AI Logic lifecycle rules
  private val aiService by lazy { Firebase.ai }

  override fun isEnvironmentConfigured(): Boolean =
    runCatching {
        val hasApp = FirebaseApp.getApps(appContext).isNotEmpty()
        val appIdRes =
          appContext.resources.getIdentifier(
            "google_app_id",
            "string",
            appContext.packageName,
          )
        hasApp && appIdRes != 0 && appContext.getString(appIdRes).isNotBlank()
      }
      .getOrDefault(false)

  private fun buildModel(targetModelName: String, context: AiProviderPromptContext) =
    aiService.generativeModel(
      modelName = targetModelName,
      generationConfig =
        generationConfig {
          // Ensure sufficient token headroom for Gemini 3 internal thinking + visible output
          maxOutputTokens =
            context.maxOutputTokens.coerceIn(
              FirebaseGeminiAssistantProvider.MIN_SAFE_OUTPUT_TOKENS,
              FirebaseGeminiAssistantProvider.MAX_OUTPUT_TOKENS_CEILING,
            )
          temperature = 0.35f
        },
      systemInstruction = content { text(context.systemInstructionText) },
    )

  private fun buildHistoryContent(history: List<AiProviderMessage>) =
    history.takeLast(FirebaseGeminiAssistantProvider.MAX_HISTORY_TURNS).map { turn ->
      val mappedRole = if (turn.role == "model" || turn.role == "assistant") "model" else "user"
      content(mappedRole) { text(turn.text.take(2500)) }
    }

  override fun streamChatChunks(
    targetModelName: String,
    context: AiProviderPromptContext,
  ): Flow<String> = flow {
    val model = buildModel(targetModelName, context)
    val chat = model.startChat(history = buildHistoryContent(context.history))
    chat.sendMessageStream(context.userPromptWithSandboxedData).collect { chunk ->
      val part = chunk.text.orEmpty()
      if (part.isNotEmpty()) {
        emit(part)
      }
    }
  }

  override suspend fun sendUnaryChat(
    targetModelName: String,
    context: AiProviderPromptContext,
  ): String {
    val model = buildModel(targetModelName, context)
    val chat = model.startChat(history = buildHistoryContent(context.history))
    val response = chat.sendMessage(context.userPromptWithSandboxedData)
    return response.text?.trim().orEmpty()
  }
}

/**
 * Production provider backed by **Firebase AI Logic** (`com.google.firebase:firebase-ai`)
 * and **Firebase App Check** attestation.
 *
 * Resilience & Mobile Network Guarantees:
 * - Zero client-side API keys are stored in APK assets, `.env`, `BuildConfig`, or logs.
 * - Executes all SDK & network I/O on [Dispatchers.IO].
 * - Preserves already-streamed partial text if an SSE stream stalls at closing time.
 * - Automatically falls back from stalled/buffered SSE streaming (`sendMessageStream`) to
 *   standard unary HTTPS POST (`sendMessage`) using `gemini-3.5-flash-lite` (and `gemini-3.8-flash`)
 *   so mobile carrier proxies never cause false live-response timeout errors.
 */
class FirebaseGeminiAssistantProvider
internal constructor(
  private val gateway: GeminiModelGateway,
  override val modelName: String = DEFAULT_MODEL_NAME,
  private val fallbackModelName: String = FALLBACK_FAST_MODEL_NAME,
  private val timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
  private val streamStageTimeoutMillis: Long = DEFAULT_STREAM_STAGE_TIMEOUT_MS,
  private val unaryFallbackTimeoutMillis: Long = DEFAULT_UNARY_FALLBACK_TIMEOUT_MS,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AiAssistantProvider {

  constructor(
    appContext: Context,
    modelName: String = DEFAULT_MODEL_NAME,
    timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
  ) : this(
    gateway = DefaultFirebaseGeminiGateway(appContext.applicationContext),
    modelName = modelName,
    fallbackModelName = FALLBACK_FAST_MODEL_NAME,
    timeoutMillis = timeoutMillis,
  )

  override val providerId: String = "firebase-ai-logic"

  override val isConfigured: Boolean
    get() = gateway.isEnvironmentConfigured()

  override suspend fun generateReply(context: AiProviderPromptContext): String =
    withContext(ioDispatcher) {
      if (!isConfigured) {
        throw FellahAiException(
          code = AiErrorCode.PROVIDER_UNAVAILABLE,
          message = "خدمة الذكاء الاصطناعي غير مهيأة حالياً في بيئة التشغيل.",
        )
      }

      val primaryBudgetMs = minOf(timeoutMillis, DEFAULT_PRIMARY_UNARY_TIMEOUT_MS).coerceAtLeast(1L)
      val fallbackBudgetMs = unaryFallbackTimeoutMillis.coerceAtLeast(1L)

      var lastError: Throwable? = null

      // Stage 1: Primary model unary request
      try {
        val primaryReply =
          withTimeout(primaryBudgetMs) { gateway.sendUnaryChat(modelName, context).trim() }
        if (primaryReply.isNotBlank()) {
          return@withContext primaryReply
        }
      } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Primary unary model ($modelName) timed out after ${primaryBudgetMs}ms; trying fast fallback")
        lastError = e
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Primary unary model ($modelName) failed (${e.javaClass.simpleName}); trying fast fallback")
        lastError = e
      }

      // Stage 2: Ultra-fast Gemini 3 Flash-Lite unary fallback
      val candidateModels =
        listOf(fallbackModelName, modelName).distinct().filter {
          lastError != null || it != modelName
        }

      for (candidate in candidateModels) {
        try {
          val fallbackReply =
            withTimeout(fallbackBudgetMs) { gateway.sendUnaryChat(candidate, context).trim() }
          if (fallbackReply.isNotBlank()) {
            return@withContext fallbackReply
          }
        } catch (e: TimeoutCancellationException) {
          Log.e(TAG, "Fallback unary model ($candidate) timed out after ${fallbackBudgetMs}ms")
          lastError = e
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Fallback unary model ($candidate) failed (${e.javaClass.simpleName})")
          lastError = e
        }
      }

      throw mapTerminalFailure(lastError)
    }

  override fun streamReply(context: AiProviderPromptContext): Flow<String> =
    flow {
        if (!isConfigured) {
          throw FellahAiException(
            code = AiErrorCode.PROVIDER_UNAVAILABLE,
            message = "خدمة الذكاء الاصطناعي غير مهيأة حالياً في بيئة التشغيل.",
          )
        }

        val streamBudgetMs = minOf(timeoutMillis, streamStageTimeoutMillis).coerceAtLeast(1L)
        val unaryBudgetMs = unaryFallbackTimeoutMillis.coerceAtLeast(1L)
        val accumulated = StringBuilder()
        var stageFailure: Throwable? = null

        // Stage 1: Live SSE streaming on primary model
        try {
          withTimeout(streamBudgetMs) {
            gateway.streamChatChunks(modelName, context).collect { part ->
              if (part.isNotEmpty()) {
                accumulated.append(part)
                emit(accumulated.toString())
              }
            }
          }
        } catch (e: TimeoutCancellationException) {
          if (accumulated.isNotBlank()) {
            Log.w(
              TAG,
              "SSE stream tail timed out after emitting ${accumulated.length} chars; completing with streamed answer.",
            )
            return@flow
          }
          Log.w(
            TAG,
            "SSE live stream stalled before first chunk (${streamBudgetMs}ms); switching to unary HTTPS fallback.",
          )
          stageFailure = e
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          if (accumulated.isNotBlank()) {
            Log.w(
              TAG,
              "SSE stream closed early (${e.javaClass.simpleName}) after ${accumulated.length} chars; preserving partial answer.",
            )
            return@flow
          }
          Log.w(
            TAG,
            "SSE stream failed (${e.javaClass.simpleName}); switching to unary HTTPS fallback.",
          )
          stageFailure = e
        }

        if (accumulated.isNotBlank()) {
          return@flow
        }

        // Stage 2: Automatic unary HTTPS POST fallback (bypasses mobile carrier SSE buffering)
        val unaryCandidates = listOf(fallbackModelName, modelName).distinct()
        for (candidate in unaryCandidates) {
          try {
            val unaryReply =
              withTimeout(unaryBudgetMs) { gateway.sendUnaryChat(candidate, context).trim() }
            if (unaryReply.isNotBlank()) {
              emit(unaryReply)
              return@flow
            }
          } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Unary fallback ($candidate) timed out after ${unaryBudgetMs}ms")
            stageFailure = e
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            Log.e(TAG, "Unary fallback ($candidate) failed (${e.javaClass.simpleName})")
            stageFailure = e
          }
        }

        throw mapTerminalFailure(stageFailure)
      }
      .flowOn(ioDispatcher)

  private fun mapTerminalFailure(cause: Throwable?): FellahAiException =
    when (cause) {
      is FellahAiException -> cause
      is TimeoutCancellationException ->
        FellahAiException(
          code = AiErrorCode.PROVIDER_TIMEOUT,
          message =
            "انتهت مهلة الاتصال بخدمة فلاح الذكي بسبب بطء الشبكة. يرجى التحقق من الإنترنت وإعادة المحاولة.",
          cause = cause,
        )
      null ->
        FellahAiException(
          code = AiErrorCode.PROVIDER_UNAVAILABLE,
          message = "لم يُرجع المزود الذكي إجابة صالحة. يرجى إعادة المحاولة.",
        )
      else ->
        FellahAiException(
          code = AiErrorCode.PROVIDER_UNAVAILABLE,
          message = sanitizeProviderErrorMessage(cause),
          cause = cause,
        )
    }

  companion object {
    private const val TAG = "FellahAiProvider"
    const val DEFAULT_MODEL_NAME = "gemini-3.8-flash"
    const val FALLBACK_FAST_MODEL_NAME = "gemini-3.5-flash-lite"
    const val DEFAULT_TIMEOUT_MS = 60_000L
    const val DEFAULT_STREAM_STAGE_TIMEOUT_MS = 25_000L
    const val DEFAULT_PRIMARY_UNARY_TIMEOUT_MS = 35_000L
    const val DEFAULT_UNARY_FALLBACK_TIMEOUT_MS = 35_000L
    internal const val MIN_SAFE_OUTPUT_TOKENS = 2048
    internal const val MAX_OUTPUT_TOKENS_CEILING = 8192
    internal const val MAX_HISTORY_TURNS = 10

    /**
     * Ensures provider error surfaces are transparent for UX while never echoing
     * raw tokens, bearer headers, or API key query parameters.
     */
    internal fun sanitizeProviderErrorMessage(error: Throwable): String {
      val raw = error.localizedMessage ?: error.message ?: "Provider error"
      val scrubbed =
        raw
          .replace(Regex("AIza[0-9A-Za-z_\\-]{20,}"), "[REDACTED]")
          .replace(
            Regex("(?i)(api[_-]?key|token|secret|authorization)\\s*[:=]\\s*\\S+"),
            "$1=[REDACTED]",
          )
          .trim()
      return scrubbed.take(220).ifBlank { "تعذر الاتصال بخدمة الذكاء الاصطناعي حالياً." }
    }
  }
}
