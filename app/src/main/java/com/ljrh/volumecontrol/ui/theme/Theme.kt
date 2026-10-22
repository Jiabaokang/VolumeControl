package com.ljrh.volumecontrol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5650A0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE7FB),
    onPrimaryContainer = Color(0xFF29234D),
    secondaryContainer = Color(0xFFFFE9CD),
    onSecondaryContainer = Color(0xFF583C16),
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF242333),
    surface = Color.White,
    onSurface = Color(0xFF242333),
    onSurfaceVariant = Color(0xFF646273),
    surfaceVariant = Color(0xFFEFEEF4),
    outlineVariant = Color(0xFFE3E1EC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC5BFFF),
    onPrimary = Color(0xFF2D275F),
    primaryContainer = Color(0xFF302B4D),
    onPrimaryContainer = Color(0xFFE7E1FF),
    secondaryContainer = Color(0xFF4B3922),
    onSecondaryContainer = Color(0xFFFFDEAE),
    background = Color(0xFF131218),
    onBackground = Color(0xFFF0EDF6),
    surface = Color(0xFF201E28),
    onSurface = Color(0xFFF0EDF6),
    onSurfaceVariant = Color(0xFFC0BBCD),
    surfaceVariant = Color(0xFF2A2735),
    outlineVariant = Color(0xFF433F50)
)

@Composable
fun VolumeControlTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
