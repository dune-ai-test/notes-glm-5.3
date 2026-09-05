@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.fieldnotes.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.fieldnotes.app.R

// The bundled TTFs are variable fonts; each weight picks the matching wght axis value.
private fun variableFont(resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val GeistFamily = FontFamily(
    variableFont(R.font.geist_variable, FontWeight.Normal),
    variableFont(R.font.geist_variable, FontWeight.Medium),
    variableFont(R.font.geist_variable, FontWeight.SemiBold),
    variableFont(R.font.geist_variable, FontWeight.Bold),
    variableFont(R.font.geist_variable, FontWeight.ExtraBold)
)

val GeistMonoFamily = FontFamily(
    variableFont(R.font.geist_mono_variable, FontWeight.Normal),
    variableFont(R.font.geist_mono_variable, FontWeight.Medium),
    variableFont(R.font.geist_mono_variable, FontWeight.Bold)
)

val InterFamily = FontFamily(
    variableFont(R.font.inter_variable, FontWeight.Normal),
    variableFont(R.font.inter_variable, FontWeight.Medium),
    variableFont(R.font.inter_variable, FontWeight.SemiBold),
    variableFont(R.font.inter_variable, FontWeight.Bold)
)

private fun geist(weight: FontWeight, size: TextUnit, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = GeistFamily,
    fontWeight = weight,
    fontSize = size,
    letterSpacing = letterSpacing
)

private fun inter(weight: FontWeight, size: TextUnit, lineHeight: TextUnit = size * 1.45f) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight
)

private fun mono(weight: FontWeight, size: TextUnit, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = GeistMonoFamily,
    fontWeight = weight,
    fontSize = size,
    letterSpacing = letterSpacing
)

object FT {
    val screenTitle = geist(FontWeight.ExtraBold, 34.sp, (-1).sp)
    val greeting = geist(FontWeight.ExtraBold, 30.sp, (-0.8).sp)
    val heroTitle = geist(FontWeight.ExtraBold, 32.sp, (-0.8).sp)
    val editorTitle = geist(FontWeight.ExtraBold, 30.sp, (-0.8).sp)
    val statNumber = geist(FontWeight.ExtraBold, 30.sp, (-0.5).sp)
    val sectionTitle = geist(FontWeight.Bold, 17.sp, (-0.3).sp)
    val cardTitleLarge = geist(FontWeight.Bold, 19.sp, (-0.5).sp)
    val cardTitle = geist(FontWeight.Bold, 14.sp, (-0.2).sp)
    val cardTitleSmall = geist(FontWeight.Bold, 13.sp)
    val heading = geist(FontWeight.Bold, 20.sp, (-0.4).sp)
    val body = inter(FontWeight.Normal, 15.sp)
    val bodySmall = inter(FontWeight.Normal, 13.sp)
    val chip = inter(FontWeight.SemiBold, 12.sp)
    val chipSmall = inter(FontWeight.Medium, 11.sp)
    val button = inter(FontWeight.SemiBold, 13.sp)
    val monoLabel = mono(FontWeight.Medium, 10.sp, 1.2.sp)
    val monoTiny = mono(FontWeight.Normal, 10.sp, 0.4.sp)
    val monoBadge = mono(FontWeight.Bold, 9.sp, 1.sp)
    val monoChip = mono(FontWeight.SemiBold, 10.sp)
}
