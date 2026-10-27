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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.AppLanguage
import com.example.ui.auth.HexagonShape
import com.example.ui.viewmodel.LoginAuthStatus
import com.example.ui.viewmodel.LoginUiState
import com.example.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
  initialLanguage: AppLanguage = AppLanguage.AR,
  onLanguageChanged: (AppLanguage) -> Unit = {},
  onAuthSuccess: () -> Unit = {},
  loginViewModel: LoginViewModel = viewModel(),
) {
  val context = LocalContext.current
  val credentialManager = remember(context) { CredentialManager.create(context) }
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
      loginViewModel.signInWithGoogle(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = onAuthSuccess,
      )
    },
    onDismissMessage = loginViewModel::dismissMessage,
  )
}

@Composable
fun LoginScreenContent(
  uiState: LoginUiState,
  onLanguageSelected: (AppLanguage) -> Unit,
  onSignInClick: () -> Unit,
  onDismissMessage: () -> Unit,
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

      // Atmospheric oasis background gradients
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(secondaryContainer.copy(alpha = 0.6f), Color.Transparent),
              center = Offset(size.width * 0.85f, size.height * 0.14f),
              radius = size.minDimension * 0.72f,
            ),
          radius = size.minDimension * 0.72f,
          center = Offset(size.width * 0.85f, size.height * 0.14f),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(primaryColor.copy(alpha = 0.14f), Color.Transparent),
              center = Offset(size.width * 0.12f, size.height * 0.86f),
              radius = size.minDimension * 0.65f,
            ),
          radius = size.minDimension * 0.65f,
          center = Offset(size.width * 0.12f, size.height * 0.86f),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(tertiaryContainer.copy(alpha = 0.35f), Color.Transparent),
              center = Offset(size.width * 0.5f, size.height * 0.95f),
              radius = size.minDimension * 0.5f,
            ),
          radius = size.minDimension * 0.5f,
          center = Offset(size.width * 0.5f, size.height * 0.95f),
        )
      }

      if (isExpanded) {
        Row(
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .padding(horizontal = 32.dp, vertical = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(28.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OasisSatelliteShowcasePanel(
            language = uiState.language,
            modifier = Modifier.weight(1.08f).fillMaxHeight(),
          )
          Box(
            modifier = Modifier.weight(0.92f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
          ) {
            LoginAuthCard(
              uiState = uiState,
              onLanguageSelected = onLanguageSelected,
              onSignInClick = onSignInClick,
              onDismissMessage = onDismissMessage,
            )
          }
        }
      } else {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          LoginAuthCard(
            uiState = uiState,
            onLanguageSelected = onLanguageSelected,
            onSignInClick = onSignInClick,
            onDismissMessage = onDismissMessage,
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
  onLanguageSelected: (AppLanguage) -> Unit,
  onSignInClick: () -> Unit,
  onDismissMessage: () -> Unit,
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top header row: Spatial telemetry badge + Language Switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
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

        LanguageSelectorBar(
          selectedLanguage = lang,
          onSelect = onLanguageSelected,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Official Fellah Numérique Hero Logo Emblem
      Surface(
        modifier =
          Modifier.size(92.dp)
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

      Spacer(modifier = Modifier.height(12.dp))

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

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = titleText,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = subtitleText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(18.dp))

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
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Feedback states (Checking session, Cancelled, or Error)
      AnimatedVisibility(visible = uiState.status is LoginAuthStatus.CheckingSavedSession) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
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
              .padding(bottom = 14.dp)
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

      val errorStatus = uiState.status as? LoginAuthStatus.Error
      AnimatedVisibility(visible = errorStatus != null) {
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .padding(bottom = 14.dp)
              .testTag("login_error_banner"),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = errorStatus?.message.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Primary Google Sign-In CTA via Jetpack CredentialManager
      Button(
        onClick = onSignInClick,
        enabled = !uiState.isBusy,
        modifier =
          Modifier.fillMaxWidth()
            .height(56.dp)
            .testTag("login_button"),
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
      ) {
        if (uiState.status is LoginAuthStatus.SigningIn) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp).testTag("login_loading_indicator"),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
        } else {
          Surface(
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            color = Color.White,
          ) {
            Box(contentAlignment = Alignment.Center) {
              GoogleLogoEmblem(modifier = Modifier.size(18.dp))
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = buttonText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Spacer(modifier = Modifier.height(14.dp))

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
private fun LanguageSelectorBar(
  selectedLanguage: AppLanguage,
  onSelect: (AppLanguage) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier.padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AppLanguage.entries.forEach { lang ->
        val selected = lang == selectedLanguage
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(9.dp))
              .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
              )
              .clickable { onSelect(lang) }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("login_lang_${lang.code}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = lang.code,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color =
              if (selected) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
          )
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
