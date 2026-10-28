package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.AiAssistantProvider
import com.example.data.ai.AiChatMessageRecord
import com.example.data.ai.AiChatRequest
import com.example.data.ai.AiConversation
import com.example.data.ai.FellahAiException
import com.example.data.ai.FellahAiOrchestrator
import com.example.data.ai.FellahPlatformDataSnapshot
import com.example.data.ai.FirebaseGeminiAssistantProvider
import com.example.data.ai.ProductionCostInput
import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.example.data.repository.AiConversationRepository
import com.google.firebase.Timestamp
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FellahAiUiState(
  val activeConversationId: String? = null,
  val draftInput: String = "",
  val isSending: Boolean = false,
  val streamingReplyText: String = "",
  val streamingVerifiedSummary: String = "",
  val streamingHasVerifiedData: Boolean = false,
  val errorMessage: String? = null,
  val lastFailedMessage: String? = null,
  val lastFailedCostInput: ProductionCostInput? = null,
  val remainingDailyQuota: Int? = null,
  val isProviderVerified: Boolean = true,
  val providerLabel: String = "firebase-ai-logic:gemini-3.8-flash",
  val showHistoryPanel: Boolean = false,
  val showCostCalculatorPanel: Boolean = false,
  val showPrivacyInfoDialog: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FellahAiViewModel(
  private val orchestrator: FellahAiOrchestrator,
  private val conversationRepository: AiConversationRepository?,
  private val authenticatedUserId: String?,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      FellahAiUiState(
        isProviderVerified = orchestrator.isProviderVerified,
        providerLabel = orchestrator.providerIdentifier,
      )
    )
  val uiState: StateFlow<FellahAiUiState> = _uiState.asStateFlow()

  private val _activeConversationIdFlow = MutableStateFlow<String?>(null)

  // Ephemeral in-memory messages used in Visitor mode (never persisted to Firestore)
  private val _ephemeralMessages = MutableStateFlow<List<AiChatMessageRecord>>(emptyList())

  private var activeGenerationJob: Job? = null

  val conversationsState: StateFlow<List<AiConversation>> =
    if (!authenticatedUserId.isNullOrBlank() && conversationRepository != null) {
      conversationRepository
        .observeConversations(authenticatedUserId)
        .catch { emit(emptyList()) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = emptyList(),
        )
    } else {
      MutableStateFlow(emptyList())
    }

  val messagesState: StateFlow<List<AiChatMessageRecord>> =
    if (!authenticatedUserId.isNullOrBlank() && conversationRepository != null) {
      _activeConversationIdFlow
        .flatMapLatest { convId ->
          if (convId.isNullOrBlank()) {
            flowOf(emptyList())
          } else {
            conversationRepository
              .observeConversationMessages(authenticatedUserId, convId)
              .catch { emit(emptyList()) }
          }
        }
        .combine(_ephemeralMessages) { cloudMsgs, optimisticMsgs ->
          if (cloudMsgs.isNotEmpty()) cloudMsgs else optimisticMsgs
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = emptyList(),
        )
    } else {
      _ephemeralMessages.asStateFlow()
    }

  fun updateDraftInput(text: String) {
    _uiState.update { it.copy(draftInput = text) }
  }

  fun toggleHistoryPanel(show: Boolean? = null) {
    _uiState.update { it.copy(showHistoryPanel = show ?: !it.showHistoryPanel) }
  }

  fun toggleCostCalculatorPanel(show: Boolean? = null) {
    _uiState.update { it.copy(showCostCalculatorPanel = show ?: !it.showCostCalculatorPanel) }
  }

  fun setShowPrivacyInfoDialog(show: Boolean) {
    _uiState.update { it.copy(showPrivacyInfoDialog = show) }
  }

  fun dismissError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun startNewConversation() {
    activeGenerationJob?.cancel()
    _activeConversationIdFlow.value = null
    _ephemeralMessages.value = emptyList()
    _uiState.update {
      it.copy(
        activeConversationId = null,
        draftInput = "",
        isSending = false,
        streamingReplyText = "",
        streamingVerifiedSummary = "",
        streamingHasVerifiedData = false,
        errorMessage = null,
        lastFailedMessage = null,
        lastFailedCostInput = null,
        showHistoryPanel = false,
      )
    }
  }

  fun selectConversation(conversationId: String) {
    activeGenerationJob?.cancel()
    _activeConversationIdFlow.value = conversationId
    _ephemeralMessages.value = emptyList()
    _uiState.update {
      it.copy(
        activeConversationId = conversationId,
        isSending = false,
        streamingReplyText = "",
        streamingVerifiedSummary = "",
        streamingHasVerifiedData = false,
        errorMessage = null,
        showHistoryPanel = false,
      )
    }
  }

  fun deleteConversation(conversationId: String) {
    val repo = conversationRepository ?: return
    viewModelScope.launch {
      val res = repo.deleteConversation(conversationId)
      if (res.isSuccess) {
        if (_activeConversationIdFlow.value == conversationId) {
          startNewConversation()
        }
      } else {
        _uiState.update {
          it.copy(
            errorMessage =
              res.exceptionOrNull()?.localizedMessage ?: "تعذر حذف المحادثة حالياً."
          )
        }
      }
    }
  }

  fun suggestedQuestions(
    activeRole: UserRole,
    language: AppLanguage,
    isAuthenticated: Boolean,
  ): List<String> =
    orchestrator.suggestedQuestionsForRole(activeRole, language, isAuthenticated)

  fun sendMessage(
    messageText: String,
    activeRole: UserRole,
    language: AppLanguage,
    platformSnapshot: FellahPlatformDataSnapshot,
    costInput: ProductionCostInput? = null,
  ) {
    if (_uiState.value.isSending) return
    val priorHistory = messagesState.value
    val currentConvId = _activeConversationIdFlow.value

    // Clear draft input immediately only if non-blank
    if (messageText.isNotBlank()) {
      _uiState.update { it.copy(draftInput = "", showCostCalculatorPanel = false) }
    }

    activeGenerationJob =
      viewModelScope.launch {
        _uiState.update {
          it.copy(
            isSending = true,
            streamingReplyText = "",
            streamingVerifiedSummary = "",
            streamingHasVerifiedData = false,
            errorMessage = null,
            lastFailedMessage = messageText,
            lastFailedCostInput = costInput,
          )
        }

        val request =
          AiChatRequest(
            conversationId = currentConvId,
            message = messageText,
            activeRole = activeRole,
            language = language,
            productionCostInput = costInput,
          )

        try {
          orchestrator
            .streamChatTurn(
              request = request,
              priorMessages = priorHistory,
              platformSnapshot = platformSnapshot,
            )
            .collect { event ->
              when (event) {
                is FellahAiOrchestrator.StreamEvent.VerifiedContextReady -> {
                  _uiState.update {
                    it.copy(
                      streamingVerifiedSummary = event.verifiedDataSummary,
                      streamingHasVerifiedData = event.hasVerifiedPlatformData,
                    )
                  }
                }
                is FellahAiOrchestrator.StreamEvent.PartialText -> {
                  _uiState.update { it.copy(streamingReplyText = event.accumulatedText) }
                }
                is FellahAiOrchestrator.StreamEvent.Completed -> {
                  val resp = event.response
                  val nowTs = Timestamp.now()
                  val userRec =
                    AiChatMessageRecord(
                      id = "local_u_${UUID.randomUUID().toString().take(8)}",
                      userId = authenticatedUserId.orEmpty(),
                      conversationId = resp.conversationId,
                      sender = "user",
                      content = messageText.trim(),
                      activeRole = activeRole.code,
                      createdAt = nowTs,
                    )
                  val assistantRec =
                    AiChatMessageRecord(
                      id = "local_a_${UUID.randomUUID().toString().take(8)}",
                      userId = authenticatedUserId.orEmpty(),
                      conversationId = resp.conversationId,
                      sender = "assistant",
                      content = resp.replyText,
                      activeRole = activeRole.code,
                      verifiedDataSummary = resp.verifiedDataSummary,
                      hasVerifiedPlatformData = resp.hasVerifiedPlatformData,
                      createdAt = nowTs,
                    )

                  if (!platformSnapshot.isAuthenticated || conversationRepository == null) {
                    _ephemeralMessages.update { it + userRec + assistantRec }
                  } else {
                    if (_activeConversationIdFlow.value != resp.conversationId) {
                      _activeConversationIdFlow.value = resp.conversationId
                    }
                    // Optimistic fallback until snapshot listener emits
                    _ephemeralMessages.value = priorHistory + userRec + assistantRec
                  }

                  _uiState.update {
                    it.copy(
                      activeConversationId = resp.conversationId,
                      isSending = false,
                      streamingReplyText = "",
                      streamingVerifiedSummary = "",
                      streamingHasVerifiedData = false,
                      errorMessage = null,
                      lastFailedMessage = null,
                      lastFailedCostInput = null,
                      remainingDailyQuota = resp.remainingDailyQuota,
                    )
                  }
                }
              }
            }
        } catch (e: CancellationException) {
          throw e
        } catch (e: FellahAiException) {
          _uiState.update {
            it.copy(
              isSending = false,
              streamingReplyText = "",
              errorMessage = e.message,
            )
          }
        } catch (e: Exception) {
          _uiState.update {
            it.copy(
              isSending = false,
              streamingReplyText = "",
              errorMessage = e.localizedMessage ?: "تعذر إتمام الطلب مع Fellah AI.",
            )
          }
        }
      }
  }

  fun retryLastTurn(
    activeRole: UserRole,
    language: AppLanguage,
    platformSnapshot: FellahPlatformDataSnapshot,
  ) {
    val failedPrompt = _uiState.value.lastFailedMessage ?: return
    val failedCost = _uiState.value.lastFailedCostInput
    sendMessage(
      messageText = failedPrompt,
      activeRole = activeRole,
      language = language,
      platformSnapshot = platformSnapshot,
      costInput = failedCost,
    )
  }

  companion object {
    fun provideFactory(
      authenticatedUserId: String?,
      customProvider: AiAssistantProvider? = null,
    ): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val appContext = this[APPLICATION_KEY] as Context
        val aiProvider = customProvider ?: FirebaseGeminiAssistantProvider(appContext)
        val convRepo =
          if (!authenticatedUserId.isNullOrBlank()) {
            AiConversationRepository(appContext)
          } else {
            null
          }
        val orchestrator =
          FellahAiOrchestrator(
            provider = aiProvider,
            conversationRepository = convRepo,
          )
        FellahAiViewModel(
          orchestrator = orchestrator,
          conversationRepository = convRepo,
          authenticatedUserId = authenticatedUserId,
        )
      }
    }
  }
}
