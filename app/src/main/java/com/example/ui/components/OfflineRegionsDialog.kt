package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.offline.OfflineMapRegionEntity
import com.example.util.GeoCoordinateMapper
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineRegionsManagerDialog(
  currentParcelName: String,
  isEffectiveOffline: Boolean,
  isManualOfflineMode: Boolean,
  isDownloading: Boolean,
  isSyncingQueue: Boolean,
  pendingWritesCount: Int,
  downloadedRegions: List<OfflineMapRegionEntity>,
  onToggleManualOffline: (Boolean) -> Unit,
  onSyncPendingNow: () -> Unit,
  onDownloadCurrentParcel: () -> Unit,
  onDownloadPresetRegion: (GeoCoordinateMapper.OfflineRegionPreset) -> Unit,
  onOpenCachedRegion: (OfflineMapRegionEntity) -> Unit,
  onDeleteRegion: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val totalMb = downloadedRegions.sumOf { it.sizeMb }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color =
            if (isEffectiveOffline) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector =
                if (isEffectiveOffline) Icons.Filled.CloudOff else Icons.Filled.CloudDownload,
              contentDescription = null,
              tint =
                if (isEffectiveOffline) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
        Column {
          Text(
            text = stringResource(R.string.offline_regions_manager_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text =
              "الذاكرة المحلية: ${downloadedRegions.size} مناطق محفوظة (${String.format(Locale.US, "%.1f", totalMb)} MB)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(R.string.offline_regions_manager_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Manual Offline Field Mode Switch + Pending Queue Sync
        Surface(
          shape = RoundedCornerShape(12.dp),
          color =
            if (isEffectiveOffline) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surfaceContainer,
          modifier =
            Modifier.fillMaxWidth()
              .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                RoundedCornerShape(12.dp),
              ),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "وضع العمل الميداني دون اتصال (Mode Hors-Ligne)",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    if (isEffectiveOffline) {
                      "نشط حالياً: قراءة بلاطات القمر الصناعي من الذاكرة المحلية بدون إنترنت"
                    } else {
                      "الوضع التجريبي النشط: خرائط قمر صناعي حية بدون مفتاح API + حفظ تلقائي أوفلاين"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = isManualOfflineMode,
                onCheckedChange = onToggleManualOffline,
                modifier = Modifier.testTag("dialog_offline_mode_switch"),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector =
                    if (pendingWritesCount > 0) Icons.Filled.CloudSync else Icons.Filled.CloudDone,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text =
                    if (pendingWritesCount > 0) {
                      "تعديلات بانتظار المزامنة: $pendingWritesCount"
                    } else {
                      "جميع السجلات المحلية متطابقة مع السحابة"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              if (pendingWritesCount > 0 || isManualOfflineMode) {
                FilledTonalButton(
                  onClick = onSyncPendingNow,
                  enabled = !isSyncingQueue,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(36.dp).testTag("sync_offline_queue_button"),
                ) {
                  if (isSyncingQueue) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                  } else {
                    Text("مزامنة الآن", style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }

        // Download Current Active Parcel Region
        Button(
          onClick = onDownloadCurrentParcel,
          enabled = !isDownloading,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("download_active_parcel_offline_btn"),
        ) {
          if (isDownloading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "تنزيل خريطة القطعة الحالية أوفلاين ($currentParcelName)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Downloaded Regions List
        Text(
          text = "المناطق المحفوظة على الجهاز (${downloadedRegions.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )

        if (downloadedRegions.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "لا توجد خرائط محملة بعد. قم بتنزيل القطعة الحالية أو أحد الأحواض الفلاحية أدناه للعمل بدون إنترنت.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          downloadedRegions.forEach { region ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = region.regionName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      "${region.wilaya} • ${String.format(Locale.US, "%.4f°N, %.4f°E", region.centerLat, region.centerLng)} • ${region.sizeMb} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  OutlinedButton(
                    onClick = { onOpenCachedRegion(region) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                  ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فتح", style = MaterialTheme.typography.labelSmall)
                  }
                  IconButton(
                    onClick = { onDeleteRegion(region.id) },
                    modifier = Modifier.size(40.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = "حذف المنطقة المحفوظة",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Algerian Basin Presets
        Text(
          text = "تنزيل سريع للأقطاب الفلاحية الوطنية (بدون مفتاح API)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )

        GeoCoordinateMapper.ALGERIAN_OFFLINE_PRESETS.forEach { preset ->
          val alreadySaved = downloadedRegions.any { it.id == preset.id }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
          ) {
            FlowRow(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Column {
                  Text(
                    text = preset.regionName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${preset.wilaya} • نصف القطر ${preset.radiusKm} كم (${preset.estimatedSizeMb} MB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              FilledTonalButton(
                onClick = { onDownloadPresetRegion(preset) },
                enabled = !isDownloading,
                shape = RoundedCornerShape(8.dp),
                colors =
                  ButtonDefaults.filledTonalButtonColors(
                    containerColor =
                      if (alreadySaved) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.secondaryContainer
                  ),
                modifier = Modifier.height(36.dp).testTag("download_preset_${preset.id}"),
              ) {
                Text(
                  text = if (alreadySaved) "مُحمّلة ✓ (تحديث)" else "تنزيل أوفلاين",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("إغلاق", fontWeight = FontWeight.Bold)
      }
    },
  )
}
