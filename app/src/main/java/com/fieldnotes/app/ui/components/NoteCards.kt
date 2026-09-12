@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.fieldnotes.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.media.AudioPlayer
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.plainText
import com.fieldnotes.app.data.model.wordCount
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.isPastelColor
import com.fieldnotes.app.ui.theme.noteColor
import com.fieldnotes.app.util.TimeFormat
import java.io.File

enum class NoteCardKind { HERO, AUDIO, CHECKLIST, SKETCH, QUOTE, TEXT }

fun noteCardKind(note: NoteEntity, blocks: List<Block>): NoteCardKind = when {
    note.kind == NoteEntity.KIND_SKETCH -> NoteCardKind.SKETCH
    note.pinned -> NoteCardKind.HERO
    blocks.any { it is Block.Audio } -> NoteCardKind.AUDIO
    blocks.any { it is Block.Checklist } -> NoteCardKind.CHECKLIST
    blocks.any { it is Block.Highlight } -> NoteCardKind.QUOTE
    else -> NoteCardKind.TEXT
}

private val PastelSoft = Color(0xFF6B665F)

@Composable
fun HomeNoteCard(
    entry: NoteWithTags,
    dotColor: Color,
    playbackState: AudioPlayer.State,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleChecklistItem: (blockIndex: Int, itemIndex: Int) -> Unit,
    onTogglePlay: (audioId: String, path: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = remember(entry.note.blocksJson) { decodeBlocks(entry.note.blocksJson) }
    when (noteCardKind(entry.note, blocks)) {
        NoteCardKind.HERO -> HeroCard(entry, blocks, onOpen, onLongPress)
        NoteCardKind.AUDIO -> AudioCard(entry, blocks, playbackState, onOpen, onLongPress, onTogglePlay)
        NoteCardKind.CHECKLIST -> ChecklistCard(entry, blocks, onOpen, onLongPress, onToggleChecklistItem)
        NoteCardKind.SKETCH -> SketchCard(entry, onOpen, onLongPress)
        NoteCardKind.QUOTE -> QuoteCard(entry, blocks, onOpen, onLongPress)
        NoteCardKind.TEXT -> TextCard(entry, blocks, dotColor, onOpen, onLongPress)
    }
}

@Composable
fun ListRowNote(
    entry: NoteWithTags,
    dotColor: Color,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = remember(entry.note.blocksJson) { decodeBlocks(entry.note.blocksJson) }
    val pastel = isPastelColor(entry.note.colorIndex)
    val kind = noteCardKind(entry.note, blocks)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (pastel) noteColor(entry.note.colorIndex) else FN.surfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = previewIcon(kind),
                    contentDescription = null,
                    tint = if (pastel) FN.inkFixed else FN.textSoft,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = entry.note.title.ifBlank { "Untitled" },
                    style = FT.cardTitle,
                    color = FN.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = previewText(blocks),
                    style = FT.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = FN.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = TimeFormat.smartDate(entry.note.updatedAt),
                    style = FT.monoTiny,
                    color = FN.muted
                )
                if (entry.note.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Pinned",
                        tint = FN.accent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

private fun previewIcon(kind: NoteCardKind): ImageVector = when (kind) {
    NoteCardKind.HERO -> Icons.Outlined.PushPin
    NoteCardKind.AUDIO -> Icons.Outlined.GraphicEq
    NoteCardKind.CHECKLIST -> Icons.Outlined.Checklist
    NoteCardKind.SKETCH -> Icons.Outlined.Edit
    NoteCardKind.QUOTE -> Icons.Outlined.MenuBook
    NoteCardKind.TEXT -> Icons.Outlined.TextFields
}

private fun previewText(blocks: List<Block>): String {
    val checklist = blocks.filterIsInstance<Block.Checklist>().firstOrNull()
    if (checklist != null) {
        val items = checklist.items.take(2).joinToString(" · ") { it.text }
        val head = "${checklist.doneCount}/${checklist.items.size} done"
        return if (items.isBlank()) head else "$head · $items"
    }
    val audio = blocks.filterIsInstance<Block.Audio>().firstOrNull()
    if (audio != null) return "Voice memo · ${TimeFormat.duration(audio.durationMs)}"
    val image = blocks.filterIsInstance<Block.Image>().firstOrNull()
    if (image != null) return image.caption.ifBlank { "Photo note" }
    val highlight = blocks.filterIsInstance<Block.Highlight>().firstOrNull()
    if (highlight != null) return highlight.text
    return blocks.filterIsInstance<Block.Paragraph>().firstOrNull()?.text ?: ""
}

@Composable
private fun CardShell(
    background: Color,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(cornerRadius.dp),
        color = background,
        border = BorderStroke(1.dp, FN.line),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun HeroCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val snippet = blocks.filterIsInstance<Block.Paragraph>().firstOrNull()?.text ?: ""
    CardShell(FN.peach, onOpen, onLongPress, cornerRadius = 28) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = FN.onStrong,
                border = BorderStroke(1.dp, FN.line)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = FN.accent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text("PINNED", style = FT.monoBadge, color = FN.inkFixed)
                }
            }
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = null,
                tint = FN.inkFixed.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitleLarge,
            color = FN.inkFixed
        )
        if (snippet.isNotBlank()) {
            Text(
                text = snippet,
                style = FT.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
                color = PastelSoft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            entry.tags.firstOrNull()?.let { tag ->
                Surface(shape = RoundedCornerShape(50), color = FN.onStrong) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ColoredDot(FN.accent, size = 6.dp)
                        Text(tag.name, style = FT.monoChip, color = FN.inkFixed)
                    }
                }
            }
            Text(
                text = "${TimeFormat.smartDate(entry.note.updatedAt)} · ${readMinutes(blocks)} min",
                style = FT.monoTiny,
                color = FN.inkFixed.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun ChecklistCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleChecklistItem: (Int, Int) -> Unit
) {
    val blockIndex = blocks.indexOfFirst { it is Block.Checklist }
    val checklist = blocks[blockIndex] as? Block.Checklist ?: return
    val bulletsOnly = isPastelColor(entry.note.colorIndex)
    val ink = if (bulletsOnly) FN.inkFixed else FN.text
    val soft = if (bulletsOnly) PastelSoft else FN.muted
    CardShell(noteColor(entry.note.colorIndex), onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Outlined.Checklist,
                contentDescription = null,
                tint = if (bulletsOnly) FN.inkFixed else FN.sage,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = soft
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = ink
        )
        checklist.items.take(4).forEachIndexed { itemIndex, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bulletsOnly) {
                    ColoredDot(ink, size = 5.dp)
                } else {
                    MiniCheckbox(done = item.done) {
                        onToggleChecklistItem(blockIndex, itemIndex)
                    }
                }
                Text(
                    text = item.text,
                    style = FT.bodySmall.copy(fontSize = 11.sp),
                    color = if (item.done && !bulletsOnly) soft else ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!bulletsOnly) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${checklist.doneCount} of ${checklist.items.size} done",
                    style = FT.monoTiny,
                    color = soft
                )
                MiniProgressBar(checklist.progress, track = FN.bg, fill = FN.sage)
            }
        }
    }
}

