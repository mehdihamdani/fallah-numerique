package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MapInteractionTool
import com.example.data.model.MapLayerMode
import com.example.data.model.NormalizedPoint
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Full-screen Satellite Delineation Studio allowing farmers to view and mark land boundaries
 * across the entire device viewport without cramped or overlapping HUD cards.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FullScreenSatelliteDelineationDialog(
  parcel: AgriculturalParcel,
  geoCenter: GeoCoordinateMapper.GeoCenter,
  latLngVertices: List<GeoLatLng>,
  wellLatLng: GeoLatLng,
  vertices: List<NormalizedPoint>,
  wellPt: NormalizedPoint,
  mapLayerMode: MapLayerMode,
  mapTool: MapInteractionTool,
  renderLiveDemoSatelliteMap: Boolean,
  isEffectiveOffline: Boolean,
  isSavingParcel: Boolean,
  onDismiss: () -> Unit,
  onSelectMapLayer: (MapLayerMode) -> Unit,
  onToggleMapTool: (MapInteractionTool) -> Unit,
  onMapTap: (NormalizedPoint) -> Unit,
  onVertexDrag: (Int, NormalizedPoint) -> Unit,
  onGoogleMapLatLngTap: (GeoLatLng) -> Unit,
  onGoogleMapVertexDrag: (Int, GeoLatLng) -> Unit,
  onUndoVertex: () -> Unit,
  onClearPolygon: () -> Unit,
  onRequestLiveGps: () -> Unit,
  onSavePlotBoundary: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  var zoomInTrigger by remember { mutableIntStateOf(0) }
  var zoomOutTrigger by remember { mutableIntStateOf(0) }
  var fitBoundsTrigger by remember { mutableIntStateOf(0) }

  val sqMetersFormatted =
    remember(parcel.areaHectares) {
      NumberFormat.getIntegerInstance(Locale.US)
        .format((parcel.areaHectares * 10000).roundToInt())
    }
  val isDrawing = mapTool == MapInteractionTool.DRAW_POLYGON

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.surface)
          .systemBarsPadding()
          .testTag("fullscreen_satellite_map_dialog"),
      color = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // 1. Clean Non-Overlapping Top Bar (Outside Map Canvas)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier =
            Modifier.fillMaxWidth()
              .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp),
              ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.map_fullscreen_title),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    "${parcel.name} • ${String.format(Locale.US, "%.2f", parcel.areaHectares)} Ha ($sqMetersFormatted م²) • ${latLngVertices.size} نقاط",
                  fontFamily = PlusJakartaSansFamily,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp).testTag("close_fullscreen_map_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.CloseFullscreen,
                  contentDescription = stringResource(R.string.map_fullscreen_collapse_btn),
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.map_fullscreen_collapse_btn),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactLayerChip(
                  label = MapLayerMode.SATELLITE.labelAr,
                  icon = Icons.Filled.Public,
                  selected = mapLayerMode == MapLayerMode.SATELLITE,
                  onClick = { onSelectMapLayer(MapLayerMode.SATELLITE) },
                )
                CompactLayerChip(
                  label = MapLayerMode.RELIEF.labelAr,
                  icon = Icons.Filled.Landscape,
                  selected = mapLayerMode == MapLayerMode.RELIEF,
                  onClick = { onSelectMapLayer(MapLayerMode.RELIEF) },
                )
                CompactLayerChip(
                  label = MapLayerMode.CADASTRE.labelAr,
                  icon = Icons.Filled.Grid4x4,
                  selected = mapLayerMode == MapLayerMode.CADASTRE,
                  onClick = { onSelectMapLayer(MapLayerMode.CADASTRE) },
                )
              }

              Text(
                text = parcel.coordinatesLabel,
                fontFamily = PlusJakartaSansFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
                maxLines = 1,
              )
            }
          }
        }

        // 2. Massive Full-Bleed Map Viewport (Zero Clutter)
        BoxWithConstraints(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(
                1.5.dp,
                if (isDrawing) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp),
              )
              .testTag("fullscreen_satellite_map_canvas")
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

          // Compact single-line guidance pill at TopStart (never collides)
          Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.86f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                      "اضغط على حدود الأرض لإضافة النقطة R${latLngVertices.size + 1} أو اسحب النقاط"
                    MapInteractionTool.PLACE_FORAGE ->
                      "اضغط لتعيين موقع البئر الفلاحي (Forage)"
                    MapInteractionTool.VIEW ->
                      "وضع المعاينة الحرة • فعّل «رسم الحدود» بالأسفل لتعيين الأرض"
                  },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
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

        // 3. Dedicated Bottom Delineation Controls Bar (Outside Map Canvas)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier =
            Modifier.fillMaxWidth()
              .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp),
              ),
        ) {
          FlowRow(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { onToggleMapTool(MapInteractionTool.DRAW_POLYGON) },
              shape = RoundedCornerShape(10.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor =
                    if (isDrawing) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
              modifier = Modifier.height(48.dp).testTag("fullscreen_draw_polygon_button"),
            ) {
              Icon(
                imageVector = if (isDrawing) Icons.Filled.CheckCircle else Icons.Filled.Polyline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text =
                  if (isDrawing) stringResource(R.string.osmdroid_btn_finish_drawing)
                  else stringResource(R.string.osmdroid_btn_start_drawing),
                style = MaterialTheme.typography.labelLarge,
              )
            }

            FilledTonalButton(
              onClick = onUndoVertex,
              enabled = latLngVertices.isNotEmpty(),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("fullscreen_undo_vertex_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = stringResource(R.string.osmdroid_btn_undo_vertex),
                style = MaterialTheme.typography.labelLarge,
              )
            }

            OutlinedButton(
              onClick = onClearPolygon,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("fullscreen_clear_polygon_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = stringResource(R.string.osmdroid_btn_clear_boundary),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
              )
            }

            OutlinedButton(
              onClick = { onToggleMapTool(MapInteractionTool.PLACE_FORAGE) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("fullscreen_place_forage_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(17.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "البئر",
                style = MaterialTheme.typography.labelLarge,
              )
            }

            OutlinedButton(
              onClick = onRequestLiveGps,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("fullscreen_live_gps_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "GPS",
                style = MaterialTheme.typography.labelLarge,
              )
            }

            Button(
              onClick = {
                onSavePlotBoundary()
                if (latLngVertices.size >= 3) {
                  onDismiss()
                }
              },
              enabled = !isSavingParcel && latLngVertices.size >= 3,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("fullscreen_save_and_close_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "حفظ وتثبيت",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CompactLayerChip(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceContainerLowest,
    modifier =
      Modifier.height(36.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint =
          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
