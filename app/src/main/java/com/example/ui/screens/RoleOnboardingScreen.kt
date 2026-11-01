package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.DocumentOcrScanner
import com.example.data.ai.FirebaseAiDocumentScanner
import com.example.data.model.AccountInputValidator
import com.example.data.model.AppLanguage
import com.example.data.model.IdentityVerificationData
import com.example.data.model.RoleSpecificProfile
import com.example.data.model.UserAccountProfile
import com.example.data.model.UserRole
import com.example.ui.components.IdentityDocumentScannerSection
import com.example.util.RoleStrings

/**
 * Progressive, resumable 4-step Multi-Role Onboarding Screen:
 * - Step 1: Welcome & National ID Card + optional Farmer Card (بطاقة الفلاح إن وجدت) OCR scan
 * - Step 2: Select 1..6 professional activities with clear visual states & accessible semantics
 * - Step 3: Default active role selection (auto-selected when 1 role chosen; explicit choice when >1)
 * - Step 4: Lightweight progressive profile setup per selected role (skippable via "Complete Later")
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleOnboardingScreen(
  existingAccount: UserAccountProfile?,
  existingRoleProfiles: Map<String, RoleSpecificProfile>,
  initialSuggestedRole: UserRole? = null,
  language: AppLanguage,
  userDisplayName: String,
  userEmail: String,
  isSaving: Boolean,
  errorMessage: String?,
  onLanguageSelected: (AppLanguage) -> Unit = {},
  onSaveProgressDraft: (selectedRoles: List<String>, defaultRole: String, step: Int) -> Unit,
  onCompleteOnboarding: (
    selectedRoles: List<String>,
    defaultRole: String,
    profilesToSave: List<RoleSpecificProfile>,
  ) -> Unit,
  onSignOut: () -> Unit,
  documentScanner: DocumentOcrScanner = FirebaseAiDocumentScanner,
  onSaveProgressDraftWithIdentity: (
    (
      selectedRoles: List<String>,
      defaultRole: String,
      step: Int,
      identityVerification: IdentityVerificationData,
    ) -> Unit
  )? = null,
  onCompleteOnboardingWithIdentity: (
    (
      selectedRoles: List<String>,
      defaultRole: String,
      profilesToSave: List<RoleSpecificProfile>,
      identityVerification: IdentityVerificationData,
    ) -> Unit
  )? = null,
  userPhoneNumber: String = "",
  isPhoneVerifiedSession: Boolean =
    userPhoneNumber.isNotBlank() || (existingAccount?.phoneVerified == true),
  onSaveAccountDraft: (
    (
      displayName: String,
      email: String,
      phoneNumber: String,
      phoneVerified: Boolean,
      selectedRoles: List<String>,
      defaultRole: String,
      step: Int,
      identityVerification: IdentityVerificationData,
    ) -> Unit
  )? = null,
  onCompleteAccountOnboarding: (
    (
      displayName: String,
      email: String,
      phoneNumber: String,
      phoneVerified: Boolean,
      selectedRoles: List<String>,
      defaultRole: String,
      profilesToSave: List<RoleSpecificProfile>,
      identityVerification: IdentityVerificationData,
    ) -> Unit
  )? = null,
) {
  val layoutDirection =
    if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

  val initialStep = (existingAccount?.onboardingStep ?: 1).coerceIn(1, 4)
  var currentStep by remember(existingAccount?.onboardingStep) { mutableIntStateOf(initialStep) }

  val selectedRoles =
    remember(existingAccount?.assignedRoles, initialSuggestedRole) {
      mutableStateListOf<String>().apply {
        val seed =
          when {
            !existingAccount?.assignedRoles.isNullOrEmpty() -> existingAccount!!.assignedRoles
            initialSuggestedRole != null -> listOf(initialSuggestedRole.code)
            else -> emptyList()
          }
        addAll(UserRole.normalizeRoleCodes(seed))
      }
    }

  var chosenDefaultRole by
    remember(existingAccount?.defaultRole, initialSuggestedRole) {
      mutableStateOf(
        existingAccount?.defaultRole?.takeIf { it.isNotBlank() }
          ?: initialSuggestedRole?.code
          ?: ""
      )
    }

  var fullNameDraft by
    remember(existingAccount?.displayName, userDisplayName) {
      mutableStateOf(
        existingAccount?.displayName?.takeIf {
          it.isNotBlank() && it != "Opérateur Fellah Numérique"
        } ?: userDisplayName.takeIf { it != "Opérateur Fellah Numérique" }.orEmpty()
      )
    }

  var emailDraft by
    remember(existingAccount?.email, userEmail) {
      mutableStateOf(existingAccount?.email?.takeIf { it.isNotBlank() } ?: userEmail)
    }

  var phoneDraft by
    remember(existingAccount?.phoneNumber, userPhoneNumber) {
      mutableStateOf(existingAccount?.phoneNumber?.takeIf { it.isNotBlank() } ?: userPhoneNumber)
    }

  var primaryWilayaDraft by
    remember(existingAccount?.farmerCardWilaya) {
      mutableStateOf(existingAccount?.farmerCardWilaya.orEmpty())
    }

  // Lightweight progressive profile drafts keyed by roleCode
  val businessDrafts = remember { mutableStateMapOf<String, String>() }
  val wilayaDrafts = remember { mutableStateMapOf<String, String>() }
  val activityDrafts = remember { mutableStateMapOf<String, String>() }
  val capacityDrafts = remember { mutableStateMapOf<String, String>() }

  var activeProfileRoleTab by remember { mutableStateOf<String?>(null) }
  var localValidationError by remember { mutableStateOf<String?>(null) }
  var showStep4ScannerExpanded by remember { mutableStateOf(false) }

  var identityVerification by
    remember(
      existingAccount?.nationalIdScanned,
      existingAccount?.nationalIdNumber,
      existingAccount?.farmerCardScanned,
      existingAccount?.farmerCardNumber,
    ) {
      mutableStateOf(
        existingAccount?.toIdentityVerificationData()
          ?: IdentityVerificationData(nationalIdHolderName = userDisplayName)
      )
    }

  fun resolvedDisplayName(): String {
    return fullNameDraft.trim().takeIf { it.isNotBlank() }
      ?: identityVerification.nationalIdHolderName.trim().takeIf {
        it.isNotBlank() && it != "Opérateur Fellah Numérique"
      }
      ?: userDisplayName.ifBlank { "Opérateur Fellah Numérique" }
  }

  fun resolvedNormalizedPhone(): String {
    val raw = phoneDraft.trim()
    if (raw.isBlank()) return ""
    return AccountInputValidator.normalizePhoneToE164(raw) ?: raw.take(32)
  }

  fun dispatchSaveDraft(roles: List<String>, defRole: String, step: Int) {
    if (onSaveAccountDraft != null) {
      onSaveAccountDraft(
        resolvedDisplayName(),
        emailDraft.trim(),
        resolvedNormalizedPhone(),
        isPhoneVerifiedSession,
        roles,
        defRole,
        step,
        identityVerification,
      )
    } else if (onSaveProgressDraftWithIdentity != null) {
      onSaveProgressDraftWithIdentity(roles, defRole, step, identityVerification)
    } else {
      onSaveProgressDraft(roles, defRole, step)
    }
  }

  fun dispatchComplete(roles: List<String>, defRole: String, profiles: List<RoleSpecificProfile>) {
    if (onCompleteAccountOnboarding != null) {
      onCompleteAccountOnboarding(
        resolvedDisplayName(),
        emailDraft.trim(),
        resolvedNormalizedPhone(),
        isPhoneVerifiedSession,
        roles,
        defRole,
        profiles,
        identityVerification,
      )
    } else if (onCompleteOnboardingWithIdentity != null) {
      onCompleteOnboardingWithIdentity(roles, defRole, profiles, identityVerification)
    } else {
      onCompleteOnboarding(roles, defRole, profiles)
    }
  }

  BackHandler(enabled = currentStep > 1) {
    currentStep = (currentStep - 1).coerceAtLeast(1)
  }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    Scaffold(
      modifier = Modifier.fillMaxSize().testTag("role_onboarding_root"),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  modifier = Modifier.size(40.dp),
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
                    text = "Fellah Numérique",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text =
                      RoleStrings.tr(
                        language,
                        ar = "الخطوة $currentStep من 4 • إعداد الحساب المهني",
                        fr = "Étape $currentStep sur 4 • Configuration du compte",
                        en = "Step $currentStep of 4 • Account Personalization",
                      ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                IconButton(
                  onClick = onSignOut,
                  modifier = Modifier.size(48.dp).testTag("onboarding_sign_out_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.sign_out),
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
            LinearProgressIndicator(
              progress = { currentStep / 4f },
              modifier = Modifier.fillMaxWidth().height(4.dp),
            )
          }
        }
      },
    ) { innerPadding ->
      Box(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .widthIn(max = 760.dp)
              .imePadding()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          val bannerError = localValidationError ?: errorMessage
          if (!bannerError.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth().testTag("onboarding_error_banner"),
            ) {
              Text(
                text = bannerError,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
              )
            }
          }

          when (currentStep) {
            1 -> {
              // STEP 1: Welcome Headline + Post-OTP Email & Account Details + Continue CTA
              Card(
                shape = RoundedCornerShape(22.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                  ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth().testTag("onboarding_step_1_card"),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(24.dp),
                  verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                  ) {
                    val headerIdentitySummary =
                      buildString {
                        append(resolvedDisplayName())
                        val contactTag = emailDraft.ifBlank { phoneDraft }
                        if (contactTag.isNotBlank()) {
                          append(" • ")
                          append(contactTag)
                        }
                      }
                    Text(
                      text = headerIdentitySummary,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                  }

                  Text(
                    text = RoleStrings.onboardingWelcomeTitle(language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("onboarding_welcome_headline"),
                  )

                  Text(
                    text = RoleStrings.onboardingWelcomeSubtitle(language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )

                  if (isPhoneVerifiedSession && phoneDraft.isNotBlank()) {
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = MaterialTheme.colorScheme.secondaryContainer,
                      modifier = Modifier.fillMaxWidth().testTag("verified_phone_banner"),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.CheckCircle,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text =
                              RoleStrings.tr(
                                language,
                                ar = "تم تأكيد رقم الهاتف ورمز الدخول بنجاح: $phoneDraft",
                                fr = "Téléphone et code SMS confirmés : $phoneDraft",
                                en = "Phone & SMS login code confirmed: $phoneDraft",
                              ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                          )
                          Text(
                            text =
                              RoleStrings.tr(
                                language,
                                ar = "أضف الآن بريدك الإلكتروني وبياناتك الشخصية ثم اختر أنشطتك المهنية.",
                                fr = "Ajoutez votre adresse e-mail et vos informations personnelles.",
                                en = "Add your email address and personal details to continue.",
                              ),
                            style = MaterialTheme.typography.bodySmall,
                            color =
                              MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                          )
                        }
                      }
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth().testTag("post_otp_account_details_card"),
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(16.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Text(
                        text =
                          when (language) {
                            AppLanguage.AR -> stringResource(R.string.post_otp_details_title_ar)
                            AppLanguage.FR -> stringResource(R.string.post_otp_details_title_fr)
                            AppLanguage.EN -> stringResource(R.string.post_otp_details_title_en)
                          },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                      )

                      OutlinedTextField(
                        value = emailDraft,
                        onValueChange = {
                          emailDraft = it.take(155)
                          localValidationError = null
                        },
                        singleLine = true,
                        label = {
                          Text(
                            RoleStrings.tr(
                              language,
                              ar = "البريد الإلكتروني (Email) *",
                              fr = "Adresse e-mail *",
                              en = "Email Address *",
                            )
                          )
                        },
                        placeholder = { Text("farmer@fellah.dz") },
                        leadingIcon = {
                          Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                          )
                        },
                        textStyle = TextStyle(textDirection = TextDirection.Ltr),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_email_input"),
                      )

                      OutlinedTextField(
                        value = fullNameDraft,
                        onValueChange = {
                          fullNameDraft = it.take(115)
                          localValidationError = null
                        },
                        singleLine = true,
                        label = {
                          Text(
                            RoleStrings.tr(
                              language,
                              ar = "الاسم الكامل / اسم المتعامل *",
                              fr = "Nom complet / Exploitant *",
                              en = "Full Name / Operator Name *",
                            )
                          )
                        },
                        leadingIcon = {
                          Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                          )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_full_name_input"),
                      )

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        OutlinedTextField(
                          value = phoneDraft,
                          onValueChange = {
                            if (!isPhoneVerifiedSession) {
                              phoneDraft = it.take(24)
                              localValidationError = null
                            }
                          },
                          readOnly = isPhoneVerifiedSession && phoneDraft.isNotBlank(),
                          singleLine = true,
                          label = {
                            Text(
                              RoleStrings.tr(
                                language,
                                ar = if (isPhoneVerifiedSession) "الهاتف المؤكد ✓" else "رقم الهاتف",
                                fr =
                                  if (isPhoneVerifiedSession) "Téléphone vérifié ✓" else "Téléphone",
                                en =
                                  if (isPhoneVerifiedSession) "Verified Phone ✓" else "Phone Number",
                              )
                            )
                          },
                          leadingIcon = {
                            Icon(
                              imageVector = Icons.Filled.PhoneAndroid,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary,
                            )
                          },
                          textStyle = TextStyle(textDirection = TextDirection.Ltr),
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.weight(1f).testTag("onboarding_phone_input"),
                        )

                        OutlinedTextField(
                          value = primaryWilayaDraft,
                          onValueChange = { primaryWilayaDraft = it.take(80) },
                          singleLine = true,
                          label = {
                            Text(
                              RoleStrings.tr(
                                language,
                                ar = "الولاية",
                                fr = "Wilaya",
                                en = "Wilaya",
                              )
                            )
                          },
                          placeholder = { Text("07 - بسكرة") },
                          leadingIcon = {
                            Icon(
                              imageVector = Icons.Filled.LocationOn,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary,
                            )
                          },
                          shape = RoundedCornerShape(12.dp),
                          modifier =
                            Modifier.weight(1f).testTag("onboarding_primary_wilaya_input"),
                        )
                      }
                    }
                  }

                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                  )

                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    UserRole.entries.forEach { role ->
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          Icon(
                            imageVector = RoleStrings.iconForRole(role),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                          )
                          Text(
                            text = role.localizedTitle(language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                          )
                        }
                      }
                    }
                  }

                  Button(
                    onClick = {
                      val trimmedEmail = emailDraft.trim()
                      val trimmedName = fullNameDraft.trim()
                      val scannedHolder =
                        identityVerification.nationalIdHolderName.trim().takeIf {
                          it != "Opérateur Fellah Numérique"
                        }.orEmpty()

                      if (
                        (isPhoneVerifiedSession && trimmedEmail.isBlank()) ||
                          (trimmedEmail.isNotBlank() &&
                            !AccountInputValidator.isValidEmail(trimmedEmail))
                      ) {
                        localValidationError =
                          RoleStrings.tr(
                            language,
                            ar = "يرجى إدخال بريد إلكتروني صحيح للمتابعة وإكمال بيانات حسابك.",
                            fr = "Veuillez saisir une adresse e-mail valide pour continuer.",
                            en = "Please enter a valid email address to continue.",
                          )
                        return@Button
                      }

                      if (
                        isPhoneVerifiedSession && trimmedName.isBlank() && scannedHolder.isBlank()
                      ) {
                        localValidationError =
                          RoleStrings.tr(
                            language,
                            ar = "يرجى إدخال الاسم الكامل أو مسح بطاقة التعريف الوطنية للمتابعة.",
                            fr = "Veuillez saisir votre nom complet ou scanner votre CNI.",
                            en = "Please enter your full name or scan your National ID card.",
                          )
                        return@Button
                      }

                      if (primaryWilayaDraft.isNotBlank()) {
                        UserRole.entries.forEach { role ->
                          if (wilayaDrafts[role.code].isNullOrBlank()) {
                            wilayaDrafts[role.code] = primaryWilayaDraft.trim()
                          }
                        }
                      }

                      localValidationError = null
                      currentStep = 2
                      dispatchSaveDraft(selectedRoles.toList(), chosenDefaultRole, 2)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_start_selection_button"),
                  ) {
                    Text(
                      text =
                        RoleStrings.tr(
                          language,
                          ar = "اختيار أنشطتي المهنية",
                          fr = "Choisir mes activités professionnelles",
                          en = "Select My Professional Activities",
                        ),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                  }
                }
              }

              // Identity & Optional Farmer Card Scanner right on Registration Step 1
              IdentityDocumentScannerSection(
                language = language,
                identityData = identityVerification,
                onIdentityDataChanged = { updated ->
                  identityVerification = updated
                  if (
                    (updated.farmerCardScanned || updated.farmerCardNumber.isNotBlank()) &&
                      !selectedRoles.contains(UserRole.FARMER.code)
                  ) {
                    selectedRoles.add(0, UserRole.FARMER.code)
                    if (chosenDefaultRole.isBlank()) {
                      chosenDefaultRole = UserRole.FARMER.code
                    }
                  }
                  if (
                    updated.farmerCardWilaya.isNotBlank() &&
                      wilayaDrafts[UserRole.FARMER.code].isNullOrBlank()
                  ) {
                    wilayaDrafts[UserRole.FARMER.code] = updated.farmerCardWilaya
                  }
                },
                documentScanner = documentScanner,
                onFarmerCardAutoPopulate = { wilaya, farmName, sector ->
                  if (!selectedRoles.contains(UserRole.FARMER.code)) {
                    selectedRoles.add(0, UserRole.FARMER.code)
                  }
                  if (chosenDefaultRole.isBlank()) {
                    chosenDefaultRole = UserRole.FARMER.code
                  }
                  if (wilaya.isNotBlank()) {
                    wilayaDrafts[UserRole.FARMER.code] = wilaya
                  }
                  if (farmName.isNotBlank()) {
                    businessDrafts[UserRole.FARMER.code] = farmName
                  }
                  if (sector.isNotBlank()) {
                    activityDrafts[UserRole.FARMER.code] = sector
                  }
                },
              )
            }

            2 -> {
              // STEP 2: Multi-Select Role Cards (1..6 roles)
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "حدد نشاطًا مهنيًا واحدًا أو عدة أنشطة لحسابك",
                    fr = "Sélectionnez une ou plusieurs activités professionnelles",
                    en = "Select One or Multiple Professional Activities",
                  ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "مثال: يمكنك الجمع بين (الفلاح + تاجر الجملة) أو (الفلاح + التخزين والتبريد) في نفس الحساب.",
                    fr = "Exemple : combinez Agriculteur + Commerçant ou Agriculteur + Stockage dans un seul compte.",
                    en = "Example: combine Farmer + Trader or Farmer + Storage Provider under the same account.",
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              UserRole.entries.forEach { role ->
                val isSelected = selectedRoles.contains(role.code)
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors =
                    CardDefaults.cardColors(
                      containerColor =
                        if (isSelected) {
                          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                          MaterialTheme.colorScheme.surfaceContainerLowest
                        }
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color =
                          if (isSelected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                      )
                      .semantics {
                        this.role = Role.Checkbox
                        this.selected = isSelected
                      }
                      .clickable {
                        localValidationError = null
                        if (isSelected) {
                          selectedRoles.remove(role.code)
                          if (chosenDefaultRole == role.code) {
                            chosenDefaultRole = selectedRoles.firstOrNull().orEmpty()
                          }
                        } else {
                          selectedRoles.add(role.code)
                          if (chosenDefaultRole.isBlank()) {
                            chosenDefaultRole = role.code
                          }
                        }
                      }
                      .testTag("onboarding_role_card_${role.code}"),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      modifier = Modifier.weight(1f).padding(end = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color =
                          if (isSelected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.size(48.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = RoleStrings.iconForRole(role),
                            contentDescription = null,
                            tint =
                              if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.primary,
                          )
                        }
                      }
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                          text = role.localizedTitle(language),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(
                          text = role.localizedDescription(language),
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    Icon(
                      imageVector =
                        if (isSelected) Icons.Filled.CheckCircle
                        else Icons.Filled.RadioButtonUnchecked,
                      contentDescription = role.localizedTitle(language),
                      tint =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                      modifier = Modifier.size(26.dp),
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = { currentStep = 1 },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(50.dp).testTag("onboarding_back_step2"),
                ) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(RoleStrings.tr(language, "السابق", "Retour", "Back"))
                }

                Button(
                  onClick = {
                    val normalized = UserRole.normalizeRoleCodes(selectedRoles)
                    if (normalized.isEmpty()) {
                      localValidationError =
                        RoleStrings.tr(
                          language,
                          ar = "يرجى اختيار نشاط مهني واحد على الأقل للمتابعة.",
                          fr = "Veuillez sélectionner au moins une activité professionnelle.",
                          en = "Please select at least one professional activity to continue.",
                        )
                    } else {
                      localValidationError = null
                      if (normalized.size == 1) {
                        // Step 3 rule: If only one role is selected, set it automatically as defaultRole!
                        chosenDefaultRole = normalized.first()
                        activeProfileRoleTab = chosenDefaultRole
                        currentStep = 4
                        dispatchSaveDraft(normalized, chosenDefaultRole, 4)
                      } else {
                        if (chosenDefaultRole !in normalized) {
                          chosenDefaultRole = normalized.first()
                        }
                        currentStep = 3
                        dispatchSaveDraft(normalized, chosenDefaultRole, 3)
                      }
                    }
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier =
                    Modifier.weight(1f)
                      .height(50.dp)
                      .testTag("onboarding_continue_step2"),
                ) {
                  Text(
                    text =
                      RoleStrings.tr(
                        language,
                        ar = "متابعة (${selectedRoles.size})",
                        fr = "Continuer (${selectedRoles.size})",
                        en = "Continue (${selectedRoles.size})",
                      ),
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }

            3 -> {
              // STEP 3: Default Role Selection (when multiple roles selected)
              val normalized = UserRole.normalizeRoleCodes(selectedRoles)
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "اختر واجهة الانطلاق الافتراضية",
                    fr = "Choisissez votre rôle actif par défaut",
                    en = "Choose Your Default Active Role",
                  ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "لقد اخترت ${normalized.size} أنشطة مهنية. حدد النشاط الذي تود فتح لوحة قيادته تلقائيًا عند الدخول (يمكنك التبديل بنقرة واحدة من الشريط العلوي).",
                    fr = "Vous avez choisi ${normalized.size} rôles. Sélectionnez le tableau de bord à ouvrir par défaut (modifiable en 1 clic dans l'en-tête).",
                    en = "You selected ${normalized.size} roles. Pick which dashboard opens first on sign-in (you can switch anytime in 1 tap from the header).",
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              normalized.mapNotNull { UserRole.fromCodeOrNull(it) }.forEach { role ->
                val isDefault = chosenDefaultRole == role.code
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors =
                    CardDefaults.cardColors(
                      containerColor =
                        if (isDefault) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .border(
                        width = if (isDefault) 2.dp else 1.dp,
                        color =
                          if (isDefault) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp),
                      )
                      .clickable { chosenDefaultRole = role.code }
                      .testTag("onboarding_default_role_${role.code}"),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      Icon(
                        imageVector = RoleStrings.iconForRole(role),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                      )
                      Column {
                        Text(
                          text = role.localizedTitle(language),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                        )
                        if (isDefault) {
                          Text(
                            text =
                              RoleStrings.tr(
                                language,
                                ar = "الواجهة الافتراضية المختارة",
                                fr = "Interface par défaut",
                                en = "Selected default workspace",
                              ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                          )
                        }
                      }
                    }
                    Icon(
                      imageVector =
                        if (isDefault) Icons.Filled.CheckCircle
                        else Icons.Filled.RadioButtonUnchecked,
                      contentDescription = null,
                      tint =
                        if (isDefault) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = { currentStep = 2 },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(50.dp).testTag("onboarding_back_step3"),
                ) {
                  Text(RoleStrings.tr(language, "تعديل الأنشطة", "Modifier", "Back"))
                }
                Button(
                  onClick = {
                    val effectiveDefault =
                      chosenDefaultRole.takeIf { it in normalized } ?: normalized.first()
                    chosenDefaultRole = effectiveDefault
                    activeProfileRoleTab = effectiveDefault
                    currentStep = 4
                    dispatchSaveDraft(normalized, effectiveDefault, 4)
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier =
                    Modifier.weight(1f)
                      .height(50.dp)
                      .testTag("onboarding_continue_step3"),
                ) {
                  Text(
                    RoleStrings.tr(language, "متابعة لإعداد الملف", "Continuer", "Continue"),
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }

            4 -> {
              // STEP 4: Progressive Role-Specific Profile Setup (Skippable / Complete Later)
              val normalized = UserRole.normalizeRoleCodes(selectedRoles)
              val effectiveDefault =
                chosenDefaultRole.takeIf { it in normalized } ?: normalized.firstOrNull().orEmpty()
              val focusedCode =
                activeProfileRoleTab?.takeIf { it in normalized } ?: effectiveDefault
              val focusedRole = UserRole.fromCode(focusedCode)

              val existingForFocused = existingRoleProfiles[focusedRole.code]
              val businessVal =
                businessDrafts.getOrPut(focusedRole.code) {
                  existingForFocused?.businessName?.takeIf { it.isNotBlank() }
                    ?: resolvedDisplayName()
                }
              val wilayaVal =
                wilayaDrafts.getOrPut(focusedRole.code) {
                  existingForFocused?.wilaya?.takeIf { it.isNotBlank() }
                    ?: primaryWilayaDraft.trim().takeIf { it.isNotBlank() }
                    ?: "07 - بسكرة (Biskra)"
                }
              val activityVal =
                activityDrafts.getOrPut(focusedRole.code) {
                  existingForFocused?.activityType.orEmpty()
                }
              val capacityVal =
                capacityDrafts.getOrPut(focusedRole.code) {
                  existingForFocused?.capacityOrArea.orEmpty()
                }

              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "معلومات أولية سريعة لنشاطك المهني (اختياري الآن)",
                    fr = "Informations rapides par activité (optionnel maintenant)",
                    en = "Lightweight Role Profile Setup (Optional Now)",
                  ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "أدخل المعلومات الأساسية لكل نشاط مختار، أو اضغط على (الإكمال لاحقًا) للدخول فورًا إلى لوحة قيادتك.",
                    fr = "Renseignez l'essentiel pour vos rôles ou cliquez sur « Compléter plus tard » pour accéder immédiatement à votre espace.",
                    en = "Fill in basic details for your selected role(s), or tap 'Complete Later' to jump straight into your personalized dashboard.",
                  ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              if (normalized.size > 1) {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  normalized.mapNotNull { UserRole.fromCodeOrNull(it) }.forEach { r ->
                    FilterChip(
                      selected = r.code == focusedRole.code,
                      onClick = { activeProfileRoleTab = r.code },
                      label = { Text(r.localizedTitle(language)) },
                      leadingIcon = {
                        Icon(
                          imageVector = RoleStrings.iconForRole(r),
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      },
                      modifier = Modifier.testTag("onboarding_profile_tab_${r.code}"),
                    )
                  }
                }
              }

              Card(
                shape = RoundedCornerShape(18.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                  ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = RoleStrings.iconForRole(focusedRole),
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = focusedRole.localizedTitle(language),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }

                  OutlinedTextField(
                    value = businessVal,
                    onValueChange = { businessDrafts[focusedRole.code] = it },
                    label = { Text(focusedRole.businessFieldLabel(language)) },
                    singleLine = true,
                    modifier =
                      Modifier.fillMaxWidth().testTag("onboarding_business_name_input"),
                  )

                  OutlinedTextField(
                    value = wilayaVal,
                    onValueChange = { wilayaDrafts[focusedRole.code] = it },
                    label = {
                      Text(
                        RoleStrings.tr(
                          language,
                          ar = "الولاية / منطقة النشاط",
                          fr = "Wilaya / Zone d'activité",
                          en = "Wilaya / Operating Region",
                        )
                      )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_wilaya_input"),
                  )

                  OutlinedTextField(
                    value = activityVal,
                    onValueChange = { activityDrafts[focusedRole.code] = it },
                    label = { Text(focusedRole.activityFieldLabel(language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_activity_input"),
                  )

                  OutlinedTextField(
                    value = capacityVal,
                    onValueChange = { capacityDrafts[focusedRole.code] = it },
                    label = { Text(focusedRole.capacityFieldLabel(language)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_capacity_input"),
                  )
                }
              }

              // Summary / Quick Toggle for Identity & Optional Farmer Card in Step 4
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_step4_identity_summary"),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            language,
                            ar = "حالة بطاقة التعريف الوطنية وبطاقة الفلاح (إن وجدت)",
                            fr = "Statut CNI & Carte d'Agriculteur (optionnelle)",
                            en = "National ID & Optional Farmer Card Status",
                          ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text =
                          buildString {
                            append(
                              if (identityVerification.hasNationalId) {
                                RoleStrings.tr(language, "بطاقة التعريف: مرفقة ✓", "CNI : Vérifiée ✓", "ID Card: Attached ✓")
                              } else {
                                RoleStrings.tr(language, "بطاقة التعريف: غير ممسوحة بعد", "CNI : Non scannée", "ID Card: Not scanned yet")
                              }
                            )
                            append(" • ")
                            append(
                              if (identityVerification.hasFarmerCard) {
                                RoleStrings.tr(language, "بطاقة الفلاح: مرفقة ✓", "Carte Fellah : Liée ✓", "Farmer Card: Linked ✓")
                              } else {
                                RoleStrings.tr(language, "بطاقة الفلاح: اختيارية", "Carte Fellah : Optionnelle", "Farmer Card: Optional")
                              }
                            )
                          },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    TextButton(
                      onClick = { showStep4ScannerExpanded = !showStep4ScannerExpanded },
                      modifier = Modifier.testTag("onboarding_step4_toggle_scanner_button"),
                    ) {
                      Text(
                        RoleStrings.tr(
                          language,
                          ar = if (showStep4ScannerExpanded) "إخفاء الماسح" else "مسح / تعديل البطاقات",
                          fr = if (showStep4ScannerExpanded) "Masquer" else "Scanner / Modifier",
                          en = if (showStep4ScannerExpanded) "Hide Scanner" else "Scan / Edit Cards",
                        ),
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }

              if (showStep4ScannerExpanded) {
                IdentityDocumentScannerSection(
                  language = language,
                  identityData = identityVerification,
                  onIdentityDataChanged = { identityVerification = it },
                  documentScanner = documentScanner,
                  onFarmerCardAutoPopulate = { wilaya, farmName, sector ->
                    if (wilaya.isNotBlank()) wilayaDrafts[focusedRole.code] = wilaya
                    if (farmName.isNotBlank()) businessDrafts[focusedRole.code] = farmName
                    if (sector.isNotBlank()) activityDrafts[focusedRole.code] = sector
                  },
                )
              }

              Button(
                onClick = {
                  val profiles =
                    normalized.mapNotNull { code ->
                      val bName = businessDrafts[code]?.trim().orEmpty()
                      val wil = wilayaDrafts[code]?.trim().orEmpty()
                      val act = activityDrafts[code]?.trim().orEmpty()
                      val cap = capacityDrafts[code]?.trim().orEmpty()
                      if (bName.isNotBlank() || act.isNotBlank() || cap.isNotBlank()) {
                        RoleSpecificProfile(
                          roleCode = code,
                          businessName = bName.ifBlank { resolvedDisplayName() },
                          wilaya =
                            wil.ifBlank {
                              primaryWilayaDraft.trim().ifBlank { "07 - بسكرة (Biskra)" }
                            },
                          commune = "",
                          activityType = act,
                          capacityOrArea = cap,
                          qualificationsOrDetails = "",
                        )
                      } else {
                        null
                      }
                    }
                  dispatchComplete(normalized, effectiveDefault, profiles)
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_finish_button"),
              ) {
                if (isSaving) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                  )
                } else {
                  Icon(Icons.Filled.CheckCircle, contentDescription = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text =
                      RoleStrings.tr(
                        language,
                        ar = "حفظ والدخول إلى لوحة القيادة",
                        fr = "Enregistrer et ouvrir mon espace",
                        en = "Save & Open My Personalized Dashboard",
                      ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                TextButton(
                  onClick = { currentStep = if (normalized.size > 1) 3 else 2 },
                  enabled = !isSaving,
                  modifier = Modifier.height(48.dp).testTag("onboarding_back_step4"),
                ) {
                  Text(RoleStrings.tr(language, "السابق", "Retour", "Back"))
                }

                TextButton(
                  onClick = {
                    dispatchComplete(normalized, effectiveDefault, emptyList())
                  },
                  enabled = !isSaving,
                  modifier = Modifier.height(48.dp).testTag("onboarding_skip_later_button"),
                ) {
                  Text(
                    text = RoleStrings.completeLaterCta(language),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
