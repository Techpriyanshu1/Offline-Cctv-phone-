package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TwinGuardColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = DarkNavyBackground,
    primaryContainer = BlueDeep,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = DarkNavyBackground,
    secondaryContainer = DarkNavyCardElevated,
    onSecondaryContainer = CyanNeon,
    tertiary = StatusConnectedGreen,
    onTertiary = DarkNavyBackground,
    background = DarkNavyBackground,
    onBackground = TextWhitePrimary,
    surface = DarkNavySurface,
    onSurface = TextWhitePrimary,
    surfaceVariant = DarkNavyCard,
    onSurfaceVariant = TextMutedSecondary,
    outline = DarkNavyBorder,
    error = StatusLiveRed,
    onError = Color.White
)

@Composable
fun TwinGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TwinGuardColorScheme,
        typography = Typography,
        content = content
    )
}
