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

private val DarkColorScheme = darkColorScheme(
    primary = YtRedPrimary,
    onPrimary = Color.White,
    primaryContainer = YtRedContainer,
    onPrimaryContainer = OnYtRedContainer,
    secondary = AccentEmerald,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextWhitePrimary,
    surface = DarkSurface,
    onSurface = TextWhitePrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMutedSecondary,
    outline = DarkSurfaceBorder
)

private val LightColorScheme = lightColorScheme(
    primary = YtRedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0E2),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    background = Color(0xFFF9F9FB),
    onBackground = Color(0xFF141418),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141418),
    surfaceVariant = Color(0xFFF0F0F4),
    onSurfaceVariant = Color(0xFF5A5A66),
    outline = Color(0xFFD4D4DE)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek music-focused dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
