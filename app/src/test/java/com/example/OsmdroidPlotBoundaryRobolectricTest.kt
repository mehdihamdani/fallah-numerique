package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MapInteractionTool
import com.example.data.model.MapLayerMode
import com.example.ui.components.InteractiveSatelliteMapSection
import com.example.ui.components.OsmdroidPlotBoundaryLayout
import com.example.ui.components.ensureOsmDroidConfigured
import com.example.ui.components.populateOsmdroidBoundaryOverlays
import com.example.ui.theme.MyApplicationTheme
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OsmdroidPlotBoundaryRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `osmdroid mapview builds polyline for 2 vertices and closed polygon for 4 wgs84 vertices`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ensureOsmDroidConfigured(context)

    val mapView = MapView(context).apply { setUseDataConnection(false) }
    val parcel = AgriculturalParcel(name = "قطعة متيجة النموذجية")
    val dummyIcon = ColorDrawable(Color.GREEN)
    val wellLatLng = GeoLatLng(36.5740, 2.9120)

    // 1. Draft state with 2 vertices -> should render open Polyline preview, no Polygon yet
    val twoPts =
      listOf(
        GeoLatLng(36.5750, 2.9110),
        GeoLatLng(36.5750, 2.9135),
      )
    populateOsmdroidBoundaryOverlays(
      mapView = mapView,
      parcel = parcel,
      latLngVertices = twoPts,
      wellLatLng = wellLatLng,
      centroidLatLng = GeoCoordinateMapper.computeCentroid(twoPts, wellLatLng),
      vertexIcons = listOf(dummyIcon, dummyIcon),
      forageIcon = dummyIcon,
      isDrawingActive = true,
      onTap = {},
      onVertexDrag = { _, _ -> },
    )

    assertEquals(0, mapView.overlays.filterIsInstance<Polygon>().size)
    assertEquals(1, mapView.overlays.filterIsInstance<Polyline>().size)
    assertEquals(1, mapView.overlays.filterIsInstance<MapEventsOverlay>().size)
    // 2 vertex markers + 1 borehole marker
    assertEquals(3, mapView.overlays.filterIsInstance<Marker>().size)
    assertEquals(0.0, GeoCoordinateMapper.computeGeodesicAreaHectares(twoPts), 0.001)
    assertTrue(GeoCoordinateMapper.computeGeodesicPerimeterMeters(twoPts) > 150.0)

    // 2. Closed boundary with 4 exact WGS84 vertices -> renders closed Polygon + geodesic telemetry
    val fourPts =
      listOf(
        GeoLatLng(36.5755, 2.9110),
        GeoLatLng(36.5755, 2.9140),
        GeoLatLng(36.5735, 2.9140),
        GeoLatLng(36.5735, 2.9110),
      )
    val encoded = fourPts.map { GeoCoordinateMapper.encodeGeoPoint(it) }
    assertTrue(encoded.all { it.startsWith("geo:") && it.length <= 40 })

    val center = GeoCoordinateMapper.GeoCenter(36.5745, 2.9125)
    val roundTrip = GeoCoordinateMapper.decodePolygonToLatLng(encoded, center)
    assertEquals(4, roundTrip.size)
    assertEquals(36.5755, roundTrip[0].latitude, 0.00001)
    assertEquals(2.9110, roundTrip[0].longitude, 0.00001)

    populateOsmdroidBoundaryOverlays(
      mapView = mapView,
      parcel = parcel,
      latLngVertices = roundTrip,
      wellLatLng = wellLatLng,
      centroidLatLng = GeoCoordinateMapper.computeCentroid(roundTrip, wellLatLng),
      vertexIcons = List(4) { dummyIcon },
      forageIcon = dummyIcon,
      isDrawingActive = true,
      onTap = {},
      onVertexDrag = { _, _ -> },
    )

    val polygons = mapView.overlays.filterIsInstance<Polygon>()
    assertEquals(1, polygons.size)
    assertTrue(polygons.first().actualPoints.size >= 4)
    // 4 draggable boundary vertex markers + 1 borehole marker = 5 markers
    val markers = mapView.overlays.filterIsInstance<Marker>()
    assertEquals(5, markers.size)
    assertTrue(markers.take(4).all { it.isDraggable })

    val areaHa = GeoCoordinateMapper.computeGeodesicAreaHectares(roundTrip)
    val perimeterM = GeoCoordinateMapper.computeGeodesicPerimeterMeters(roundTrip)
    assertTrue("Expected ~5-6 Ha plot area, got $areaHa", areaHa in 4.0..8.0)
    assertTrue("Expected ~950m perimeter, got $perimeterM", perimeterM in 800.0..1200.0)
    mapView.onDetach()
  }

  @Test
  fun `osmdroid plot boundary layout supports clear draw undo rename and save plot flow`() {
    var savedParcelSnapshot: AgriculturalParcel? = null
    lateinit var tapDispatcher: (GeoLatLng) -> Unit

    composeTestRule.setContent {
      var parcelState by remember { mutableStateOf(AgriculturalParcel()) }
      var toolState by remember { mutableStateOf(MapInteractionTool.VIEW) }
      var layerState by remember { mutableStateOf(MapLayerMode.SATELLITE) }

      val onTapHandler: (GeoLatLng) -> Unit = { tapped ->
        if (toolState == MapInteractionTool.DRAW_POLYGON) {
          val center = GeoCoordinateMapper.parseCenterCoordinates(parcelState.coordinatesLabel)
          val updated =
            GeoCoordinateMapper.decodePolygonToLatLng(parcelState.polygonPoints, center) + tapped
          val area = GeoCoordinateMapper.computeGeodesicAreaHectares(updated)
          val centroid = GeoCoordinateMapper.computeCentroid(updated, tapped)
          parcelState =
            parcelState.copy(
              polygonPoints = updated.map { GeoCoordinateMapper.encodeGeoPoint(it) },
              areaHectares = area,
              coordinatesLabel =
                GeoCoordinateMapper.formatDecimalLabel(centroid.latitude, centroid.longitude),
            )
        }
      }
      tapDispatcher = onTapHandler

      MyApplicationTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          OsmdroidPlotBoundaryLayout(
            parcel = parcelState,
            mapLayerMode = layerState,
            mapTool = toolState,
            isEffectiveOffline = true,
            isSavingParcel = false,
            onSelectMapLayer = { layerState = it },
            onToggleMapTool = { requested ->
              toolState = if (toolState == requested) MapInteractionTool.VIEW else requested
            },
            onMapLatLngTap = onTapHandler,
            onMapVertexDrag = { _, _ -> },
            onUndoVertex = {
              val trimmed = parcelState.polygonPoints.dropLast(1)
              val center = GeoCoordinateMapper.parseCenterCoordinates(parcelState.coordinatesLabel)
              val latLngs = GeoCoordinateMapper.decodePolygonToLatLng(trimmed, center)
              parcelState =
                parcelState.copy(
                  polygonPoints = trimmed,
                  areaHectares = GeoCoordinateMapper.computeGeodesicAreaHectares(latLngs),
                )
            },
            onClearPolygon = {
              toolState = MapInteractionTool.DRAW_POLYGON
              parcelState = parcelState.copy(polygonPoints = emptyList(), areaHectares = 0.0)
            },
            onResetDefaultPolygon = {
              parcelState = AgriculturalParcel()
            },
            onRemoveVertexAt = { idx ->
              val mutable = parcelState.polygonPoints.toMutableList()
              if (idx in mutable.indices) {
                mutable.removeAt(idx)
                val center = GeoCoordinateMapper.parseCenterCoordinates(parcelState.coordinatesLabel)
                val latLngs = GeoCoordinateMapper.decodePolygonToLatLng(mutable, center)
                parcelState =
                  parcelState.copy(
                    polygonPoints = mutable,
                    areaHectares = GeoCoordinateMapper.computeGeodesicAreaHectares(latLngs),
                  )
              }
            },
            onPlotNameChanged = { newName -> parcelState = parcelState.copy(name = newName) },
            onSavePlotBoundary = {
              if (parcelState.polygonPoints.size >= 3) {
                savedParcelSnapshot = parcelState
              }
            },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("osmdroid_plot_boundary_layout").assertIsDisplayed()
    composeTestRule.onNodeWithTag("demo_live_satellite_map_view").assertIsDisplayed()
    composeTestRule.onNodeWithTag("osmdroid_zoom_in_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("osmdroid_fit_bounds_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("osmdroid_zoom_in_button").performClick()
    composeTestRule.onNodeWithTag("osmdroid_fit_bounds_button").performClick()

    // 1. Scroll to and clear existing points to start drawing a fresh plot boundary from 0 points
    composeTestRule.onNodeWithTag("clear_polygon_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("osmdroid_empty_vertices_prompt").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("osmdroid_min_points_warning").performScrollTo().assertIsDisplayed()

    // 2. Tap 4 boundary corners on the Osmdroid map
    composeTestRule.runOnIdle {
      tapDispatcher(GeoLatLng(36.1910, 5.4100))
      tapDispatcher(GeoLatLng(36.1910, 5.4135))
      tapDispatcher(GeoLatLng(36.1885, 5.4135))
      tapDispatcher(GeoLatLng(36.1885, 5.4100))
    }
    composeTestRule.waitForIdle()

    // 3. Undo 1 point -> 3 vertices remain (closed triangle boundary)
    composeTestRule.onNodeWithTag("undo_vertex_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // 4. Rename plot and Save Plot Boundary
    composeTestRule
      .onNodeWithTag("osmdroid_plot_name_input")
      .performScrollTo()
      .performTextReplacement("مزرعة القمح الصلب - سطيف")
    composeTestRule.onNodeWithTag("osmdroid_save_boundary_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    val saved = savedParcelSnapshot
    assertNotNull("Expected plot boundary to be saved", saved)
    assertEquals("مزرعة القمح الصلب - سطيف", saved!!.name)
    assertEquals(3, saved.polygonPoints.size)
    assertTrue(saved.polygonPoints.all { it.startsWith("geo:") })
    assertTrue("Expected positive geodesic area in Ha, got ${saved.areaHectares}", saved.areaHectares > 1.0)
  }

  @Test
  fun `interactive satellite map section exposes osmdroid boundary studio panel and vertex deletion`() {
    var deletedIndex = -1
    var saveClicked = false

    composeTestRule.setContent {
      MyApplicationTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          InteractiveSatelliteMapSection(
            parcel = AgriculturalParcel(),
            mapLayerMode = MapLayerMode.SATELLITE,
            mapTool = MapInteractionTool.DRAW_POLYGON,
            onSelectMapLayer = {},
            onToggleMapTool = {},
            onMapTap = {},
            onVertexDrag = { _, _ -> },
            onResetPolygon = {},
            onRedrawNewPolygon = {},
            onRequestLiveGps = {},
            onOpenCadastralEditor = {},
            onOpenNdviHistory = {},
            onRemoveVertexAt = { deletedIndex = it },
            onSavePlotBoundary = { saveClicked = true },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("osmdroid_boundary_studio_card")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("remove_vertex_button_0")
      .performScrollTo()
      .performClick()
    assertEquals(0, deletedIndex)

    composeTestRule
      .onNodeWithTag("osmdroid_save_boundary_button")
      .performScrollTo()
      .performClick()
    assertTrue(saveClicked)
  }

  @Test
  fun `enlarged satellite map separates telemetry and tools without overlap and supports fullscreen delineation`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          InteractiveSatelliteMapSection(
            parcel = AgriculturalParcel(),
            mapLayerMode = MapLayerMode.SATELLITE,
            mapTool = MapInteractionTool.DRAW_POLYGON,
            onSelectMapLayer = {},
            onToggleMapTool = {},
            onMapTap = {},
            onVertexDrag = { _, _ -> },
            onResetPolygon = {},
            onRedrawNewPolygon = {},
            onRequestLiveGps = {},
            onOpenCadastralEditor = {},
            onOpenNdviHistory = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()

    val telemetryBounds =
      composeTestRule.onNodeWithTag("satellite_telemetry_bar").getUnclippedBoundsInRoot()
    val mapBounds =
      composeTestRule.onNodeWithTag("satellite_map_canvas").getUnclippedBoundsInRoot()
    val bannerBounds =
      composeTestRule.onNodeWithTag("active_delineation_mode_banner").getUnclippedBoundsInRoot()

    // Map viewport is enlarged (>= 520.dp) and does not collide with telemetry above or active banner below
    assertTrue("Expected enlarged map height >= 500.dp, got ${mapBounds.height}", mapBounds.height >= 500.dp)
    assertTrue(
      "Expected telemetry bar above map without overlap (${telemetryBounds.bottom} <= ${mapBounds.top})",
      telemetryBounds.bottom <= mapBounds.top,
    )
    assertTrue(
      "Expected active delineation banner below map without overlap (${mapBounds.bottom} <= ${bannerBounds.top})",
      mapBounds.bottom <= bannerBounds.top,
    )

    // Open Full-Screen Satellite Delineation Studio and collapse back
    composeTestRule.onNodeWithTag("toggle_fullscreen_map_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fullscreen_satellite_map_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fullscreen_satellite_map_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_fullscreen_map_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fullscreen_satellite_map_dialog").assertDoesNotExist()
  }
}
