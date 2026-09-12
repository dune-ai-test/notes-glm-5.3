@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.fieldnotes.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.media.AudioPlayer
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.AppSettings
import com.fieldnotes.app.data.repo.NoteSort
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.HomeNoteCard
import com.fieldnotes.app.ui.components.ListRowNote
import com.fieldnotes.app.ui.components.PillChip
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.components.TagChipView
import com.fieldnotes.app.ui.components.noteCardKind
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.LocalHapticsEnabled
import com.fieldnotes.app.ui.theme.noteColor
import com.fieldnotes.app.util.TimeFormat
import java.util.Calendar

@Composable
fun HomeScreen(
    navController: NavHostController,
    onOpenSettings: () -> Unit
) {
    val container = LocalAppContainer.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(container.noteRepository) } }
    )
    val notes by vm.visibleNotes.collectAsStateWithLifecycle()
    val noteCount by vm.noteCount.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val pinnedOnly by vm.pinnedOnly.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val tagFilter by vm.tagFilter.collectAsStateWithLifecycle()
    val playback by container.audioPlayer.state.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    var gridMode by rememberSaveable { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }
    var actionNote by remember { mutableStateOf<NoteWithTags?>(null) }

    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val foldersById = remember(folders) { folders.associateBy { it.id } }

    val openNote: (Long) -> Unit = { id -> navController.navigate("editor/$id") }
    val dotColorFor: @Composable (NoteWithTags) -> Color = { entry ->
        foldersById[entry.note.folderId]?.let { noteColor(it.colorIndex) } ?: FN.accent
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalItemSpacing = 14.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 132.dp)
        ) {
            item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HeaderRow(
                        noteCount = noteCount,
                        initial = settings.userName.take(1).uppercase().ifBlank { "F" }
                    ) { onOpenSettings() }
                    SearchBarRow(
                        onSearch = { navController.navigate("search") },
                        onFilters = { showFilters = true }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        PillChip(
                            label = "All",
                            selected = !pinnedOnly,
                            onClick = { vm.setPinnedOnly(false) },
                            count = noteCount
                        )
                        PillChip(
                            label = "Pinned",
                            selected = pinnedOnly,
                            onClick = { vm.setPinnedOnly(true) },
                            icon = Icons.Outlined.PushPin
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel("Your notes")
                        GridListToggle(
                            gridMode = gridMode,
                            onGrid = { gridMode = true },
                            onList = { gridMode = false }
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 70.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Nothing here yet", style = FT.sectionTitle, color = FN.text)
                        Text("Tap + to capture something.", style = FT.bodySmall, color = FN.muted)
                    }
                }
            }

            if (gridMode) {
                notes.forEach { entry ->
                    val blocks = decodeBlocks(entry.note.blocksJson)
                    val kind = noteCardKind(entry.note, blocks)
                    val wide = kind == com.fieldnotes.app.ui.components.NoteCardKind.HERO
                    val card: @Composable () -> Unit = {
                        HomeNoteCard(
                            entry = entry,
                            dotColor = dotColorFor(entry),
                            playbackState = playback,
                            onOpen = { openNote(entry.note.id) },
                            onLongPress = {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                actionNote = entry
                            },
                            onToggleChecklistItem = { blockIndex, itemIndex ->
                                vm.toggleChecklistItem(entry, blockIndex, itemIndex)
                            },
                            onTogglePlay = { id, path -> container.audioPlayer.toggle(id, path) }
                        )
                    }
                    if (wide) {
                        item(key = "home-${entry.note.id}", span = StaggeredGridItemSpan.FullLine) { card() }
                    } else {
                        item(key = "home-${entry.note.id}") { card() }
                    }
                }
            } else {
                notes.forEach { entry ->
                    item(key = "list-${entry.note.id}", span = StaggeredGridItemSpan.FullLine) {
                        ListRowNote(
                            entry = entry,
                            dotColor = dotColorFor(entry),
                            onOpen = { openNote(entry.note.id) },
                            onLongPress = {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                actionNote = entry
                            }
                        )
                    }
                }
            }
        }

        if (showFilters) {
            FilterSheet(
                tags = tags,
                sort = sort,
                tagFilter = tagFilter,
                onSort = vm::setSort,
                onToggleTag = vm::toggleTag,
                onReset = vm::resetFilters,
                onDismiss = { showFilters = false }
            )
        }

        actionNote?.let { entry ->
            NoteActionSheet(
                entry = entry,
                onDismiss = { actionNote = null },
                onPin = { vm.togglePin(entry) },
                onDelete = { vm.delete(entry) }
            )
        }
    }
}

