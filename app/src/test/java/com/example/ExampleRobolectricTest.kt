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
import kotlinx.coroutines.flow.first
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

  @Test
  fun `google maps wgs84 projection and room offline region cache persist accurately`() =
    kotlinx.coroutines.runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val center =
        com.example.util.GeoCoordinateMapper.parseCenterCoordinates("34°43'12.4\"N 5°22'48.9\"E")
      assertEquals(34.7201, center.latitude, 0.001)
      assertEquals(5.3802, center.longitude, 0.001)

      val defaultTolgaPolygon =
        listOf("0.20,0.23", "0.74,0.20", "0.85,0.75", "0.51,0.85", "0.24,0.71")
          .map { NormalizedPoint.decode(it) }
      val latLngVertices =
        defaultTolgaPolygon.map {
          com.example.util.GeoCoordinateMapper.normalizedToLatLng(it, center)
        }
      val geodesicHa =
        com.example.util.GeoCoordinateMapper.computeGeodesicAreaHectares(latLngVertices)
      assertTrue("Expected positive geodesic area in Ha, got $geodesicHa", geodesicHa > 3.5)

      val db =
        androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.offline.FellahOfflineDatabase::class.java,
          )
          .allowMainThreadQueries()
          .build()
      val cacheRepo = com.example.data.offline.OfflineMapCacheRepository(db.offlineMapRegionDao())

      cacheRepo.saveDownloadedRegion(
        com.example.data.offline.OfflineMapRegionEntity(
          id = "region_biskra_tolga",
          regionName = "حوض واحات طولقة وفوغالة",
          wilaya = "07 - بسكرة (Biskra)",
          parcelReference = "Ilot N 108 / P-24",
          centerLat = center.latitude,
          centerLng = center.longitude,
          zoomLevel = 16.5f,
          radiusKm = 12.5,
          mapLayerCode = "SATELLITE",
          polygonSnapshotEncoded = "0.20,0.23|0.74,0.20|0.85,0.75",
          sizeMb = 18.4,
        )
      )

      val saved = cacheRepo.downloadedRegions.first()
      assertEquals(1, saved.size)
      assertEquals("region_biskra_tolga", saved.first().id)

      cacheRepo.deleteRegion("region_biskra_tolga")
      val afterDelete = cacheRepo.downloadedRegions.first()
      assertTrue(afterDelete.isEmpty())
      db.close()
    }

  @Test
  fun `keyless demo satellite map section renders without api key and toggles engine`() {
    var preferLiveDemoEngine = true
    composeTestRule.setContent {
      var liveEngine by remember { mutableStateOf(true) }
      MyApplicationTheme {
        com.example.ui.components.InteractiveSatelliteMapSection(
          parcel = com.example.data.model.AgriculturalParcel(),
          mapLayerMode = com.example.data.model.MapLayerMode.SATELLITE,
          mapTool = com.example.data.model.MapInteractionTool.VIEW,
          onSelectMapLayer = {},
          onToggleMapTool = {},
          onMapTap = {},
          onVertexDrag = { _, _ -> },
          onResetPolygon = {},
          onRedrawNewPolygon = {},
          onRequestLiveGps = {},
          onOpenCadastralEditor = {},
          onOpenNdviHistory = {},
          preferGoogleMapsEngine = liveEngine,
          onToggleMapEngine = {
            liveEngine = it
            preferLiveDemoEngine = it
          },
        )
      }
    }

    composeTestRule.onNodeWithTag("satellite_map_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_external_google_maps_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("toggle_google_maps_engine_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("toggle_google_maps_engine_chip").performClick()
    assertEquals(false, preferLiveDemoEngine)
  }

  @Test
  fun `agri services catalog includes all five domains and twelve requested services`() {
    val domains = com.example.data.model.AgriServiceDomain.entries.map { it.titleAr }
    assertTrue(domains.containsAll(
      listOf(
        "الإنتاج الزراعي",
        "التجهيزات والمستلزمات",
        "ما بعد الحصاد",
        "التجارة والنقل",
        "الاستشارات والخدمات الرقمية",
      )
    ))

    val catalogTitles =
      com.example.data.model.AgriServiceCategory.primaryCatalogEntries.map { it.titleAr }
    val expectedTwelve =
      listOf(
        "حرث وتسوية الأرض",
        "كراء الجرارات والآلات",
        "تركيب وصيانة الري",
        "توفير العمالة",
        "الحصاد والجني",
        "البذور والشتلات",
        "الأسمدة",
        "مخازن التبريد",
        "النقل الزراعي",
        "مهندس زراعي",
        "أسعار السوق",
        "الطقس الزراعي",
      )
    assertTrue("Missing requested services in $catalogTitles", catalogTitles.containsAll(expectedTwelve))

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.AgriServicesScreen(
          bookingsState = com.example.ui.viewmodel.UiState.Success(emptyList()),
          defaultWilaya = "07 - بسكرة (Biskra)",
          activeParcelRef = "Ilot N 108 / P-24",
          onSubmitBooking = { _, _, _, _, _ -> },
          onCancelBooking = {},
          onBackToDelineation = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("agri_services_catalog_root").assertIsDisplayed()
    composeTestRule.onNodeWithTag("service_domain_filter_agricultural_production").assertIsDisplayed()
    composeTestRule.onNodeWithTag("service_card_plowing_leveling").assertIsDisplayed()
  }
}
