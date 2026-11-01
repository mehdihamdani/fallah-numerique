package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MapInteractionTool
import com.example.data.model.MapLayerMode
import com.example.data.model.NormalizedPoint
import com.example.data.offline.SavedPlotWithCoordinates
import com.example.ui.theme.OasisAmberDim
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveSatelliteMapSection(
  parcel: AgriculturalParcel,
  mapLayerMode: MapLayerMode,
  mapTool: MapInteractionTool,
  onSelectMapLayer: (MapLayerMode) -> Unit,
  onToggleMapTool: (MapInteractionTool) -> Unit,
  onMapTap: (NormalizedPoint) -> Unit,
  onVertexDrag: (Int, NormalizedPoint) -> Unit,
  onResetPolygon: () -> Unit,
  onRedrawNewPolygon: () -> Unit,
  onRequestLiveGps: () -> Unit,
  onOpenCadastralEditor: () -> Unit,
  onOpenNdviHistory: () -> Unit,
  modifier: Modifier = Modifier,
  preferGoogleMapsEngine: Boolean = true,
  isEffectiveOffline: Boolean = false,
  downloadedRegionsCount: Int = 0,
  isSavingParcel: Boolean = false,
  forceEnableOsmdroidInTests: Boolean = false,
  onToggleMapEngine: (Boolean) -> Unit = {},
  onGoogleMapLatLngTap: (GeoLatLng) -> Unit = {},
  onGoogleMapVertexDrag: (Int, GeoLatLng) -> Unit = { _, _ -> },
  onOpenOfflineManager: () -> Unit = {},
  onUndoVertex: () -> Unit = {},
  onClearPolygon: () -> Unit = {},
  onRemoveVertexAt: (Int) -> Unit = {},
  onPlotNameChanged: (String) -> Unit = {},
  onSavePlotBoundary: () -> Unit = {},
  localSavedPlots: List<SavedPlotWithCoordinates> = emptyList(),
  activeLocalPlotId: Long = 0L,
  onLoadLocalPlot: (SavedPlotWithCoordinates) -> Unit = {},
  onRenameLocalPlot: (Long, String) -> Unit = { _, _ -> },
  onDeleteLocalPlot: (Long) -> Unit = {},
) {
  val context = LocalContext.current
  val isUnitTest = remember { isRunningInUnitTestEnvironment() }
  val isCurrentParcelCachedOffline = downloadedRegionsCount > 0

  // Keyless Live Satellite Osmdroid Engine is enabled out-of-the-box without requiring any API key!
  val renderLiveDemoSatelliteMap =
    preferGoogleMapsEngine && (!isUnitTest || forceEnableOsmdroidInTests)

  var zoomInTrigger by remember { mutableIntStateOf(0) }
  var zoomOutTrigger by remember { mutableIntStateOf(0) }
  var fitBoundsTrigger by remember { mutableIntStateOf(0) }
  var isFullScreenMapOpen by rememberSaveable { mutableStateOf(false) }

  val geoCenter =
    remember(parcel.coordinatesLabel) {
      GeoCoordinateMapper.parseCenterCoordinates(parcel.coordinatesLabel)
    }
  val latLngVertices =
    remember(parcel.polygonPoints, geoCenter) {
      GeoCoordinateMapper.decodePolygonToLatLng(parcel.polygonPoints, geoCenter)
    }
  val wellLatLng =
    remember(parcel.wellPoint, geoCenter) {
      GeoCoordinateMapper.decodePointToLatLng(parcel.wellPoint, geoCenter)
    }
  val vertices =
    remember(parcel.polygonPoints, geoCenter) {
      GeoCoordinateMapper.decodePolygonToNormalized(parcel.polygonPoints, geoCenter)
    }
  val wellPt =
    remember(wellLatLng, geoCenter) {
      GeoCoordinateMapper.latLngToNormalized(wellLatLng, geoCenter)
    }
  val sqMetersFormatted =
    remember(parcel.areaHectares) {
      NumberFormat.getIntegerInstance(Locale.US).format((parcel.areaHectares * 10000).roundToInt())
    }

  if (isFullScreenMapOpen) {
    FullScreenSatelliteDelineationDialog(
      parcel = parcel,
      geoCenter = geoCenter,
      latLngVertices = latLngVertices,
      wellLatLng = wellLatLng,
      vertices = vertices,
      wellPt = wellPt,
      mapLayerMode = mapLayerMode,
      mapTool = mapTool,
      renderLiveDemoSatelliteMap = renderLiveDemoSatelliteMap,
      isEffectiveOffline = isEffectiveOffline,
      isSavingParcel = isSavingParcel,
      onDismiss = { isFullScreenMapOpen = false },
      onSelectMapLayer = onSelectMapLayer,
      onToggleMapTool = onToggleMapTool,
      onMapTap = onMapTap,
      onVertexDrag = onVertexDrag,
      onGoogleMapLatLngTap = onGoogleMapLatLngTap,
      onGoogleMapVertexDrag = onGoogleMapVertexDrag,
      onUndoVertex = onUndoVertex,
      onClearPolygon = onClearPolygon,
      onRequestLiveGps = onRequestLiveGps,
      onSavePlotBoundary = onSavePlotBoundary,
    )
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            RoundedCornerShape(16.dp),
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // 1. Map Header & Layer Switcher
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false),
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector =
                    if (renderLiveDemoSatelliteMap) Icons.Filled.SatelliteAlt else Icons.Filled.Map,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(21.dp),
                )
              }
            }
            Column {
              Text(
                text = stringResource(R.string.map_header_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (isEffectiveOffline) {
                    "وضع أوفلاين نشط • ${parcel.name}"
                  } else if (renderLiveDemoSatelliteMap) {
                    "قمر صناعي حي عالي الدقة • ${parcel.name}"
                  } else {
                    "مخطط طبوغرافي هندسي • ${parcel.name}"
                  },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
          ) {
            Row(
              modifier = Modifier.padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              MapModePill(
                mode = MapLayerMode.SATELLITE,
                icon = Icons.Filled.Public,
                selected = mapLayerMode == MapLayerMode.SATELLITE,
                onClick = { onSelectMapLayer(MapLayerMode.SATELLITE) },
                testTag = "map_mode_satellite",
              )
              MapModePill(
                mode = MapLayerMode.RELIEF,
                icon = Icons.Filled.Landscape,
                selected = mapLayerMode == MapLayerMode.RELIEF,
                onClick = { onSelectMapLayer(MapLayerMode.RELIEF) },
                testTag = "map_mode_relief",
              )
              MapModePill(
                mode = MapLayerMode.CADASTRE,
                icon = Icons.Filled.Grid4x4,
                selected = mapLayerMode == MapLayerMode.CADASTRE,
                onClick = { onSelectMapLayer(MapLayerMode.CADASTRE) },
                testTag = "map_mode_cadastre",
              )
            }
          }
        }

        // 2. Engine, Offline Cache & External Google Maps Action Strip
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(50),
              color =
                if (isEffectiveOffline) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer,
              modifier =
                Modifier.clip(RoundedCornerShape(50))
                  .clickable(onClick = onOpenOfflineManager)
                  .testTag("map_offline_status_badge"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector =
                    if (isEffectiveOffline) Icons.Filled.CloudOff
                    else if (isCurrentParcelCachedOffline) Icons.Filled.CloudDone
                    else Icons.Filled.CloudDownload,
                  contentDescription = null,
                  tint =
                    if (isEffectiveOffline) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text =
                    if (isEffectiveOffline) {
                      "أوفلاين نشط ($downloadedRegionsCount)"
                    } else if (isCurrentParcelCachedOffline) {
                      "جاهز أوفلاين ($downloadedRegionsCount)"
                    } else {
                      stringResource(R.string.btn_download_offline_map)
                    },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color =
                    if (isEffectiveOffline) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier =
                Modifier.clip(RoundedCornerShape(50))
                  .clickable { onToggleMapEngine(!preferGoogleMapsEngine) }
                  .testTag("toggle_google_maps_engine_chip"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Layers,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text =
                    if (renderLiveDemoSatelliteMap) {
                      "قمر صناعي حي ✓"
                    } else {
                      "مخطط طبوغرافي ✓"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .clickable {
                  openInExternalGoogleMaps(
                    context = context,
                    lat = geoCenter.latitude,
                    lng = geoCenter.longitude,
                    label = "${parcel.name} - ${parcel.ilotNumber}",
                  )
                }
                .testTag("open_external_google_maps_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = stringResource(R.string.btn_open_in_google_maps),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        // 3. Dedicated Non-Overlapping Geodesic Telemetry Bar (Moved OUTSIDE Map Canvas!)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier =
            Modifier.fillMaxWidth()
              .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp),
              )
              .testTag("satellite_telemetry_bar"),
        ) {
          FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.CropFree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Text(
                  text = String.format(Locale.US, "%.2f", parcel.areaHectares),
                  fontFamily = PlusJakartaSansFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 20.sp,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = stringResource(R.string.hectare_unit),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(bottom = 1.dp),
                )
                Text(
                  text = "($sqMetersFormatted م²)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(bottom = 2.dp),
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.align(Alignment.CenterVertically),
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = parcel.coordinatesLabel,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }

        // 4. Enlarged, Unobstructed Satellite Map Viewport (520dp–560dp + Fullscreen Expand)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
          val viewportHeight = if (maxWidth >= 600.dp) 560.dp else 520.dp
          BoxWithConstraints(
            modifier =
              Modifier.fillMaxWidth()
                .height(viewportHeight)
                .clip(RoundedCornerShape(16.dp))
                .border(
                  width = if (mapTool != MapInteractionTool.VIEW) 2.dp else 1.dp,
                  color =
                    if (mapTool != MapInteractionTool.VIEW) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(16.dp),
                )
                .testTag("satellite_map_canvas")
          ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            if (renderLiveDemoSatelliteMap) {
              LiveDemoSatelliteMapViewport(
                parcel = parcel,
                geoCenter = geoCenter,
                latLngVertices = latLngVertices,
                wellLatLng = wellLatLng,
                mapLayerMode = mapLayerMode,
                mapTool = mapTool,
                isEffectiveOffline = isEffectiveOffline,
                zoomInTrigger = zoomInTrigger,
                zoomOutTrigger = zoomOutTrigger,
                fitBoundsTrigger = fitBoundsTrigger,
                onMapLatLngTap = onGoogleMapLatLngTap,
                onMapVertexDrag = onGoogleMapVertexDrag,
              )
            } else {
              OfflineTopographicCanvasViewport(
                parcel = parcel,
                vertices = vertices,
                wellPt = wellPt,
                mapLayerMode = mapLayerMode,
                mapTool = mapTool,
                widthPx = widthPx,
                heightPx = heightPx,
                onMapTap = onMapTap,
                onVertexDrag = onVertexDrag,
              )
            }

            // Single Non-Colliding Top Row inside Canvas: Compact Status Pill + Fullscreen Button
            Row(
              modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                modifier = Modifier.weight(1f, fill = false),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.86f),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(8.dp)
                        .clip(CircleShape)
                        .background(
                          if (mapTool != MapInteractionTool.VIEW) Color(0xFF4ADE80)
                          else Color(0xFF93C5FD)
                        )
                  )
                  Text(
                    text =
                      when (mapTool) {
                        MapInteractionTool.DRAW_POLYGON ->
                          "تعيين الأرض • ${latLngVertices.size} نقاط"
                        MapInteractionTool.PLACE_FORAGE ->
                          "تحديد البئر (Forage)"
                        MapInteractionTool.VIEW ->
                          "${parcel.ilotNumber} • ${latLngVertices.size} نقاط"
                      },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                modifier =
                  Modifier.height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isFullScreenMapOpen = true }
                    .testTag("toggle_fullscreen_map_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.OpenInFull,
                    contentDescription = stringResource(R.string.map_fullscreen_expand_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = stringResource(R.string.map_fullscreen_expand_btn),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                  )
                }
              }
            }

            if (renderLiveDemoSatelliteMap) {
              OsmdroidMapFloatingControls(
                onZoomIn = { zoomInTrigger++ },
                onZoomOut = { zoomOutTrigger++ },
                onFitBounds = { fitBoundsTrigger++ },
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
              )
            }
          }
        }

        // 5. Active Mode Guidance Banner & Action Buttons (Cleanly Below Map — Zero Map Overlap!)
        if (mapTool != MapInteractionTool.VIEW) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("active_delineation_mode_banner"),
          ) {
            FlowRow(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text =
                  if (mapTool == MapInteractionTool.DRAW_POLYGON) {
                    stringResource(R.string.map_drawing_active_hint)
                  } else {
                    stringResource(R.string.map_forage_active_hint)
                  },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f, fill = false),
              )

              if (mapTool == MapInteractionTool.DRAW_POLYGON) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  OutlinedButton(
                    onClick = onRedrawNewPolygon,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                      ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                      ),
                    modifier = Modifier.height(48.dp),
                  ) {
                    Text(
                      text = "مضلع جديد",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  OutlinedButton(
                    onClick = onResetPolygon,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                      ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                      ),
                    modifier = Modifier.height(48.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "إعادة ضبط",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onToggleMapTool(MapInteractionTool.DRAW_POLYGON) },
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor =
                  if (mapTool == MapInteractionTool.DRAW_POLYGON) {
                    Color(0xFF1E7E34)
                  } else {
                    MaterialTheme.colorScheme.primary
                  },
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.height(48.dp).testTag("draw_polygon_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Polyline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.btn_draw_polygon),
              style = MaterialTheme.typography.labelLarge,
            )
          }

          FilledTonalButton(
            onClick = { onToggleMapTool(MapInteractionTool.PLACE_FORAGE) },
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor =
                  if (mapTool == MapInteractionTool.PLACE_FORAGE) {
                    MaterialTheme.colorScheme.secondaryContainer
                  } else {
                    MaterialTheme.colorScheme.surfaceContainer
                  },
                contentColor = MaterialTheme.colorScheme.onSurface,
              ),
            modifier = Modifier.height(48.dp).testTag("place_forage_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.WaterDrop,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.btn_place_forage),
              style = MaterialTheme.typography.labelLarge,
            )
          }

          FilledTonalButton(
            onClick = onRequestLiveGps,
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
              ),
            modifier = Modifier.height(48.dp).testTag("live_gps_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.MyLocation,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = stringResource(R.string.btn_live_gps),
              style = MaterialTheme.typography.labelLarge,
            )
          }

          FilledTonalButton(
            onClick = onOpenOfflineManager,
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
              ),
            modifier = Modifier.height(48.dp).testTag("offline_maps_manager_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.CloudDownload,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "خرائط أوفلاين",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 6. Interactive Osmdroid Plot Boundary Drawing & Saving Studio Panel
        OsmdroidPlotBoundaryPanel(
          parcel = parcel,
          latLngVertices = latLngVertices,
          mapTool = mapTool,
          isSavingParcel = isSavingParcel,
          onToggleDrawMode = { onToggleMapTool(MapInteractionTool.DRAW_POLYGON) },
          onUndoVertex = onUndoVertex,
          onClearPolygon = onClearPolygon,
          onResetDefaultPolygon = onResetPolygon,
          onRemoveVertexAt = onRemoveVertexAt,
          onPlotNameChanged = onPlotNameChanged,
          onSavePlotBoundary = onSavePlotBoundary,
          localSavedPlots = localSavedPlots,
          activeLocalPlotId = activeLocalPlotId,
          onLoadLocalPlot = onLoadLocalPlot,
          onRenameLocalPlot = onRenameLocalPlot,
          onDeleteLocalPlot = onDeleteLocalPlot,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 7. Administrative Cadastral Metadata Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "البيانات العقارية والإدارية (Données Cadastrales)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          IconButton(
            onClick = onOpenCadastralEditor,
            modifier = Modifier.size(48.dp).testTag("edit_cadastral_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "تعديل البيانات العقارية",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CadastralMetaTile(
              label = "الولاية (Wilaya)",
              value = parcel.wilaya,
              onClick = onOpenCadastralEditor,
              modifier = Modifier.weight(1f),
            )
            CadastralMetaTile(
              label = "البلدية والمنطقة",
              value = parcel.commune,
              onClick = onOpenCadastralEditor,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CadastralMetaTile(
              label = "رقم القطعة / الجزيرة",
              value = parcel.ilotNumber,
              onClick = onOpenCadastralEditor,
              modifier = Modifier.weight(1f),
            )
            CadastralMetaTile(
              label = "طبيعة الحيازة العقارية",
              value = parcel.tenureType,
              onClick = onOpenCadastralEditor,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // NDVI Biomass Summary Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            RoundedCornerShape(16.dp),
          ),
    ) {
      FlowRow(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.weight(1f, fill = false),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(58.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Eco,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp),
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.Center,
            ) {
              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = "مؤشر نشط: NDVI ${String.format(Locale.US, "%.2f", parcel.ndviScore)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
              }
              Text(
                text = stringResource(R.string.ndvi_scan_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = stringResource(R.string.ndvi_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                "الكتلة الحيوية متجانسة عبر كامل مساحة ${String.format(Locale.US, "%.2f", parcel.areaHectares)} هكتار مع غياب مؤشرات الجفاف أو تعري التربة.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        FilledTonalButton(
          onClick = onOpenNdviHistory,
          shape = RoundedCornerShape(10.dp),
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer,
              contentColor = MaterialTheme.colorScheme.primary,
            ),
          modifier = Modifier.height(48.dp).testTag("ndvi_history_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Analytics,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.ndvi_history_btn),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
internal fun OfflineTopographicCanvasViewport(
  parcel: AgriculturalParcel,
  vertices: List<NormalizedPoint>,
  wellPt: NormalizedPoint,
  mapLayerMode: MapLayerMode,
  mapTool: MapInteractionTool,
  @Suppress("UNUSED_PARAMETER") widthPx: Float,
  @Suppress("UNUSED_PARAMETER") heightPx: Float,
  onMapTap: (NormalizedPoint) -> Unit,
  onVertexDrag: (Int, NormalizedPoint) -> Unit,
) {
  val density = LocalDensity.current
  val latestVertices by rememberUpdatedState(vertices)
  val latestOnTap by rememberUpdatedState(onMapTap)
  val latestOnDrag by rememberUpdatedState(onVertexDrag)
  var draggedVertexIndex by remember { mutableIntStateOf(-1) }
  val isActivelyMarking = mapTool != MapInteractionTool.VIEW

  Box(modifier = Modifier.fillMaxSize()) {
    Canvas(
      modifier =
        Modifier.fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures { tapOffset ->
              if (size.width > 0 && size.height > 0) {
                val norm =
                  NormalizedPoint(
                    tapOffset.x / size.width.toFloat(),
                    tapOffset.y / size.height.toFloat(),
                  )
                latestOnTap(norm)
              }
            }
          }
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { startOffset ->
                if (size.width <= 0 || size.height <= 0) return@detectDragGestures
                val hitRadiusPx = 36f * density.density
                var closestIdx = -1
                var minDist = Float.MAX_VALUE
                latestVertices.forEachIndexed { idx, v ->
                  val vx = v.x * size.width
                  val vy = v.y * size.height
                  val d = hypot(startOffset.x - vx, startOffset.y - vy)
                  if (d < hitRadiusPx && d < minDist) {
                    minDist = d
                    closestIdx = idx
                  }
                }
                draggedVertexIndex = closestIdx
              },
              onDrag = { change, _ ->
                if (draggedVertexIndex >= 0 && size.width > 0 && size.height > 0) {
                  change.consume()
                  val norm =
                    NormalizedPoint(
                      change.position.x / size.width.toFloat(),
                      change.position.y / size.height.toFloat(),
                    )
                  latestOnDrag(draggedVertexIndex, norm)
                }
              },
              onDragEnd = { draggedVertexIndex = -1 },
              onDragCancel = { draggedVertexIndex = -1 },
            )
          }
    ) {
      when (mapLayerMode) {
        MapLayerMode.SATELLITE ->
          drawTolgaOasisSatelliteTerrain(hideCentralPlaceLabels = isActivelyMarking || vertices.isNotEmpty())
        MapLayerMode.RELIEF ->
          drawReliefTopographyTerrain(hideCentralPlaceLabels = isActivelyMarking)
        MapLayerMode.CADASTRE ->
          drawCadastreLotGrid(parcel.ilotNumber, dimGridLabels = isActivelyMarking)
      }

      // Subtle top vignette only for top pill readability (keeps full map bright & clear)
      drawRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                Color.Black.copy(alpha = 0.24f),
                Color.Transparent,
                Color.Transparent,
              )
          ),
        size = size,
      )

      // Compute centroid in canvas pixels for radial outward non-overlapping vertex badges
      val centroid =
        if (vertices.isNotEmpty()) {
          var sx = 0f
          var sy = 0f
          vertices.forEach {
            sx += it.x * size.width
            sy += it.y * size.height
          }
          Offset(sx / vertices.size, sy / vertices.size)
        } else {
          Offset(size.width * 0.5f, size.height * 0.5f)
        }

      // Draw Cadastral Polygon / Draft Boundary Overlay
      if (vertices.size == 2) {
        val p0 = Offset(vertices[0].x * size.width, vertices[0].y * size.height)
        val p1 = Offset(vertices[1].x * size.width, vertices[1].y * size.height)
        drawLine(
          color = Color(0xFF4ADE80),
          start = p0,
          end = p1,
          strokeWidth = 3.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f),
        )
      }

      val wellOffset = Offset(wellPt.x * size.width, wellPt.y * size.height)

      if (vertices.size >= 3) {
        val polyPath = Path()
        vertices.forEachIndexed { idx, pt ->
          val px = pt.x * size.width
          val py = pt.y * size.height
          if (idx == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
        }
        polyPath.close()

        drawPath(
          path = polyPath,
          color = Color(0xFF1E7E34).copy(alpha = 0.26f),
        )
        drawPath(
          path = polyPath,
          color = Color(0xFF4ADE80),
          style =
            Stroke(
              width = 3.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f),
            ),
        )

        drawLine(
          color = OasisAmberDim,
          start = wellOffset,
          end = centroid,
          strokeWidth = 2.4.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
        )
      }

      // Draw Borehole (Forage) Point directly on Canvas (never intercepts touch gestures!)
      val wellRadiusPx = 9.dp.toPx()
      drawCircle(color = Color.White, radius = wellRadiusPx + 2.dp.toPx(), center = wellOffset)
      drawCircle(color = Color(0xFF0277BD), radius = wellRadiusPx, center = wellOffset)
      drawCircle(color = Color(0xFFB3E5FC), radius = wellRadiusPx * 0.38f, center = wellOffset)

      // Collision-free Radial Vertex Nodes & Badges (R1..Rn)
      val placedBadgeCenters = mutableListOf<Offset>()
      val minBadgeSeparationPx = 32.dp.toPx()
      val radialPushPx = 19.dp.toPx()
      val badgeBgPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = android.graphics.Color.argb(215, 17, 36, 23)
          style = Paint.Style.FILL
        }
      val badgeBorderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = android.graphics.Color.argb(210, 134, 239, 172)
          style = Paint.Style.STROKE
          strokeWidth = 1.2.dp.toPx()
        }
      val badgeTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = android.graphics.Color.WHITE
          textSize = 10.sp.toPx()
          textAlign = Paint.Align.CENTER
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

      vertices.forEachIndexed { idx, pt ->
        val nodeOffset = Offset(pt.x * size.width, pt.y * size.height)
        val dotRadius = if (vertices.size >= 6) 5.5.dp.toPx() else 6.5.dp.toPx()
        drawCircle(
          color = Color.White,
          radius = dotRadius,
          center = nodeOffset,
        )
        drawCircle(
          color = Color(0xFF15803D),
          radius = dotRadius,
          center = nodeOffset,
          style = Stroke(width = 2.6.dp.toPx()),
        )

        // Push R-label outward away from polygon centroid so labels never crowd inside the land plot
        val dx = nodeOffset.x - centroid.x
        val dy = nodeOffset.y - centroid.y
        val dist = hypot(dx, dy)
        val dirX = if (dist > 1f) dx / dist else 0f
        val dirY = if (dist > 1f) dy / dist else -1f
        val candidateCenter =
          Offset(
            x = (nodeOffset.x + dirX * radialPushPx).coerceIn(24.dp.toPx(), (size.width - 24.dp.toPx()).coerceAtLeast(24.dp.toPx())),
            y = (nodeOffset.y + dirY * radialPushPx).coerceIn(58.dp.toPx(), (size.height - 20.dp.toPx()).coerceAtLeast(58.dp.toPx())),
          )

        val collidesWithExisting =
          placedBadgeCenters.any { existing ->
            hypot(existing.x - candidateCenter.x, existing.y - candidateCenter.y) < minBadgeSeparationPx
          }
        if (!collidesWithExisting) {
          placedBadgeCenters.add(candidateCenter)
          val label = "R${idx + 1}"
          val halfW = (badgeTextPaint.measureText(label) / 2f) + 6.dp.toPx()
          val halfH = 9.dp.toPx()
          val rect =
            RectF(
              candidateCenter.x - halfW,
              candidateCenter.y - halfH,
              candidateCenter.x + halfW,
              candidateCenter.y + halfH,
            )
          drawContext.canvas.nativeCanvas.drawRoundRect(rect, 6.dp.toPx(), 6.dp.toPx(), badgeBgPaint)
          drawContext.canvas.nativeCanvas.drawRoundRect(rect, 6.dp.toPx(), 6.dp.toPx(), badgeBorderPaint)
          val textY = candidateCenter.y - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
          drawContext.canvas.nativeCanvas.drawText(label, candidateCenter.x, textY, badgeTextPaint)
        }
      }

      // Draw Forage depth label ONLY when not actively plotting polygon corners and when non-colliding
      if (mapTool != MapInteractionTool.DRAW_POLYGON && parcel.wellDepthLabel.isNotBlank()) {
        val labelCenter =
          Offset(
            x = wellOffset.x.coerceIn(48.dp.toPx(), (size.width - 48.dp.toPx()).coerceAtLeast(48.dp.toPx())),
            y = (wellOffset.y + 20.dp.toPx()).coerceIn(60.dp.toPx(), (size.height - 22.dp.toPx()).coerceAtLeast(60.dp.toPx())),
          )
        val collides =
          placedBadgeCenters.any { existing ->
            hypot(existing.x - labelCenter.x, existing.y - labelCenter.y) < 42.dp.toPx()
          }
        if (!collides) {
          val wellLabel = parcel.wellDepthLabel.take(22)
          val halfW = (badgeTextPaint.measureText(wellLabel) / 2f) + 7.dp.toPx()
          val halfH = 9.5.dp.toPx()
          val wellBgPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              color = android.graphics.Color.argb(215, 2, 62, 102)
              style = Paint.Style.FILL
            }
          val rect =
            RectF(
              labelCenter.x - halfW,
              labelCenter.y - halfH,
              labelCenter.x + halfW,
              labelCenter.y + halfH,
            )
          drawContext.canvas.nativeCanvas.drawRoundRect(rect, 6.dp.toPx(), 6.dp.toPx(), wellBgPaint)
          val textY = labelCenter.y - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
          drawContext.canvas.nativeCanvas.drawText(wellLabel, labelCenter.x, textY, badgeTextPaint)
        }
      }
    }
  }
}

