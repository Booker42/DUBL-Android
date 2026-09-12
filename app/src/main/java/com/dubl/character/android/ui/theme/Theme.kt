package com.dubl.character.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = DublAccent,
    onPrimary = Color(0xFF211A11),
    primaryContainer = DublAccentSoft,
    onPrimaryContainer = DublText,
    background = DublBackground,
    onBackground = DublText,
    surface = DublSurface,
    onSurface = DublText,
    surfaceVariant = DublSurfaceRaised,
    onSurfaceVariant = DublMuted,
    outline = DublBorder,
    error = DublDanger,
)

@Composable
fun DublTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = DublTypography,
        content = content,
    )
}