@Composable
private fun SketchCard(
    entry: NoteWithTags,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    CardShell(FN.butter, onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(shape = RoundedCornerShape(50), color = FN.onStrong) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = FN.inkFixed,
                        modifier = Modifier.size(11.dp)
                    )
                    Text("SKETCH", style = FT.monoBadge, color = FN.inkFixed)
                }
            }
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = FN.inkFixed.copy(alpha = 0.5f)
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = FN.inkFixed
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FN.onStrong,
            border = BorderStroke(1.dp, FN.line)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .size(width = 68.dp, height = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FN.onStrong)
                        .border(1.dp, FN.inkFixed, RoundedCornerShape(8.dp))
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(FN.dotGray)
                )
                Box(
                    Modifier
                        .size(width = 92.dp, height = 1.dp)
                        .background(FN.dotGray)
                )
            }
        }
        Text("Pen · 3 layers", style = FT.monoTiny, color = FN.inkFixed.copy(alpha = 0.45f))
    }
}

@Composable
private fun AudioCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    playbackState: AudioPlayer.State,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onTogglePlay: (String, String) -> Unit
) {
    val audio = blocks.filterIsInstance<Block.Audio>().firstOrNull() ?: return
    val audioId = "note-${entry.note.id}"
    val active = playbackState.activeId == audioId
    CardShell(FN.surface, onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(FN.strong)
                    .combinedClickable(
                        onClick = { onTogglePlay(audioId, audio.path) },
                        onLongClick = onLongPress
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (active && playbackState.isPlaying) {
                        Icons.Outlined.Pause
                    } else {
                        Icons.Outlined.PlayArrow
                    },
                    contentDescription = if (active && playbackState.isPlaying) "Pause" else "Play",
                    tint = FN.onStrong,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = TimeFormat.duration(audio.durationMs),
                style = FT.monoTiny,
                color = FN.muted
            )
        }
        Text(
            text = audio.title.ifBlank { entry.note.title.ifBlank { "Voice memo" } },
            style = FT.cardTitle.copy(fontSize = 12.5.sp),
            color = FN.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Waveform(
            amplitudes = audio.amplitudes,
            progress = if (active) playbackState.progress else 0f,
            modifier = Modifier.fillMaxWidth(),
            maxHeight = 14.dp,
            barWidth = 3.dp,
            gap = 2.dp,
            bars = 18
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = FN.surfaceAlt,
                border = BorderStroke(1.dp, FN.line)
            ) {
                Row(
                    Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ColoredDot(FN.sage, size = 5.dp)
                    Text("Audio", style = FT.monoChip, color = FN.text)
                }
            }
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = FN.muted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuoteCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val highlight = blocks.filterIsInstance<Block.Highlight>().firstOrNull()
    val pastel = isPastelColor(entry.note.colorIndex)
    val ink = if (pastel) FN.inkFixed else FN.text
    val soft = if (pastel) PastelSoft else FN.muted
    CardShell(noteColor(entry.note.colorIndex), onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = soft
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = ink
        )
        if (highlight != null && highlight.text.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (pastel) FN.onStrong else FN.surfaceAlt
            ) {
                Text(
                    text = highlight.text,
                    style = FT.bodySmall.copy(fontSize = 12.sp, fontStyle = FontStyle.Italic),
                    color = FN.textSoft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        entry.tags.firstOrNull()?.let { tag ->
            Surface(shape = RoundedCornerShape(50), color = if (pastel) FN.onStrong else FN.surfaceAlt) {
                Text(
                    text = tag.name.replaceFirstChar { it.uppercase() },
                    style = FT.chipSmall,
                    color = ink,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun TextCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    dotColor: Color,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val snippet = blocks.filterIsInstance<Block.Paragraph>().firstOrNull()?.text ?: ""
    val image = blocks.filterIsInstance<Block.Image>().firstOrNull()
    val pastel = isPastelColor(entry.note.colorIndex)
    val ink = if (pastel) FN.inkFixed else FN.text
    val soft = if (pastel) PastelSoft else FN.muted
    CardShell(noteColor(entry.note.colorIndex), onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ColoredDot(dotColor, size = 9.dp)
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = soft
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = ink
        )
        if (image != null && image.path.isNotBlank()) {
            AsyncImage(
                model = File(image.path),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FN.surfaceAlt)
            )
        } else if (snippet.isNotBlank()) {
            Text(
                text = snippet,
                style = FT.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = FN.textSoft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (entry.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.tags.take(2).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (pastel) FN.onStrong else FN.surfaceAlt
                    ) {
                        Text(
                            text = "#${tag.name}",
                            style = FT.monoChip,
                            color = ink,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniProgressBar(progress: Float, track: Color, fill: Color) {
    Box(
        Modifier
            .size(width = 48.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(fill)
        )
    }
}

private fun readMinutes(blocks: List<Block>): Int =
    (wordCount(blocks.plainTextSafe()) / 200).coerceAtLeast(1)

private fun List<Block>.plainTextSafe(): String = try {
    plainText()
} catch (_: Exception) {
    ""
}
