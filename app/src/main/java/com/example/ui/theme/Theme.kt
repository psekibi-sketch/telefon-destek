package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = iOSBlue,
    onPrimary = Color.White,
    secondary = iOSBlue,
    onSecondary = Color.White,
    tertiary = iOSOrange,
    onTertiary = Color.White,
    background = iOSBackground,
    surface = iOSCard,
    onSurface = iOSTextPrimary,
    surfaceVariant = iOSCardSecondary,
    onSurfaceVariant = iOSTextSecondary,
    surfaceContainer = iOSCard,
    surfaceContainerHigh = iOSCardSecondary,
    surfaceContainerHighest = iOSCardSecondary,
    surfaceContainerLow = iOSCard,
    surfaceContainerLowest = iOSBackground,
    outline = iOSBorder,
    outlineVariant = iOSCardTertiary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = iOSBlue,
    onPrimary = Color.White,
    secondary = iOSBlue,
    tertiary = iOSOrange,
    background = iOSBackground,
    surface = iOSCard,
    onSurface = iOSTextPrimary,
    surfaceVariant = iOSCardSecondary,
    onSurfaceVariant = iOSTextSecondary,
    surfaceContainer = iOSCard,
    surfaceContainerHigh = iOSCardSecondary,
    surfaceContainerHighest = iOSCardSecondary,
    surfaceContainerLow = iOSCard,
    surfaceContainerLowest = iOSBackground,
    outline = iOSBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to iOS dark mode as requested in requirement 2
  dynamicColor: Boolean = false, // Keep consistent iOS theme across devices
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
