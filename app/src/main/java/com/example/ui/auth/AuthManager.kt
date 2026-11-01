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
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.screens.LoginScreen
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val AUTH_TAG = "Auth"

sealed interface GoogleSignInOutcome {
  data object Success : GoogleSignInOutcome

  data object Cancelled : GoogleSignInOutcome

  data object NoSavedCredential : GoogleSignInOutcome

  data class Error(val message: String) : GoogleSignInOutcome
}

sealed interface PhoneOtpSendOutcome {
  data class CodeSent(
    val verificationId: String,
    val normalizedPhone: String,
    val resendToken: Any? = null,
  ) : PhoneOtpSendOutcome

  data class InstantVerified(val normalizedPhone: String) : PhoneOtpSendOutcome

  data class Error(
    val messageAr: String,
    val messageFr: String,
    val messageEn: String,
  ) : PhoneOtpSendOutcome
}

sealed interface PhoneOtpVerifyOutcome {
  data class Success(
    val normalizedPhone: String,
    val userUid: String? = null,
  ) : PhoneOtpVerifyOutcome

  data class Error(
    val messageAr: String,
    val messageFr: String,
    val messageEn: String,
    val isSessionExpired: Boolean = false,
  ) : PhoneOtpVerifyOutcome
}

/**
 * Abstraction over Firebase Phone Authentication (`PhoneAuthProvider`) so UI & ViewModel flows
 * work identically on physical Android devices and deterministic Robolectric JVM tests.
 */
interface PhoneAuthGateway {
  suspend fun sendVerificationCode(
    context: Context,
    e164PhoneNumber: String,
    resendToken: Any? = null,
  ): PhoneOtpSendOutcome

  suspend fun verifyOtpCode(
    verificationId: String,
    otpCode: String,
    e164PhoneNumber: String,
  ): PhoneOtpVerifyOutcome
}

object FirebasePhoneAuthGateway : PhoneAuthGateway {

