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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
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
import androidx.compose.material3.Switch
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.MapLayerMode
import com.example.data.model.RoleSpecificProfile
import com.example.data.model.UserRole
import com.example.util.RoleStrings

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
  isEffectiveOffline: Boolean = false,
  isManualOfflineMode: Boolean = false,
  downloadedRegionsCount: Int = 0,
  downloadedRegionsMb: Double = 0.0,
  pendingWritesCount: Int = 0,
  onToggleManualOffline: (Boolean) -> Unit = {},
  onOpenOfflineMapsManager: () -> Unit = {},
  onSyncPendingWrites: () -> Unit = {},
  onClearOfflineCache: () -> Unit = {},
  assignedRoles: List<UserRole> = listOf(UserRole.FARMER),
  activeRole: UserRole = UserRole.FARMER,
  roleProfiles: Map<String, RoleSpecificProfile> = emptyMap(),
  onSwitchActiveRole: (UserRole) -> Unit = {},
  onAddRoles: (List<String>) -> Unit = {},
  onRemoveRole: (String) -> Unit = {},
  onSaveRoleProfile: (RoleSpecificProfile) -> Unit = {},
) {
  BackHandler(onBack = onBackToDelineation)

  var operatorInput by remember(operatorName) { mutableStateOf(operatorName) }
  var wilayaInput by remember(defaultWilaya) { mutableStateOf(defaultWilaya) }
  var showAddRoleDialog by remember { mutableStateOf(false) }
  var editingRoleProfileTarget by remember { mutableStateOf<UserRole?>(null) }

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

    // 1B. Multi-Role Activities & Role-Specific Profiles Management Card
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
          )
          .testTag("settings_multi_role_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                RoleStrings.tr(
                  currentLanguage,
                  ar = "إدارة الأنشطة المهنية وتعدد الأدوار",
                  fr = "Mes Activités Professionnelles & Rôles",
                  en = "Professional Activities & Multi-Role Settings",
                ),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                RoleStrings.tr(
                  currentLanguage,
                  ar = "بدّل نشاطك الفعال بنقرة واحدة، أضف أنشطة جديدة، أو حدّث ملف كل نشاط مع الحفاظ على سجلاتك.",
                  fr = "Basculez de rôle actif, ajoutez des activités ou complétez vos fiches métier.",
                  en = "Switch active role, add activities, or update role-specific profiles safely.",
                ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (assignedRoles.size < UserRole.entries.size) {
            Button(
              onClick = { showAddRoleDialog = true },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("settings_add_role_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(RoleStrings.tr(currentLanguage, "إضافة نشاط", "Ajouter", "Add Role"))
            }
          }
        }

        assignedRoles.forEach { role ->
          val isActive = role == activeRole
          val profile = roleProfiles[role.code]
          Surface(
            shape = RoundedCornerShape(12.dp),
            color =
              if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
              else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier =
              Modifier.fillMaxWidth()
                .border(
                  width = if (isActive) 1.5.dp else 1.dp,
                  color =
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                  shape = RoundedCornerShape(12.dp),
                )
                .testTag("settings_assigned_role_${role.code}"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    imageVector = RoleStrings.iconForRole(role),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Column {
                    Text(
                      text = role.localizedTitle(currentLanguage),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text =
                        if (profile?.isProfileComplete == true) {
                          "${profile.businessName} • ${profile.wilaya}"
                        } else {
                          RoleStrings.tr(
                            currentLanguage,
                            ar = "الملف المهني غير مكتمل بعد",
                            fr = "Profil métier à compléter",
                            en = "Role profile incomplete",
                          )
                        },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                if (isActive) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                  ) {
                    Text(
                      text = RoleStrings.tr(currentLanguage, "النشاط الفعال", "Actif", "Active"),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimary,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                if (!isActive) {
                  Button(
                    onClick = { onSwitchActiveRole(role) },
                    shape = RoundedCornerShape(10.dp),
                    modifier =
                      Modifier.height(48.dp)
                        .testTag("settings_switch_to_${role.code}"),
                  ) {
                    Icon(
                      Icons.Filled.SwapHoriz,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(RoleStrings.tr(currentLanguage, "تفعيل هذا النشاط", "Activer", "Switch Active"))
                  }
                }

                OutlinedButton(
                  onClick = { editingRoleProfileTarget = role },
                  shape = RoundedCornerShape(10.dp),
                  modifier =
                    Modifier.height(48.dp)
                      .testTag("settings_edit_profile_${role.code}"),
                ) {
                  Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(RoleStrings.tr(currentLanguage, "البطاقة المهنية", "Profil métier", "Edit Profile"))
                }

                if (assignedRoles.size > 1) {
                  OutlinedButton(
                    onClick = { onRemoveRole(role.code) },
                    shape = RoundedCornerShape(10.dp),
                    modifier =
                      Modifier.height(48.dp)
                        .testTag("settings_remove_role_${role.code}"),
                  ) {
                    Text(
                      text = RoleStrings.tr(currentLanguage, "إزالة النشاط", "Retirer", "Remove"),
                      color = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
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

    // 3B. Google Maps & Offline Field Mode Management Card
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
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector =
                if (isEffectiveOffline) Icons.Filled.CloudOff else Icons.Filled.CloudDownload,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = "خرائط القمر الصناعي التجريبية (بدون مفتاح) ووضع أوفلاين",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "المناطق المحملة: $downloadedRegionsCount (${"%.1f".format(java.util.Locale.US, downloadedRegionsMb)} MB) • تعديلات قيد الانتظار: $pendingWritesCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = isManualOfflineMode,
            onCheckedChange = onToggleManualOffline,
            modifier = Modifier.testTag("settings_offline_mode_switch"),
          )
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onOpenOfflineMapsManager,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("settings_open_offline_manager_btn"),
          ) {
            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("إدارة وتنزيل الخرائط أوفلاين", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onSyncPendingWrites,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("settings_sync_offline_btn"),
          ) {
            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("مزامنة السجل المحلي")
          }

          if (downloadedRegionsCount > 0) {
            OutlinedButton(
              onClick = onClearOfflineCache,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("settings_clear_offline_cache_btn"),
            ) {
              Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("مسح الخرائط المحملة")
            }
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

  if (showAddRoleDialog) {
    val unassigned = UserRole.entries.filter { it !in assignedRoles }
    var pickedRoles by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
      onDismissRequest = { showAddRoleDialog = false },
      title = {
        Text(
          RoleStrings.tr(
            currentLanguage,
            ar = "إضافة نشاط مهني جديد للحساب",
            fr = "Ajouter une activité professionnelle",
            en = "Add Professional Activity",
          ),
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          unassigned.forEach { role ->
            val checked = role.code in pickedRoles
            FilterChip(
              selected = checked,
              onClick = {
                pickedRoles =
                  if (checked) pickedRoles - role.code else pickedRoles + role.code
              },
              label = { Text(role.localizedTitle(currentLanguage)) },
              leadingIcon = {
                Icon(
                  imageVector = RoleStrings.iconForRole(role),
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              modifier = Modifier.testTag("add_role_chip_${role.code}"),
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (pickedRoles.isNotEmpty()) {
              onAddRoles(pickedRoles.toList())
            }
            showAddRoleDialog = false
          },
          enabled = pickedRoles.isNotEmpty(),
          modifier = Modifier.height(48.dp).testTag("confirm_add_roles_btn"),
        ) {
          Text(RoleStrings.tr(currentLanguage, "إضافة وتفعيل", "Ajouter", "Add Selected"))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddRoleDialog = false }) {
          Text(RoleStrings.tr(currentLanguage, "إلغاء", "Annuler", "Cancel"))
        }
      },
    )
  }

  val roleToEdit = editingRoleProfileTarget
  if (roleToEdit != null) {
    val existing = roleProfiles[roleToEdit.code]
    var bName by remember(roleToEdit) { mutableStateOf(existing?.businessName ?: operatorName) }
    var rWilaya by remember(roleToEdit) { mutableStateOf(existing?.wilaya ?: defaultWilaya) }
    var rAct by remember(roleToEdit) { mutableStateOf(existing?.activityType.orEmpty()) }
    var rCap by remember(roleToEdit) { mutableStateOf(existing?.capacityOrArea.orEmpty()) }

    AlertDialog(
      onDismissRequest = { editingRoleProfileTarget = null },
      title = {
        Text(
          text = "${roleToEdit.localizedTitle(currentLanguage)} — البطاقة المهنية",
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = bName,
            onValueChange = { bName = it },
            label = { Text(roleToEdit.businessFieldLabel(currentLanguage)) },
            singleLine = true,
          )
          OutlinedTextField(
            value = rWilaya,
            onValueChange = { rWilaya = it },
            label = { Text(RoleStrings.tr(currentLanguage, "الولاية", "Wilaya", "Wilaya")) },
            singleLine = true,
          )
          OutlinedTextField(
            value = rAct,
            onValueChange = { rAct = it },
            label = { Text(roleToEdit.activityFieldLabel(currentLanguage)) },
            singleLine = true,
          )
          OutlinedTextField(
            value = rCap,
            onValueChange = { rCap = it },
            label = { Text(roleToEdit.capacityFieldLabel(currentLanguage)) },
            singleLine = true,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveRoleProfile(
              RoleSpecificProfile(
                roleCode = roleToEdit.code,
                businessName = bName,
                wilaya = rWilaya,
                commune = existing?.commune.orEmpty(),
                activityType = rAct,
                capacityOrArea = rCap,
                qualificationsOrDetails = existing?.qualificationsOrDetails.orEmpty(),
              )
            )
            editingRoleProfileTarget = null
          },
          modifier = Modifier.height(48.dp).testTag("save_role_profile_dialog_btn"),
        ) {
          Text(RoleStrings.tr(currentLanguage, "حفظ البطاقة", "Enregistrer", "Save"))
        }
      },
      dismissButton = {
        TextButton(onClick = { editingRoleProfileTarget = null }) {
          Text(RoleStrings.tr(currentLanguage, "إغلاق", "Fermer", "Close"))
        }
      },
    )
  }
}
