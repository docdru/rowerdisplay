package io.github.docdru.rowerdisplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colours from the UI proposal on the design canvas. Dark theme only. */
@Immutable
data class RowerColors(
    val background: Color = Color(0xFF0B0F14),
    val surface: Color = Color(0xFF141A22),
    val outline: Color = Color(0xFF2A3441),
    val tick: Color = Color(0xFF5B6775),
    val text: Color = Color(0xFFF3F5F7),
    val textMuted: Color = Color(0xFF9AA6B2),
    val goal: Color = Color(0xFFFF8A3D),
    val goalPassed: Color = Color(0xFF7CE0C3),
    val target: Color = Color(0xFF3BA7FF),
    val targetBand: Color = Color(0x4D3BA7FF),
)

val LocalRowerColors = staticCompositionLocalOf { RowerColors() }

@Composable
fun RowerDisplayTheme(content: @Composable () -> Unit) {
    val colors = RowerColors()
    val scheme = darkColorScheme(
        primary = colors.goal,
        onPrimary = colors.background,
        secondary = colors.target,
        onSecondary = colors.background,
        secondaryContainer = colors.outline,
        onSecondaryContainer = colors.text,
        background = colors.background,
        onBackground = colors.text,
        surface = colors.surface,
        onSurface = colors.text,
        surfaceContainer = colors.surface,
        onSurfaceVariant = colors.textMuted,
        outline = colors.tick,
        outlineVariant = colors.outline,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalRowerColors provides colors, content = content)
    }
}
