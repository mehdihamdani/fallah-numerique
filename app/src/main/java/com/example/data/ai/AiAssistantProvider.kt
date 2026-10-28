package com.example.data.ai

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class AiProviderMessage(
  val role: String, // "user" or "model"
  val text: String,
)

data class AiProviderPromptContext(
  val systemInstructionText: String,
  val history: List<AiProviderMessage>,
  val userPromptWithSandboxedData: String,
  val maxOutputTokens: Int = 1024,
)

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
 * Production provider backed by **Firebase AI Logic** (`com.google.firebase:firebase-ai`)
 * and **Firebase App Check** attestation.
 *
 * Security & Secrets Invariant:
 * - Zero client-side API keys are stored in APK assets, `.env`, `BuildConfig`, or logs.
 * - Uses `gemini-3.8-flash` lazily with explicit token limits and 60-second coroutine timeouts.
 */
class FirebaseGeminiAssistantProvider(
  private val appContext: Context,
  override val modelName: String = DEFAULT_MODEL_NAME,
  private val timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
) : AiAssistantProvider {

  override val providerId: String = "firebase-ai-logic"

  override val isConfigured: Boolean
    get() =
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

  // Lazy base check to satisfy lazy initialization lifecycle rules before first inference
  private val aiService by lazy { Firebase.ai }

  private fun buildModel(context: AiProviderPromptContext) =
    aiService.generativeModel(
      modelName = modelName,
      generationConfig =
        generationConfig {
          maxOutputTokens = context.maxOutputTokens.coerceIn(128, 2048)
          temperature = 0.35f
        },
      systemInstruction = content { text(context.systemInstructionText) },
    )

  private fun buildHistoryContent(history: List<AiProviderMessage>) =
    history.takeLast(MAX_HISTORY_TURNS).map { turn ->
      val mappedRole = if (turn.role == "model" || turn.role == "assistant") "model" else "user"
      content(mappedRole) { text(turn.text.take(2500)) }
    }

  override suspend fun generateReply(context: AiProviderPromptContext): String {
    if (!isConfigured) {
      throw FellahAiException(
        code = AiErrorCode.PROVIDER_UNAVAILABLE,
        message = "خدمة الذكاء الاصطناعي غير مهيأة حالياً في بيئة التشغيل.",
      )
    }

    return try {
      withTimeout(timeoutMillis) {
        val model = buildModel(context)
        val chat = model.startChat(history = buildHistoryContent(context.history))
        val response = chat.sendMessage(context.userPromptWithSandboxedData)
        val reply = response.text?.trim().orEmpty()
        if (reply.isBlank()) {
          throw FellahAiException(
            code = AiErrorCode.PROVIDER_UNAVAILABLE,
            message = "لم يُرجع المزود الذكي إجابة صالحة. يرجى إعادة المحاولة.",
          )
        }
        reply
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Fellah AI unary generation timed out after ${timeoutMillis}ms")
      throw FellahAiException(
        code = AiErrorCode.PROVIDER_TIMEOUT,
        message = "انتهت مهلة الاتصال بـ Fellah AI (60 ثانية). تحقق من الشبكة وأعد المحاولة.",
        cause = e,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: FellahAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Fellah AI generation failed (${e.javaClass.simpleName})")
      throw FellahAiException(
        code = AiErrorCode.PROVIDER_UNAVAILABLE,
        message = sanitizeProviderErrorMessage(e),
        cause = e,
      )
    }
  }

  override fun streamReply(context: AiProviderPromptContext): Flow<String> = flow {
    if (!isConfigured) {
      throw FellahAiException(
        code = AiErrorCode.PROVIDER_UNAVAILABLE,
        message = "خدمة الذكاء الاصطناعي غير مهيأة حالياً في بيئة التشغيل.",
      )
    }

    val accumulated = StringBuilder()
    try {
      withTimeout(timeoutMillis) {
        val model = buildModel(context)
        val chat = model.startChat(history = buildHistoryContent(context.history))
        chat.sendMessageStream(context.userPromptWithSandboxedData).collect { chunk ->
          val part = chunk.text.orEmpty()
          if (part.isNotEmpty()) {
            accumulated.append(part)
            emit(accumulated.toString())
          }
        }
      }
      if (accumulated.isBlank()) {
        throw FellahAiException(
          code = AiErrorCode.PROVIDER_UNAVAILABLE,
          message = "لم يُرجع المزود الذكي إجابة صالحة. يرجى إعادة المحاولة.",
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Fellah AI streaming timed out after ${timeoutMillis}ms")
      throw FellahAiException(
        code = AiErrorCode.PROVIDER_TIMEOUT,
        message = "انتهت مهلة الاستجابة المباشرة من Fellah AI. يرجى إعادة المحاولة.",
        cause = e,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: FellahAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Fellah AI stream failed (${e.javaClass.simpleName})")
      throw FellahAiException(
        code = AiErrorCode.PROVIDER_UNAVAILABLE,
        message = sanitizeProviderErrorMessage(e),
        cause = e,
      )
    }
  }

  companion object {
    private const val TAG = "FellahAiProvider"
    const val DEFAULT_MODEL_NAME = "gemini-3.8-flash"
    const val DEFAULT_TIMEOUT_MS = 60_000L
    private const val MAX_HISTORY_TURNS = 10

    /**
     * Ensures provider error surfaces are transparent for UX while never echoing
     * raw tokens, bearer headers, or API key query parameters.
     */
    internal fun sanitizeProviderErrorMessage(error: Throwable): String {
      val raw = error.localizedMessage ?: error.message ?: "Provider error"
      val scrubbed =
        raw
          .replace(Regex("AIza[0-9A-Za-z_\\-]{20,}"), "[REDACTED]")
          .replace(Regex("(?i)(api[_-]?key|token|secret|authorization)\\s*[:=]\\s*\\S+"), "$1=[REDACTED]")
          .trim()
      return scrubbed.take(220).ifBlank { "تعذر الاتصال بخدمة الذكاء الاصطناعي حالياً." }
    }
  }
}