  override suspend fun sendVerificationCode(
    context: Context,
    e164PhoneNumber: String,
    resendToken: Any?,
  ): PhoneOtpSendOutcome {
    val activity =
      context.findActivity()
        ?: return PhoneOtpSendOutcome.Error(
          messageAr = "يتطلب إرسال رمز SMS نافذة تطبيق نشطة.",
          messageFr = "Une activité active est requise pour envoyer le code SMS.",
          messageEn = "An active Activity context is required to send the SMS code.",
        )

    return try {
      val auth = Firebase.auth
      suspendCancellableCoroutine { cont ->
        val callbacks =
          object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
              Log.d(AUTH_TAG, "onVerificationCompleted:$credential")
              signInWithPhoneAuthCredential(
                auth = auth,
                credential = credential,
                e164PhoneNumber = e164PhoneNumber,
              ) { verifyOutcome ->
                if (cont.isActive) {
                  when (verifyOutcome) {
                    is PhoneOtpVerifyOutcome.Success ->
                      cont.resume(PhoneOtpSendOutcome.InstantVerified(verifyOutcome.normalizedPhone))
                    is PhoneOtpVerifyOutcome.Error ->
                      cont.resume(
                        PhoneOtpSendOutcome.Error(
                          messageAr = verifyOutcome.messageAr,
                          messageFr = verifyOutcome.messageFr,
                          messageEn = verifyOutcome.messageEn,
                        )
                      )
                  }
                }
              }
            }

            override fun onVerificationFailed(e: FirebaseException) {
              Log.w(AUTH_TAG, "onVerificationFailed", e)
              if (cont.isActive) {
                cont.resume(mapPhoneExceptionToSendError(e))
              }
            }

            override fun onCodeSent(
              verificationId: String,
              token: PhoneAuthProvider.ForceResendingToken,
            ) {
              Log.d(AUTH_TAG, "onCodeSent:$verificationId")
              if (cont.isActive) {
                cont.resume(
                  PhoneOtpSendOutcome.CodeSent(
                    verificationId = verificationId,
                    normalizedPhone = e164PhoneNumber,
                    resendToken = token,
                  )
                )
              }
            }
          }

        val builder =
          PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164PhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken is PhoneAuthProvider.ForceResendingToken) {
          builder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Unexpected error requesting Phone OTP", e)
      mapPhoneExceptionToSendError(e)
    }
  }

  override suspend fun verifyOtpCode(
    verificationId: String,
    otpCode: String,
    e164PhoneNumber: String,
  ): PhoneOtpVerifyOutcome {
    return try {
      val credential = PhoneAuthProvider.getCredential(verificationId, otpCode.trim())
      val auth = Firebase.auth
      suspendCancellableCoroutine { cont ->
        signInWithPhoneAuthCredential(
          auth = auth,
          credential = credential,
          e164PhoneNumber = e164PhoneNumber,
        ) { outcome ->
          if (cont.isActive) {
            cont.resume(outcome)
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      mapCredentialSignInExceptionToVerifyError(e)
    }
  }

  /**
   * Signs in to Firebase Auth using a [PhoneAuthCredential] (either auto-retrieved via
   * `onVerificationCompleted` or constructed from `PhoneAuthProvider.getCredential`), handling
   * completion, signed-in `FirebaseUser` extraction, and `FirebaseAuthInvalidCredentialsException`.
   */
  private fun signInWithPhoneAuthCredential(
    auth: FirebaseAuth,
    credential: PhoneAuthCredential,
    e164PhoneNumber: String,
    onResult: (PhoneOtpVerifyOutcome) -> Unit,
  ) {
    auth
      .signInWithCredential(credential)
      .addOnCompleteListener { task ->
        if (task.isSuccessful) {
          // Sign in success, update UI with the signed-in user's information
          Log.d(AUTH_TAG, "signInWithCredential:success")
          val user = task.result?.user
          val resolvedPhone = user?.phoneNumber?.takeIf { it.isNotBlank() } ?: e164PhoneNumber
          if (user != null) {
            onResult(
              PhoneOtpVerifyOutcome.Success(
                normalizedPhone = resolvedPhone,
                userUid = user.uid,
              )
            )
          } else {
            onResult(
              PhoneOtpVerifyOutcome.Error(
                messageAr = "تم تأكيد الرمز لكن تعذر استرداد بيانات المستخدم. يرجى المحاولة مجدداً.",
                messageFr = "Code vérifié mais impossible de récupérer l'utilisateur. Veuillez réessayer.",
                messageEn = "Code verified, but signed-in user info was unavailable. Please try again.",
              )
            )
          }
        } else {
          // Sign in failed, display a message and update the UI
          val exception = task.exception
          Log.w(AUTH_TAG, "signInWithCredential:failure", exception)
          onResult(mapCredentialSignInExceptionToVerifyError(exception))
        }
      }
  }

  internal fun mapCredentialSignInExceptionToVerifyError(
    exception: Exception?,
  ): PhoneOtpVerifyOutcome.Error {
    if (exception is FirebaseAuthInvalidCredentialsException) {
      // The verification code entered was invalid or the SMS verification session expired
      val errorCode = exception.errorCode
      if (errorCode == "ERROR_SESSION_EXPIRED") {
        return PhoneOtpVerifyOutcome.Error(
          messageAr =
            "انتهت صلاحية رمز التحقق (SMS). يرجى الضغط على «إعادة إرسال الرمز» للحصول على رمز جديد.",
          messageFr =
            "Le code SMS a expiré. Appuyez sur « Renvoyer le code » pour recevoir un nouveau code.",
          messageEn =
            "The verification code has expired. Please tap 'Resend Code' to get a new SMS code.",
          isSessionExpired = true,
        )
      }
      return PhoneOtpVerifyOutcome.Error(
        messageAr =
          "رمز الدخول الذي تم إدخاله غير صحيح. يرجى التحقق من الأرقام الستة والمحاولة مجددًا.",
        messageFr =
          "Le code de vérification saisi est invalide. Vérifiez les 6 chiffres et réessayez.",
        messageEn =
          "The verification code entered was invalid. Please check the 6 digits and try again.",
      )
    }

    if (exception is FirebaseTooManyRequestsException) {
      return PhoneOtpVerifyOutcome.Error(
        messageAr =
          "تم تجاوز عدد المحاولات المسموح بها مؤقتاً. انتظر قليلاً ثم أعد المحاولة أو استخدم حساب Google.",
        messageFr =
          "Trop de tentatives. Patientez quelques instants ou continuez avec Google.",
        messageEn =
          "Too many verification attempts. Please wait briefly or continue with Google.",
      )
    }

    return PhoneOtpVerifyOutcome.Error(
      messageAr = exception?.localizedMessage ?: "تعذر تأكيد رمز الدخول حاليًا.",
      messageFr = exception?.localizedMessage ?: "Impossible de vérifier le code actuellement.",
      messageEn = exception?.localizedMessage ?: "Unable to verify login code right now.",
    )
  }

  private fun mapPhoneExceptionToSendError(e: Exception): PhoneOtpSendOutcome.Error {
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    val rawMsg = e.message.orEmpty()
    return when {
      e is FirebaseTooManyRequestsException ->
        PhoneOtpSendOutcome.Error(
          messageAr = "تم تجاوز الحد المسموح لطلبات SMS مؤقتًا. يرجى الانتظار قليلًا أو المتابعة عبر حساب Google.",
          messageFr = "Trop de demandes SMS. Patientez quelques instants ou continuez avec Google.",
          messageEn = "SMS quota temporarily exceeded. Please wait briefly or continue with Google.",
        )
      e is FirebaseAuthInvalidCredentialsException ->
        PhoneOtpSendOutcome.Error(
          messageAr = "صيغة رقم الهاتف غير صحيحة. أدخل رقمًا صحيحًا مثل 0550123456 أو +213550123456.",
          messageFr = "Format de numéro invalide. Exemple : 0550123456 ou +213550123456.",
          messageEn = "Invalid phone format. Example: 0550123456 or +213550123456.",
        )
      code == "ERROR_OPERATION_NOT_ALLOWED" ||
        rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
        rawMsg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ->
        PhoneOtpSendOutcome.Error(
          messageAr = "مزود التسجيل بالهاتف (Phone SMS) غير مفعّل بعد في لوحة Firebase Console (Authentication > Sign-in method > Phone). قم بتفعيله أو استخدم زر حساب Google بالأسفل.",
          messageFr = "L'authentification par téléphone (SMS) doit être activée dans Firebase Console (Authentication > Sign-in method > Phone), ou utilisez Google ci-dessous.",
          messageEn = "Phone SMS sign-in must be enabled in Firebase Console (Authentication > Sign-in method > Phone), or continue with Google below.",
        )
      else ->
        PhoneOtpSendOutcome.Error(
          messageAr =
            "تعذر إرسال رمز SMS على هذا الجهاز (${e.localizedMessage ?: "تحقق من خدمات Google Play أو استخدم حساب Google"}).",
          messageFr =
            "Envoi du SMS impossible sur cet appareil (${e.localizedMessage ?: "vérifiez Google Play Services ou utilisez Google"}).",
          messageEn =
            "Unable to send SMS on this device (${e.localizedMessage ?: "check Google Play Services or use Google Sign-In"}).",
        )
    }
  }
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

internal fun createCredentialManagerSafely(context: Context): CredentialManager? {
  return runCatching { CredentialManager.create(context) }
    .onFailure { Log.w(AUTH_TAG, "CredentialManager unavailable on this device: ${it.message}") }
    .getOrNull()
}

/**
 * Silent startup credential check using Jetpack CredentialManager.
 * Bounded by a strict 3.5s timeout so older Android 9–12 One UI devices (e.g. Samsung Galaxy S10)
 * never stall the Login screen if Play Services CredentialProvider IPC does not respond.
 */
suspend fun checkSilentGoogleCredential(
  context: Context,
  credentialManager: CredentialManager?,
): GoogleSignInOutcome {
  val existingUser = runCatching { Firebase.auth.currentUser }.getOrNull()
  if (existingUser != null) {
    return GoogleSignInOutcome.Success
  }
  if (credentialManager == null) {
    return GoogleSignInOutcome.NoSavedCredential
  }
  val activity = context.findActivity() ?: return GoogleSignInOutcome.NoSavedCredential
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
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
    val result =
      withTimeoutOrNull(3_500L) { credentialManager.getCredential(activity, request) }
        ?: return GoogleSignInOutcome.NoSavedCredential
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
 * to keep the Android bottom sheet responsive across One UI (Galaxy S10) through API 36.
 */
suspend fun signInInteractivelyWithGoogle(
  context: Context,
  credentialManager: CredentialManager?,
): GoogleSignInOutcome {
  if (credentialManager == null) {
    return GoogleSignInOutcome.Error(
      "خدمات Google Play لتسجيل الدخول غير متوفرة على هذا الهاتف — يرجى تحديث Google Play Services أو التسجيل برقم الهاتف."
    )
  }
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
  } catch (e: GetCredentialProviderConfigurationException) {
    Log.e(AUTH_TAG, "Google Play Services CredentialProvider missing on device", e)
    GoogleSignInOutcome.Error(
      "يرجى تحديث خدمات Google Play على هاتفك أو إضافة حساب Google في إعدادات الهاتف، أو استخدم خيار التسجيل بالهاتف."
    )
  } catch (e: GetCredentialUnsupportedException) {
    Log.e(AUTH_TAG, "CredentialManager unsupported on device", e)
    GoogleSignInOutcome.Error(
      "خدمة إدارة بيانات الاعتماد غير مدعومة في إصدار خدمات Google الحالي — حدّث Google Play Services أو سجّل برقم الهاتف."
    )
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.e(AUTH_TAG, "Google Sign-In failed", e)
    GoogleSignInOutcome.Error(e.localizedMessage ?: "تعذر تسجيل الدخول عبر Google")
  }
}

fun signOutUser(
  credentialManager: CredentialManager?,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope,
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager?.clearCredentialState(ClearCredentialStateRequest())
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
