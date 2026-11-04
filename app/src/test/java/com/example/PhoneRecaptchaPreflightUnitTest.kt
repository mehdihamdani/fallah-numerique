package com.example

import android.content.Context
import com.example.ui.auth.FirebasePhoneAuthGateway
import com.example.ui.auth.FirebasePhoneAuthGateway.PhoneRecaptchaReadiness
import com.example.ui.auth.GoogleSignInOutcome
import com.example.ui.auth.PhoneAuthGateway
import com.example.ui.auth.PhoneOtpSendOutcome
import com.example.ui.auth.PhoneOtpVerifyOutcome
import com.example.ui.viewmodel.LoginAuthStatus
import com.example.ui.viewmodel.LoginViewModel
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneRecaptchaPreflightUnitTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `missing or empty recaptchaKey returns NOT_CONFIGURED preventing E zza logcat error`() {
    assertEquals(
      PhoneRecaptchaReadiness.NOT_CONFIGURED,
      FirebasePhoneAuthGateway.parseRecaptchaConfigReadiness(
        200,
        """{"recaptchaEnforcementState":[]}""",
      ),
    )
    assertEquals(
      PhoneRecaptchaReadiness.NOT_CONFIGURED,
      FirebasePhoneAuthGateway.parseRecaptchaConfigReadiness(
        200,
        """{"recaptchaKey": "   "}""",
      ),
    )
    assertEquals(
      PhoneRecaptchaReadiness.NOT_CONFIGURED,
      FirebasePhoneAuthGateway.parseRecaptchaConfigReadiness(
        400,
        """{"error":{"message":"CONFIGURATION_NOT_FOUND"}}""",
      ),
    )
  }

  @Test
  fun `configured recaptchaKey returns READY unless PHONE_PROVIDER is blocked`() {
    assertEquals(
      PhoneRecaptchaReadiness.READY,
      FirebasePhoneAuthGateway.parseRecaptchaConfigReadiness(
        200,
        """{"recaptchaKey":"projects/gen-lang-client-0408878957/keys/6Lf9vg","recaptchaEnforcementState":[{"provider":"PHONE_PROVIDER","enforcementState":"ENFORCE"}]}""",
      ),
    )
    assertEquals(
      PhoneRecaptchaReadiness.NOT_CONFIGURED,
      FirebasePhoneAuthGateway.parseRecaptchaConfigReadiness(
        200,
        """{"recaptchaKey":"projects/gen-lang-client-0408878957/keys/6Lf9vg","recaptchaEnforcementState":[{"provider":"PHONE_PROVIDER","enforcementState":"BLOCK"}]}""",
      ),
    )
    assertEquals(
      PhoneRecaptchaReadiness.NETWORK_ERROR,
      FirebasePhoneAuthGateway.parseRecaptchaConfigReadiness(503, "Service Unavailable"),
    )
  }

  @Test
  fun `disabled phone provider and missing recaptcha both resolve to RequireGoogleIdentityHandoff without Firebase Console error`() {
    assertTrue(
      FirebasePhoneAuthGateway.isBackendSmsUnconfigured(
        code = "ERROR_OPERATION_NOT_ALLOWED",
        rawMsg = "",
      )
    )

    val operationNotAllowedOutcome =
      FirebasePhoneAuthGateway.resolveUnconfiguredOrPhoneSendOutcome(
        RuntimeException(
          "This operation is not allowed. This may be due to the given sign-in provider being disabled for this Firebase project."
        ),
        "+213550123456",
      )
    assertEquals(
      PhoneOtpSendOutcome.RequireGoogleIdentityHandoff("+213550123456"),
      operationNotAllowedOutcome,
    )

    val recaptchaOutcome =
      FirebasePhoneAuthGateway.resolveUnconfiguredOrPhoneSendOutcome(
        RuntimeException(
          "Failed to initialize reCAPTCHA config: No Recaptcha Enterprise siteKey configured for tenant/project *"
        ),
        "+213661987654",
      )
    assertEquals(
      PhoneOtpSendOutcome.RequireGoogleIdentityHandoff("+213661987654"),
      recaptchaOutcome,
    )

    val fallbackError =
      FirebasePhoneAuthGateway.mapPhoneExceptionToSendError(
        RuntimeException("OPERATION_NOT_ALLOWED")
      )
    assertFalse(fallbackError.messageAr.contains("Firebase Console"))
    assertFalse(fallbackError.messageFr.contains("Firebase Console"))
    assertFalse(fallbackError.messageEn.contains("Firebase Console"))
  }

  @Test
  fun `three-step Android Phone Auth + reCAPTCHA + 6-digit OTP confirmation signs in user with E164 phone`() =
    runTest {
      val realFlowGateway =
        object : PhoneAuthGateway {
          override suspend fun sendVerificationCode(
            context: Context?,
            e164PhoneNumber: String,
            resendToken: Any?,
          ): PhoneOtpSendOutcome =
            PhoneOtpSendOutcome.CodeSent(
              verificationId = "verif_dz_213",
              normalizedPhone = e164PhoneNumber,
              resendToken = "token_1",
            )

          override suspend fun verifyOtpCode(
            verificationId: String,
            otpCode: String,
            e164PhoneNumber: String,
          ): PhoneOtpVerifyOutcome {
            assertEquals("verif_dz_213", verificationId)
            assertEquals("482915", otpCode)
            return PhoneOtpVerifyOutcome.Success(
              normalizedPhone = e164PhoneNumber,
              userUid = "uid_fellah_dz_01",
            )
          }
        }

      var verifiedPhoneResult = ""
      val vm = LoginViewModel(phoneAuthGateway = realFlowGateway)

      // Step 1 & 2: Enter Algerian number -> normalized to +213XXXXXXXXX -> send SMS after reCAPTCHA
      vm.onPhoneInputChanged("0550 12 34 56")
      vm.requestPhoneOtpCode(context = null)

      assertEquals("+213550123456", vm.uiState.value.normalizedPhone)
      assertEquals(
        com.example.ui.viewmodel.PhoneOtpStep.VERIFY_CODE,
        vm.uiState.value.phoneStep,
      )
      assertEquals("verif_dz_213", vm.uiState.value.verificationId)

      // Step 3: Enter 6-digit SMS code and confirm sign-in
      vm.onOtpCodeInputChanged("482915")
      vm.confirmPhoneOtpCode { confirmedPhone -> verifiedPhoneResult = confirmedPhone }

      assertEquals("+213550123456", verifiedPhoneResult)
      assertEquals("uid_fellah_dz_01", vm.uiState.value.signedInUserUid)
      assertEquals(LoginAuthStatus.Idle, vm.uiState.value.status)
    }

  @Test
  fun `LoginViewModel preserves phone draft and shows explicit Gmail guidance without surprise Google popup when SMS provider is disabled`() =
    runTest {
      val handoffGateway =
        object : PhoneAuthGateway {
          override suspend fun sendVerificationCode(
            context: Context?,
            e164PhoneNumber: String,
            resendToken: Any?,
          ): PhoneOtpSendOutcome =
            PhoneOtpSendOutcome.RequireGoogleIdentityHandoff(normalizedPhone = e164PhoneNumber)

          override suspend fun verifyOtpCode(
            verificationId: String,
            otpCode: String,
            e164PhoneNumber: String,
          ): PhoneOtpVerifyOutcome = PhoneOtpVerifyOutcome.Success(e164PhoneNumber)
        }

      var googleLaunchCalls = 0
      var capturedPhoneDraft = ""

      val vm =
        LoginViewModel(
          phoneAuthGateway = handoffGateway,
          googleSignInRunner = { _, _ ->
            googleLaunchCalls++
            GoogleSignInOutcome.Success
          },
        )

      vm.onPhoneInputChanged("0550 12 34 56")
      vm.requestPhoneOtpCode(
        context = null,
        onPhoneDraftCaptured = { capturedPhoneDraft = it },
      )

      assertEquals("+213550123456", capturedPhoneDraft)
      assertEquals("+213550123456", vm.uiState.value.normalizedPhone)
      // Must NOT surprise-open Google's "Email or phone" window unannounced!
      assertEquals(0, googleLaunchCalls)
      assertEquals(
        com.example.ui.viewmodel.AuthEntryMethod.GOOGLE,
        vm.uiState.value.selectedAuthMethod,
      )
      val status = vm.uiState.value.status
      assertTrue(status is LoginAuthStatus.PhoneSavedReadyForGoogle)
      val guidanceStatus = status as LoginAuthStatus.PhoneSavedReadyForGoogle
      assertEquals("+213550123456", guidanceStatus.savedPhone)
      assertTrue(guidanceStatus.message.contains("gmail.com", ignoreCase = true))
    }

  @Test
  fun `Google phone lookup error is recognized and mapped to explicit Gmail guidance`() {
    val googlePhoneLookupRaw =
      "Google: Something went wrong. We weren't able to check for accounts connected to your phone number."
    assertTrue(FirebasePhoneAuthGateway.isGooglePhoneAccountLookupError(googlePhoneLookupRaw))

    val mapped =
      FirebasePhoneAuthGateway.mapPhoneExceptionToSendError(
        RuntimeException(googlePhoneLookupRaw)
      )
    assertTrue(mapped.suggestGoogleFallback)
    assertTrue(mapped.messageAr.contains("Gmail"))
    assertTrue(mapped.messageFr.contains("Gmail"))
    assertTrue(mapped.messageEn.contains("Gmail"))
  }
}

