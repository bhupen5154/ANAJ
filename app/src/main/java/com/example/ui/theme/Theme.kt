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
    primary = PnbGoldLight,
    onPrimary = PnbPrimaryDark,
    primaryContainer = PnbPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = PnbSecondaryLight,
    onSecondary = Color.White,
    tertiary = PnbTertiary,
    background = PnbDarkBg,
    surface = PnbDarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PnbPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = PnbPrimaryDark,
    secondary = PnbSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4F0),
    onSecondaryContainer = PnbSecondary,
    tertiary = PnbTertiary,
    onTertiary = Color.White,
    background = PnbLightBg,
    surface = PnbLightSurface,
    onBackground = PnbTextPrimary,
    onSurface = PnbTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = PnbTextSecondary,
    outline = PnbCardStroke
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use customized PNB banking colors for consistent identity
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

