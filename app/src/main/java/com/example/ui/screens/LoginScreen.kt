package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.AppLanguage
import com.example.ui.auth.createCredentialManagerSafely
import com.example.ui.viewmodel.AuthEntryMethod
import com.example.ui.viewmodel.LoginAuthStatus
import com.example.ui.viewmodel.LoginUiState
import com.example.ui.viewmodel.LoginViewModel
import com.example.ui.viewmodel.PhoneOtpStep

@Composable
fun LoginScreen(
  initialLanguage: AppLanguage = AppLanguage.AR,
  onLanguageChanged: (AppLanguage) -> Unit = {},
  onAuthSuccess: () -> Unit = {},
  onPhoneVerified: (String) -> Unit = {},
  onPhoneDraftCaptured: (String) -> Unit = {},
  onEnterVisitorMode: () -> Unit = {},
  loginViewModel: LoginViewModel = viewModel(),
) {
  val context = LocalContext.current
  val credentialManager = remember(context) { createCredentialManagerSafely(context) }
  val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(initialLanguage) {
    if (uiState.language != initialLanguage) {
      loginViewModel.setLanguage(initialLanguage)
    }
  }

  LaunchedEffect(Unit) {
    loginViewModel.runSilentSignInOnce(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
    )
  }

  LoginScreenContent(
    uiState = uiState,
    onLanguageSelected = { lang ->
      loginViewModel.setLanguage(lang)
      onLanguageChanged(lang)
    },
    onSignInClick = {
      loginViewModel.currentNormalizedDraftPhone()?.let { draftPhone ->
        onPhoneDraftCaptured(draftPhone)
      }
      loginViewModel.signInWithGoogle(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = onAuthSuccess,
      )
    },
    onDismissMessage = loginViewModel::dismissMessage,
    onEnterVisitorMode = onEnterVisitorMode,
    onAuthMethodSelected = loginViewModel::selectAuthMethod,
    onPhoneInputChanged = loginViewModel::onPhoneInputChanged,
    onOtpCodeChanged = loginViewModel::onOtpCodeInputChanged,
    onSendPhoneCodeClick = {
      loginViewModel.requestPhoneOtpCode(
        context = context,
        isResend = false,
        credentialManager = credentialManager,
        onPhoneDraftCaptured = onPhoneDraftCaptured,
        onGoogleHandoffAuthSuccess = onAuthSuccess,
        onInstantAuthSuccess = { verifiedPhone ->
          onPhoneVerified(verifiedPhone)
          onAuthSuccess()
        },
      )
    },
    onVerifyPhoneCodeClick = {
      loginViewModel.confirmPhoneOtpCode { verifiedPhone ->
        onPhoneVerified(verifiedPhone)
        onAuthSuccess()
      }
    },
    onResendPhoneCodeClick = {
      loginViewModel.requestPhoneOtpCode(
        context = context,
        isResend = true,
        credentialManager = credentialManager,
        onPhoneDraftCaptured = onPhoneDraftCaptured,
        onGoogleHandoffAuthSuccess = onAuthSuccess,
        onInstantAuthSuccess = { verifiedPhone ->
          onPhoneVerified(verifiedPhone)
          onAuthSuccess()
        },
      )
    },
    onEditPhoneNumberClick = loginViewModel::editPhoneNumber,
  )
}

