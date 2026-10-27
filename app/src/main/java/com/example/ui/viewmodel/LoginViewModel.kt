package com.example.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.ui.auth.GoogleSignInOutcome
import com.example.ui.auth.checkSilentGoogleCredential
import com.example.ui.auth.signInInteractivelyWithGoogle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoginAuthStatus {
  data object Idle : LoginAuthStatus

  data object CheckingSavedSession : LoginAuthStatus

  data object SigningIn : LoginAuthStatus

  data object Cancelled : LoginAuthStatus

  data class Error(val message: String) : LoginAuthStatus
}

data class LoginUiState(
  val status: LoginAuthStatus = LoginAuthStatus.Idle,
  val language: AppLanguage = AppLanguage.AR,
  val hasCheckedSilentSignIn: Boolean = false,
) {
  val isBusy: Boolean
    get() = status is LoginAuthStatus.CheckingSavedSession || status is LoginAuthStatus.SigningIn
}

class LoginViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  fun setLanguage(language: AppLanguage) {
    _uiState.update { it.copy(language = language) }
  }

  fun dismissMessage() {
    _uiState.update { state ->
      if (state.status is LoginAuthStatus.Cancelled || state.status is LoginAuthStatus.Error) {
        state.copy(status = LoginAuthStatus.Idle)
      } else {
        state
      }
    }
  }

  fun runSilentSignInOnce(
    context: Context,
    credentialManager: CredentialManager,
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
      when (val outcome = checkSilentGoogleCredential(context, credentialManager)) {
        GoogleSignInOutcome.Success -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
          onAuthSuccess()
        }
        GoogleSignInOutcome.NoSavedCredential,
        GoogleSignInOutcome.Cancelled -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
        }
        is GoogleSignInOutcome.Error -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
        }
      }
    }
  }

  fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
  ) {
    if (_uiState.value.status is LoginAuthStatus.SigningIn) return
    _uiState.update { it.copy(status = LoginAuthStatus.SigningIn) }
    viewModelScope.launch {
      when (val outcome = signInInteractivelyWithGoogle(context, credentialManager)) {
        GoogleSignInOutcome.Success -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
          onAuthSuccess()
        }
        GoogleSignInOutcome.Cancelled -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Cancelled) }
        }
        GoogleSignInOutcome.NoSavedCredential -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Idle) }
        }
        is GoogleSignInOutcome.Error -> {
          _uiState.update { it.copy(status = LoginAuthStatus.Error(outcome.message)) }
        }
      }
    }
  }
}
