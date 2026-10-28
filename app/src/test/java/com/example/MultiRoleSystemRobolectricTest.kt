package com.example

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.AppLanguage
import com.example.data.model.MapLayerMode
import com.example.data.model.RoleNavigationConfig
import com.example.data.model.UserAccountProfile
import com.example.data.model.UserRole
import com.example.data.model.WorkspaceSection
import com.example.ui.screens.FellahBottomNavigationBar
import com.example.ui.screens.OperatorSettingsScreen
import com.example.ui.screens.RoleOnboardingScreen
import com.example.ui.screens.VisitorExploreScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiRoleSystemRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `normalizeRoleCodes deduplicates preserves order and filters invalid codes`() {
    val raw =
      listOf("trader", "FARMER", "trader", "invalid_admin", "storage_provider", "  farmer  ")
    val normalized = UserRole.normalizeRoleCodes(raw)
    assertEquals(listOf("trader", "farmer", "storage_provider"), normalized)

    val account =
      UserAccountProfile(
        userId = "u1",
        assignedRoles = normalized,
        defaultRole = "farmer",
        onboardingCompleted = true,
      )
    assertEquals(UserRole.FARMER, account.resolvedActiveRole())

    val fallbackAccount =
      account.copy(defaultRole = "transporter") // not in assignedRoles -> falls back to first
    assertEquals(UserRole.TRADER, fallbackAccount.resolvedActiveRole())
  }

  @Test
  fun `onboarding welcome renders trilingual prompt and single role auto advances to step 4`() {
    var completedRoles: List<String> = emptyList()
    var completedDefaultRole = ""

    composeTestRule.setContent {
      var lang by remember { mutableStateOf(AppLanguage.AR) }
      MyApplicationTheme {
        RoleOnboardingScreen(
          existingAccount = null,
          existingRoleProfiles = emptyMap(),
          language = lang,
          userDisplayName = "أحمد الفلاح",
          userEmail = "ahmed@fellah.dz",
          isSaving = false,
          errorMessage = null,
          onLanguageSelected = { lang = it },
          onSaveProgressDraft = { _, _, _ -> },
          onCompleteOnboarding = { roles, defaultRole, _ ->
            completedRoles = roles
            completedDefaultRole = defaultRole
          },
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // Verify Arabic welcome message
    composeTestRule
      .onNodeWithText("مرحبًا بك في فلاح نوميريك. كيف تريد استخدام المنصة؟")
      .assertIsDisplayed()

    // Switch to French and verify exact prompt text
    composeTestRule.onNodeWithTag("onboarding_lang_FR").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Bienvenue sur Fellah Numérique. Comment souhaitez-vous utiliser la plateforme ?")
      .assertIsDisplayed()

    // Switch to English and verify exact prompt text
    composeTestRule.onNodeWithTag("onboarding_lang_EN").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Welcome to Fellah Numérique. How would you like to use the platform?")
      .assertIsDisplayed()

    // Advance to Step 2
    composeTestRule.onNodeWithTag("onboarding_start_selection_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Select single role (Storage Provider) -> Continue should auto-select defaultRole & jump to Step 4
    composeTestRule
      .onNodeWithTag("onboarding_role_card_storage_provider")
      .performScrollTo()
      .performClick()
    composeTestRule.onNodeWithTag("onboarding_continue_step2").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Step 4 is displayed directly and supports "Complete Later"
    composeTestRule
      .onNodeWithTag("onboarding_skip_later_button")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(listOf("storage_provider"), completedRoles)
    assertEquals("storage_provider", completedDefaultRole)
  }

  @Test
  fun `multi role onboarding requires explicit default role in step 3 and supports resume`() {
    var savedStep = 1
    var completedRoles: List<String> = emptyList()
    var completedDefault = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        RoleOnboardingScreen(
          existingAccount =
            UserAccountProfile(
              userId = "u_multi",
              assignedRoles = listOf("farmer", "trader"),
              defaultRole = "farmer",
              onboardingCompleted = false,
              onboardingStep = 2,
            ),
          existingRoleProfiles = emptyMap(),
          language = AppLanguage.FR,
          userDisplayName = "Karim",
          userEmail = "karim@fellah.dz",
          isSaving = false,
          errorMessage = null,
          onLanguageSelected = {},
          onSaveProgressDraft = { _, _, step -> savedStep = step },
          onCompleteOnboarding = { roles, defRole, _ ->
            completedRoles = roles
            completedDefault = defRole
          },
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // Resumed directly at Step 2 with farmer + trader pre-selected
    composeTestRule.onNodeWithTag("onboarding_continue_step2").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(3, savedStep)

    // Step 3: Choose Trader as default active role
    composeTestRule
      .onNodeWithTag("onboarding_default_role_trader")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.onNodeWithTag("onboarding_continue_step3").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(4, savedStep)

    // Complete onboarding from Step 4
    composeTestRule.onNodeWithTag("onboarding_finish_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(listOf("farmer", "trader"), completedRoles)
    assertEquals("trader", completedDefault)
  }

  @Test
  fun `role switcher updates personalized bottom navigation and settings role management`() {
    val farmerTabs = RoleNavigationConfig.bottomBarSectionsForRole(UserRole.FARMER)
    val traderTabs = RoleNavigationConfig.bottomBarSectionsForRole(UserRole.TRADER)
    val transporterTabs = RoleNavigationConfig.bottomBarSectionsForRole(UserRole.TRANSPORTER)

    assertTrue(farmerTabs.contains(WorkspaceSection.PARCEL_DELINEATION))
    assertFalse(traderTabs.contains(WorkspaceSection.PARCEL_DELINEATION))
    assertTrue(traderTabs.contains(WorkspaceSection.ORDERS_AND_REQUESTS))
    assertTrue(transporterTabs.contains(WorkspaceSection.PARTNER_CATALOG))

    var currentRoleState = UserRole.FARMER

    composeTestRule.setContent {
      var activeRole by remember { mutableStateOf(UserRole.FARMER) }
      var assigned by remember { mutableStateOf(listOf(UserRole.FARMER, UserRole.TRADER)) }
      MyApplicationTheme {
        androidx.compose.material3.Scaffold(
          bottomBar = {
            FellahBottomNavigationBar(
              activeSection = WorkspaceSection.ROLE_DASHBOARD,
              language = AppLanguage.AR,
              onSelectSection = {},
              activeRole = activeRole,
            )
          }
        ) { innerPadding ->
          Column(
            modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())
          ) {
            OperatorSettingsScreen(
              userDisplayName = "مزارع وتاجر",
              userEmail = "dual@fellah.dz",
              operatorName = "مستثمرة الواحة",
              defaultWilaya = "07 - بسكرة (Biskra)",
              currentLanguage = AppLanguage.AR,
              currentMapLayer = MapLayerMode.SATELLITE,
              savedParcelsCount = 2,
              onSavePreferences = { _, _ -> },
              onLanguageSelected = {},
              onMapLayerSelected = {},
              onOpenMyParcels = {},
              onExportCurrentParcelPdf = {},
              onSignOut = {},
              onBackToDelineation = {},
              assignedRoles = assigned,
              activeRole = activeRole,
              onSwitchActiveRole = {
                activeRole = it
                currentRoleState = it
              },
              onRemoveRole = { code -> assigned = assigned.filterNot { it.code == code } },
            )
          }
        }
      }
    }

    composeTestRule.waitForIdle()
    // Initially Farmer: parcel delineation is in bottom bar
    composeTestRule.onNodeWithTag("bottom_nav_parcel_delineation").assertIsDisplayed()

    // Switch active role to Trader via Settings 1-tap switch
    composeTestRule.onNodeWithTag("settings_switch_to_trader").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(UserRole.TRADER, currentRoleState)
    composeTestRule.onNodeWithTag("bottom_nav_orders_and_requests").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag("bottom_nav_parcel_delineation").assertCountEquals(0)

    // Remove Farmer role -> only 1 role left, so Remove button must disappear to prevent 0-role state
    composeTestRule.onNodeWithTag("settings_remove_role_farmer").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag("settings_remove_role_trader").assertCountEquals(0)
  }

  @Test
  fun `visitor mode browses public catalog without auth and preserves destination on protected action`() {
    var capturedDestination: WorkspaceSection? = null
    var capturedSuggestedRole: UserRole? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        VisitorExploreScreen(
          language = AppLanguage.AR,
          onLanguageSelected = {},
          onBackToLogin = {},
          onRequireSignInForAction = { section, role ->
            capturedDestination = section
            capturedSuggestedRole = role
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("visitor_explore_root").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("visitor_market_card_pub_deglet")
      .performScrollTo()
      .assertIsDisplayed()

    // Click protected Order button on Deglet Nour public listing
    composeTestRule
      .onNodeWithTag("visitor_order_action_pub_deglet")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    // Confirm sign-in gate modal preserves destination & suggested role
    composeTestRule.onNodeWithTag("visitor_gate_confirm_signin_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertEquals(WorkspaceSection.PUBLIC_MARKET, capturedDestination)
    assertEquals(UserRole.TRADER, capturedSuggestedRole)
  }
}
