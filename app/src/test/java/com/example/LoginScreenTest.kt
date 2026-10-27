package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.AppLanguage
import com.example.ui.screens.LoginScreenContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LoginAuthStatus
import com.example.ui.viewmodel.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LoginScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `login screen displays Google Sign-In button, switches languages, and triggers sign in`() {
    var uiState by mutableStateOf(LoginUiState(language = AppLanguage.AR))
    var signInClicks = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        LoginScreenContent(
          uiState = uiState,
          onLanguageSelected = { selected -> uiState = uiState.copy(language = selected) },
          onSignInClick = { signInClicks++ },
          onDismissMessage = { uiState = uiState.copy(status = LoginAuthStatus.Idle) },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_logo_login").assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("المتابعة باستخدام حساب Google").assertIsDisplayed()

    // Switch to French
    composeTestRule.onNodeWithTag("login_lang_FR").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppLanguage.FR, uiState.language)
    composeTestRule.onNodeWithTag("login_button").performScrollTo()
    composeTestRule.onNodeWithText("Continuer avec Google").assertIsDisplayed()

    // Switch to English
    composeTestRule.onNodeWithTag("login_lang_EN").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppLanguage.EN, uiState.language)
    composeTestRule.onNodeWithTag("login_button").performScrollTo()
    composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()

    // Click primary Google Sign-In CTA
    composeTestRule.onNodeWithTag("login_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, signInClicks)
  }

  @Test
  fun `login screen renders cancelled retry banner and signing-in busy state`() {
    var uiState by
      mutableStateOf(
        LoginUiState(
          status = LoginAuthStatus.Cancelled,
          language = AppLanguage.EN,
        )
      )
    var retrySignInCount = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        LoginScreenContent(
          uiState = uiState,
          onLanguageSelected = {},
          onSignInClick = {
            retrySignInCount++
            uiState = uiState.copy(status = LoginAuthStatus.SigningIn)
          },
          onDismissMessage = { uiState = uiState.copy(status = LoginAuthStatus.Idle) },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("login_cancelled_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_retry_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, retrySignInCount)
    composeTestRule.onNodeWithTag("login_button").performScrollTo().assertIsNotEnabled()
  }
}