private fun openInExternalGoogleMaps(
  context: Context,
  lat: Double,
  lng: Double,
  label: String,
) {
  val encodedLabel = Uri.encode(label)
  val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
  val mapsIntent =
    Intent(Intent.ACTION_VIEW, geoUri).apply {
      setPackage("com.google.android.apps.maps")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(mapsIntent) }
    .onFailure {
      val webUri =
        Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
      runCatching {
        context.startActivity(
          Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        )
      }
    }
}

@Composable
private fun MapModePill(
  mode: MapLayerMode,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color =
      if (selected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
    shadowElevation = if (selected) 2.dp else 0.dp,
    modifier =
      Modifier.height(36.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = mode.labelAr,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CadastralMetaTile(
  label: String,
  value: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier =
      modifier
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private fun DrawScope.drawTolgaOasisSatelliteTerrain(hideCentralPlaceLabels: Boolean) {
  drawRect(
    brush =
      Brush.linearGradient(
        colors = listOf(Color(0xFF9C937A), Color(0xFF6B7B5C), Color(0xFF4F6647), Color(0xFF8C856F)),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
      ),
    size = size,
  )

  val groveBlocks =
    listOf(
      Triple(Offset(size.width * 0.14f, size.height * 0.16f), Size(size.width * 0.62f, size.height * 0.66f), Color(0xFF2E4D2B)),
      Triple(Offset(size.width * 0.04f, size.height * 0.45f), Size(size.width * 0.24f, size.height * 0.35f), Color(0xFF395B34)),
      Triple(Offset(size.width * 0.72f, size.height * 0.24f), Size(size.width * 0.23f, size.height * 0.48f), Color(0xFF345430)),
    )
  groveBlocks.forEach { (topLeft, blockSize, tint) ->
    drawRect(color = tint.copy(alpha = 0.72f), topLeft = topLeft, size = blockSize)
  }

  val stepX = (size.width / 36f).coerceAtLeast(14f)
  val stepY = (size.height / 24f).coerceAtLeast(14f)
  var x = size.width * 0.12f
  while (x < size.width * 0.88f) {
    var y = size.height * 0.14f
    while (y < size.height * 0.84f) {
      drawCircle(
        color = Color(0xFF1B361A).copy(alpha = 0.65f),
        radius = 2.6f,
        center = Offset(x, y),
      )
      y += stepY
    }
    x += stepX
  }

  val roadStroke = Stroke(width = 3.2.dp.toPx())
  val n46Path =
    Path().apply {
      moveTo(0f, size.height * 0.56f)
      lineTo(size.width * 0.34f, size.height * 0.50f)
      lineTo(size.width * 0.56f, size.height * 0.52f)
      lineTo(size.width, size.height * 0.46f)
    }
  drawPath(n46Path, color = Color(0xFFE6C77A).copy(alpha = 0.78f), style = roadStroke)

  val n46bPath =
    Path().apply {
      moveTo(size.width * 0.48f, size.height * 0.51f)
      lineTo(size.width * 0.42f, size.height * 0.76f)
      lineTo(size.width * 0.35f, size.height)
    }
  drawPath(n46bPath, color = Color(0xFFD8D4C5).copy(alpha = 0.65f), style = Stroke(width = 2.dp.toPx()))

  // Keep peripheral geographic context without drawing text through the active land polygon
  if (!hideCentralPlaceLabels) {
    val labelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(195, 245, 250, 245)
        textSize = 10.5.sp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
      }
    drawContext.canvas.nativeCanvas.drawText("Tolga • طولقة", size.width * 0.05f, size.height * 0.93f, labelPaint)
    drawContext.canvas.nativeCanvas.drawText("Biskra Oasis", size.width * 0.72f, size.height * 0.93f, labelPaint)
  }
}

private fun DrawScope.drawReliefTopographyTerrain(hideCentralPlaceLabels: Boolean) {
  drawRect(
    brush =
      Brush.verticalGradient(
        colors = listOf(Color(0xFF4A5D46), Color(0xFF6B7F5E), Color(0xFF8B9474), Color(0xFFA79C7B))
      ),
    size = size,
  )
  val contourStroke = Stroke(width = 1.6.dp.toPx())
  for (i in 1..6) {
    drawOval(
      color = Color.White.copy(alpha = 0.25f),
      topLeft = Offset(size.width * (0.06f * i), size.height * (0.05f * i)),
      size = Size(size.width * (1f - 0.11f * i), size.height * (1f - 0.09f * i)),
      style = contourStroke,
    )
  }
  if (!hideCentralPlaceLabels) {
    val elevPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(200, 255, 255, 255)
        textSize = 10.5.sp.toPx()
        typeface = Typeface.MONOSPACE
      }
    drawContext.canvas.nativeCanvas.drawText("ALT 148m • حوض وادي جدي", size.width * 0.05f, size.height * 0.93f, elevPaint)
  }
}

private fun DrawScope.drawCadastreLotGrid(activeIlot: String, dimGridLabels: Boolean) {
  drawRect(color = Color(0xFF1B2B20), size = size)
  val cols = 5
  val rows = 4
  val cellW = size.width / cols
  val cellH = size.height / rows
  val gridStroke = Stroke(width = 1.2.dp.toPx())
  val lotPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color =
        if (dimGridLabels) android.graphics.Color.argb(85, 185, 235, 195)
        else android.graphics.Color.argb(165, 185, 235, 195)
      textSize = 9.5.sp.toPx()
      typeface = Typeface.MONOSPACE
    }
  for (c in 0 until cols) {
    for (r in 0 until rows) {
      val left = c * cellW
      val top = r * cellH
      drawRect(
        color = Color(0xFF7EDB85).copy(alpha = 0.26f),
        topLeft = Offset(left, top),
        size = Size(cellW, cellH),
        style = gridStroke,
      )
      if (!dimGridLabels || r == 0 || r == rows - 1) {
        val code = if (c == 2 && r == 1) activeIlot else "P-${10 + r * cols + c}"
        drawContext.canvas.nativeCanvas.drawText(code, left + 8f, top + 20f, lotPaint)
      }
    }
  }
}
