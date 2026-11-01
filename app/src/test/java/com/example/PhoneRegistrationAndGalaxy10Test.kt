package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AccountInputValidator
import com.example.data.model.AppLanguage
import com.example.data.model.IdentityVerificationData
import com.example.data.model.RoleSpecificProfile
import com.example.ui.auth.PhoneAuthGateway
import com.example.ui.auth.PhoneOtpSendOutcome
import com.example.ui.auth.PhoneOtpVerifyOutcome
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.LoginScreenContent
import com.example.ui.screens.RoleOnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LoginAuthStatus
import com.example.ui.viewmodel.LoginUiState
import com.example.ui.viewmodel.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhoneRegistrationAndGalaxy10Test {

  @get:Rule val composeTestRule = createComposeRule()

  private class FakePhoneAuthGateway(private val expectedCode: String = "123456") :
    PhoneAuthGateway {
    var lastRequestedPhone: String? = null

    override suspend fun sendVerificationCode(
      context: Context,
      e164PhoneNumber: String,
      resendToken: Any?,
    ): PhoneOtpSendOutcome {
      lastRequestedPhone = e164PhoneNumber
      return PhoneOtpSendOutcome.CodeSent(
        verificationId = "verif-algeria-001",
        normalizedPhone = e164PhoneNumber,
        resendToken = "token-001",
      )
    }

    override suspend fun verifyOtpCode(
      verificationId: String,
      otpCode: String,
      e164PhoneNumber: String,
    ): PhoneOtpVerifyOutcome {
      return if (verificationId == "verif-algeria-001" && otpCode == expectedCode) {
        PhoneOtpVerifyOutcome.Success(e164PhoneNumber)
      } else {
        PhoneOtpVerifyOutcome.Error(
          messageAr = "رمز الدخول غير صحيح",
          messageFr = "Code invalide",
          messageEn = "Invalid code",
        )
      }
    }
  }

  @Test
  fun `algerian phone normalization and email validator handle local and international formats`() {
    assertEquals("+213550123456", AccountInputValidator.normalizePhoneToE164("0550 12 34 56"))
    assertEquals("+213661987654", AccountInputValidator.normalizePhoneToE164("213661987654"))
    assertEquals("+213770112233", AccountInputValidator.normalizePhoneToE164("00213770112233"))
    assertEquals("+33612345678", AccountInputValidator.normalizePhoneToE164("+33 6 12 34 56 78"))
    assertNull(AccountInputValidator.normalizePhoneToE164("123"))

    assertTrue(AccountInputValidator.isValidEmail("mehdi.farmer@fellah.dz"))
    assertFalse(AccountInputValidator.isValidEmail("not-an-email"))
  }

  @Test
  fun `phone credential sign in maps FirebaseAuthInvalidCredentialsException and expired session accurately`() {
    val invalidCodeEx =
      com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(
        "ERROR_INVALID_VERIFICATION_CODE",
        "The sms verification code used to create the phone auth credential is invalid",
      )
    val invalidOutcome =
      com.example.ui.auth.FirebasePhoneAuthGateway.mapCredentialSignInExceptionToVerifyError(
        invalidCodeEx
      )
    assertFalse(invalidOutcome.isSessionExpired)
    assertTrue(invalidOutcome.messageAr.contains("رمز الدخول الذي تم إدخاله غير صحيح"))

    val expiredEx =
      com.google.firebase.auth.FirebaseAuthInvalidCredentialsException(
        "ERROR_SESSION_EXPIRED",
        "The sms code has expired",
      )
    val expiredOutcome =
      com.example.ui.auth.FirebasePhoneAuthGateway.mapCredentialSignInExceptionToVerifyError(
        expiredEx
      )
    assertTrue(expiredOutcome.isSessionExpired)
    assertTrue(expiredOutcome.messageAr.contains("إعادة إرسال الرمز"))
  }

  @Test
  fun `galaxy 10 silent credential check never disables interactive buttons on login screen`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        LoginScreenContent(
          uiState =
            LoginUiState(
              status = LoginAuthStatus.CheckingSavedSession,
              language = AppLanguage.AR,
            ),
          onLanguageSelected = {},
          onSignInClick = {},
          onDismissMessage = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("send_phone_otp_button").performScrollTo().assertIsEnabled()
    composeTestRule.onNodeWithTag("login_button").performScrollTo().assertIsEnabled()
    composeTestRule.onNodeWithTag("enter_visitor_mode_button").performScrollTo().assertIsEnabled()
    composeTestRule.onNodeWithTag("open_login_diagnostics_button").assertDoesNotExist()
  }

  @Test
  fun `operator settings screen renders cleanly without galaxy s10 diagnostics report card`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.OperatorSettingsScreen(
          userDisplayName = "أحمد بلقاسم",
          userEmail = "ahmed@fellah.dz",
          operatorName = "مستثمرة الواحة",
          defaultWilaya = "07 - بسكرة",
          currentLanguage = AppLanguage.AR,
          currentMapLayer = com.example.data.model.MapLayerMode.entries.first(),
          savedParcelsCount = 2,
          onSavePreferences = { _, _ -> },
          onLanguageSelected = {},
          onMapLayerSelected = {},
          onOpenMyParcels = {},
          onExportCurrentParcelPdf = {},
          onSignOut = {},
          onBackToDelineation = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("performance_diagnostics_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("settings_save_profile_button").assertExists()
  }

  @Test
  fun `phone registration sends sms code and verifies 6 digit otp before proceeding`() {
    val fakeGateway = FakePhoneAuthGateway(expectedCode = "654321")
    val viewModel = LoginViewModel(phoneAuthGateway = fakeGateway)
    var verifiedPhoneResult = ""
    var authSuccessCount = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        LoginScreen(
          initialLanguage = AppLanguage.AR,
          loginViewModel = viewModel,
          onPhoneVerified = { verifiedPhoneResult = it },
          onAuthSuccess = { authSuccessCount++ },
        )
      }
    }

    composeTestRule.waitForIdle()

    // Enter Algerian phone number and request SMS login code
    composeTestRule.onNodeWithTag("phone_number_input").performScrollTo().performTextInput("0550123456")
    composeTestRule.onNodeWithTag("send_phone_otp_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals("+213550123456", fakeGateway.lastRequestedPhone)
    composeTestRule.onNodeWithTag("otp_sent_target_banner").performScrollTo().assertIsDisplayed()

    // Short / invalid OTP shows error banner and does not authenticate
    composeTestRule.onNodeWithTag("phone_otp_code_input").performScrollTo().performTextInput("111111")
    composeTestRule.onNodeWithTag("verify_phone_otp_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("login_error_banner").performScrollTo().assertIsDisplayed()
    assertEquals(0, authSuccessCount)

    // Enter correct 6-digit OTP code and confirm
    composeTestRule.onNodeWithTag("phone_otp_code_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("phone_otp_code_input").performScrollTo().performTextInput("654321")
    composeTestRule.onNodeWithTag("verify_phone_otp_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals("+213550123456", verifiedPhoneResult)
    assertEquals(1, authSuccessCount)
  }

  @Test
  fun `post otp onboarding requires email and full name then saves complete profile`() {
    var completedName = ""
    var completedEmail = ""
    var completedPhone = ""
    var completedPhoneVerified = false
    var completedRoles = emptyList<String>()
    var completedProfiles = emptyList<RoleSpecificProfile>()

    composeTestRule.setContent {
      MyApplicationTheme {
        RoleOnboardingScreen(
          existingAccount = null,
          existingRoleProfiles = emptyMap(),
          language = AppLanguage.AR,
          userDisplayName = "Opérateur Fellah Numérique",
          userEmail = "",
          userPhoneNumber = "+213550123456",
          isPhoneVerifiedSession = true,
          isSaving = false,
          errorMessage = null,
          onSaveProgressDraft = { _, _, _ -> },
          onCompleteOnboarding = { _, _, _ -> },
          onCompleteAccountOnboarding = {
            displayName,
            email,
            phoneNumber,
            phoneVerified,
            roles,
            _,
            profiles,
            _: IdentityVerificationData ->
            completedName = displayName
            completedEmail = email
            completedPhone = phoneNumber
            completedPhoneVerified = phoneVerified
            completedRoles = roles
            completedProfiles = profiles
          },
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("verified_phone_banner").performScrollTo().assertIsDisplayed()

    // Attempting to advance without email triggers validation error on Step 1
    composeTestRule
      .onNodeWithTag("onboarding_start_selection_button")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_error_banner").performScrollTo().assertIsDisplayed()

    // Fill post-OTP email, full name, and wilaya
    composeTestRule
      .onNodeWithTag("onboarding_email_input")
      .performScrollTo()
      .performTextInput("ahmed.biskra@fellah.dz")
    composeTestRule
      .onNodeWithTag("onboarding_full_name_input")
      .performScrollTo()
      .performTextInput("أحمد بلقاسم")
    composeTestRule
      .onNodeWithTag("onboarding_primary_wilaya_input")
      .performScrollTo()
      .performTextInput("07 - بسكرة")

    composeTestRule
      .onNodeWithTag("onboarding_start_selection_button")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    // Step 2: Select Farmer role and continue to Step 4
    composeTestRule.onNodeWithTag("onboarding_role_card_farmer").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("onboarding_continue_step2").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Step 4: Finish and verify email, name, verified phone, and wilaya are persisted
    composeTestRule.onNodeWithTag("onboarding_finish_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals("أحمد بلقاسم", completedName)
    assertEquals("ahmed.biskra@fellah.dz", completedEmail)
    assertEquals("+213550123456", completedPhone)
    assertTrue(completedPhoneVerified)
    assertEquals(listOf("farmer"), completedRoles)
    assertEquals("07 - بسكرة", completedProfiles.firstOrNull()?.wilaya)
  }
}
