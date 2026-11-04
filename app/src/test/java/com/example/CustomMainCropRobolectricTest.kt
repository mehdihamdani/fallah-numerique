package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AgriculturalParcel
import com.example.data.model.CropVarietyType
import com.example.ui.components.CropsAndCampaignCard
import com.example.ui.screens.PublishMarketDialog
import com.example.ui.theme.MyApplicationTheme
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
class CustomMainCropRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun defaultParcel_startsWithEmptyMainCrop_andCustomCropHelpersPreserveFarmerProducts() {
    val defaultParcel = AgriculturalParcel()
    assertEquals("", defaultParcel.mainCrop)

    assertNull(CropVarietyType.fromCodeOrNull(""))
    assertNull(CropVarietyType.fromCodeOrNull("فراولة جيجل"))
    assertFalse(CropVarietyType.isCustomCrop(""))
    assertFalse(CropVarietyType.isCustomCrop("deglet_nour"))
    assertTrue(CropVarietyType.isCustomCrop("فراولة جيجل"))

    assertEquals("غير محدد", CropVarietyType.resolveDisplayTitleAr(""))
    assertEquals("نخيل دقلة نور", CropVarietyType.resolveDisplayTitleAr("deglet_nour"))
    assertEquals("فراولة جيجل", CropVarietyType.resolveDisplayTitleAr("فراولة جيجل"))
    assertEquals("Produit personnalisé", CropVarietyType.resolveDisplaySubtitleFr("فراولة جيجل"))
  }

  @Test
  fun cropsAndCampaignCard_startsEmpty_allowsTypingAddingAndClearingCustomProduct() {
    var parcelState by mutableStateOf(AgriculturalParcel())
    var savedCustomCrops by mutableStateOf(emptyList<String>())

    composeTestRule.setContent {
      MyApplicationTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          CropsAndCampaignCard(
            parcel = parcelState,
            onSelectMainCrop = { preset ->
              parcelState =
                if (parcelState.mainCrop == preset.code) {
                  parcelState.copy(mainCrop = "")
                } else {
                  parcelState.copy(
                    mainCrop = preset.code,
                    estimatedYieldTonsPerHa = preset.defaultYieldTonsHa,
                  )
                }
            },
            onUpdateIrrigation = { parcelState = parcelState.copy(irrigationSystem = it) },
            onUpdateYield = {},
            onUpdatePlantingYear = {},
            onUpdateIntercropping = {},
            customUserCrops = savedCustomCrops,
            onUpdateCustomMainCrop = { typed ->
              parcelState = parcelState.copy(mainCrop = typed.take(80))
            },
            onAddCustomCropChip = { typed ->
              val cleaned = typed.trim().take(80)
              if (cleaned.isNotEmpty()) {
                parcelState = parcelState.copy(mainCrop = cleaned)
                if (savedCustomCrops.none { it.equals(cleaned, ignoreCase = true) }) {
                  savedCustomCrops = listOf(cleaned) + savedCustomCrops
                }
              }
            },
            onRemoveCustomCropChip = { target ->
              savedCustomCrops = savedCustomCrops.filterNot { it == target }
              if (parcelState.mainCrop == target) {
                parcelState = parcelState.copy(mainCrop = "")
              }
            },
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    // 1. Field starts empty ("فارغ حالياً") and add button is disabled while empty
    assertEquals("", parcelState.mainCrop)
    composeTestRule.onNodeWithText("فارغ حالياً").assertIsDisplayed()
    composeTestRule.onNodeWithTag("add_custom_crop_button").assertIsNotEnabled()

    // 2. User types their own custom product freely
    composeTestRule.onNodeWithTag("custom_main_crop_input").performScrollTo().performTextInput("فراولة جيجل")
    composeTestRule.waitForIdle()
    assertEquals("فراولة جيجل", parcelState.mainCrop)
    composeTestRule.onNodeWithTag("add_custom_crop_button").assertIsEnabled()

    // 3. User clicks "+ إضافة منتوج خاص" to save it as a reusable custom product chip
    composeTestRule.onNodeWithTag("add_custom_crop_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(listOf("فراولة جيجل"), savedCustomCrops)
    composeTestRule.onNodeWithTag("saved_custom_crop_chip_0").performScrollTo().assertIsDisplayed()

    // 4. User clears the field back to empty ("خانة فارغة")
    composeTestRule.onNodeWithTag("clear_main_crop_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("", parcelState.mainCrop)

    // 5. User can re-select their saved custom product with one tap, or toggle a preset back to empty
    composeTestRule.onNodeWithTag("saved_custom_crop_chip_0").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("فراولة جيجل", parcelState.mainCrop)

    composeTestRule.onNodeWithTag("crop_option_olivier").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("olivier", parcelState.mainCrop)

    // Clicking the selected preset again returns mainCrop to empty ("")
    composeTestRule.onNodeWithTag("crop_option_olivier").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("", parcelState.mainCrop)
  }

  @Test
  fun publishMarketDialog_withEmptyParcelMainCrop_allowsTypingCustomProductBeforePublishing() {
    var publishedCrop = ""
    var publishedPrice = 0.0

    composeTestRule.setContent {
      MyApplicationTheme {
        PublishMarketDialog(
          parcel = AgriculturalParcel(mainCrop = ""),
          defaultOperatorName = "مزرعة الهضاب",
          onDismiss = {},
          onConfirmPublish = { _, _, price, crop, _ ->
            publishedPrice = price
            publishedCrop = crop
          },
        )
      }
    }
    composeTestRule.waitForIdle()

    // Confirm button is disabled while crop input is blank
    composeTestRule.onNodeWithTag("confirm_market_publish_button").assertIsNotEnabled()

    // Farmer types a custom product directly in the publish dialog
    composeTestRule.onNodeWithTag("publish_market_crop_input").performTextInput("ثوم أحمر محلي")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("confirm_market_publish_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()

    assertEquals("ثوم أحمر محلي", publishedCrop)
    assertEquals(120.0, publishedPrice, 0.01)
  }
}
