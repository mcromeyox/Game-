package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PubgDarkColorScheme = darkColorScheme(
    primary = PubgYellow,
    onPrimary = Color(0xFF000000),
    primaryContainer = PubgYellowDark,
    onPrimaryContainer = Color.White,
    secondary = PubgCyan,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = PubgBoostOrange,
    onTertiary = Color.White,
    background = PubgDarkBg,
    onBackground = PubgTextPrimary,
    surface = PubgCardBg,
    onSurface = PubgTextPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = PubgTextSecondary,
    outline = PubgCardBorder,
    error = PubgRedZone,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Force dark tactical esports palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PubgDarkColorScheme,
        typography = Typography,
        content = content
    )
}