@Composable
private fun HeaderRow(noteCount: Int, initial: String, onAvatar: () -> Unit) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val today = remember { TimeFormat.headerDate() }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(TimeFormat.greeting(hour), style = FT.greeting, color = FN.text)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(today, style = FT.monoTiny, color = FN.muted)
                ColoredDot(FN.dotGray, size = 3.dp)
                Text("$noteCount notes", style = FT.monoTiny, color = FN.muted)
            }
        }
        Box(Modifier.size(44.dp)) {
            Surface(
                shape = CircleShape,
                color = FN.surface,
                border = BorderStroke(1.dp, FN.line),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .clickable(onClick = onAvatar),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, style = FT.cardTitle.copy(fontSize = 17.sp), color = FN.text)
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(FN.accent)
            )
        }
    }
}

@Composable
private fun SearchBarRow(onSearch: () -> Unit, onFilters: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .clickable(onClick = onSearch)
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = FN.muted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Search notes, tags, sketches…",
                style = FT.bodySmall.copy(fontSize = 13.5.sp),
                color = FN.muted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = FN.bg,
                border = BorderStroke(1.dp, FN.line)
            ) {
                Row(
                    Modifier
                        .clickable(onClick = onFilters)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = FN.textSoft,
                        modifier = Modifier.size(13.dp)
                    )
                    Text("Filters", style = FT.chipSmall, color = FN.textSoft)
                }
            }
        }
    }
}

@Composable
private fun GridListToggle(gridMode: Boolean, onGrid: () -> Unit, onList: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line)
    ) {
        Row(
            Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SegToggleLabel(Icons.Outlined.GridView, "Grid", gridMode, onGrid)
            SegToggleLabel(Icons.AutoMirrored.Outlined.List, "List", !gridMode, onList)
        }
    }
}

@Composable
private fun SegToggleLabel(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) FN.strong else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) FN.onStrong else FN.muted,
            modifier = Modifier.size(14.dp)
        )
        Text(
            label,
            style = FT.chip.copy(fontSize = 11.sp),
            color = if (selected) FN.onStrong else FN.muted
        )
    }
}

@Composable
private fun FilterSheet(
    tags: List<TagEntity>,
    sort: NoteSort,
    tagFilter: Set<Long>,
    onSort: (NoteSort) -> Unit,
    onToggleTag: (Long) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Filters", style = FT.sectionTitle, color = FN.text)
            SectionLabel("Sort by")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    NoteSort.RECENT to "Recent",
                    NoteSort.OLDEST to "Oldest",
                    NoteSort.TITLE to "A–Z"
                ).forEach { (value, label) ->
                    PillChip(label = label, selected = sort == value, onClick = { onSort(value) })
                }
            }
            SectionLabel("Tags")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                tags.forEach { tag ->
                    TagChipView(
                        name = tag.name,
                        colorIndex = tag.colorIndex,
                        selected = tag.id in tagFilter,
                        onClick = { onToggleTag(tag.id) }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reset",
                    style = FT.button,
                    color = FN.muted,
                    modifier = Modifier.clickable(onClick = onReset)
                )
                Spacer(Modifier.size(20.dp))
                Text(
                    "Done",
                    style = FT.button,
                    color = FN.text,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}

@Composable
private fun NoteActionSheet(
    entry: NoteWithTags,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                entry.note.title.ifBlank { "Untitled" },
                style = FT.sectionTitle,
                color = FN.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            SheetAction(
                icon = Icons.Outlined.PushPin,
                label = if (entry.note.pinned) "Unpin note" else "Pin note",
                tint = FN.text
            ) {
                onPin()
                onDismiss()
            }
            SheetAction(icon = Icons.Outlined.Delete, label = "Delete note", tint = FN.accent) {
                confirmDelete = true
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete note?", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "“${entry.note.title.ifBlank { "Untitled" }}” will be removed permanently.",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                    onDismiss()
                }) {
                    Text("Delete", color = FN.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = FN.textSoft)
                }
            }
        )
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = FT.button, color = tint)
    }
}
