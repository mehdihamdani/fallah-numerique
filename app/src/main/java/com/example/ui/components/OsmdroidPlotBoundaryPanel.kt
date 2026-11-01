package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.offline.SavedPlotWithCoordinates
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Floating vertical Osmdroid camera control stack (Zoom In, Zoom Out, Fit Plot Bounds).
 * Every control enforces a minimum 48.dp touch target and descriptive contentDescription.
 */
@Composable
fun OsmdroidMapFloatingControls(
  onZoomIn: () -> Unit,
  onZoomOut: () -> Unit,
  onFitBounds: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.testTag("osmdroid_floating_controls"),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
    shadowElevation = 6.dp,
  ) {
    Column(
      modifier = Modifier.padding(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      IconButton(
        onClick = onZoomIn,
        modifier = Modifier.size(48.dp).testTag("osmdroid_zoom_in_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = stringResource(R.string.osmdroid_zoom_in_desc),
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(22.dp),
        )
      }
      HorizontalDivider(
        modifier = Modifier.width(32.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
      )
      IconButton(
        onClick = onZoomOut,
        modifier = Modifier.size(48.dp).testTag("osmdroid_zoom_out_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Remove,
          contentDescription = stringResource(R.string.osmdroid_zoom_out_desc),
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(22.dp),
        )
      }
      HorizontalDivider(
        modifier = Modifier.width(32.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
      )
      IconButton(
        onClick = onFitBounds,
        modifier = Modifier.size(48.dp).testTag("osmdroid_fit_bounds_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.CenterFocusStrong,
          contentDescription = stringResource(R.string.osmdroid_fit_bounds_desc),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(21.dp),
        )
      }
    }
  }
}

/**
 * Interactive Plot Boundary Studio Panel paired with Osmdroid MapView.
 * Provides live geodesic Area/Perimeter/Vertex telemetry, Draw/Undo/Clear/Reset actions,
 * inspectable & removable WGS84 vertex chips, inline plot naming, and validated plot saving.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OsmdroidPlotBoundaryPanel(
  parcel: AgriculturalParcel,
  latLngVertices: List<GeoLatLng>,
  mapTool: MapInteractionTool,
  isSavingParcel: Boolean,
  onToggleDrawMode: () -> Unit,
  onUndoVertex: () -> Unit,
  onClearPolygon: () -> Unit,
  onResetDefaultPolygon: () -> Unit,
  onRemoveVertexAt: (Int) -> Unit,
  onPlotNameChanged: (String) -> Unit,
  onSavePlotBoundary: () -> Unit,
  localSavedPlots: List<SavedPlotWithCoordinates> = emptyList(),
  activeLocalPlotId: Long = 0L,
  onLoadLocalPlot: (SavedPlotWithCoordinates) -> Unit = {},
  onRenameLocalPlot: (Long, String) -> Unit = { _, _ -> },
  onDeleteLocalPlot: (Long) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val isDrawing = mapTool == MapInteractionTool.DRAW_POLYGON
  val isValidPolygon = latLngVertices.size >= 3
  val perimeterMeters =
    remember(latLngVertices) { GeoCoordinateMapper.computeGeodesicPerimeterMeters(latLngVertices) }
  val formattedPerimeter =
    remember(perimeterMeters) {
      if (perimeterMeters >= 1000.0) {
        String.format(Locale.US, "%.2f km", perimeterMeters / 1000.0)
      } else {
        String.format(Locale.US, "%.1f m", perimeterMeters)
      }
    }
  val sqMetersFormatted =
    remember(parcel.areaHectares) {
      NumberFormat.getIntegerInstance(Locale.US).format((parcel.areaHectares * 10000).roundToInt())
    }

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color =
            if (isDrawing) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(14.dp),
        )
        .testTag("osmdroid_boundary_studio_card"),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 1. Header & Polygon Closure Status Pill
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
            color =
              if (isDrawing) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Polyline,
                contentDescription = null,
                tint =
                  if (isDrawing) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Column {
            Text(
              text = stringResource(R.string.osmdroid_boundary_panel_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.osmdroid_boundary_panel_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(50),
          color =
            if (isValidPolygon) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
          modifier = Modifier.testTag("osmdroid_polygon_validity_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier =
                Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(
                    if (isValidPolygon) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary
                  )
            )
            Text(
              text =
                if (isValidPolygon) {
                  "مضلع مغلق (${latLngVertices.size} رؤوس) • Valid"
                } else {
                  "يحتاج ${3 - latLngVertices.size} نقاط للإغلاق (${latLngVertices.size}/3)"
                },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color =
                if (isValidPolygon) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }
      }

      // 2. Concise Geodesic Telemetry Metrics Row (Area Ha / m², Perimeter m, Vertex Count)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BoundaryMetricTile(
          icon = Icons.Filled.CropFree,
          label = "المساحة",
          primaryValue = "${String.format(Locale.US, "%.2f", parcel.areaHectares)} Ha",
          secondaryValue = "$sqMetersFormatted m²",
          modifier = Modifier.weight(1f).testTag("osmdroid_metric_area"),
        )
        BoundaryMetricTile(
          icon = Icons.Filled.Straighten,
          label = "المحيط",
          primaryValue = formattedPerimeter,
          secondaryValue = if (isValidPolygon) "محيط مغلق" else "ضلع مفتوح",
          modifier = Modifier.weight(1f).testTag("osmdroid_metric_perimeter"),
        )
        BoundaryMetricTile(
          icon = Icons.Filled.Place,
          label = "النقاط",
          primaryValue = "${latLngVertices.size} / 20",
          secondaryValue = "WGS84",
          modifier = Modifier.weight(1f).testTag("osmdroid_metric_vertices"),
        )
      }

      // 3. Boundary Drawing & Vertex Editing Toolbar
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onToggleDrawMode,
          shape = RoundedCornerShape(10.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor =
                if (isDrawing) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.height(48.dp).testTag("osmdroid_toggle_draw_button"),
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
          modifier = Modifier.height(48.dp).testTag("undo_vertex_button"),
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
          modifier = Modifier.height(48.dp).testTag("clear_polygon_button"),
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
          onClick = onResetDefaultPolygon,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(48.dp).testTag("reset_polygon_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = stringResource(R.string.osmdroid_btn_reset_default),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      // 4. WGS84 Vertex Coordinate Inspector Chips (or Guided Empty State)
      if (latLngVertices.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth().testTag("osmdroid_empty_vertices_prompt"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.TouchApp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
            Text(
              text =
                "اضغط مباشرةً على خريطة Osmdroid لوضع النقطة الحدودية الأولى (R1)، ثم الثانية (R2)، والثالثة (R3) لإغلاق حدود القطعة.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      } else {
        FlowRow(
          modifier = Modifier.fillMaxWidth().testTag("osmdroid_vertex_list_row"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          latLngVertices.forEachIndexed { idx, vertex ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLowest,
              modifier =
                Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    RoundedCornerShape(10.dp),
                  )
                  .testTag("vertex_chip_$idx"),
            ) {
              Row(
                modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "R${idx + 1}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimary,
                      fontSize = 10.sp,
                    )
                  }
                }
                Text(
                  text = GeoCoordinateMapper.formatDecimalLabel(vertex.latitude, vertex.longitude),
                  fontFamily = PlusJakartaSansFamily,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                )
                IconButton(
                  onClick = { onRemoveVertexAt(idx) },
                  modifier = Modifier.size(48.dp).testTag("remove_vertex_button_$idx"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "حذف النقطة R${idx + 1}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

      // 5. Inline Plot Name Input + Save Plot Boundary Button
      AnimatedVisibility(visible = !isValidPolygon) {
        Text(
          text =
            "تنبيه: يلزم تحديد 3 نقاط حدودية على الأقل على الخريطة قبل حفظ القطعة الفلاحية (${latLngVertices.size} / 3 حالياً).",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.testTag("osmdroid_min_points_warning"),
        )
      }

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = parcel.name,
          onValueChange = onPlotNameChanged,
          singleLine = true,
          label = { Text(stringResource(R.string.osmdroid_plot_name_label)) },
          shape = RoundedCornerShape(12.dp),
          modifier =
            Modifier.weight(1f, fill = true).testTag("osmdroid_plot_name_input"),
        )

        Button(
          onClick = onSavePlotBoundary,
          enabled = !isSavingParcel,
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier =
            Modifier.height(56.dp)
              .align(Alignment.Bottom)
              .testTag("osmdroid_save_boundary_button"),
        ) {
          if (isSavingParcel) {
            CircularProgressIndicator(
              strokeWidth = 2.2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
          } else {
            Icon(
              imageVector = Icons.Filled.Save,
              contentDescription = null,
              modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(
            text = stringResource(R.string.osmdroid_btn_save_boundary),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

      // 6. Locally Saved Plots on Device (Room SQLite Schema)
      LocalSavedPlotsSection(
        savedPlots = localSavedPlots,
        activeLocalPlotId = activeLocalPlotId,
        onLoadPlot = onLoadLocalPlot,
        onRenamePlot = onRenameLocalPlot,
        onDeletePlot = onDeleteLocalPlot,
      )
    }
  }
}

@Composable
private fun BoundaryMetricTile(
  icon: ImageVector,
  label: String,
  primaryValue: String,
  secondaryValue: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = primaryValue,
        fontFamily = PlusJakartaSansFamily,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = secondaryValue,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/**
 * Self-contained Osmdroid Plot Boundary Drawing & Saving Layout combining an interactive
 * `org.osmdroid.views.MapView` viewport, layer selector, floating zoom/bounds controls,
 * and [OsmdroidPlotBoundaryPanel].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OsmdroidPlotBoundaryLayout(
  parcel: AgriculturalParcel,
  mapLayerMode: MapLayerMode,
  mapTool: MapInteractionTool,
  isEffectiveOffline: Boolean,
  isSavingParcel: Boolean,
  onSelectMapLayer: (MapLayerMode) -> Unit,
  onToggleMapTool: (MapInteractionTool) -> Unit,
  onMapLatLngTap: (GeoLatLng) -> Unit,
  onMapVertexDrag: (Int, GeoLatLng) -> Unit,
  onUndoVertex: () -> Unit,
  onClearPolygon: () -> Unit,
  onResetDefaultPolygon: () -> Unit,
  onRemoveVertexAt: (Int) -> Unit,
  onPlotNameChanged: (String) -> Unit,
  onSavePlotBoundary: () -> Unit,
  localSavedPlots: List<SavedPlotWithCoordinates> = emptyList(),
  activeLocalPlotId: Long = 0L,
  onLoadLocalPlot: (SavedPlotWithCoordinates) -> Unit = {},
  onRenameLocalPlot: (Long, String) -> Unit = { _, _ -> },
  onDeleteLocalPlot: (Long) -> Unit = {},
  onRequestLiveGps: () -> Unit = {},
  forceEnableOsmdroidInTests: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val isUnitTest = remember { isRunningInUnitTestEnvironment() }
  val renderLiveOsmdroidView = !isUnitTest || forceEnableOsmdroidInTests
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

  var zoomInTrigger by remember { mutableIntStateOf(0) }
  var zoomOutTrigger by remember { mutableIntStateOf(0) }
  var fitBoundsTrigger by remember { mutableIntStateOf(0) }

  Card(
    modifier = modifier.fillMaxWidth().testTag("osmdroid_plot_boundary_layout"),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(42.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.SatelliteAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Column {
            Text(
              text = stringResource(R.string.osmdroid_boundary_panel_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${parcel.name} • ${parcel.coordinatesLabel}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(
            onClick = { onSelectMapLayer(MapLayerMode.SATELLITE) },
            modifier = Modifier.size(48.dp).testTag("osmdroid_layer_satellite"),
          ) {
            Icon(
              imageVector = Icons.Filled.Public,
              contentDescription = "Satellite Layer",
              tint =
                if (mapLayerMode == MapLayerMode.SATELLITE) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = { onSelectMapLayer(MapLayerMode.RELIEF) },
            modifier = Modifier.size(48.dp).testTag("osmdroid_layer_relief"),
          ) {
            Icon(
              imageVector = Icons.Filled.Landscape,
              contentDescription = "Topographic Layer",
              tint =
                if (mapLayerMode == MapLayerMode.RELIEF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = { onSelectMapLayer(MapLayerMode.CADASTRE) },
            modifier = Modifier.size(48.dp).testTag("osmdroid_layer_cadastre"),
          ) {
            Icon(
              imageVector = Icons.Filled.Grid4x4,
              contentDescription = "OSM Street Cadastre Layer",
              tint =
                if (mapLayerMode == MapLayerMode.CADASTRE) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onRequestLiveGps,
            modifier = Modifier.size(48.dp).testTag("osmdroid_gps_center_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.MyLocation,
              contentDescription = stringResource(R.string.btn_live_gps),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(480.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("osmdroid_map_container")
      ) {
        if (renderLiveOsmdroidView) {
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
            onMapLatLngTap = onMapLatLngTap,
            onMapVertexDrag = onMapVertexDrag,
          )
        } else {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(480.dp)
                .background(Color(0xFF1B3022))
                .testTag("demo_live_satellite_map_view")
          )
        }

        OsmdroidMapFloatingControls(
          onZoomIn = { zoomInTrigger++ },
          onZoomOut = { zoomOutTrigger++ },
          onFitBounds = { fitBoundsTrigger++ },
          modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp),
        )
      }

      OsmdroidPlotBoundaryPanel(
        parcel = parcel,
        latLngVertices = latLngVertices,
        mapTool = mapTool,
        isSavingParcel = isSavingParcel,
        onToggleDrawMode = { onToggleMapTool(MapInteractionTool.DRAW_POLYGON) },
        onUndoVertex = onUndoVertex,
        onClearPolygon = onClearPolygon,
        onResetDefaultPolygon = onResetDefaultPolygon,
        onRemoveVertexAt = onRemoveVertexAt,
        onPlotNameChanged = onPlotNameChanged,
        onSavePlotBoundary = onSavePlotBoundary,
        localSavedPlots = localSavedPlots,
        activeLocalPlotId = activeLocalPlotId,
        onLoadLocalPlot = onLoadLocalPlot,
        onRenameLocalPlot = onRenameLocalPlot,
        onDeleteLocalPlot = onDeleteLocalPlot,
      )
    }
  }
}
