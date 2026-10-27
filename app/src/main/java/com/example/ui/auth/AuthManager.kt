package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.screens.LoginScreen
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "Auth"

sealed interface GoogleSignInOutcome {
  data object Success : GoogleSignInOutcome

  data object Cancelled : GoogleSignInOutcome

  data object NoSavedCredential : GoogleSignInOutcome

  data class Error(val message: String) : GoogleSignInOutcome
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

internal fun Context.findActivity(): Activity? {
  var current: Context? = this
  while (current is ContextWrapper) {
    if (current is Activity) return current
    current = current.baseContext
  }
  return null
}

/**
 * Silent startup credential check using Jetpack CredentialManager.
 * Builds a GetCredentialRequest with strictly ONE option (GetGoogleIdOption).
 */
suspend fun checkSilentGoogleCredential(
  context: Context,
  credentialManager: CredentialManager,
): GoogleSignInOutcome {
  if (Firebase.auth.currentUser != null) {
    return GoogleSignInOutcome.Success
  }
  val activity = context.findActivity() ?: return GoogleSignInOutcome.NoSavedCredential
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      return GoogleSignInOutcome.NoSavedCredential
    }
  if (clientId.isBlank()) {
    return GoogleSignInOutcome.NoSavedCredential
  }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  return try {
    val result = credentialManager.getCredential(activity, request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
      Firebase.auth.signInWithCredential(authCredential).await()
      GoogleSignInOutcome.Success
    } else {
      GoogleSignInOutcome.NoSavedCredential
    }
  } catch (e: CancellationException) {
    throw e
  } catch (_: NoCredentialException) {
    GoogleSignInOutcome.NoSavedCredential
  } catch (_: GetCredentialCancellationException) {
    GoogleSignInOutcome.NoSavedCredential
  } catch (e: Exception) {
    Log.d(AUTH_TAG, "Silent Google credential check skipped: ${e.message}")
    GoogleSignInOutcome.NoSavedCredential
  }
}

/**
 * Interactive Google Sign-In using Jetpack CredentialManager.
 * Builds a GetCredentialRequest with strictly ONE option (GetSignInWithGoogleOption)
 * to keep the Android 16 (API 36) bottom sheet responsive.
 */
suspend fun signInInteractivelyWithGoogle(
  context: Context,
  credentialManager: CredentialManager,
): GoogleSignInOutcome {
  val activity =
    context.findActivity()
      ?: return GoogleSignInOutcome.Error("Activity context required for Google Sign-In")

  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Missing default_web_client_id resource", e)
      return GoogleSignInOutcome.Error(
        "Google Sign-In configuration missing: default_web_client_id not found"
      )
    }
  if (clientId.isBlank()) {
    return GoogleSignInOutcome.Error("Google Sign-In client ID is empty")
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  return try {
    val result = credentialManager.getCredential(activity, request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
      Firebase.auth.signInWithCredential(authCredential).await()
      GoogleSignInOutcome.Success
    } else {
      GoogleSignInOutcome.Error("Unexpected credential type received from Credential Manager")
    }
  } catch (e: GetCredentialCancellationException) {
    Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
    GoogleSignInOutcome.Cancelled
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.e(AUTH_TAG, "Google Sign-In failed", e)
    GoogleSignInOutcome.Error(e.localizedMessage ?: "تعذر تسجيل الدخول عبر Google")
  }
}

fun signOutUser(
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope,
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

val HexagonShape = GenericShape { size, _ ->
  val cx = size.width / 2f
  val cy = size.height / 2f
  val radius = minOf(cx, cy)
  for (i in 0 until 6) {
    val angleRad = Math.toRadians((60.0 * i) - 90.0)
    val x = cx + (radius * cos(angleRad)).toFloat()
    val y = cy + (radius * sin(angleRad)).toFloat()
    if (i == 0) moveTo(x, y) else lineTo(x, y)
  }
  close()
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit = {}) {
  LoginScreen(onAuthSuccess = onAuthSuccess)
}
