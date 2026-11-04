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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

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

  data class RequireGoogleIdentityHandoff(val normalizedPhone: String) : PhoneOtpSendOutcome

  data class Error(
    val messageAr: String,
    val messageFr: String,
    val messageEn: String,
    val suggestGoogleFallback: Boolean = false,
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
    context: Context?,
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

  internal enum class PhoneRecaptchaReadiness {
    READY,
    NOT_CONFIGURED,
    NETWORK_ERROR,
  }

  @Volatile private var cachedReadiness: PhoneRecaptchaReadiness? = null
  @Volatile private var cachedReadinessExpiresAtMs: Long = 0L

  override suspend fun sendVerificationCode(
    context: Context?,
    e164PhoneNumber: String,
    resendToken: Any?,
  ): PhoneOtpSendOutcome {
    val activity =
      context?.findActivity()
        ?: return PhoneOtpSendOutcome.Error(
          messageAr = "يتطلب إرسال رمز SMS نافذة تطبيق نشطة.",
          messageFr = "Une activité active est requise pour envoyer le code SMS.",
          messageEn = "An active Activity context is required to send the SMS code.",
        )

    // Native Android equivalent of Web `RecaptchaVerifier` + `signInWithPhoneNumber`:
    // `PhoneAuthOptions` with `.setActivity(activity)` automatically performs silent Play Integrity
    // attestation ('invisible' mode without bothering the user) and falls back to Firebase Auth's
    // built-in `RecaptchaActivity` Custom Tab ('normal' mode) when needed (e.g. on emulators).
    return try {
      val auth = Firebase.auth
      runCatching { auth.useAppLanguage() }
      withTimeoutOrNull<PhoneOtpSendOutcome>(90_000L) {
        suspendCancellableCoroutine<PhoneOtpSendOutcome> { cont ->
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
                        cont.resume(
                          PhoneOtpSendOutcome.InstantVerified(verifyOutcome.normalizedPhone)
                        )
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
                Log.w(AUTH_TAG, "onVerificationFailed: ${e.message}")
                if (cont.isActive) {
                  cont.resume(resolveUnconfiguredOrPhoneSendOutcome(e, e164PhoneNumber))
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
      }
        ?: PhoneOtpSendOutcome.Error(
          messageAr =
            "انتهت مهلة إرسال رمز SMS على الشبكة الحالية. يرجى المحاولة مجدداً أو المتابعة فوراً بحساب Google.",
          messageFr =
            "Délai d'envoi du SMS dépassé. Réessayez ou continuez immédiatement avec Google.",
          messageEn =
            "SMS verification timed out on the current network. Please retry or continue with Google.",
          suggestGoogleFallback = true,
        )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "Phone OTP request failed: ${e.message}")
      resolveUnconfiguredOrPhoneSendOutcome(e, e164PhoneNumber)
    }
  }

  private suspend fun verifyBackendPhoneRecaptchaReady(
    appContext: Context,
  ): PhoneRecaptchaReadiness {
    val now = System.currentTimeMillis()
    val cached = cachedReadiness
    if (cached != null && now < cachedReadinessExpiresAtMs) {
      return cached
    }

    val apiKey =
      runCatching {
          if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
          }
          FirebaseApp.getInstance().options.apiKey
        }
        .getOrNull()
        .orEmpty()
        .trim()

    if (apiKey.isEmpty()) {
      return PhoneRecaptchaReadiness.NOT_CONFIGURED
    }

    val readiness =
      withContext(Dispatchers.IO) {
        withTimeoutOrNull(4_000L) {
          var connection: HttpURLConnection? = null
          try {
            val endpoint =
              "https://identitytoolkit.googleapis.com/v2/projects/recaptchaConfig" +
                "?key=${Uri.encode(apiKey)}&clientType=CLIENT_TYPE_ANDROID&version=RECAPTCHA_ENTERPRISE"
            connection =
              (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_500
                readTimeout = 3_500
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Android-Package", appContext.packageName)
                getSigningCertSha1Hex(appContext)?.let { certHex ->
                  setRequestProperty("X-Android-Cert", certHex)
                }
              }
            val code = connection.responseCode
            val stream =
              if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            parseRecaptchaConfigReadiness(code, body)
          } catch (_: Exception) {
            PhoneRecaptchaReadiness.NETWORK_ERROR
          } finally {
            connection?.disconnect()
          }
        } ?: PhoneRecaptchaReadiness.NETWORK_ERROR
      }

    if (readiness != PhoneRecaptchaReadiness.NETWORK_ERROR) {
      cachedReadiness = readiness
      val ttlMs =
        if (readiness == PhoneRecaptchaReadiness.READY) 10 * 60_000L else 90_000L
      cachedReadinessExpiresAtMs = now + ttlMs
    }
    return readiness
  }

  internal fun parseRecaptchaConfigReadiness(
    httpCode: Int,
    responseBody: String,
  ): PhoneRecaptchaReadiness {
    if (httpCode in 400..499) {
      return PhoneRecaptchaReadiness.NOT_CONFIGURED
    }
    if (httpCode !in 200..299) {
      return PhoneRecaptchaReadiness.NETWORK_ERROR
    }
    val trimmed = responseBody.trim()
    if (trimmed.isEmpty()) {
      return PhoneRecaptchaReadiness.NOT_CONFIGURED
    }
    val siteKeyMatch = Regex(""""recaptchaKey"\s*:\s*"([^"]*)"""").find(trimmed)
    val siteKey = siteKeyMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
    if (siteKey.isEmpty()) {
      return PhoneRecaptchaReadiness.NOT_CONFIGURED
    }
    val phoneBlockedRegex =
      Regex(
        """\{\s*[^}]*"provider"\s*:\s*"PHONE_PROVIDER"[^}]*"enforcementState"\s*:\s*"BLOCK"[^}]*\}|\{\s*[^}]*"enforcementState"\s*:\s*"BLOCK"[^}]*"provider"\s*:\s*"PHONE_PROVIDER"[^}]*\}""",
        RegexOption.IGNORE_CASE,
      )
    if (phoneBlockedRegex.containsMatchIn(trimmed)) {
      return PhoneRecaptchaReadiness.NOT_CONFIGURED
    }
    return PhoneRecaptchaReadiness.READY
  }

  @Suppress("DEPRECATION")
  private fun getSigningCertSha1Hex(context: Context): String? =
    runCatching {
        val signatureBytes =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info =
              context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
              )
            val signingInfo = info.signingInfo
            val sigs =
              if (signingInfo?.hasMultipleSigners() == true) {
                signingInfo.apkContentsSigners
              } else {
                signingInfo?.signingCertificateHistory
              }
            sigs?.firstOrNull()?.toByteArray()
          } else {
            val info =
              context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
              )
            info.signatures?.firstOrNull()?.toByteArray()
          } ?: return@runCatching null

        MessageDigest.getInstance("SHA-1")
          .digest(signatureBytes)
          .joinToString("") { "%02X".format(Locale.US, it) }
      }
      .getOrNull()

  internal fun isBackendSmsUnconfigured(
    code: String,
    rawMsg: String,
    isMissingRecaptchaActivity: Boolean = false,
  ): Boolean =
    isMissingRecaptchaActivity ||
      rawMsg.contains("recaptcha", ignoreCase = true) ||
      rawMsg.contains("No Recaptcha Enterprise siteKey", ignoreCase = true) ||
      rawMsg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
      rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
      rawMsg.contains("sign-in provider is disabled", ignoreCase = true) ||
      rawMsg.contains("given sign-in provider being disabled", ignoreCase = true) ||
      rawMsg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
      rawMsg.contains("missing a valid app identifier", ignoreCase = true) ||
      rawMsg.contains("Play Integrity", ignoreCase = true) ||
      rawMsg.contains("SafetyNet", ignoreCase = true) ||
      code == "ERROR_OPERATION_NOT_ALLOWED" ||
      code == "ERROR_MISSING_CLIENT_IDENTIFIER" ||
      code == "ERROR_APP_NOT_AUTHORIZED" ||
      code == "ERROR_INTERNAL_ERROR"

  internal fun resolveUnconfiguredOrPhoneSendOutcome(
    e: Exception,
    e164PhoneNumber: String,
  ): PhoneOtpSendOutcome {
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    val rawMsg = e.message.orEmpty()
    if (
      isBackendSmsUnconfigured(
        code = code,
        rawMsg = rawMsg,
        isMissingRecaptchaActivity = e is FirebaseAuthMissingActivityForRecaptchaException,
      )
    ) {
      cachedReadiness = PhoneRecaptchaReadiness.NOT_CONFIGURED
      cachedReadinessExpiresAtMs = System.currentTimeMillis() + 10 * 60_000L
      return PhoneOtpSendOutcome.RequireGoogleIdentityHandoff(normalizedPhone = e164PhoneNumber)
    }
    return mapPhoneExceptionToSendError(e)
  }

  internal fun buildPhoneRecaptchaNotConfiguredError(): PhoneOtpSendOutcome.Error =
    PhoneOtpSendOutcome.Error(
      messageAr =
        "خدمة الرسائل القصيرة (SMS) غير متاحة حالياً على هذه الشبكة. يمكنك المتابعة فوراً عبر حساب Google وسيُحفظ رقم هاتفك تلقائياً في ملفك.",
      messageFr =
        "Le service SMS est momentanément indisponible. Continuez immédiatement avec Google : votre numéro sera conservé.",
      messageEn =
        "SMS verification is currently unavailable. Continue with Google and your phone number will be saved to your profile.",
      suggestGoogleFallback = true,
    )

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
          val user = task.result?.user
          Log.d(AUTH_TAG, "مرحباً بك! تم تسجيل الدخول عبر الهاتف: ${user?.uid}")
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

  internal fun isGooglePhoneAccountLookupError(rawMessage: String): Boolean {
    if (rawMessage.isBlank()) return false
    return rawMessage.contains("accounts connected to your phone number", ignoreCase = true) ||
      rawMessage.contains("check for accounts", ignoreCase = true) ||
      rawMessage.contains("Unable to get sync account", ignoreCase = true)
  }

  internal fun mapPhoneExceptionToSendError(e: Exception): PhoneOtpSendOutcome.Error {
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    val rawMsg = e.message.orEmpty()
    return when {
      isGooglePhoneAccountLookupError(rawMsg) ->
        PhoneOtpSendOutcome.Error(
          messageAr =
            "تعذر على Google التحقق من الحسابات المرتبطة برقم الهاتف. يرجى اختيار حساب بريدك الإلكتروني (Gmail) المسجل على الهاتف بدلاً من إدخال الرقم داخل نافذة Google — رقم هاتفك محفوظ تلقائياً.",
          messageFr =
            "Google n'a pas pu vérifier les comptes liés à ce numéro. Sélectionnez votre adresse Gmail existante dans la fenêtre Google — votre numéro est déjà conservé.",
          messageEn =
            "Google couldn't check accounts linked to your phone number. Please choose your existing Gmail address in the Google prompt — your phone number is already saved.",
          suggestGoogleFallback = true,
        )
      e is FirebaseTooManyRequestsException ->
        PhoneOtpSendOutcome.Error(
          messageAr =
            "تم تجاوز الحد المسموح لطلبات SMS مؤقتًا. يرجى الانتظار قليلًا أو المتابعة عبر حساب Google.",
          messageFr =
            "Trop de demandes SMS. Patientez quelques instants ou continuez avec Google.",
          messageEn =
            "SMS quota temporarily exceeded. Please wait briefly or continue with Google.",
          suggestGoogleFallback = true,
        )
      e is FirebaseAuthMissingActivityForRecaptchaException ||
        rawMsg.contains("recaptcha", ignoreCase = true) ||
        rawMsg.contains("No Recaptcha Enterprise siteKey", ignoreCase = true) ||
        rawMsg.contains("CONFIGURATION_NOT_ALLOWED", ignoreCase = true) ||
        rawMsg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
        rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
        rawMsg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
        rawMsg.contains("missing a valid app identifier", ignoreCase = true) ||
        code == "ERROR_OPERATION_NOT_ALLOWED" ||
        code == "ERROR_MISSING_CLIENT_IDENTIFIER" ||
        code == "ERROR_INTERNAL_ERROR" -> buildPhoneRecaptchaNotConfiguredError()
      e is FirebaseAuthInvalidCredentialsException ->
        PhoneOtpSendOutcome.Error(
          messageAr =
            "صيغة رقم الهاتف غير صحيحة. أدخل رقمًا صحيحًا مثل 0550123456 أو +213550123456.",
          messageFr = "Format de numéro invalide. Exemple : 0550123456 ou +213550123456.",
          messageEn = "Invalid phone format. Example: 0550123456 or +213550123456.",
        )
      else ->
        PhoneOtpSendOutcome.Error(
          messageAr =
            "تعذر إرسال رمز SMS على هذا الجهاز. يرجى المتابعة عبر حساب Google أدناه.",
          messageFr =
            "Envoi du SMS indisponible sur cet appareil. Veuillez continuer avec Google ci-dessous.",
          messageEn =
            "SMS verification is unavailable on this device. Please continue with Google below.",
          suggestGoogleFallback = true,
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
 * 1. First queries existing on-device Google accounts (`GetGoogleIdOption` with
 *    `setFilterByAuthorizedAccounts(false)`) so if the user already has a Google account signed in
 *    on their phone, they tap it directly in 1 tap WITHOUT entering Google's webview "Add account"
 *    wizard (which triggers *"We weren't able to check for accounts connected to your phone number"*
 *    when the user re-types their phone number inside Google's prompt).
 * 2. Falls back automatically to `GetSignInWithGoogleOption` when no Google account is signed in on
 *    the device (`NoCredentialException`), so adding an account via Gmail address still works cleanly.
 */
suspend fun signInInteractivelyWithGoogle(
  context: Context?,
  credentialManager: CredentialManager?,
): GoogleSignInOutcome {
  if (credentialManager == null) {
    return GoogleSignInOutcome.Error(
      "خدمات Google Play لتسجيل الدخول غير متوفرة على هذا الهاتف — يرجى تحديث Google Play Services أو إضافة حساب Google في إعدادات الجهاز."
    )
  }
  val activity =
    context?.findActivity()
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

  // Interactive Sign in with Google button flow (`GetSignInWithGoogleOption` — single option per request)
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
      "يرجى إضافة حساب Google باستخدام عنوان البريد الإلكتروني (example@gmail.com) في إعدادات الهاتف لإتمام تسجيل الدخول."
    )
  } catch (e: GetCredentialUnsupportedException) {
    Log.e(AUTH_TAG, "CredentialManager unsupported on device", e)
    GoogleSignInOutcome.Error(
      "خدمة إدارة بيانات الاعتماد غير مدعومة في إصدار خدمات Google الحالي — يرجى تحديث Google Play Services على جهازك."
    )
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.e(AUTH_TAG, "Google Sign-In failed", e)
    val raw = e.message.orEmpty()
    if (FirebasePhoneAuthGateway.isGooglePhoneAccountLookupError(raw)) {
      GoogleSignInOutcome.Error(
        "تعذر على Google البحث عن حساب عبر رقم الهاتف. عند فتح نافذة Google، يرجى كتابة عنوان بريدك الإلكتروني (example@gmail.com) بدلاً من رقم الهاتف — رقم هاتفك محفوظ تلقائياً في ملفك."
      )
    } else {
      GoogleSignInOutcome.Error(e.localizedMessage ?: "تعذر تسجيل الدخول عبر Google")
    }
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
