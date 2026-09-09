package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = UstozPrimary,
  onPrimary = UstozOnPrimary,
  primaryContainer = UstozOnPrimaryContainer,
  onPrimaryContainer = UstozPrimaryContainer,
  secondary = UstozSecondary,
  onSecondary = UstozOnSecondary,
  secondaryContainer = UstozSecondaryContainer,
  onSecondaryContainer = UstozOnSecondaryContainer,
  tertiary = UstozTertiary,
  onTertiary = UstozOnTertiary,
  background = UstozBackgroundDark,
  surface = UstozSurfaceDark,
  onBackground = UstozOnSurfaceDark,
  onSurface = UstozOnSurfaceDark,
  surfaceVariant = UstozSurfaceVariantDark,
  onSurfaceVariant = UstozOnSurfaceVariantDark,
  error = UstozError,
  onError = UstozOnError,
  errorContainer = UstozErrorContainer,
  onErrorContainer = UstozOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
  primary = UstozPrimary,
  onPrimary = UstozOnPrimary,
  primaryContainer = UstozPrimaryContainer,
  onPrimaryContainer = UstozOnPrimaryContainer,
  secondary = UstozSecondary,
  onSecondary = UstozOnSecondary,
  secondaryContainer = UstozSecondaryContainer,
  onSecondaryContainer = UstozOnSecondaryContainer,
  tertiary = UstozTertiary,
  onTertiary = UstozOnTertiary,
  background = UstozBackgroundLight,
  surface = UstozSurfaceLight,
  onBackground = UstozOnSurfaceLight,
  onSurface = UstozOnSurfaceLight,
  surfaceVariant = UstozSurfaceVariantLight,
  onSurfaceVariant = UstozOnSurfaceVariantLight,
  error = UstozError,
  onError = UstozOnError,
  errorContainer = UstozErrorContainer,
  onErrorContainer = UstozOnErrorContainer
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Keep brand aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
