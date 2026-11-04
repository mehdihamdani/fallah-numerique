package com.example.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountInputValidator
import com.example.data.model.AppLanguage
import com.example.ui.auth.FirebasePhoneAuthGateway
import com.example.ui.auth.GoogleSignInOutcome
import com.example.ui.auth.PhoneAuthGateway
import com.example.ui.auth.PhoneOtpSendOutcome
import com.example.ui.auth.PhoneOtpVerifyOutcome
import com.example.ui.auth.checkSilentGoogleCredential
import com.example.ui.auth.signInInteractivelyWithGoogle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthEntryMethod {
  PHONE_OTP,
  GOOGLE,
}

enum class PhoneOtpStep {
  ENTER_PHONE,
  VERIFY_CODE,
}

sealed interface LoginAuthStatus {
  data object Idle : LoginAuthStatus

  data object CheckingSavedSession : LoginAuthStatus

  data object SigningIn : LoginAuthStatus

  data object SendingPhoneCode : LoginAuthStatus

  data object VerifyingPhoneCode : LoginAuthStatus

  data object Cancelled : LoginAuthStatus

  data class PhoneSavedReadyForGoogle(
    val savedPhone: String,
    val message: String,
  ) : LoginAuthStatus

  data class Error(
    val message: String,
    val suggestGoogleFallback: Boolean = false,
  ) : LoginAuthStatus
}

data class LoginUiState(
  val status: LoginAuthStatus = LoginAuthStatus.Idle,
  val language: AppLanguage = AppLanguage.AR,
  val hasCheckedSilentSignIn: Boolean = false,
  val selectedAuthMethod: AuthEntryMethod = AuthEntryMethod.PHONE_OTP,
  val phoneStep: PhoneOtpStep = PhoneOtpStep.ENTER_PHONE,
  val phoneInput: String = "",
  val normalizedPhone: String = "",
  val otpCodeInput: String = "",
  val verificationId: String = "",
  val resendToken: Any? = null,
  val signedInUserUid: String? = null,
) {
  /**
   * Only block interactive buttons during active user-initiated sign-in / OTP network operations.
   * Passive `CheckingSavedSession` must NEVER disable buttons on devices like Samsung Galaxy S10.
   */
  val isBusy: Boolean
    get() =
      status is LoginAuthStatus.SigningIn ||
        status is LoginAuthStatus.SendingPhoneCode ||
        status is LoginAuthStatus.VerifyingPhoneCode
}

