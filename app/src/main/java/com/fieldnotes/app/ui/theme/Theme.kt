package com.fieldnotes.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private fun lightColors(p: FNPalette) = lightColorScheme(
    primary = p.strong,
    onPrimary = p.onStrong,
    secondary = p.accent,
    onSecondary = p.onStrong,
    tertiary = p.accent,
    background = p.bg,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.surfaceAlt,
    onSurfaceVariant = p.textSoft,
    outline = p.line,
    error = p.accent,
    onError = p.onStrong
)

private fun darkColors(p: FNPalette) = darkColorScheme(
    primary = p.strong,
    onPrimary = p.onStrong,
    secondary = p.accent,
    onSecondary = p.onStrong,
    tertiary = p.accent,
    background = p.bg,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.surfaceAlt,
    onSurfaceVariant = p.textSoft,
    outline = p.line,
    error = p.accent,
    onError = p.onStrong
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

val LocalFN = staticCompositionLocalOf { LightFN }
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

@Composable
fun FieldNotesTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) DarkFN else LightFN
    CompositionLocalProvider(LocalFN provides palette) {
        MaterialTheme(
            colorScheme = if (dark) darkColors(palette) else lightColors(palette),
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Resolves the palette from the stored appearance preference. */
@Composable
fun rememberDarkTheme(appearance: String): Boolean = when (appearance) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}
