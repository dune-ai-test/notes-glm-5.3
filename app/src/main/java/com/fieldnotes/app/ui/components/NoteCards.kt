@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.fieldnotes.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.fieldnotes.app.ui.theme.Accent
import com.fieldnotes.app.ui.theme.Butter
import com.fieldnotes.app.ui.theme.CardWhite
import com.fieldnotes.app.ui.theme.DotGray
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.Ink
import com.fieldnotes.app.ui.theme.InkSoft
import com.fieldnotes.app.ui.theme.Line
import com.fieldnotes.app.ui.theme.Muted
import com.fieldnotes.app.ui.theme.Peach
import com.fieldnotes.app.ui.theme.Sage
import com.fieldnotes.app.ui.theme.WarmPaper
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
    val snippet = remember(entry.note.blocksJson) {
        decodeBlocks(entry.note.blocksJson).plainText()
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, Line),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColoredDot(noteColor(entry.note.colorIndex), size = 10.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = entry.note.title.ifBlank { "Untitled" },
                    style = FT.cardTitle,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (snippet.isNotBlank()) {
                    Text(
                        text = snippet,
                        style = FT.bodySmall.copy(fontSize = 11.5.sp),
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = Muted
            )
            if (entry.note.pinned) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Pinned",
                    tint = Accent,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
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
        border = BorderStroke(1.dp, Line),
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
    CardShell(Peach, onOpen, onLongPress, cornerRadius = 28) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = CardWhite,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text("PINNED", style = FT.monoBadge, color = Ink)
                }
            }
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = null,
                tint = Ink.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitleLarge,
            color = Ink
        )
        if (snippet.isNotBlank()) {
            Text(
                text = snippet,
                style = FT.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
                color = InkSoft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            entry.tags.firstOrNull()?.let { tag ->
                Surface(shape = RoundedCornerShape(50), color = CardWhite) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ColoredDot(Accent, size = 6.dp)
                        Text(tag.name, style = FT.monoChip, color = Ink)
                    }
                }
            }
            Text(
                text = "${TimeFormat.smartDate(entry.note.updatedAt)} · ${readMinutes(blocks)} min",
                style = FT.monoTiny,
                color = Ink.copy(alpha = 0.55f)
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
    val bulletsOnly = entry.note.colorIndex > 0
    CardShell(noteColor(entry.note.colorIndex), onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Outlined.Checklist,
                contentDescription = null,
                tint = Sage,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = Muted
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = Ink
        )
        checklist.items.take(4).forEachIndexed { itemIndex, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bulletsOnly) {
                    ColoredDot(InkSoft, size = 5.dp)
                } else {
                    MiniCheckbox(done = item.done) {
                        onToggleChecklistItem(blockIndex, itemIndex)
                    }
                }
                Text(
                    text = item.text,
                    style = FT.bodySmall.copy(fontSize = 11.sp),
                    color = if (item.done && !bulletsOnly) Muted else InkSoft,
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
                    color = Muted
                )
                MiniProgressBar(checklist.progress, track = WarmPaper, fill = Sage)
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
    CardShell(Butter, onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(shape = RoundedCornerShape(50), color = CardWhite) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(11.dp)
                    )
                    Text("SKETCH", style = FT.monoBadge, color = Ink)
                }
            }
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = Ink.copy(alpha = 0.5f)
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = Ink
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardWhite,
            border = BorderStroke(1.dp, Line)
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
                        .background(CardWhite)
                        .border(1.dp, Ink, RoundedCornerShape(8.dp))
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DotGray)
                )
                Box(
                    Modifier
                        .size(width = 92.dp, height = 1.dp)
                        .background(DotGray)
                )
            }
        }
        Text("Pen · 3 layers", style = FT.monoTiny, color = Ink.copy(alpha = 0.45f))
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
    CardShell(CardWhite, onOpen, onLongPress) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink)
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
                    tint = CardWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = audio.title.ifBlank { entry.note.title },
                        style = FT.cardTitleSmall.copy(fontSize = 12.5.sp),
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = TimeFormat.duration(audio.durationMs),
                        style = FT.monoTiny,
                        color = Muted
                    )
                }
                Waveform(
                    amplitudes = audio.amplitudes,
                    progress = if (active) playbackState.progress else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = WarmPaper,
                        border = BorderStroke(1.dp, Line)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            ColoredDot(Sage, size = 6.dp)
                            Text("Audio", style = FT.monoChip, color = Ink)
                        }
                    }
                    Text(
                        text = TimeFormat.smartDate(entry.note.updatedAt),
                        style = FT.monoTiny,
                        color = Muted
                    )
                }
            }
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
    CardShell(noteColor(entry.note.colorIndex), onOpen, onLongPress) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = InkSoft,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = Ink.copy(alpha = 0.5f)
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = Ink
        )
        if (highlight != null && highlight.text.isNotBlank()) {
            Surface(shape = RoundedCornerShape(12.dp), color = CardWhite) {
                Text(
                    text = highlight.text,
                    style = FT.bodySmall.copy(fontSize = 12.sp, fontStyle = FontStyle.Italic),
                    color = InkSoft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        entry.tags.firstOrNull()?.let { tag ->
            Surface(shape = RoundedCornerShape(50), color = CardWhite) {
                Text(
                    text = tag.name.replaceFirstChar { it.uppercase() },
                    style = FT.chipSmall,
                    color = Ink,
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
                color = Muted
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = Ink
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
                    .background(WarmPaper)
            )
        } else if (snippet.isNotBlank()) {
            Text(
                text = snippet,
                style = FT.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = InkSoft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (entry.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.tags.take(2).forEach { tag ->
                    Surface(shape = RoundedCornerShape(50), color = CardWhite) {
                        Text(
                            text = "#${tag.name}",
                            style = FT.monoChip,
                            color = Ink,
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
