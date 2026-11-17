package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Intentionally warm ivory light theme optimized for 60+ visual comfort and contrast
private val SeniorIvoryColorScheme =
  lightColorScheme(
    primary = MedCompletedGreen,
    onPrimary = Color.White,
    primaryContainer = MedCompletedGreenSoft,
    onPrimaryContainer = MedCompletedGreenDark,
    secondary = WaterPrimaryBlue,
    onSecondary = Color.White,
    secondaryContainer = WaterCardSurface,
    onSecondaryContainer = WaterPrimaryBlueDark,
    background = IvoryBackground,
    onBackground = InkPrimary,
    surface = WarmCardSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmHeaderSurface,
    onSurfaceVariant = InkSecondary,
    outline = WarmStoneBorder,
  )

@Composable
fun SeniorCareTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = SeniorIvoryColorScheme, typography = AppTypography, content = content)
}
