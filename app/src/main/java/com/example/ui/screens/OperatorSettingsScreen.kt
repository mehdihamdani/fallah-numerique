package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.MapLayerMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperatorSettingsScreen(
  userDisplayName: String,
  userEmail: String,
  operatorName: String,
  defaultWilaya: String,
  currentLanguage: AppLanguage,
  currentMapLayer: MapLayerMode,
  savedParcelsCount: Int,
  onSavePreferences: (String, String) -> Unit,
  onLanguageSelected: (AppLanguage) -> Unit,
  onMapLayerSelected: (MapLayerMode) -> Unit,
  onOpenMyParcels: () -> Unit,
  onExportCurrentParcelPdf: () -> Unit,
  onSignOut: () -> Unit,
  onBackToDelineation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackToDelineation)

  var operatorInput by remember(operatorName) { mutableStateOf(operatorName) }
  var wilayaInput by remember(defaultWilaya) { mutableStateOf(defaultWilaya) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column {
      Text(
        text = stringResource(R.string.settings_screen_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.settings_screen_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // 1. Authenticated Operator Identity Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp),
          ),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            modifier =
              Modifier.size(56.dp)
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  RoundedCornerShape(14.dp),
                ),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEFEFE),
          ) {
            Image(
              painter = painterResource(R.drawable.img_fellah_logo),
              contentDescription = stringResource(R.string.app_name),
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize().padding(4.dp),
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = userDisplayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = userEmail,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = "حساب موثق عبر Google • مزامنة سحابية نشطة",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    // 2. Farm / Cooperative Profile Settings
    Card(
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp),
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "بيانات المستثمرة الفلاحية والتعاونية",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
          value = operatorInput,
          onValueChange = { operatorInput = it },
          label = { Text("اسم الفلاح / التعاونية الفلاحية") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("settings_operator_name_input"),
        )
        OutlinedTextField(
          value = wilayaInput,
          onValueChange = { wilayaInput = it },
          label = { Text("الولاية الفلاحية الافتراضية") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("settings_default_wilaya_input"),
        )
        Button(
          onClick = { onSavePreferences(operatorInput, wilayaInput) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(48.dp).testTag("settings_save_profile_button"),
        ) {
          Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("حفظ بيانات المستثمرة", fontWeight = FontWeight.Bold)
        }
      }
    }

    // 3. Language & Map Layer Preferences
    Card(
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp),
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = "لغة الواجهة (Langue de l'application)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            AppLanguage.AR to "العربية (AR)",
            AppLanguage.FR to "Français (FR)",
            AppLanguage.EN to "English (EN)",
          ).forEach { (lang, label) ->
            FilterChip(
              selected = currentLanguage == lang,
              onClick = { onLanguageSelected(lang) },
              label = { Text(label) },
              leadingIcon =
                if (currentLanguage == lang) {
                  {
                    Icon(
                      Icons.Filled.CheckCircle,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                } else null,
              modifier = Modifier.testTag("settings_lang_${lang.code}"),
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Filled.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = "طبقة الخريطة الفضائية الافتراضية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MapLayerMode.entries.forEach { mode ->
            FilterChip(
              selected = currentMapLayer == mode,
              onClick = { onMapLayerSelected(mode) },
              label = { Text("${mode.labelAr} (${mode.labelFr})") },
            )
          }
        }
      }
    }

    // 4. Saved Registry, PDF Export & Sign Out
    Card(
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp),
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "إدارة السجل الفلاحي والجلسة",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onOpenMyParcels,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("settings_open_parcels_button"),
          ) {
            Icon(Icons.Filled.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("سجل القطع المحفوظة ($savedParcelsCount)")
          }

          OutlinedButton(
            onClick = onExportCurrentParcelPdf,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("settings_export_pdf_button"),
          ) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تصدير بطاقة السجل PDF")
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Button(
          onClick = onSignOut,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.sign_out), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
