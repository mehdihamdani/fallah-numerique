package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightOasisColorScheme =
  lightColorScheme(
    primary = OasisPrimary,
    onPrimary = OasisOnPrimary,
    primaryContainer = OasisPrimaryFixed,
    onPrimaryContainer = OasisOnPrimaryFixed,
    secondary = OasisSecondary,
    onSecondary = OasisOnSecondary,
    secondaryContainer = OasisSecondaryContainer,
    onSecondaryContainer = OasisOnSecondaryContainer,
    tertiary = OasisTertiary,
    onTertiary = OasisOnTertiary,
    tertiaryContainer = OasisTertiaryContainer,
    onTertiaryContainer = OasisOnTertiaryContainer,
    background = OasisBackground,
    onBackground = OasisOnBackground,
    surface = OasisSurface,
    onSurface = OasisOnSurface,
    surfaceVariant = OasisSurfaceHighest,
    onSurfaceVariant = OasisOnSurfaceVariant,
    surfaceContainerLowest = OasisSurfaceLowest,
    surfaceContainerLow = OasisSurfaceLow,
    surfaceContainer = OasisSurfaceContainer,
    surfaceContainerHigh = OasisSurfaceHigh,
    surfaceContainerHighest = OasisSurfaceHighest,
    outline = OasisOutline,
    outlineVariant = OasisOutlineVariant,
    inverseSurface = OasisInverseSurface,
    inverseOnSurface = OasisInverseOnSurface,
    error = OasisError,
    onError = OasisOnError,
    errorContainer = OasisErrorContainer,
    onErrorContainer = OasisOnErrorContainer,
  )

private val DarkOasisColorScheme =
  darkColorScheme(
    primary = OasisDarkPrimary,
    onPrimary = OasisDarkOnPrimary,
    primaryContainer = OasisDarkPrimaryContainer,
    onPrimaryContainer = OasisDarkOnPrimaryContainer,
    secondary = OasisDarkSecondary,
    onSecondary = OasisDarkOnSecondary,
    secondaryContainer = OasisDarkSecondaryContainer,
    onSecondaryContainer = OasisDarkOnSecondaryContainer,
    tertiary = OasisDarkTertiary,
    onTertiary = OasisDarkOnTertiary,
    tertiaryContainer = OasisDarkTertiaryContainer,
    onTertiaryContainer = OasisDarkOnTertiaryContainer,
    background = OasisDarkBackground,
    onBackground = OasisDarkOnBackground,
    surface = OasisDarkSurface,
    onSurface = OasisDarkOnSurface,
    surfaceVariant = OasisDarkSurfaceHighest,
    onSurfaceVariant = OasisDarkOnSurfaceVariant,
    surfaceContainerLowest = OasisDarkBackground,
    surfaceContainerLow = OasisDarkSurfaceContainer,
    surfaceContainer = OasisDarkSurfaceContainer,
    surfaceContainerHigh = OasisDarkSurfaceHigh,
    surfaceContainerHighest = OasisDarkSurfaceHighest,
    outline = OasisDarkOutline,
    outlineVariant = OasisDarkOutlineVariant,
    error = OasisError,
    onError = OasisOnError,
    errorContainer = OasisErrorContainer,
    onErrorContainer = OasisOnErrorContainer,
  )

val OasisShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkOasisColorScheme else LightOasisColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = OasisShapes,
    content = content,
  )
}
