package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AppLanguage
import com.example.data.model.IdentityVerificationData
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
import org.junit.Assert.assertNotNull
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
  fun `onboarding welcome omits top language pills renders trilingual prompt and single role advances to step 4`() {
    var completedRoles: List<String> = emptyList()
    var completedDefaultRole = ""
    var lang by mutableStateOf(AppLanguage.AR)

    composeTestRule.setContent {
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
    // Top bar language pills are removed from onboarding header
    composeTestRule.onAllNodesWithTag("onboarding_lang_AR").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("onboarding_lang_FR").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("onboarding_lang_EN").assertCountEquals(0)

    // Verify Arabic welcome message
    composeTestRule
      .onNodeWithText("مرحبًا بك في فلاح نوميريك. كيف تريد استخدام المنصة؟")
      .assertIsDisplayed()

    // Update language to French and verify exact prompt text
    lang = AppLanguage.FR
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText(
        "Bienvenue sur Fellah Numérique. Comment souhaitez-vous utiliser la plateforme ?"
      )
      .assertIsDisplayed()

    // Update language to English and verify exact prompt text
    lang = AppLanguage.EN
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Welcome to Fellah Numérique. How would you like to use the platform?")
      .assertIsDisplayed()

    // Advance to Step 2
    composeTestRule
      .onNodeWithTag("onboarding_start_selection_button")
      .performScrollTo()
      .performClick()
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
  fun `registration supports National ID scan and optional Farmer Card with or without Farmer Card`() {
    var capturedIdentity: IdentityVerificationData? = null
    var capturedRoles: List<String> = emptyList()

    composeTestRule.setContent {
      MyApplicationTheme {
        RoleOnboardingScreen(
          existingAccount = null,
          existingRoleProfiles = emptyMap(),
          language = AppLanguage.AR,
          userDisplayName = "بلقاسم",
          userEmail = "belkacem@fellah.dz",
          isSaving = false,
          errorMessage = null,
          onSaveProgressDraft = { _, _, _ -> },
          onCompleteOnboarding = { _, _, _ -> },
          onCompleteOnboardingWithIdentity = { roles, _, _, identity ->
            capturedRoles = roles
            capturedIdentity = identity
          },
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()

    // Step 1 renders both National ID Card scanner AND Optional Farmer Card scanner
    composeTestRule.onNodeWithTag("national_id_scanner_box").performScrollTo().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("scan_national_id_camera_button")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("scan_national_id_gallery_button")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("farmer_card_scanner_box").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("farmer_card_status_badge").performScrollTo().assertIsDisplayed()

    // Confirm National ID details AND optional Farmer Card details
    composeTestRule
      .onNodeWithTag("national_id_number_input")
      .performScrollTo()
      .performTextReplacement("109981234567890001")
    composeTestRule
      .onNodeWithTag("national_id_holder_name_input")
      .performScrollTo()
      .performTextReplacement("بلقاسم بن عمارة")
    composeTestRule
      .onNodeWithTag("farmer_card_number_input")
      .performScrollTo()
      .performTextReplacement("DZ-CH-AGR-07-4491")
    composeTestRule
      .onNodeWithTag("farmer_card_wilaya_input")
      .performScrollTo()
      .performTextReplacement("07 - بسكرة (Biskra)")
    composeTestRule.waitForIdle()

    // Proceed through Step 2 (Farmer is pre-selected) -> Step 4 -> Finish
    composeTestRule
      .onNodeWithTag("onboarding_start_selection_button")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("onboarding_continue_step2").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("onboarding_finish_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(listOf("farmer"), capturedRoles)
    val savedIdentity = capturedIdentity
    assertNotNull(savedIdentity)
    assertTrue(savedIdentity!!.nationalIdScanned)
    assertEquals("109981234567890001", savedIdentity.nationalIdNumber)
    assertEquals("بلقاسم بن عمارة", savedIdentity.nationalIdHolderName)
    assertTrue(savedIdentity.farmerCardScanned)
    assertEquals("DZ-CH-AGR-07-4491", savedIdentity.farmerCardNumber)
    assertEquals("07 - بسكرة (Biskra)", savedIdentity.farmerCardWilaya)
  }

  @Test
  fun `registration succeeds with National ID only when user has no Farmer Card`() {
    var capturedIdentity: IdentityVerificationData? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        RoleOnboardingScreen(
          existingAccount = null,
          existingRoleProfiles = emptyMap(),
          language = AppLanguage.AR,
          userDisplayName = "سمير التاجر",
          userEmail = "samir@fellah.dz",
          isSaving = false,
          errorMessage = null,
          onSaveProgressDraft = { _, _, _ -> },
          onCompleteOnboarding = { _, _, _ -> },
          onCompleteOnboardingWithIdentity = { _, _, _, identity -> capturedIdentity = identity },
          onSignOut = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("national_id_number_input")
      .performScrollTo()
      .performTextReplacement("19880412009876")
    composeTestRule.waitForIdle()

    // Leave Farmer Card blank and register as Trader
    composeTestRule
      .onNodeWithTag("onboarding_start_selection_button")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    // Select Trader directly (since Farmer Card was left empty, no Farmer role was auto-selected)
    composeTestRule
      .onNodeWithTag("onboarding_role_card_trader")
      .performScrollTo()
      .performClick()
    composeTestRule.onNodeWithTag("onboarding_continue_step2").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("onboarding_finish_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    val savedIdentity = capturedIdentity
    assertNotNull(savedIdentity)
    assertTrue(savedIdentity!!.nationalIdScanned)
    assertEquals("19880412009876", savedIdentity.nationalIdNumber)
    assertFalse(savedIdentity.farmerCardScanned)
    assertEquals("", savedIdentity.farmerCardNumber)
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
  fun `role switcher updates personalized bottom navigation and settings retains language and identity controls`() {
    val farmerTabs = RoleNavigationConfig.bottomBarSectionsForRole(UserRole.FARMER)
    val traderTabs = RoleNavigationConfig.bottomBarSectionsForRole(UserRole.TRADER)
    val transporterTabs = RoleNavigationConfig.bottomBarSectionsForRole(UserRole.TRANSPORTER)

    assertTrue(farmerTabs.contains(WorkspaceSection.PARCEL_DELINEATION))
    assertFalse(traderTabs.contains(WorkspaceSection.PARCEL_DELINEATION))
    assertTrue(traderTabs.contains(WorkspaceSection.ORDERS_AND_REQUESTS))
    assertTrue(transporterTabs.contains(WorkspaceSection.PARTNER_CATALOG))

    var currentRoleState = UserRole.FARMER
    var selectedSettingsLang = AppLanguage.AR
    var savedSettingsIdentity: IdentityVerificationData? = null

    composeTestRule.setContent {
      var activeRole by remember { mutableStateOf(UserRole.FARMER) }
      var assigned by remember { mutableStateOf(listOf(UserRole.FARMER, UserRole.TRADER)) }
      var lang by remember { mutableStateOf(AppLanguage.AR) }
      MyApplicationTheme {
        androidx.compose.material3.Scaffold(
          bottomBar = {
            FellahBottomNavigationBar(
              activeSection = WorkspaceSection.ROLE_DASHBOARD,
              language = lang,
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
              currentLanguage = lang,
              currentMapLayer = MapLayerMode.SATELLITE,
              savedParcelsCount = 2,
              onSavePreferences = { _, _ -> },
              onLanguageSelected = {
                lang = it
                selectedSettingsLang = it
              },
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
              onSaveIdentityVerification = { savedSettingsIdentity = it },
            )
          }
        }
      }
    }

    composeTestRule.waitForIdle()
    // Initially Farmer: parcel delineation is in bottom bar
    composeTestRule.onNodeWithTag("bottom_nav_parcel_delineation").assertIsDisplayed()

    // Settings still preserves language selector chips (while top menu bars have none)
    composeTestRule.onNodeWithTag("settings_lang_FR").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppLanguage.FR, selectedSettingsLang)

    // Settings also exposes ID & optional Farmer Card verification + save
    composeTestRule
      .onNodeWithTag("national_id_number_input")
      .performScrollTo()
      .performTextReplacement("10990011223344")
    composeTestRule.onNodeWithTag("settings_save_identity_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("10990011223344", savedSettingsIdentity?.nationalIdNumber)

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
  fun `visitor mode omits top language pills browses public catalog without auth and preserves destination on protected action`() {
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
    // Top bar language pills are removed from VisitorExploreScreen as well
    composeTestRule.onAllNodesWithTag("visitor_lang_AR").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("visitor_lang_FR").assertCountEquals(0)
    composeTestRule.onAllNodesWithTag("visitor_lang_EN").assertCountEquals(0)

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
    composeTestRule
      .onNodeWithTag("visitor_gate_confirm_signin_button")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(WorkspaceSection.PUBLIC_MARKET, capturedDestination)
    assertEquals(UserRole.TRADER, capturedSuggestedRole)
  }
}
