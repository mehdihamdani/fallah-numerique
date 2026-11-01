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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.offline.SavedPlotWithCoordinates
import com.example.ui.theme.PlusJakartaSansFamily
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Displays plots saved locally in the on-device Room database (`saved_plots` +
 * `saved_plot_coordinates`), showing custom plot names, area measurements (`Ha`, `m²`, perimeter),
 * and saved WGS84 coordinates with Load, Rename, and Delete controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalSavedPlotsSection(
  savedPlots: List<SavedPlotWithCoordinates>,
  activeLocalPlotId: Long = 0L,
  onLoadPlot: (SavedPlotWithCoordinates) -> Unit,
  onRenamePlot: (Long, String) -> Unit,
  onDeletePlot: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  var plotBeingRenamed by remember { mutableStateOf<SavedPlotWithCoordinates?>(null) }
  var renameText by remember { mutableStateOf("") }

  Column(
    modifier = modifier.fillMaxWidth().testTag("local_saved_plots_section"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = Icons.Filled.Storage,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = stringResource(R.string.local_saved_plots_title),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.testTag("local_saved_plots_count_badge"),
      ) {
        Text(
          text = "${savedPlots.size} Room DB",
          fontFamily = PlusJakartaSansFamily,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    if (savedPlots.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().testTag("local_saved_plots_empty_state"),
      ) {
        Text(
          text = stringResource(R.string.local_saved_plots_empty),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(12.dp),
        )
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        savedPlots.take(6).forEach { savedItem ->
          val plot = savedItem.plot
          val isSelected = activeLocalPlotId > 0L && activeLocalPlotId == plot.localId
          val sqMetersFormatted =
            NumberFormat.getIntegerInstance(Locale.US).format(plot.areaSquareMeters.roundToInt())

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier =
              Modifier.fillMaxWidth()
                .border(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(12.dp),
                )
                .testTag("local_saved_plot_card_${plot.localId}"),
          ) {
            FlowRow(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Text(
                  text = plot.plotName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text =
                    "${String.format(Locale.US, "%.2f", plot.areaHectares)} Ha ($sqMetersFormatted m²) • ${String.format(Locale.US, "%.1f", plot.perimeterMeters)} m",
                  fontFamily = PlusJakartaSansFamily,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "${savedItem.orderedCoordinates.size} نقاط WGS84 • ${plot.coordinatesLabel}",
                  fontFamily = PlusJakartaSansFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                FilledTonalButton(
                  onClick = { onLoadPlot(savedItem) },
                  shape = RoundedCornerShape(10.dp),
                  modifier =
                    Modifier.height(48.dp).testTag("load_local_plot_button_${plot.localId}"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = stringResource(R.string.local_plot_load_btn),
                    style = MaterialTheme.typography.labelMedium,
                  )
                }

                IconButton(
                  onClick = {
                    plotBeingRenamed = savedItem
                    renameText = plot.plotName
                  },
                  modifier =
                    Modifier.size(48.dp).testTag("rename_local_plot_button_${plot.localId}"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.local_plot_rename_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                  )
                }

                IconButton(
                  onClick = { onDeletePlot(plot.localId) },
                  modifier =
                    Modifier.size(48.dp).testTag("delete_local_plot_button_${plot.localId}"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.local_plot_delete_desc),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(19.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  val targetRename = plotBeingRenamed
  if (targetRename != null) {
    AlertDialog(
      onDismissRequest = { plotBeingRenamed = null },
      title = { Text(stringResource(R.string.local_plot_rename_desc)) },
      text = {
        Box(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = renameText,
            onValueChange = { renameText = it.take(120) },
            singleLine = true,
            label = { Text(stringResource(R.string.osmdroid_plot_name_label)) },
            modifier = Modifier.fillMaxWidth().testTag("local_plot_rename_input"),
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val clean = renameText.trim()
            if (clean.isNotEmpty()) {
              onRenamePlot(targetRename.plot.localId, clean)
            }
            plotBeingRenamed = null
          },
          modifier = Modifier.height(48.dp).testTag("local_plot_rename_confirm_button"),
        ) {
          Text("حفظ الاسم")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { plotBeingRenamed = null },
          modifier = Modifier.height(48.dp),
        ) {
          Text("إلغاء")
        }
      },
    )
  }
}
