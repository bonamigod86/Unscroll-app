package com.unscroll.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF0B0F0E)
val SurfaceDark = Color(0xFF141B19)
val SurfaceRaised = Color(0xFF1B2622)
val AccentGreen = Color(0xFF34E1A1)
val AccentDim = Color(0xFF1D8F66)
val TextPrimary = Color(0xFFEDF5F1)
val TextSecondary = Color(0xFF9DB4AB)
val DangerRed = Color(0xFFFF6B5E)

private val UnscrollColors = darkColorScheme(
    primary = AccentGreen,
    onPrimary = BackgroundDark,
    primaryContainer = AccentDim,
    secondary = AccentDim,
    onSecondary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = BackgroundDark,
    outline = TextSecondary
)

@Composable
fun UnscrollTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = UnscrollColors,
        content = content
    )
}
