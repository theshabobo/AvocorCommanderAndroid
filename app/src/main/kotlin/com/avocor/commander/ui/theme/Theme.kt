package com.avocor.commander.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AvocorDarkColorScheme = darkColorScheme(
    primary = AvocorBlue,
    onPrimary = TextPrimary,
    primaryContainer = AvocorBlueDark,
    onPrimaryContainer = TextPrimary,
    secondary = AvocorBlueLight,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = StatusError,
    onError = TextPrimary,
    outline = TextMuted
)

@Composable
fun AvocorCommanderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AvocorDarkColorScheme,
        typography = AvocorTypography,
        content = content
    )
}
