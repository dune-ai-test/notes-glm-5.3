package com.fieldnotes.app.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = CardWhite,
    secondary = Accent,
    onSecondary = CardWhite,
    tertiary = Accent,
    background = WarmPaper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Accent,
    onError = CardWhite
)

private val AppTypography = Typography(
    displayLarge = FT.screenTitle,
    displayMedium = FT.heroTitle,
    headlineLarge = FT.editorTitle,
    headlineMedium = FT.greeting,
    titleLarge = FT.sectionTitle,
    titleMedium = FT.cardTitle,
    bodyLarge = FT.body,
    bodyMedium = FT.bodySmall,
    labelLarge = FT.button,
    labelMedium = FT.chipSmall
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

@androidx.compose.runtime.Composable
fun FieldNotesTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