class LoginViewModel(
  private val phoneAuthGateway: PhoneAuthGateway = FirebasePhoneAuthGateway,
  private val googleSignInRunner: suspend (Context?, CredentialManager?) -> GoogleSignInOutcome =
    ::signInInteractivelyWithGoogle,
) : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  fun setLanguage(language: AppLanguage) {
    _uiState.update { state ->
      val updatedStatus =
        when (val currentStatus = state.status) {
          is LoginAuthStatus.PhoneSavedReadyForGoogle ->
            currentStatus.copy(
              message = buildPhoneSavedGmailGuidance(language, currentStatus.savedPhone)
            )
          else -> currentStatus
        }
      state.copy(language = language, status = updatedStatus)
    }
  }

  fun selectAuthMethod(method: AuthEntryMethod) {
    _uiState.update { state ->
      state.copy(
        selectedAuthMethod = method,
        status =
          if (
            state.status is LoginAuthStatus.Error ||
              state.status is LoginAuthStatus.Cancelled ||
              state.status is LoginAuthStatus.PhoneSavedReadyForGoogle
          ) {
            LoginAuthStatus.Idle
          } else {
            state.status
          },
      )
    }
  }

  fun onPhoneInputChanged(raw: String) {
    val sanitized = raw.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }.take(20)
    _uiState.update { state ->
      state.copy(
        phoneInput = sanitized,
        status =
          if (
            state.status is LoginAuthStatus.Error ||
              state.status is LoginAuthStatus.PhoneSavedReadyForGoogle
          ) {
            LoginAuthStatus.Idle
          } else {
            state.status
          },
      )
    }
  }

  fun onOtpCodeInputChanged(raw: String) {
    val digits = raw.filter { it.isDigit() }.take(6)
    _uiState.update { state ->
      state.copy(
        otpCodeInput = digits,
        status = if (state.status is LoginAuthStatus.Error) LoginAuthStatus.Idle else state.status,
      )
    }
  }

  fun editPhoneNumber() {
    _uiState.update {
      it.copy(
        phoneStep = PhoneOtpStep.ENTER_PHONE,
        otpCodeInput = "",
        verificationId = "",
        status = LoginAuthStatus.Idle,
      )
    }
  }

  fun dismissMessage() {
    _uiState.update { state ->
      if (
        state.status is LoginAuthStatus.Cancelled ||
          state.status is LoginAuthStatus.Error ||
          state.status is LoginAuthStatus.PhoneSavedReadyForGoogle
      ) {
        state.copy(status = LoginAuthStatus.Idle)
      } else {
        state
      }
    }
  }

  /**
   * Returns a normalized E.164 phone number if the user already typed a valid mobile number,
   * allowing Google Sign-In fallback to pre-fill their phone in Onboarding without losing input.
   */
  fun currentNormalizedDraftPhone(): String? {
    val current = _uiState.value
    return current.normalizedPhone.takeIf { it.isNotBlank() }
      ?: AccountInputValidator.normalizePhoneToE164(current.phoneInput)
  }

  fun requestPhoneOtpCode(
    context: Context?,
    isResend: Boolean = false,
    credentialManager: CredentialManager? = null,
    onPhoneDraftCaptured: (String) -> Unit = {},
    onGoogleHandoffAuthSuccess: () -> Unit = {},
    onInstantAuthSuccess: (String) -> Unit = {},
  ) {
    val current = _uiState.value
    if (current.isBusy) return

    val e164 = AccountInputValidator.normalizePhoneToE164(current.phoneInput)
    if (e164 == null) {
      val msg =
        when (current.language) {
          AppLanguage.AR ->
            "يرجى إدخال رقم هاتف نقال صحيح (مثال: 0550123456 أو +213550123456)."
          AppLanguage.FR ->
            "Veuillez saisir un numéro de téléphone valide (ex : 0550123456 ou +213550123456)."
          AppLanguage.EN ->
            "Please enter a valid mobile phone number (e.g. 0550123456 or +213550123456)."
        }
      _uiState.update { it.copy(status = LoginAuthStatus.Error(msg)) }
      return
    }

    _uiState.update {
      it.copy(
        normalizedPhone = e164,
        status = LoginAuthStatus.SendingPhoneCode,
      )
    }

    viewModelScope.launch {
      val token = if (isResend) _uiState.value.resendToken else null
      when (val outcome = phoneAuthGateway.sendVerificationCode(context, e164, token)) {
        is PhoneOtpSendOutcome.CodeSent -> {
          _uiState.update {
            it.copy(
              status = LoginAuthStatus.Idle,
              phoneStep = PhoneOtpStep.VERIFY_CODE,
              normalizedPhone = outcome.normalizedPhone,
              verificationId = outcome.verificationId,
              resendToken = outcome.resendToken,
              otpCodeInput = "",
            )
          }
        }
        is PhoneOtpSendOutcome.InstantVerified -> {
          _uiState.update {
            it.copy(
              status = LoginAuthStatus.Idle,
              normalizedPhone = outcome.normalizedPhone,
            )
          }
          onInstantAuthSuccess(outcome.normalizedPhone)
        }
        is PhoneOtpSendOutcome.RequireGoogleIdentityHandoff -> {
          // Save the farmer's normalized +213XXXXXXXXX phone number immediately without popping open
          // Google's "Email or phone" webview unannounced (which previously caused users to type their
          // phone number into Google's email prompt). Instead, present clear localized guidance with
          // 1-tap Google/Gmail sign-in below.
          onPhoneDraftCaptured(outcome.normalizedPhone)
          val guidance = buildPhoneSavedGmailGuidance(_uiState.value.language, outcome.normalizedPhone)
          _uiState.update {
            it.copy(
              normalizedPhone = outcome.normalizedPhone,
              selectedAuthMethod = AuthEntryMethod.GOOGLE,
              status =
                LoginAuthStatus.PhoneSavedReadyForGoogle(
                  savedPhone = outcome.normalizedPhone,
                  message = guidance,
                ),
            )
          }
        }
        is PhoneOtpSendOutcome.Error -> {
          val localized =
            when (_uiState.value.language) {
              AppLanguage.AR -> outcome.messageAr
              AppLanguage.FR -> outcome.messageFr
              AppLanguage.EN -> outcome.messageEn
            }
          _uiState.update {
            it.copy(
              status =
                LoginAuthStatus.Error(
                  message = localized,
                  suggestGoogleFallback = outcome.suggestGoogleFallback,
                )
            )
          }
        }
      }
    }
  }

  fun confirmPhoneOtpCode(onAuthSuccess: (String) -> Unit) {
    val current = _uiState.value
    if (current.isBusy) return

    val code = current.otpCodeInput.trim()
    if (code.length != 6 || !code.all { it.isDigit() }) {
      val msg =
        when (current.language) {
          AppLanguage.AR -> "يرجى إدخال رمز الدخول المكون من 6 أرقام كاملاً."
          AppLanguage.FR -> "Veuillez saisir le code complet à 6 chiffres."
          AppLanguage.EN -> "Please enter the complete 6-digit login code."
        }
      _uiState.update { it.copy(status = LoginAuthStatus.Error(msg)) }
      return
    }

    if (current.verificationId.isBlank()) {
      editPhoneNumber()
      return
    }

    _uiState.update { it.copy(status = LoginAuthStatus.VerifyingPhoneCode) }

    viewModelScope.launch {
      when (
        val outcome =
          phoneAuthGateway.verifyOtpCode(
            verificationId = current.verificationId,
            otpCode = code,
            e164PhoneNumber = current.normalizedPhone,
          )
      ) {
        is PhoneOtpVerifyOutcome.Success -> {
          _uiState.update {
            it.copy(
              status = LoginAuthStatus.Idle,
              normalizedPhone = outcome.normalizedPhone,
              signedInUserUid = outcome.userUid,
            )
          }
          onAuthSuccess(outcome.normalizedPhone)
        }
        is PhoneOtpVerifyOutcome.Error -> {
          val localized =
            when (_uiState.value.language) {
              AppLanguage.AR -> outcome.messageAr
              AppLanguage.FR -> outcome.messageFr
              AppLanguage.EN -> outcome.messageEn
            }
          _uiState.update { state ->
            state.copy(
              status = LoginAuthStatus.Error(localized),
              otpCodeInput = if (outcome.isSessionExpired) "" else state.otpCodeInput,
            )
          }
        }
      }
    }
  }

  fun runSilentSignInOnce(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
  ) {
    if (_uiState.value.hasCheckedSilentSignIn) return
    _uiState.update {
      it.copy(
        hasCheckedSilentSignIn = true,
        status = LoginAuthStatus.CheckingSavedSession,
      )
    }
    viewModelScope.launch {
      when (checkSilentGoogleCredential(context, credentialManager)) {
        GoogleSignInOutcome.Success -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
          onAuthSuccess()
        }
        GoogleSignInOutcome.NoSavedCredential,
        GoogleSignInOutcome.Cancelled,
        is GoogleSignInOutcome.Error -> {
          _uiState.update { state ->
            if (state.status is LoginAuthStatus.CheckingSavedSession) {
              state.copy(status = LoginAuthStatus.Idle)
            } else {
              state
            }
          }
        }
      }
    }
  }

  fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
  ) {
    if (_uiState.value.isBusy) return
    _uiState.update { it.copy(status = LoginAuthStatus.SigningIn) }
    viewModelScope.launch {
      handleGoogleSignInOutcome(
        outcome = googleSignInRunner(context, credentialManager),
        onAuthSuccess = onAuthSuccess,
      )
    }
  }

  private fun handleGoogleSignInOutcome(
    outcome: GoogleSignInOutcome,
    onAuthSuccess: () -> Unit,
  ) {
    when (outcome) {
      GoogleSignInOutcome.Success -> {
        _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
        onAuthSuccess()
      }
      GoogleSignInOutcome.Cancelled -> {
        val draftPhone = currentNormalizedDraftPhone()
        if (draftPhone != null) {
          val guidance = buildPhoneSavedGmailGuidance(_uiState.value.language, draftPhone)
          _uiState.update {
            it.copy(
              selectedAuthMethod = AuthEntryMethod.GOOGLE,
              status =
                LoginAuthStatus.PhoneSavedReadyForGoogle(
                  savedPhone = draftPhone,
                  message = guidance,
                ),
            )
          }
        } else {
          _uiState.update { it.copy(status = LoginAuthStatus.Cancelled) }
        }
      }
      GoogleSignInOutcome.NoSavedCredential -> {
        _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
      }
      is GoogleSignInOutcome.Error -> {
        _uiState.update { it.copy(status = LoginAuthStatus.Error(outcome.message)) }
      }
    }
  }

  private fun buildPhoneSavedGmailGuidance(language: AppLanguage, savedPhone: String): String =
    when (language) {
      AppLanguage.AR ->
        "تم حفظ رقم هاتفك ($savedPhone) بنجاح في التطبيق ✓. إرسال رسائل SMS المباشرة غير مفعّل حالياً في مشروع Firebase (لتفعيله: افتح Firebase Console > Authentication > Sign-in method > Phone أو أضف رقم اختبار). للدخول الفوري الآن، اضغط زر Google بالأسفل واكتب بريدك (example@gmail.com) ولا تكتب رقم الهاتف داخل نافذة Google."
      AppLanguage.FR ->
        "Votre numéro ($savedPhone) est enregistré ✓. L’envoi SMS n’est pas encore activé dans Firebase Console (Authentication > Sign-in method > Phone). Pour continuer immédiatement, appuyez sur le bouton Google ci-dessous avec votre adresse Gmail (exemple@gmail.com) — sans saisir votre numéro dans Google."
      AppLanguage.EN ->
        "Your phone number ($savedPhone) is saved ✓. Direct SMS is not yet enabled in Firebase Console (Authentication > Sign-in method > Phone). To sign in immediately, tap the Google button below and enter your Gmail address (example@gmail.com) — do NOT enter your phone number inside Google."
    }
}
