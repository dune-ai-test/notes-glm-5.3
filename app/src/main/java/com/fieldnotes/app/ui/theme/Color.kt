package com.fieldnotes.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed brand constants (theme-independent).
val AccentLight = Color(0xFFE85D3F)
val AccentDark = Color(0xFFF0704F)
val PeachFixed = Color(0xFFFFD4B8)
val ButterFixed = Color(0xFFF6E8A6)
val SageFixed = Color(0xFFA8D5BA)
val LilacFixed = Color(0xFFD8C7F0)
val SkyFixed = Color(0xFFB8D4F0)
val InkFixed = Color(0xFF1A1A1A)
val CreamFixed = Color(0xFFFFF8F0)

/**
 * Semantic palette resolved per theme. Read through [FN] (composable properties)
 * so call sites stay terse.
 */
data class FNPalette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val textSoft: Color,
    val muted: Color,
    val line: Color,
    val dotGray: Color,
    val strong: Color,
    val onStrong: Color,
    val accent: Color,
    val accentSoft: Color,
    val inkCard: Color,
    val inkCardTile: Color,
    val peach: Color,
    val butter: Color,
    val sage: Color,
    val lilac: Color,
    val sky: Color,
    val inkFixed: Color
)

val LightFN = FNPalette(
    isDark = false,
    bg = Color(0xFFF3EBE2),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFFFF8F0),
    text = Color(0xFF1A1A1A),
    textSoft = Color(0xFF3D3D3D),
    muted = Color(0xFF9A9691),
    line = Color(0xFFE8E0D6),
    dotGray = Color(0xFFC5C0B8),
    strong = Color(0xFF1A1A1A),
    onStrong = Color(0xFFFFF8F0),
    accent = AccentLight,
    accentSoft = Color(0xFFF7DCD3),
    inkCard = Color(0xFF232323),
    inkCardTile = Color(0xFF2E2E2E),
    peach = PeachFixed,
    butter = ButterFixed,
    sage = SageFixed,
    lilac = LilacFixed,
    sky = SkyFixed,
    inkFixed = InkFixed
)

val DarkFN = FNPalette(
    isDark = true,
    bg = Color(0xFF161310),
    surface = Color(0xFF221F1B),
    surfaceAlt = Color(0xFF2C2823),
    text = Color(0xFFEDE5D8),
    textSoft = Color(0xFFC6BEAF),
    muted = Color(0xFF8A837A),
    line = Color(0xFF38332C),
    dotGray = Color(0xFF57514A),
    strong = Color(0xFF3A342E),
    onStrong = Color(0xFFF3EBE2),
    accent = AccentDark,
    accentSoft = Color(0xFF46281F),
    inkCard = Color(0xFF1B1815),
    inkCardTile = Color(0xFF2E2E2E),
    peach = PeachFixed,
    butter = ButterFixed,
    sage = SageFixed,
    lilac = LilacFixed,
    sky = SkyFixed,
    inkFixed = InkFixed
)

/** Theme-aware color accessors: `FN.bg`, `FN.text`, `FN.accent`, … */
object FN {
    val isDark @Composable get() = LocalFN.current.isDark
    val bg @Composable get() = LocalFN.current.bg
    val surface @Composable get() = LocalFN.current.surface
    val surfaceAlt @Composable get() = LocalFN.current.surfaceAlt
    val text @Composable get() = LocalFN.current.text
    val textSoft @Composable get() = LocalFN.current.textSoft
    val muted @Composable get() = LocalFN.current.muted
    val line @Composable get() = LocalFN.current.line
    val dotGray @Composable get() = LocalFN.current.dotGray
    val strong @Composable get() = LocalFN.current.strong
    val onStrong @Composable get() = LocalFN.current.onStrong
    val accent @Composable get() = LocalFN.current.accent
    val accentSoft @Composable get() = LocalFN.current.accentSoft
    val inkCard @Composable get() = LocalFN.current.inkCard
    val inkCardTile @Composable get() = LocalFN.current.inkCardTile
    val peach @Composable get() = LocalFN.current.peach
    val butter @Composable get() = LocalFN.current.butter
    val sage @Composable get() = LocalFN.current.sage
    val lilac @Composable get() = LocalFN.current.lilac
    val sky @Composable get() = LocalFN.current.sky
    val inkFixed @Composable get() = LocalFN.current.inkFixed
}

/** Note card backgrounds: index 0 follows the theme, 1–5 are fixed pastels. */
@Composable
fun noteColor(index: Int): Color =
    if (index <= 0) FN.surface
    else listOf(PeachFixed, ButterFixed, SageFixed, LilacFixed, SkyFixed)[(index - 1).coerceIn(0, 4)]

/** True when the note card uses a fixed pastel background (dark text required). */
fun isPastelColor(index: Int): Boolean = index > 0

/** Tag chip backgrounds: index 6 is the ink/strong chip. */
@Composable
fun tagColor(index: Int): Color =
    if (index >= 6) FN.strong
    else listOf(FN.surfaceAlt, PeachFixed, ButterFixed, SageFixed, LilacFixed, SkyFixed)[index.coerceIn(0, 5)]

/** Text color for a tag chip of the given color index. */
@Composable
fun tagTextColor(index: Int): Color = if (index >= 6) FN.onStrong else FN.text

/** Default tag color indexes for newly created tags. */
fun randomTagColorIndex(): Int = (0..5).random()
