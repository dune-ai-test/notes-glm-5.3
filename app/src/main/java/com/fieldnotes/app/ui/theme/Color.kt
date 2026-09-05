package com.fieldnotes.app.ui.theme

import androidx.compose.ui.graphics.Color

val WarmPaper = Color(0xFFF3EBE2)
val Cream = Color(0xFFFFF8F0)
val CardWhite = Color(0xFFFFFFFF)
val Ink = Color(0xFF1A1A1A)
val InkSoft = Color(0xFF3D3D3D)
val Muted = Color(0xFF9A9691)
val Line = Color(0xFFE8E0D6)
val Accent = Color(0xFFE85D3F)
val Peach = Color(0xFFFFD4B8)
val Butter = Color(0xFFF6E8A6)
val Sage = Color(0xFFA8D5BA)
val Lilac = Color(0xFFD8C7F0)
val Sky = Color(0xFFB8D4F0)
val DotGray = Color(0xFFC5C0B8)
val InkCard = Color(0xFF232323)

val NoteColors = listOf(CardWhite, Peach, Butter, Sage, Lilac, Sky)
val TagColors = listOf(Cream, Peach, Butter, Sage, Lilac, Sky, Ink)

fun noteColor(index: Int): Color = NoteColors[index.coerceIn(0, NoteColors.lastIndex)]
fun tagColor(index: Int): Color = TagColors[index.coerceIn(0, TagColors.lastIndex)]
