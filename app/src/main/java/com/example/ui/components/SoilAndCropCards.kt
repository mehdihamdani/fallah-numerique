package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AgriculturalParcel
import com.example.data.model.CropVarietyType
import com.example.data.model.SoilTextureType
import com.example.ui.auth.HexagonShape
import com.example.ui.theme.OasisAmberDim
import com.example.ui.theme.PlusJakartaSansFamily
import java.util.Locale

@Composable
fun SoilCharacteristicsCard(
  parcel: AgriculturalParcel,
  isSlidersExpanded: Boolean,
  onToggleSliders: () -> Unit,
  onSelectTexture: (SoilTextureType) -> Unit,
  onPhChanged: (Float) -> Unit,
  onOrganicMatterChanged: (Float) -> Unit,
  onSalinityChanged: (Float) -> Unit,
  onCycleDrainage: () -> Unit,
  onBrowseLabDocument: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
          RoundedCornerShape(16.dp),
        ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Texture,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Column {
            Text(
              text = stringResource(R.string.soil_card_title),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.soil_card_subtitle),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onToggleSliders,
            modifier = Modifier.size(48.dp).testTag("toggle_soil_sliders_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Tune,
              contentDescription = "تعديل قيم التحليل الفيزيوكيميائي",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = stringResource(R.string.lab_verified_badge),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.soil_texture_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 2x2 Soil Texture Selector Grid
      val textures = SoilTextureType.entries
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowIdx in 0..1) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            for (colIdx in 0..1) {
              val item = textures[rowIdx * 2 + colIdx]
              val isSelected = parcel.soilTexture == item.code
              SoilTextureRadioCard(
                item = item,
                selected = isSelected,
                onClick = { onSelectTexture(item) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Physico-Chemical Metrics Box
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // 1. Soil pH
          val phStateLabel =
            when {
              parcel.soilPh < 6.8 -> "حمضي خفيف / Acide"
              parcel.soilPh <= 7.6 -> "متوازن / Légèrement Basique"
              else -> "قلوي كلسي / Alcalin"
            }
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "درجة الحموضة (pH du Sol)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "${String.format(Locale.US, "%.1f", parcel.soilPh)} ($phStateLabel)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .height(11.dp)
                  .clip(RoundedCornerShape(50))
                  .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
              Box(
                modifier =
                  Modifier.weight(0.30f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
              )
              Box(
                modifier =
                  Modifier.weight(0.45f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
              )
              Box(
                modifier =
                  Modifier.weight(0.25f)
                    .fillMaxHeight()
                    .background(OasisAmberDim)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "6.0 (حمضي)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "7.0 - 7.5 (مثالي)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "8.5 (قلوي)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            AnimatedVisibility(visible = isSlidersExpanded) {
              Slider(
                value = parcel.soilPh.toFloat(),
                onValueChange = onPhChanged,
                valueRange = 5.0f..9.0f,
                modifier = Modifier.testTag("soil_ph_slider"),
              )
            }
          }

          // 2. Organic Matter %
          val omProgress = ((parcel.organicMatterPercent / 4.2).toFloat()).coerceIn(0.05f, 1f)
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "المادة العضوية (Matière Organique)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "${String.format(Locale.US, "%.2f", parcel.organicMatterPercent)}% (${if (parcel.organicMatterPercent >= 2.0) "مستوى جيد" else "يحتاج تسميد عضوي"})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(50))
                  .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(omProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
              )
            }
            AnimatedVisibility(visible = isSlidersExpanded) {
              Slider(
                value = parcel.organicMatterPercent.toFloat(),
                onValueChange = onOrganicMatterChanged,
                valueRange = 0.3f..5.0f,
                modifier = Modifier.testTag("organic_matter_slider"),
              )
            }
          }

          // 3. Salinity / Electrical Conductivity (EC)
          val ecProgress = ((parcel.salinityEc / 5.0).toFloat()).coerceIn(0.05f, 1f)
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "الملوحة والناقلية الكهربائية (Conductivité EC)",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                  imageVector = Icons.Filled.Info,
                  contentDescription = "مؤشر ملوحة التربة بالواحات",
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(15.dp),
                )
              }
              Text(
                text =
                  "${String.format(Locale.US, "%.1f", parcel.salinityEc)} dS/m (${if (parcel.salinityEc <= 2.5) "تحت السيطرة" else "ملوحة مرتفعة"})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(50))
                  .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(ecProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(OasisAmberDim)
              )
            }
            AnimatedVisibility(visible = isSlidersExpanded) {
              Slider(
                value = parcel.salinityEc.toFloat(),
                onValueChange = onSalinityChanged,
                valueRange = 0.4f..6.0f,
                modifier = Modifier.testTag("salinity_ec_slider"),
              )
            }
          }

          // 4. Drainage & Root Depth
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "تصريف المياه والعمق الصالح للجذور:",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onCycleDrainage)
                  .testTag("drainage_cycle_chip"),
            ) {
              Text(
                text = parcel.drainageStatus,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // INRAA / Laboratory Document Upload Banner
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onBrowseLabDocument)
            .testTag("upload_lab_doc_button"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Filled.UploadFile,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp),
            )
            Column {
              Text(
                text = stringResource(R.string.upload_lab_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (parcel.labDocumentName.isNotBlank()) {
                    "المرفق الحالي: ${parcel.labDocumentName}"
                  } else {
                    stringResource(R.string.upload_lab_subtitle)
                  },
                style = MaterialTheme.typography.bodySmall,
                color =
                  if (parcel.labDocumentName.isNotBlank()) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 1.dp,
          ) {
            Text(
              text = stringResource(R.string.browse_btn),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SoilTextureRadioCard(
  item: SoilTextureType,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color =
      if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
      else MaterialTheme.colorScheme.surfaceContainer,
    modifier =
      modifier
        .clip(RoundedCornerShape(12.dp))
        .border(
          width = if (selected) 1.5.dp else 0.dp,
          color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
          shape = RoundedCornerShape(12.dp),
        )
        .clickable(onClick = onClick)
        .testTag("soil_texture_${item.code}"),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = item.titleAr,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        RadioButton(
          selected = selected,
          onClick = null,
          colors =
            RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.size(20.dp),
        )
      }
      Text(
        text = item.titleFr,
        fontFamily = PlusJakartaSansFamily,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = item.descriptionAr,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropsAndCampaignCard(
  parcel: AgriculturalParcel,
  onSelectMainCrop: (CropVarietyType) -> Unit,
  onUpdateIrrigation: (String) -> Unit,
  onUpdateYield: (String) -> Unit,
  onUpdatePlantingYear: (String) -> Unit,
  onUpdateIntercropping: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var yieldInput by
    remember(parcel.id, parcel.mainCrop) {
      mutableStateOf(String.format(Locale.US, "%.1f", parcel.estimatedYieldTonsPerHa))
    }
  var yearInput by remember(parcel.id) { mutableStateOf(parcel.plantingYear) }
  var intercropInput by remember(parcel.id) { mutableStateOf(parcel.intercropping) }
  var irrigationDropdownExpanded by remember { mutableStateOf(false) }

  val irrigationOptions =
    listOf(
      "السقي بالتقطير الحديث (Goutte-à-goutte)",
      "الرش المحوري الذكي (Pivot d'irrigation)",
      "السقي التقليدي بالجاذبية (Séguias / Gravitaire)",
    )

  Card(
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
          RoundedCornerShape(16.dp),
        ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Column {
            Text(
              text = stringResource(R.string.crop_card_title),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.crop_card_subtitle),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
          Text(
            text = stringResource(R.string.campaign_season),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.main_crop_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 2x2 Main Crop Grid
      val crops = CropVarietyType.entries
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowIdx in 0..1) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            for (colIdx in 0..1) {
              val crop = crops[rowIdx * 2 + colIdx]
              val isSelected = parcel.mainCrop == crop.code
              CropOptionTile(
                crop = crop,
                selected = isSelected,
                onClick = { onSelectMainCrop(crop) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Irrigation Dropdown
      Text(
        text = "نظام الري المعتمد (Irrigation):",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      ExposedDropdownMenuBox(
        expanded = irrigationDropdownExpanded,
        onExpandedChange = { irrigationDropdownExpanded = it },
      ) {
        OutlinedTextField(
          value = parcel.irrigationSystem,
          onValueChange = {},
          readOnly = true,
          singleLine = true,
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = irrigationDropdownExpanded)
          },
          shape = RoundedCornerShape(10.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          modifier =
            Modifier.fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .testTag("irrigation_dropdown"),
        )
        ExposedDropdownMenu(
          expanded = irrigationDropdownExpanded,
          onDismissRequest = { irrigationDropdownExpanded = false },
        ) {
          irrigationOptions.forEach { option ->
            DropdownMenuItem(
              text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
              onClick = {
                onUpdateIrrigation(option)
                irrigationDropdownExpanded = false
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Yield + Planting Year Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "الإنتاج المتوقع (Rendement):",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = yieldInput,
            onValueChange = {
              yieldInput = it
              onUpdateYield(it)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = {
              Text(
                text = "T/Ha",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            },
            shape = RoundedCornerShape(10.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
            modifier = Modifier.fillMaxWidth().testTag("yield_input"),
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "سنة الغرس / الاستغلال:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = yearInput,
            onValueChange = {
              yearInput = it
              onUpdatePlantingYear(it)
            },
            singleLine = true,
            trailingIcon = {
              Icon(
                imageVector = Icons.Filled.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            },
            shape = RoundedCornerShape(10.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
            modifier = Modifier.fillMaxWidth().testTag("planting_year_input"),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Intercropping Field
      Text(
        text = "محاصيل بينية مرافقة (Associées):",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = intercropInput,
        onValueChange = {
          intercropInput = it
          onUpdateIntercropping(it)
        },
        singleLine = true,
        trailingIcon = {
          Icon(
            imageVector = Icons.Filled.Layers,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        },
        shape = RoundedCornerShape(10.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        modifier = Modifier.fillMaxWidth().testTag("intercropping_input"),
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Fellah Numérique Biometric Certification Badge
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              modifier =
                Modifier.size(44.dp)
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                  ),
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFEFEFE),
            ) {
              Image(
                painter = painterResource(R.drawable.img_fellah_logo),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(3.dp),
              )
            }
            Column {
              Text(
                text = stringResource(R.string.certification_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.certification_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = "موثق",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CropOptionTile(
  crop: CropVarietyType,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val icon: ImageVector =
    when (crop) {
      CropVarietyType.DEGLET_NOUR -> Icons.Filled.Park
      CropVarietyType.POMME_DE_TERRE -> Icons.Filled.Spa
      CropVarietyType.OLIVIER -> Icons.Filled.Nature
      CropVarietyType.TOMATE_INDUSTRIELLE -> Icons.Filled.Grass
    }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color =
      if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
      else MaterialTheme.colorScheme.surfaceContainer,
    modifier =
      modifier
        .clip(RoundedCornerShape(12.dp))
        .border(
          width = if (selected) 1.5.dp else 0.dp,
          color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
          shape = RoundedCornerShape(12.dp),
        )
        .clickable(onClick = onClick)
        .testTag("crop_option_${crop.code}"),
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
              if (selected) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = crop.titleAr,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = crop.subtitleFr,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (selected) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
