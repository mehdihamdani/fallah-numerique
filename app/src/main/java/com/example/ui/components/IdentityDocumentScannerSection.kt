package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ai.DocumentOcrScanner
import com.example.data.ai.FirebaseAiDocumentScanner
import com.example.data.model.AppLanguage
import com.example.data.model.IdentityVerificationData
import com.example.util.RoleStrings
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ScanDocumentTarget {
  NATIONAL_ID,
  FARMER_CARD,
}

/**
 * Reusable registration & settings card for scanning:
 * 1. **بطاقة التعريف الوطنية (National Identity Card — CNI)**
 * 2. **بطاقة الفلاح — إن وجدت (Chamber of Agriculture Farmer Card — Optional)**
 *
 * Uses runtime `CAMERA` permission + `TakePicturePreview` AND zero-permission Android Photo Picker
 * (`ActivityResultContracts.PickVisualMedia`), backed by multimodal Gemini Vision OCR.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentityDocumentScannerSection(
  language: AppLanguage,
  identityData: IdentityVerificationData,
  onIdentityDataChanged: (IdentityVerificationData) -> Unit,
  modifier: Modifier = Modifier,
  documentScanner: DocumentOcrScanner = FirebaseAiDocumentScanner,
  onFarmerCardAutoPopulate: (wilaya: String, farmName: String, sector: String) -> Unit = { _, _, _ -> },
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var nationalIdPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var farmerCardPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

  var isScanningNationalId by remember { mutableStateOf(false) }
  var isScanningFarmerCard by remember { mutableStateOf(false) }

  var nationalIdError by remember { mutableStateOf<String?>(null) }
  var farmerCardError by remember { mutableStateOf<String?>(null) }
  var cameraPermissionTarget by remember { mutableStateOf<ScanDocumentTarget?>(null) }

  fun processNationalIdBitmap(bitmap: Bitmap) {
    nationalIdPreviewBitmap = bitmap
    nationalIdError = null
    isScanningNationalId = true
    scope.launch {
      try {
        val result = documentScanner.scanNationalIdCard(bitmap)
        isScanningNationalId = false
        result
          .onSuccess { extracted ->
            if (!extracted.isDocumentDetected && extracted.nationalIdNumber.isBlank() && extracted.fullName.isBlank()) {
              nationalIdError =
                RoleStrings.tr(
                  language,
                  ar = "لم يتم التعرف على بيانات واضحة في البطاقة. يرجى إعادة المسح بإضاءة أوضح أو تأكيد الرقم يدويًا.",
                  fr = "Texte illisible sur la carte. Réessayez avec un meilleur éclairage ou saisissez le numéro.",
                  en = "Could not read clear fields on the card. Retake in good lighting or enter manually.",
                )
            } else {
              onIdentityDataChanged(
                identityData.copy(
                  nationalIdScanned = true,
                  nationalIdNumber =
                    extracted.nationalIdNumber.ifBlank { identityData.nationalIdNumber },
                  nationalIdHolderName =
                    extracted.fullName.ifBlank { identityData.nationalIdHolderName },
                )
              )
            }
          }
          .onFailure { err ->
            nationalIdError =
              err.localizedMessage
                ?: RoleStrings.tr(
                  language,
                  ar = "تعذر مسح بطاقة التعريف تلقائيًا. يمكنك إدخال البيانات يدويًا.",
                  fr = "Échec de la lecture automatique de la CNI. Vous pouvez saisir les champs manuellement.",
                  en = "Automatic ID scan failed. You can enter the details manually.",
                )
          }
      } catch (e: CancellationException) {
        throw e
      }
    }
  }

  fun processFarmerCardBitmap(bitmap: Bitmap) {
    farmerCardPreviewBitmap = bitmap
    farmerCardError = null
    isScanningFarmerCard = true
    scope.launch {
      try {
        val result = documentScanner.scanFarmerCard(bitmap)
        isScanningFarmerCard = false
        result
          .onSuccess { extracted ->
            if (!extracted.isDocumentDetected && extracted.farmerCardNumber.isBlank()) {
              farmerCardError =
                RoleStrings.tr(
                  language,
                  ar = "لم يتم التعرف بوضوح على بطاقة الفلاح. يرجى إعادة التصوير أو إدخال رقم البطاقة يدويًا.",
                  fr = "Carte d'agriculteur illisible. Réessayez ou saisissez le numéro manuellement.",
                  en = "Could not clearly read Farmer Card. Retake photo or enter card number manually.",
                )
            } else {
              val nextData =
                identityData.copy(
                  farmerCardScanned = true,
                  farmerCardNumber =
                    extracted.farmerCardNumber.ifBlank { identityData.farmerCardNumber },
                  farmerCardWilaya =
                    extracted.chamberWilaya.ifBlank { identityData.farmerCardWilaya },
                  nationalIdHolderName =
                    identityData.nationalIdHolderName.ifBlank { extracted.holderName },
                  detectedFarmName = extracted.farmOrExploitationName,
                  detectedSector = extracted.agriculturalSector,
                )
              onIdentityDataChanged(nextData)
              onFarmerCardAutoPopulate(
                extracted.chamberWilaya,
                extracted.farmOrExploitationName.ifBlank { extracted.holderName },
                extracted.agriculturalSector,
              )
            }
          }
          .onFailure { err ->
            farmerCardError =
              err.localizedMessage
                ?: RoleStrings.tr(
                  language,
                  ar = "تعذر مسح بطاقة الفلاح تلقائيًا. يمكنك إدخال رقم البطاقة يدويًا.",
                  fr = "Échec de la lecture automatique de la carte d'agriculteur.",
                  en = "Automatic Farmer Card scan failed. You can enter the card number manually.",
                )
          }
      } catch (e: CancellationException) {
        throw e
      }
    }
  }

  val takeNationalIdPictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
      if (bitmap != null) {
        processNationalIdBitmap(bitmap)
      }
    }

  val takeFarmerCardPictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
      if (bitmap != null) {
        processFarmerCardBitmap(bitmap)
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val target = cameraPermissionTarget
      cameraPermissionTarget = null
      if (granted && target != null) {
        runCatching {
          when (target) {
            ScanDocumentTarget.NATIONAL_ID -> takeNationalIdPictureLauncher.launch(null)
            ScanDocumentTarget.FARMER_CARD -> takeFarmerCardPictureLauncher.launch(null)
          }
        }
      } else if (!granted && target != null) {
        val msg = context.getString(R.string.camera_permission_denied_msg)
        when (target) {
          ScanDocumentTarget.NATIONAL_ID -> nationalIdError = msg
          ScanDocumentTarget.FARMER_CARD -> farmerCardError = msg
        }
      }
    }

  val pickNationalIdMediaLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      if (uri != null) {
        scope.launch {
          val bmp = decodeSampledBitmapFromUri(context, uri)
          if (bmp != null) {
            processNationalIdBitmap(bmp)
          } else {
            nationalIdError =
              RoleStrings.tr(
                language,
                ar = "تعذرت قراءة الصورة المختارة.",
                fr = "Impossible de lire l'image sélectionnée.",
                en = "Unable to decode selected image.",
              )
          }
        }
      }
    }

  val pickFarmerCardMediaLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      if (uri != null) {
        scope.launch {
          val bmp = decodeSampledBitmapFromUri(context, uri)
          if (bmp != null) {
            processFarmerCardBitmap(bmp)
          } else {
            farmerCardError =
              RoleStrings.tr(
                language,
                ar = "تعذرت قراءة صورة بطاقة الفلاح المختارة.",
                fr = "Impossible de lire l'image sélectionnée.",
                en = "Unable to decode selected image.",
              )
          }
        }
      }
    }

  fun launchCameraWithPermission(target: ScanDocumentTarget) {
    val hasPerm =
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    if (hasPerm) {
      runCatching {
        when (target) {
          ScanDocumentTarget.NATIONAL_ID -> takeNationalIdPictureLauncher.launch(null)
          ScanDocumentTarget.FARMER_CARD -> takeFarmerCardPictureLauncher.launch(null)
        }
      }
    } else {
      cameraPermissionTarget = target
      runCatching { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
    }
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(20.dp),
        )
        .testTag("identity_document_scanner_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.VerifiedUser,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              RoleStrings.tr(
                language,
                ar = stringResource(R.string.id_scan_section_title),
                fr = "Scan de la Carte d'Identité & Carte d'Agriculteur",
                en = "National ID & Optional Farmer Card Scan",
              ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              RoleStrings.tr(
                language,
                ar = "امسح بطاقة التعريف الوطنية وبطاقة الفلاح (إن وجدت) للتعبئة الذكية وتوثيق حسابك بأمان.",
                fr = "Scannez votre CNI et votre Carte d'Agriculteur (si disponible) pour pré-remplir et sécuriser votre compte.",
                en = "Scan your National ID Card and Farmer Card (if available) for smart auto-fill and secure verification.",
              ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

      // -----------------------------------------------------------------------
      // 1. National Identity Card (بطاقة التعريف الوطنية - CNI)
      // -----------------------------------------------------------------------
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("national_id_scanner_box"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
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
                imageVector = Icons.Filled.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "مسح بطاقة التعريف الوطنية",
                    fr = "Carte Nationale d'Identité (CNI)",
                    en = "National Identity Card (CNI)",
                  ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }

            if (identityData.hasNationalId) {
              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("national_id_verified_badge"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = RoleStrings.tr(language, "تم الإرفاق", "Vérifiée", "Attached"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { launchCameraWithPermission(ScanDocumentTarget.NATIONAL_ID) },
              enabled = !isScanningNationalId,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("scan_national_id_camera_button"),
            ) {
              Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                RoleStrings.tr(
                  language,
                  ar = "مسح بالكاميرا",
                  fr = "Scanner (Caméra)",
                  en = "Scan with Camera",
                )
              )
            }

            OutlinedButton(
              onClick = {
                runCatching {
                  pickNationalIdMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
              },
              enabled = !isScanningNationalId,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("scan_national_id_gallery_button"),
            ) {
              Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                RoleStrings.tr(
                  language,
                  ar = "استيراد صورة البطاقة",
                  fr = "Choisir une photo",
                  en = "Import Card Photo",
                )
              )
            }

            if (nationalIdPreviewBitmap != null || identityData.hasNationalId) {
              IconButton(
                onClick = {
                  nationalIdPreviewBitmap = null
                  nationalIdError = null
                  onIdentityDataChanged(
                    identityData.copy(
                      nationalIdScanned = false,
                      nationalIdNumber = "",
                      nationalIdHolderName = "",
                    )
                  )
                },
                modifier = Modifier.size(48.dp).testTag("clear_national_id_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription =
                    RoleStrings.tr(
                      language,
                      ar = "مسح بيانات بطاقة التعريف",
                      fr = "Effacer la CNI",
                      en = "Clear ID Card",
                    ),
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }

          AnimatedVisibility(visible = isScanningNationalId) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(vertical = 4.dp).testTag("national_id_scanning_indicator"),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "جارٍ قراءة بطاقة التعريف الوطنية بالذكاء الاصطناعي...",
                    fr = "Analyse optique de la carte d'identité en cours…",
                    en = "Extracting National ID details…",
                  ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          nationalIdPreviewBitmap?.let { bmp ->
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = stringResource(R.string.national_id_card_title),
              contentScale = ContentScale.Crop,
              modifier =
                Modifier.fillMaxWidth()
                  .height(120.dp)
                  .clip(RoundedCornerShape(10.dp)),
            )
          }

          if (!nationalIdError.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth().testTag("national_id_scan_error"),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = nationalIdError.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
            }
          }

          OutlinedTextField(
            value = identityData.nationalIdNumber,
            onValueChange = { value ->
              onIdentityDataChanged(
                identityData.copy(
                  nationalIdNumber = value.take(40),
                  nationalIdScanned = identityData.nationalIdScanned || value.isNotBlank(),
                )
              )
            },
            label = {
              Text(
                RoleStrings.tr(
                  language,
                  ar = "رقم بطاقة التعريف الوطنية (NIN)",
                  fr = "Numéro de la Carte Nationale d'Identité (NIN)",
                  en = "National ID Number (NIN)",
                )
              )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("national_id_number_input"),
          )

          OutlinedTextField(
            value = identityData.nationalIdHolderName,
            onValueChange = { value ->
              onIdentityDataChanged(identityData.copy(nationalIdHolderName = value.take(120)))
            },
            label = {
              Text(
                RoleStrings.tr(
                  language,
                  ar = "الاسم واللقب كما في بطاقة التعريف",
                  fr = "Nom et prénom sur la CNI",
                  en = "Full Name on National ID",
                )
              )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("national_id_holder_name_input"),
          )
        }
      }

      // -----------------------------------------------------------------------
      // 2. Farmer Card — Optional (مسح بطاقة الفلاح — إن وجدت)
      // -----------------------------------------------------------------------
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("farmer_card_scanner_box"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
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
                imageVector = Icons.Filled.Agriculture,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Column {
                Text(
                  text =
                    RoleStrings.tr(
                      language,
                      ar = "مسح بطاقة الفلاح (إن وجدت)",
                      fr = "Carte d'Agriculteur (Si disponible)",
                      en = "Farmer Card (If Available)",
                    ),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    RoleStrings.tr(
                      language,
                      ar = "اختياري للفلاحين والتعاونيات المسجلين لدى الغرفة الفلاحية",
                      fr = "Optionnel — Pour les exploitants inscrits à la Chambre d'Agriculture",
                      en = "Optional — For operators holding a Chamber of Agriculture card",
                    ),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(50),
              color =
                if (identityData.hasFarmerCard) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.testTag("farmer_card_status_badge"),
            ) {
              Text(
                text =
                  if (identityData.hasFarmerCard) {
                    RoleStrings.tr(language, "بطاقة فلاح موثقة", "Carte liée", "Card Linked")
                  } else {
                    RoleStrings.tr(language, "اختياري • إن وجدت", "Optionnel", "Optional")
                  },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color =
                  if (identityData.hasFarmerCard) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { launchCameraWithPermission(ScanDocumentTarget.FARMER_CARD) },
              enabled = !isScanningFarmerCard,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("scan_farmer_card_camera_button"),
            ) {
              Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                RoleStrings.tr(
                  language,
                  ar = "مسح بطاقة الفلاح",
                  fr = "Scanner Carte Fellah",
                  en = "Scan Farmer Card",
                )
              )
            }

            OutlinedButton(
              onClick = {
                runCatching {
                  pickFarmerCardMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
              },
              enabled = !isScanningFarmerCard,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("scan_farmer_card_gallery_button"),
            ) {
              Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                RoleStrings.tr(
                  language,
                  ar = "استيراد صورة بطاقة الفلاح",
                  fr = "Importer une photo",
                  en = "Import Farmer Card",
                )
              )
            }

            if (farmerCardPreviewBitmap != null || identityData.hasFarmerCard) {
              IconButton(
                onClick = {
                  farmerCardPreviewBitmap = null
                  farmerCardError = null
                  onIdentityDataChanged(
                    identityData.copy(
                      farmerCardScanned = false,
                      farmerCardNumber = "",
                      farmerCardWilaya = "",
                    )
                  )
                },
                modifier = Modifier.size(48.dp).testTag("clear_farmer_card_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription =
                    RoleStrings.tr(
                      language,
                      ar = "إزالة بطاقة الفلاح",
                      fr = "Effacer Carte Fellah",
                      en = "Clear Farmer Card",
                    ),
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }

          AnimatedVisibility(visible = isScanningFarmerCard) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(vertical = 4.dp).testTag("farmer_card_scanning_indicator"),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "جارٍ استخراج بيانات بطاقة الفلاح والغرفة الفلاحية...",
                    fr = "Analyse de la Carte d'Agriculteur en cours…",
                    en = "Extracting Farmer Card details…",
                  ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          farmerCardPreviewBitmap?.let { bmp ->
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = stringResource(R.string.farmer_card_optional_title),
              contentScale = ContentScale.Crop,
              modifier =
                Modifier.fillMaxWidth()
                  .height(120.dp)
                  .clip(RoundedCornerShape(10.dp)),
            )
          }

          if (!farmerCardError.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth().testTag("farmer_card_scan_error"),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = farmerCardError.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
            }
          }

          OutlinedTextField(
            value = identityData.farmerCardNumber,
            onValueChange = { value ->
              onIdentityDataChanged(
                identityData.copy(
                  farmerCardNumber = value.take(60),
                  farmerCardScanned = identityData.farmerCardScanned || value.isNotBlank(),
                )
              )
            },
            label = {
              Text(
                RoleStrings.tr(
                  language,
                  ar = "رقم بطاقة الفلاح (إن وجدت — اختياري)",
                  fr = "N° Carte d'Agriculteur (optionnel)",
                  en = "Farmer Card Number (optional)",
                )
              )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("farmer_card_number_input"),
          )

          OutlinedTextField(
            value = identityData.farmerCardWilaya,
            onValueChange = { value ->
              onIdentityDataChanged(identityData.copy(farmerCardWilaya = value.take(80)))
            },
            label = {
              Text(
                RoleStrings.tr(
                  language,
                  ar = "الغرفة الفلاحية بالولاية (اختياري)",
                  fr = "Chambre d'Agriculture — Wilaya (optionnel)",
                  en = "Chamber of Agriculture Wilaya (optional)",
                )
              )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("farmer_card_wilaya_input"),
          )
        }
      }
    }
  }
}

private suspend fun decodeSampledBitmapFromUri(context: Context, uri: Uri): Bitmap? =
  withContext(Dispatchers.IO) {
    runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var sampleSize = 1
        val maxDim = 1280
        while (
          boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim
        ) {
          sampleSize *= 2
        }
        val decodeOpts =
          BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
      }
      .getOrNull()
  }