@Composable
fun LoginScreenContent(
  uiState: LoginUiState,
  onLanguageSelected: (AppLanguage) -> Unit,
  onSignInClick: () -> Unit,
  onDismissMessage: () -> Unit,
  onEnterVisitorMode: () -> Unit = {},
  onAuthMethodSelected: (AuthEntryMethod) -> Unit = {},
  onPhoneInputChanged: (String) -> Unit = {},
  onOtpCodeChanged: (String) -> Unit = {},
  onSendPhoneCodeClick: () -> Unit = {},
  onVerifyPhoneCodeClick: () -> Unit = {},
  onResendPhoneCodeClick: () -> Unit = {},
  onEditPhoneNumberClick: () -> Unit = {},
) {
  val layoutDirection =
    if (uiState.language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
  val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    BoxWithConstraints(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .testTag("login_screen_root")
    ) {
      val isExpanded = maxWidth >= 840.dp

      Spacer(
        modifier =
          Modifier.fillMaxSize().drawWithCache {
            val center1 = Offset(size.width * 0.85f, size.height * 0.14f)
            val radius1 = (size.minDimension * 0.72f).coerceAtLeast(1f)
            val brush1 =
              Brush.radialGradient(
                colors = listOf(secondaryContainer.copy(alpha = 0.6f), Color.Transparent),
                center = center1,
                radius = radius1,
              )
            val center2 = Offset(size.width * 0.12f, size.height * 0.86f)
            val radius2 = (size.minDimension * 0.65f).coerceAtLeast(1f)
            val brush2 =
              Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.14f), Color.Transparent),
                center = center2,
                radius = radius2,
              )
            val center3 = Offset(size.width * 0.5f, size.height * 0.95f)
            val radius3 = (size.minDimension * 0.5f).coerceAtLeast(1f)
            val brush3 =
              Brush.radialGradient(
                colors = listOf(tertiaryContainer.copy(alpha = 0.35f), Color.Transparent),
                center = center3,
                radius = radius3,
              )
            onDrawBehind {
              drawCircle(brush = brush1, radius = radius1, center = center1)
              drawCircle(brush = brush2, radius = radius2, center = center2)
              drawCircle(brush = brush3, radius = radius3, center = center3)
            }
          }
      )

      if (isExpanded) {
        Row(
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .imePadding()
              .padding(horizontal = 32.dp, vertical = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(28.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OasisSatelliteShowcasePanel(
            language = uiState.language,
            modifier = Modifier.weight(1.08f).fillMaxHeight(),
          )
          Box(
            modifier =
              Modifier.weight(0.92f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
          ) {
            LoginAuthCard(
              uiState = uiState,
              onSignInClick = onSignInClick,
              onDismissMessage = onDismissMessage,
              onEnterVisitorMode = onEnterVisitorMode,
              onAuthMethodSelected = onAuthMethodSelected,
              onPhoneInputChanged = onPhoneInputChanged,
              onOtpCodeChanged = onOtpCodeChanged,
              onSendPhoneCodeClick = onSendPhoneCodeClick,
              onVerifyPhoneCodeClick = onVerifyPhoneCodeClick,
              onResendPhoneCodeClick = onResendPhoneCodeClick,
              onEditPhoneNumberClick = onEditPhoneNumberClick,
            )
          }
        }
      } else {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .imePadding()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          LoginAuthCard(
            uiState = uiState,
            onSignInClick = onSignInClick,
            onDismissMessage = onDismissMessage,
            onEnterVisitorMode = onEnterVisitorMode,
            onAuthMethodSelected = onAuthMethodSelected,
            onPhoneInputChanged = onPhoneInputChanged,
            onOtpCodeChanged = onOtpCodeChanged,
            onSendPhoneCodeClick = onSendPhoneCodeClick,
            onVerifyPhoneCodeClick = onVerifyPhoneCodeClick,
            onResendPhoneCodeClick = onResendPhoneCodeClick,
            onEditPhoneNumberClick = onEditPhoneNumberClick,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginAuthCard(
  uiState: LoginUiState,
  onSignInClick: () -> Unit,
  onDismissMessage: () -> Unit,
  onEnterVisitorMode: () -> Unit = {},
  onAuthMethodSelected: (AuthEntryMethod) -> Unit = {},
  onPhoneInputChanged: (String) -> Unit = {},
  onOtpCodeChanged: (String) -> Unit = {},
  onSendPhoneCodeClick: () -> Unit = {},
  onVerifyPhoneCodeClick: () -> Unit = {},
  onResendPhoneCodeClick: () -> Unit = {},
  onEditPhoneNumberClick: () -> Unit = {},
) {
  val lang = uiState.language
  val badgeText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_portal_badge_ar)
      AppLanguage.FR -> stringResource(R.string.login_portal_badge_fr)
      AppLanguage.EN -> stringResource(R.string.login_portal_badge_en)
    }
  val titleText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_title_ar)
      AppLanguage.FR -> stringResource(R.string.login_title_fr)
      AppLanguage.EN -> stringResource(R.string.login_title_en)
    }
  val subtitleText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_subtitle_ar)
      AppLanguage.FR -> stringResource(R.string.login_subtitle_fr)
      AppLanguage.EN -> stringResource(R.string.login_subtitle_en)
    }
  val buttonText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_google_btn_ar)
      AppLanguage.FR -> stringResource(R.string.login_google_btn_fr)
      AppLanguage.EN -> stringResource(R.string.login_google_btn_en)
    }
  val checkingText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_checking_ar)
      AppLanguage.FR -> stringResource(R.string.login_checking_fr)
      AppLanguage.EN -> stringResource(R.string.login_checking_en)
    }
  val cancelledText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_cancelled_ar)
      AppLanguage.FR -> stringResource(R.string.login_cancelled_fr)
      AppLanguage.EN -> stringResource(R.string.login_cancelled_en)
    }
  val retryText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_retry_ar)
      AppLanguage.FR -> stringResource(R.string.login_retry_fr)
      AppLanguage.EN -> stringResource(R.string.login_retry_en)
    }
  val footerText =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.login_security_footer_ar)
      AppLanguage.FR -> stringResource(R.string.login_security_footer_fr)
      AppLanguage.EN -> stringResource(R.string.login_security_footer_en)
    }
  val phoneTabLabel =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.auth_tab_phone_ar)
      AppLanguage.FR -> stringResource(R.string.auth_tab_phone_fr)
      AppLanguage.EN -> stringResource(R.string.auth_tab_phone_en)
    }
  val googleTabLabel =
    when (lang) {
      AppLanguage.AR -> stringResource(R.string.auth_tab_google_ar)
      AppLanguage.FR -> stringResource(R.string.auth_tab_google_fr)
      AppLanguage.EN -> stringResource(R.string.auth_tab_google_en)
    }

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .widthIn(max = 500.dp)
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
          shape = RoundedCornerShape(28.dp),
        ),
    shape = RoundedCornerShape(28.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top header row: Spatial telemetry badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
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
              imageVector = Icons.Filled.SatelliteAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "WGS84 • NDVI 0.74",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Official Fellah Numérique Hero Logo Emblem
      Surface(
        modifier =
          Modifier.size(86.dp)
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(20.dp),
            )
            .testTag("app_logo_login"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFEFEFE),
        shadowElevation = 3.dp,
      ) {
        Image(
          painter = painterResource(R.drawable.img_fellah_logo),
          contentDescription = stringResource(R.string.app_name),
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().padding(5.dp),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Portal badge
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
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
                .background(MaterialTheme.colorScheme.primary)
          )
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = titleText,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = subtitleText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Agrological capabilities pills
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CapabilityChip(
          icon = Icons.Filled.SatelliteAlt,
          label =
            when (lang) {
              AppLanguage.AR -> "ترسيم الحدود بالساتل"
              AppLanguage.FR -> "Polygone Satellite"
              AppLanguage.EN -> "Satellite Polygon"
            },
        )
        CapabilityChip(
          icon = Icons.Filled.Science,
          label =
            when (lang) {
              AppLanguage.AR -> "مخبر التربة INRAA"
              AppLanguage.FR -> "Analyse Sol INRAA"
              AppLanguage.EN -> "INRAA Soil Lab"
            },
        )
        CapabilityChip(
          icon = Icons.Filled.WaterDrop,
          label =
            when (lang) {
              AppLanguage.AR -> "تحديد نقاط السقي"
              AppLanguage.FR -> "Points de Forage"
              AppLanguage.EN -> "Forage Wells"
            },
        )
        CapabilityChip(
          icon = Icons.Filled.Storefront,
          label =
            when (lang) {
              AppLanguage.AR -> "السوق العام الفلاحي"
              AppLanguage.FR -> "Marché Public"
              AppLanguage.EN -> "Public Market"
            },
        )
        CapabilityChip(
          icon = Icons.Filled.Lock,
          label =
            when (lang) {
              AppLanguage.AR -> "مسح بطاقة التعريف وبطاقة الفلاح"
              AppLanguage.FR -> "Scan CNI & Carte Fellah"
              AppLanguage.EN -> "ID & Farmer Card Scan"
            },
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Segmented Entry Method Selector: Phone + SMS OTP vs Google Account
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier =
          Modifier.fillMaxWidth()
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(14.dp),
            )
            .testTag("auth_method_selector"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AuthMethodTabButton(
            selected = uiState.selectedAuthMethod == AuthEntryMethod.PHONE_OTP,
            icon = Icons.Filled.PhoneAndroid,
            label = phoneTabLabel,
            onClick = { onAuthMethodSelected(AuthEntryMethod.PHONE_OTP) },
            modifier = Modifier.weight(1f).testTag("auth_method_phone_tab"),
          )
          AuthMethodTabButton(
            selected = uiState.selectedAuthMethod == AuthEntryMethod.GOOGLE,
            icon = null,
            label = googleTabLabel,
            onClick = { onAuthMethodSelected(AuthEntryMethod.GOOGLE) },
            modifier = Modifier.weight(1f).testTag("auth_method_google_tab"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Feedback states (Checking session, Cancelled, or Error)
      AnimatedVisibility(visible = uiState.status is LoginAuthStatus.CheckingSavedSession) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = checkingText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      AnimatedVisibility(visible = uiState.status is LoginAuthStatus.Cancelled) {
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .padding(bottom = 12.dp)
              .testTag("login_cancelled_banner"),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = cancelledText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
            TextButton(
              onClick = {
                onDismissMessage()
                onSignInClick()
              },
              modifier = Modifier.testTag("login_retry_button"),
            ) {
              Text(
                text = retryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
          }
        }
      }

      val phoneSavedGuidanceStatus = uiState.status as? LoginAuthStatus.PhoneSavedReadyForGoogle
      AnimatedVisibility(visible = phoneSavedGuidanceStatus != null) {
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .padding(bottom = 12.dp)
              .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
              )
              .testTag("phone_saved_google_guidance_banner"),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
              )
              Text(
                text =
                  when (lang) {
                    AppLanguage.AR ->
                      "رقمك محفوظ: ${phoneSavedGuidanceStatus?.savedPhone.orEmpty()}"
                    AppLanguage.FR ->
                      "Numéro enregistré : ${phoneSavedGuidanceStatus?.savedPhone.orEmpty()}"
                    AppLanguage.EN ->
                      "Saved phone: ${phoneSavedGuidanceStatus?.savedPhone.orEmpty()}"
                  },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp).padding(top = 2.dp),
              )
              Text(
                text = phoneSavedGuidanceStatus?.message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      val errorStatus = uiState.status as? LoginAuthStatus.Error
      AnimatedVisibility(visible = errorStatus != null) {
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .padding(bottom = 12.dp)
              .testTag("login_error_banner"),
          shape = RoundedCornerShape(14.dp),
          color =
            if (errorStatus?.suggestGoogleFallback == true) {
              MaterialTheme.colorScheme.secondaryContainer
            } else {
              MaterialTheme.colorScheme.errorContainer
            },
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector =
                  if (errorStatus?.suggestGoogleFallback == true) Icons.Filled.Info
                  else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint =
                  if (errorStatus?.suggestGoogleFallback == true) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                  } else {
                    MaterialTheme.colorScheme.onErrorContainer
                  },
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = errorStatus?.message.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color =
                  if (errorStatus?.suggestGoogleFallback == true) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                  } else {
                    MaterialTheme.colorScheme.onErrorContainer
                  },
                modifier = Modifier.weight(1f),
              )
            }

            if (errorStatus?.suggestGoogleFallback == true) {
              Button(
                onClick = {
                  onDismissMessage()
                  onSignInClick()
                },
                enabled = !uiState.isBusy,
                shape = RoundedCornerShape(10.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(48.dp)
                    .testTag("error_banner_google_fallback_button"),
              ) {
                Text(
                  text = buttonText,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }

      // Phone + SMS OTP Registration & Login Section
      AnimatedVisibility(visible = uiState.selectedAuthMethod == AuthEntryMethod.PHONE_OTP) {
        PhoneOtpAuthSection(
          uiState = uiState,
          onPhoneInputChanged = onPhoneInputChanged,
          onOtpCodeChanged = onOtpCodeChanged,
          onSendPhoneCodeClick = onSendPhoneCodeClick,
          onVerifyPhoneCodeClick = onVerifyPhoneCodeClick,
          onResendPhoneCodeClick = onResendPhoneCodeClick,
          onEditPhoneNumberClick = onEditPhoneNumberClick,
        )
      }

      if (uiState.selectedAuthMethod == AuthEntryMethod.PHONE_OTP) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )
          Text(
            text =
              when (lang) {
                AppLanguage.AR -> "أو عبر حساب Google"
                AppLanguage.FR -> "OU AVEC GOOGLE"
                AppLanguage.EN -> "OR WITH GOOGLE"
              },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (
        uiState.selectedAuthMethod == AuthEntryMethod.GOOGLE &&
          uiState.status !is LoginAuthStatus.PhoneSavedReadyForGoogle
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text =
                when (lang) {
                  AppLanguage.AR ->
                    "اختر حسابك المسجل في الهاتف، أو اكتب بريدك الإلكتروني (example@gmail.com) ولا تكتب رقم الهاتف داخل نافذة Google."
                  AppLanguage.FR ->
                    "Choisissez votre compte sur l’appareil ou saisissez votre adresse Gmail (exemple@gmail.com) — pas votre numéro dans Google."
                  AppLanguage.EN ->
                    "Select your account on this device or enter your Gmail address (example@gmail.com) — do not enter a phone number inside Google."
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Google Sign-In CTA via Jetpack CredentialManager (always accessible!)
      val isGooglePrimary = uiState.selectedAuthMethod == AuthEntryMethod.GOOGLE
      Button(
        onClick = onSignInClick,
        enabled = !uiState.isBusy,
        modifier =
          Modifier.fillMaxWidth()
            .height(if (isGooglePrimary) 56.dp else 50.dp)
            .testTag("login_button"),
        shape = RoundedCornerShape(14.dp),
        colors =
          if (isGooglePrimary) {
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isGooglePrimary) 2.dp else 0.dp),
      ) {
        if (uiState.status is LoginAuthStatus.SigningIn) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp).testTag("login_loading_indicator"),
            color =
              if (isGooglePrimary) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSecondaryContainer,
            strokeWidth = 2.5.dp,
          )
        } else {
          Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = Color.White,
          ) {
            Box(contentAlignment = Alignment.Center) {
              GoogleLogoEmblem(modifier = Modifier.size(17.dp))
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = buttonText,
            style =
              if (isGooglePrimary) MaterialTheme.typography.titleMedium
              else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedButton(
        onClick = onEnterVisitorMode,
        enabled = !uiState.isBusy,
        modifier =
          Modifier.fillMaxWidth()
            .height(50.dp)
            .testTag("enter_visitor_mode_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Storefront,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            when (lang) {
              AppLanguage.AR -> stringResource(R.string.visitor_mode_btn_ar)
              AppLanguage.FR -> stringResource(R.string.visitor_mode_btn_fr)
              AppLanguage.EN -> stringResource(R.string.visitor_mode_btn_en)
            },
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Spacer(modifier = Modifier.height(12.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = footerText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun AuthMethodTabButton(
  selected: Boolean,
  icon: ImageVector?,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(11.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primary
      else Color.Transparent,
    modifier =
      modifier
        .height(48.dp)
        .semantics {
          this.role = Role.Tab
          this.selected = selected
        }
        .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(17.dp),
        )
      } else {
        Surface(
          modifier = Modifier.size(20.dp),
          shape = CircleShape,
          color = Color.White,
        ) {
          Box(contentAlignment = Alignment.Center) {
            GoogleLogoEmblem(modifier = Modifier.size(13.dp))
          }
        }
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun PhoneOtpAuthSection(
  uiState: LoginUiState,
  onPhoneInputChanged: (String) -> Unit,
  onOtpCodeChanged: (String) -> Unit,
  onSendPhoneCodeClick: () -> Unit,
  onVerifyPhoneCodeClick: () -> Unit,
  onResendPhoneCodeClick: () -> Unit,
  onEditPhoneNumberClick: () -> Unit,
) {
  val lang = uiState.language
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
          shape = RoundedCornerShape(18.dp),
        )
        .testTag("phone_auth_section"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (uiState.phoneStep) {
        PhoneOtpStep.ENTER_PHONE -> {
          val phoneLabel =
            when (lang) {
              AppLanguage.AR -> stringResource(R.string.phone_input_label_ar)
              AppLanguage.FR -> stringResource(R.string.phone_input_label_fr)
              AppLanguage.EN -> stringResource(R.string.phone_input_label_en)
            }
          val sendBtnLabel =
            when (lang) {
              AppLanguage.AR -> stringResource(R.string.phone_send_code_btn_ar)
              AppLanguage.FR -> stringResource(R.string.phone_send_code_btn_fr)
              AppLanguage.EN -> stringResource(R.string.phone_send_code_btn_en)
            }
          val helperLabel =
            when (lang) {
              AppLanguage.AR ->
                "أدخل رقم هاتفك بالصيغة المحلية (0550123456) أو الدولية (+213550123456). يتم التحقق تلقائياً عبر حماية reCAPTCHA المدمجة في أندرويد ثم إرسال رمز SMS من 6 أرقام."
              AppLanguage.FR ->
                "Saisissez votre numéro (ex : 0550123456 ou +213550123456). Vérification reCAPTCHA automatique intégrée puis envoi d’un code SMS à 6 chiffres."
              AppLanguage.EN ->
                "Enter your phone number (e.g. 0550123456 or +213550123456). Built-in Android reCAPTCHA runs automatically before sending a 6-digit SMS code."
            }

          Text(
            text = helperLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          OutlinedTextField(
            value = uiState.phoneInput,
            onValueChange = onPhoneInputChanged,
            enabled = !uiState.isBusy,
            singleLine = true,
            label = { Text(phoneLabel) },
            placeholder = { Text("+213550123456") },
            leadingIcon = {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(start = 8.dp),
              ) {
                Text(
                  text = "+213",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            },
            textStyle =
              TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textDirection = TextDirection.Ltr,
              ),
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Send,
              ),
            keyboardActions = KeyboardActions(onSend = { onSendPhoneCodeClick() }),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("phone_number_input"),
          )

          val previewE164 =
            remember(uiState.phoneInput) {
              com.example.data.model.AccountInputValidator.normalizePhoneToE164(uiState.phoneInput)
            }
          AnimatedVisibility(visible = previewE164 != null) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text =
                  when (lang) {
                    AppLanguage.AR -> "الصيغة الدولية المعتمدة للإرسال: ${previewE164.orEmpty()}"
                    AppLanguage.FR -> "Format international E.164 : ${previewE164.orEmpty()}"
                    AppLanguage.EN -> "International E.164 format: ${previewE164.orEmpty()}"
                  },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Button(
            onClick = onSendPhoneCodeClick,
            enabled = !uiState.isBusy,
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(56.dp)
                .testTag("send_phone_otp_button"),
          ) {
            if (uiState.status is LoginAuthStatus.SendingPhoneCode) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text =
                  when (lang) {
                    AppLanguage.AR -> "جاري التحقق عبر reCAPTCHA وإرسال الرمز..."
                    AppLanguage.FR -> "Vérification reCAPTCHA et envoi du SMS..."
                    AppLanguage.EN -> "Verifying reCAPTCHA & sending SMS..."
                  },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Sms,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = sendBtnLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        PhoneOtpStep.VERIFY_CODE -> {
          val otpLabel =
            when (lang) {
              AppLanguage.AR -> stringResource(R.string.phone_otp_input_label_ar)
              AppLanguage.FR -> stringResource(R.string.phone_otp_input_label_fr)
              AppLanguage.EN -> stringResource(R.string.phone_otp_input_label_en)
            }
          val verifyBtnLabel =
            when (lang) {
              AppLanguage.AR -> stringResource(R.string.phone_verify_code_btn_ar)
              AppLanguage.FR -> stringResource(R.string.phone_verify_code_btn_fr)
              AppLanguage.EN -> stringResource(R.string.phone_verify_code_btn_en)
            }
          val resendLabel =
            when (lang) {
              AppLanguage.AR -> stringResource(R.string.phone_resend_code_btn_ar)
              AppLanguage.FR -> stringResource(R.string.phone_resend_code_btn_fr)
              AppLanguage.EN -> stringResource(R.string.phone_resend_code_btn_en)
            }
          val changeNumLabel =
            when (lang) {
              AppLanguage.AR -> stringResource(R.string.phone_change_number_btn_ar)
              AppLanguage.FR -> stringResource(R.string.phone_change_number_btn_fr)
              AppLanguage.EN -> stringResource(R.string.phone_change_number_btn_en)
            }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("otp_sent_target_banner"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text =
                  when (lang) {
                    AppLanguage.AR -> "تم إرسال رمز الدخول إلى: ${uiState.normalizedPhone}"
                    AppLanguage.FR -> "Code SMS envoyé au : ${uiState.normalizedPhone}"
                    AppLanguage.EN -> "SMS code sent to: ${uiState.normalizedPhone}"
                  },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }

          OutlinedTextField(
            value = uiState.otpCodeInput,
            onValueChange = onOtpCodeChanged,
            enabled = !uiState.isBusy,
            singleLine = true,
            label = { Text(otpLabel) },
            placeholder = { Text("123456") },
            textStyle =
              TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Ltr,
              ),
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
              ),
            keyboardActions = KeyboardActions(onDone = { onVerifyPhoneCodeClick() }),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("phone_otp_code_input"),
          )

          Button(
            onClick = onVerifyPhoneCodeClick,
            enabled = !uiState.isBusy,
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(52.dp)
                .testTag("verify_phone_otp_button"),
          ) {
            if (uiState.status is LoginAuthStatus.VerifyingPhoneCode) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = verifyBtnLabel,
                style = MaterialTheme.typography.titleSmall,
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
              onClick = onEditPhoneNumberClick,
              enabled = !uiState.isBusy,
              modifier = Modifier.height(48.dp).testTag("change_phone_number_button"),
            ) {
              Text(
                text = changeNumLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            TextButton(
              onClick = onResendPhoneCodeClick,
              enabled = !uiState.isBusy,
              modifier = Modifier.height(48.dp).testTag("resend_phone_otp_button"),
            ) {
              Text(
                text = resendLabel,
                style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun CapabilityChip(icon: ImageVector, label: String) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier =
      Modifier.border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
      ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

/**
 * Crisp 4-color Google "G" emblem rendered natively in Jetpack Compose Canvas.
 */
@Composable
private fun GoogleLogoEmblem(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val strokeWidth = size.minDimension * 0.21f
    val inset = strokeWidth / 2f
    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
    val topLeft = Offset(inset, inset)

    // Red top arc
    drawArc(
      color = Color(0xFFEA4335),
      startAngle = 215f,
      sweepAngle = 100f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Yellow left arc
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 145f,
      sweepAngle = 70f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Green bottom arc
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 45f,
      sweepAngle = 100f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Blue right arc + crossbar
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -12f,
      sweepAngle = 57f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(size.width * 0.50f, size.height * 0.50f),
      end = Offset(size.width - inset * 0.4f, size.height * 0.50f),
      strokeWidth = strokeWidth * 0.92f,
      cap = StrokeCap.Butt,
    )
  }
}

/**
 * Supporting Agro-Satellite preview pane displayed on tablets/landscape (Expanded width).
 */
@Composable
private fun OasisSatelliteShowcasePanel(
  language: AppLanguage,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

  Card(
    modifier = modifier,
    shape = RoundedCornerShape(28.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.inverseSurface
      ),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle cadastral grid lines
        val step = size.minDimension / 7f
        for (i in 1..8) {
          drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(i * step, 0f),
            end = Offset(i * step, size.height),
            strokeWidth = 1f,
          )
          drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(0f, i * step),
            end = Offset(size.width, i * step),
            strokeWidth = 1f,
          )
        }

        // Stylized calibrated Tolga Oasis parcel polygon
        val poly =
          listOf(
            Offset(size.width * 0.20f, size.height * 0.28f),
            Offset(size.width * 0.72f, size.height * 0.24f),
            Offset(size.width * 0.84f, size.height * 0.64f),
            Offset(size.width * 0.52f, size.height * 0.74f),
            Offset(size.width * 0.24f, size.height * 0.60f),
          )
        val path =
          Path().apply {
            poly.forEachIndexed { idx, pt ->
              if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
            close()
          }
        drawPath(path = path, color = Color(0xFF34A853).copy(alpha = 0.28f))
        drawPath(
          path = path,
          color = Color(0xFF99F89E),
          style = Stroke(width = 3.dp.toPx()),
        )
        poly.forEach { pt ->
          drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
          drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = pt)
        }
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = primaryColor,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = onPrimaryContainer,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "Biskra • Tolga Oasis (4.82 Ha)",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.45f),
          ) {
            Text(
              text = "34°43'12.4\"N 5°22'48.9\"E",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF99F89E),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text =
              when (language) {
                AppLanguage.AR -> "تعيين القطعة الفلاحية وسجل التربة والمحاصيل"
                AppLanguage.FR -> "Délimitation Parcellaire & Fiche Agrologique"
                AppLanguage.EN -> "Parcel Delineation & Soil-Crop Registry"
              },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
          )
          Text(
            text =
              when (language) {
                AppLanguage.AR -> "مزامنة فورية للمضلعات الجغرافية، نقاط الآبار الارتوازية، وشهادات التحليل المخبري مع حسابك الفلاحي."
                AppLanguage.FR -> "Synchronisation cloud instantanée de vos îlots cadastraux, forages et bilans pédologiques."
                AppLanguage.EN -> "Instant cloud sync for cadastral polygons, artesian wells, and agrological lab records."
              },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f),
          )
        }
      }
    }
  }
}
