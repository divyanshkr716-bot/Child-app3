package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = AirDroidGreen,
  onPrimary = Color(0xFF003824),
  primaryContainer = Color(0xFF005237),
  onPrimaryContainer = AirDroidGreenLight,
  secondary = AirDroidCyan,
  onSecondary = Color(0xFF003544),
  secondaryContainer = Color(0xFF004D63),
  onSecondaryContainer = Color(0xFFBCE9FF),
  tertiary = Color(0xFF8B5CF6),
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkBorder,
  error = StatusOffline
)

private val LightColorScheme = lightColorScheme(
  primary = AirDroidGreenDark,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFB8F3E5),
  onPrimaryContainer = Color(0xFF002114),
  secondary = AirDroidTeal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFC7EEFF),
  onSecondaryContainer = Color(0xFF001F29),
  tertiary = Color(0xFF7C3AED),
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightBorder,
  error = StatusOffline
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to sleek tech dark theme favored by desktop/remote clients
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

