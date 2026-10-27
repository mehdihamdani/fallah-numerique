package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.data.model.NormalizedPoint
import com.example.data.model.WorkspaceSection
import com.example.ui.screens.FellahBottomNavigationBar
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read app_name and verify calibrated polygon surface calculation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Fellah Numérique", appName)

    val defaultTolgaPolygon =
      listOf("0.20,0.23", "0.74,0.20", "0.85,0.75", "0.51,0.85", "0.24,0.71")
        .map { NormalizedPoint.decode(it) }
    val areaHa = NormalizedPoint.computeAreaHectares(defaultTolgaPolygon)
    assertTrue("Expected positive calibrated oasis area in Ha, got $areaHa", areaHa > 4.0)
  }

  @Test
  fun `bottom navigation menu displays market transport services and settings tabs`() {
    var selectedSection = WorkspaceSection.PARCEL_DELINEATION

    composeTestRule.setContent {
      var active by remember { mutableStateOf(WorkspaceSection.PARCEL_DELINEATION) }
      MyApplicationTheme {
        FellahBottomNavigationBar(
          activeSection = active,
          language = AppLanguage.AR,
          onSelectSection = {
            active = it
            selectedSection = it
          },
        )
      }
    }

    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("السوق").assertIsDisplayed()
    composeTestRule.onNodeWithText("النقل").assertIsDisplayed()
    composeTestRule.onNodeWithText("الخدمات").assertIsDisplayed()
    composeTestRule.onNodeWithText("الإعدادات").assertIsDisplayed()

    composeTestRule.onNodeWithTag("bottom_nav_public_market").performClick()
    assertEquals(WorkspaceSection.PUBLIC_MARKET, selectedSection)
    composeTestRule.onNodeWithTag("bottom_nav_public_market").assertIsSelected()

    composeTestRule.onNodeWithTag("bottom_nav_transport").performClick()
    assertEquals(WorkspaceSection.TRANSPORT, selectedSection)

    composeTestRule.onNodeWithTag("bottom_nav_services").performClick()
    assertEquals(WorkspaceSection.SERVICES, selectedSection)

    composeTestRule.onNodeWithTag("bottom_nav_settings").performClick()
    assertEquals(WorkspaceSection.SETTINGS, selectedSection)
    composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsSelected()
  }
}
