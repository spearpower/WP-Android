package com.workspace.disciplehub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Always DH colours (no Android 12 wallpaper colours), light and dark.
private val Light = lightColorScheme(
    primary = DhForest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE6DE),
    onPrimaryContainer = DhForest,
    secondary = DhGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7EDE5),
    onSecondaryContainer = DhForest,
    tertiary = DhLime,
    onTertiary = DhOnLime,
    tertiaryContainer = Color(0xFFF2F6C4),
    onTertiaryContainer = DhOnLime,
    background = Color(0xFFF6F8F7),
    onBackground = DhInk,
    surface = Color.White,
    onSurface = DhInk,
    surfaceVariant = DhMist,
    onSurfaceVariant = Color(0xFF51605D),
    surfaceContainer = Color(0xFFF0F4F2),
    surfaceContainerHigh = Color(0xFFE8EEEB),
    outline = Color(0xFFB8C6C2),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD3BD),
    onPrimary = DhForestDark,
    primaryContainer = DhGreen,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF8FD3BD),
    onSecondary = DhForestDark,
    secondaryContainer = Color(0xFF1D4A42),
    onSecondaryContainer = Color(0xFFD7EDE5),
    tertiary = DhLime,
    onTertiary = DhOnLime,
    tertiaryContainer = Color(0xFF4A4F12),
    onTertiaryContainer = Color(0xFFF2F6C4),
    background = DhForestDark,
    onBackground = Color(0xFFE3ECE9),
    surface = DhSurfaceDark,
    onSurface = Color(0xFFE3ECE9),
    surfaceVariant = Color(0xFF223330),
    onSurfaceVariant = Color(0xFFB4C4C0),
    surfaceContainer = Color(0xFF182624),
    surfaceContainerHigh = Color(0xFF1E2E2B),
    outline = Color(0xFF5C6F6B),
)

@Composable
fun WPAndroidTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Typography, content = content)
}
