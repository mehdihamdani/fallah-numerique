package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MapInteractionTool
import com.example.data.model.MapLayerMode
import com.example.data.model.NormalizedPoint
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
  onToggleMapEngine: (Boolean) -> Unit = {},
  onGoogleMapLatLngTap: (GeoLatLng) -> Unit = {},
  onGoogleMapVertexDrag: (Int, GeoLatLng) -> Unit = { _, _ -> },
  onOpenOfflineManager: () -> Unit = {},
) {
  val context = LocalContext.current
  val isUnitTest = remember { isRunningInUnitTestEnvironment() }
  val isCurrentParcelCachedOffline = downloadedRegionsCount > 0

  // Keyless Live Satellite Demo Engine is enabled out-of-the-box without requiring any API key!
  // Users can also toggle between Live Satellite Tiles (cached on disk for offline use) and Vector Topographic Canvas.
  val renderLiveDemoSatelliteMap = preferGoogleMapsEngine && !isUnitTest

  val geoCenter =
    remember(parcel.coordinatesLabel) {
      GeoCoordinateMapper.parseCenterCoordinates(parcel.coordinatesLabel)
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
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Map Header & Layer Switcher
        FlowRow(
          modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(10.dp),
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
                  imageVector =
                    if (renderLiveDemoSatelliteMap) Icons.Filled.SatelliteAlt else Icons.Filled.Map,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column {
              Text(
                text = stringResource(R.string.map_header_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (isEffectiveOffline) {
                    "الوضع التجريبي أوفلاين (ذاكرة القمر الصناعي المحلية) • ${parcel.name}"
                  } else if (renderLiveDemoSatelliteMap) {
                    "الوضع التجريبي النشط (قمر صناعي حي بدون مفتاح API) • ${parcel.name}"
                  } else {
                    "المخطط الطبوغرافي الهندسي + ربط Google Maps • ${parcel.name}"
                  },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Google Maps Engine & Offline Regions Action Strip
        FlowRow(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text =
                    if (isEffectiveOffline) {
                      "أوفلاين نشط (${downloadedRegionsCount} مناطق محفوظة)"
                    } else if (isCurrentParcelCachedOffline) {
                      "جاهز أوفلاين (${downloadedRegionsCount} مناطق)"
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
                      "خريطة تجريبية حية (بدون مفتاح) ✓"
                    } else {
                      "مخطط طبوغرافي محلي ✓ (تبديل للحي)"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          // Direct Deep-Link to Google Maps App with exact WGS84 coordinates (works online & with Google Maps offline areas)
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
                imageVector = Icons.Filled.OpenInNew,
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

        // Interactive Map Viewport + Floating HUDs
        val vertices =
          remember(parcel.polygonPoints) {
            parcel.polygonPoints.map { NormalizedPoint.decode(it) }
          }
        val wellPt = remember(parcel.wellPoint) { NormalizedPoint.decode(parcel.wellPoint) }

        BoxWithConstraints(
          modifier =
            Modifier.fillMaxWidth()
              .height(390.dp)
              .clip(RoundedCornerShape(16.dp))
              .testTag("satellite_map_canvas")
        ) {
          val density = LocalDensity.current
          val widthPx = with(density) { maxWidth.toPx() }
          val heightPx = with(density) { maxHeight.toPx() }

          if (renderLiveDemoSatelliteMap) {
            LiveDemoSatelliteMapViewport(
              parcel = parcel,
              geoCenter = geoCenter,
              vertices = vertices,
              wellPt = wellPt,
              mapLayerMode = mapLayerMode,
              isEffectiveOffline = isEffectiveOffline,
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

          // Top-Start Live Calibrated Surface Area HUD Card
          Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.CropFree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
              Column {
                Text(
                  text = stringResource(R.string.calibrated_area_label),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val sqMetersFormatted =
                  NumberFormat.getIntegerInstance(Locale.US)
                    .format((parcel.areaHectares * 10000).roundToInt())
                Row(
                  verticalAlignment = Alignment.Bottom,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = String.format(Locale.US, "%.2f", parcel.areaHectares),
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = stringResource(R.string.hectare_unit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp),
                  )
                  Text(
                    text = "(${sqMetersFormatted} م²)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                  )
                }
              }
            }
          }

          // Top-End Coordinate Chip
          Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Explore,
                contentDescription = null,
                tint = Color(0xFF99F89E),
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = parcel.coordinatesLabel,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
              )
            }
          }

          // Active Mode Helper Banner & Bottom Map Tools
          Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (mapTool != MapInteractionTool.VIEW) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text =
                      if (mapTool == MapInteractionTool.DRAW_POLYGON) {
                        "وضع الرسم نشط: اضغط على الخريطة لإضافة رأس جديد أو اسحب النقاط"
                      } else {
                        "وضع تحديد البئر: اضغط على موقع نقطة السقي (Forage) فوق الخريطة"
                      },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  if (mapTool == MapInteractionTool.DRAW_POLYGON) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.surfaceContainerLowest,
                      modifier = Modifier.clickable { onRedrawNewPolygon() },
                    ) {
                      Text(
                        text = "مضلع جديد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.surfaceContainerLowest,
                      modifier = Modifier.clickable { onResetPolygon() },
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Refresh,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(13.dp),
                        )
                        Text(
                          text = "إعادة ضبط",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  }
                }
              }
            }

            FlowRow(
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

              Button(
                onClick = { onToggleMapTool(MapInteractionTool.PLACE_FORAGE) },
                shape = RoundedCornerShape(10.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor =
                      if (mapTool == MapInteractionTool.PLACE_FORAGE) {
                        MaterialTheme.colorScheme.secondaryContainer
                      } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
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

              Button(
                onClick = onRequestLiveGps,
                shape = RoundedCornerShape(10.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
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

              Button(
                onClick = onOpenOfflineManager,
                shape = RoundedCornerShape(10.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
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
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Administrative Cadastral Metadata Bar
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
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
private fun OfflineTopographicCanvasViewport(
  parcel: AgriculturalParcel,
  vertices: List<NormalizedPoint>,
  wellPt: NormalizedPoint,
  mapLayerMode: MapLayerMode,
  mapTool: MapInteractionTool,
  widthPx: Float,
  heightPx: Float,
  onMapTap: (NormalizedPoint) -> Unit,
  onVertexDrag: (Int, NormalizedPoint) -> Unit,
) {
  val density = LocalDensity.current
  val latestVertices by rememberUpdatedState(vertices)
  val latestOnTap by rememberUpdatedState(onMapTap)
  val latestOnDrag by rememberUpdatedState(onVertexDrag)
  var draggedVertexIndex by remember { mutableIntStateOf(-1) }

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
        MapLayerMode.SATELLITE -> drawTolgaOasisSatelliteTerrain()
        MapLayerMode.RELIEF -> drawReliefTopographyTerrain()
        MapLayerMode.CADASTRE -> drawCadastreLotGrid(parcel.ilotNumber)
      }

      // Atmospheric top & bottom scrims for crisp HUD contrast
      drawRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                Color.Black.copy(alpha = 0.45f),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = 0.58f),
              )
          ),
        size = size,
      )

      // Draw Cadastral Polygon Overlay
      if (vertices.size >= 3) {
        val polyPath = Path()
        var cxSum = 0f
        var cySum = 0f
        vertices.forEachIndexed { idx, pt ->
          val px = pt.x * size.width
          val py = pt.y * size.height
          cxSum += px
          cySum += py
          if (idx == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
        }
        polyPath.close()

        drawPath(
          path = polyPath,
          color = Color(0xFF1E7E34).copy(alpha = 0.30f),
        )
        drawPath(
          path = polyPath,
          color = Color(0xFF38B000),
          style =
            Stroke(
              width = 3.5.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f),
            ),
        )

        val centroid = Offset(cxSum / vertices.size, cySum / vertices.size)
        val wellOffset = Offset(wellPt.x * size.width, wellPt.y * size.height)
        drawLine(
          color = OasisAmberDim,
          start = wellOffset,
          end = centroid,
          strokeWidth = 2.8.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
        )

        vertices.forEach { pt ->
          val nodeOffset = Offset(pt.x * size.width, pt.y * size.height)
          drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = nodeOffset,
          )
          drawCircle(
            color = Color(0xFF1E7E34),
            radius = 7.dp.toPx(),
            center = nodeOffset,
            style = Stroke(width = 3.dp.toPx()),
          )
        }
      }
    }

    // Floating Borehole (Forage) Marker
    val wellXPx = (wellPt.x * widthPx).roundToInt()
    val wellYPx = (wellPt.y * heightPx).roundToInt()
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
        Modifier.offset {
          IntOffset(
            x =
              (wellXPx - with(density) { 64.dp.toPx() }.roundToInt()).coerceIn(
                8,
                (widthPx - 140).roundToInt().coerceAtLeast(8),
              ),
            y =
              (wellYPx - with(density) { 28.dp.toPx() }.roundToInt()).coerceIn(
                60,
                (heightPx - 95).roundToInt().coerceAtLeast(60),
              ),
          )
        },
    ) {
      Box(
        modifier =
          Modifier.size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .border(3.dp, Color.White.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.WaterDrop,
          contentDescription = parcel.wellDepthLabel,
          tint = Color.White,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.height(3.dp))
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.90f),
      ) {
        Text(
          text = parcel.wellDepthLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.inverseOnSurface,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          maxLines = 1,
        )
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
        text = "${mode.labelAr} (${mode.labelFr})",
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

private fun DrawScope.drawTolgaOasisSatelliteTerrain() {
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

  val labelPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.argb(225, 245, 250, 245)
      textSize = 11.sp.toPx()
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
    }
  val labels =
    listOf(
      Triple("Tolga • طولقة", 0.43f, 0.50f),
      Triple("Foughala • فوغالة", 0.22f, 0.53f),
      Triple("Lichana • ليشانة", 0.58f, 0.49f),
      Triple("Bouchagroune", 0.75f, 0.47f),
      Triple("Bordj Ben Azzouz", 0.36f, 0.72f),
    )
  labels.forEach { (text, rx, ry) ->
    drawContext.canvas.nativeCanvas.drawText(text, size.width * rx, size.height * ry, labelPaint)
  }
}

private fun DrawScope.drawReliefTopographyTerrain() {
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
  val elevPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      textSize = 11.sp.toPx()
      typeface = Typeface.MONOSPACE
    }
  drawContext.canvas.nativeCanvas.drawText("ALT 148m • حوض وادي جدي", size.width * 0.34f, size.height * 0.30f, elevPaint)
  drawContext.canvas.nativeCanvas.drawText("ALT 132m • سهل الواحة", size.width * 0.44f, size.height * 0.62f, elevPaint)
}

private fun DrawScope.drawCadastreLotGrid(activeIlot: String) {
  drawRect(color = Color(0xFF1B2B20), size = size)
  val cols = 5
  val rows = 4
  val cellW = size.width / cols
  val cellH = size.height / rows
  val gridStroke = Stroke(width = 1.2.dp.toPx())
  val lotPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.argb(180, 185, 235, 195)
      textSize = 10.sp.toPx()
      typeface = Typeface.MONOSPACE
    }
  for (c in 0 until cols) {
    for (r in 0 until rows) {
      val left = c * cellW
      val top = r * cellH
      drawRect(
        color = Color(0xFF7EDB85).copy(alpha = 0.28f),
        topLeft = Offset(left, top),
        size = Size(cellW, cellH),
        style = gridStroke,
      )
      val code = if (c == 2 && r == 1) activeIlot else "P-${10 + r * cols + c}"
      drawContext.canvas.nativeCanvas.drawText(code, left + 10f, top + 24f, lotPaint)
    }
  }
}
