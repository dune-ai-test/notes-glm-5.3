@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.fieldnotes.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.fieldnotes.app.data.repo.NoteFilter.NoteSort
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.HomeNoteCard
import com.fieldnotes.app.ui.components.ListRowNote
import com.fieldnotes.app.ui.components.PillChip
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.noteColor
import com.fieldnotes.app.util.plural

private val SortOptions = listOf(
    NoteSort.RECENT to "Recent",
    NoteSort.OLDEST to "Oldest",
    NoteSort.TITLE to "A–Z"
)

private fun sortLabel(sort: NoteSort): String = when (sort) {
    NoteSort.RECENT -> "Recent"
    NoteSort.OLDEST -> "Oldest"
    NoteSort.TITLE -> "A–Z"
    NoteSort.CUSTOM -> "Custom"
}

@Composable
fun FolderScreen(folderId: Long, navController: NavHostController) {
    val container = LocalAppContainer.current
    val vm: FolderViewModel = viewModel(
        key = "folder-$folderId",
        factory = viewModelFactory {
            initializer { FolderViewModel(container.noteRepository, folderId) }
        }
    )
    val folder by vm.folder.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val visibleNotes by vm.visibleNotes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val pinnedOnly by vm.pinnedOnly.collectAsStateWithLifecycle()
    val playback by container.audioPlayer.state.collectAsStateWithLifecycle()

    var gridMode by remember { mutableStateOf(true) }
    var showSort by remember { mutableStateOf(false) }

    val dotColor = noteColor(folder?.colorIndex ?: 0)
    val folderName = folder?.name ?: "Folder"

    Column(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back") {
                navController.popBackStack()
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    folderName,
                    style = FT.sectionTitle.copy(fontSize = 20.sp),
                    color = FN.text
                )
                Text(
                    "${notes.size} ${plural(notes.size, "note", "notes")}",
                    style = FT.monoTiny,
                    color = FN.muted
                )
            }
            CircleIconButton(Icons.Outlined.Add, "New note", background = FN.strong, tint = FN.surface) {
                navController.navigate("editor/-1?folderId=$folderId")
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = FN.surface,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = FN.muted,
                        modifier = Modifier.size(17.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        textStyle = FT.bodySmall.copy(color = FN.text, fontSize = 13.sp),
                        cursorBrush = SolidColor(FN.accent),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text("Search in $folderName…", style = FT.bodySmall, color = FN.muted)
                                }
                                inner()
                            }
                        }
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = FN.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (sort == NoteSort.RECENT) FN.line else FN.accent
                )
            ) {
                Text(
                    text = if (sort == NoteSort.RECENT) "Sort" else "Sort · ${sortLabel(sort)}",
                    style = FT.chipSmall,
                    color = if (sort == NoteSort.RECENT) FN.textSoft else FN.accent,
                    modifier = Modifier
                        .clickable { showSort = true }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PillChip(
                    label = "All",
                    selected = !pinnedOnly,
                    onClick = { vm.setPinnedOnly(false) },
                    count = notes.size
                )
                PillChip(
                    label = "Pinned",
                    selected = pinnedOnly,
                    onClick = { vm.setPinnedOnly(true) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PillChip(label = "Grid", selected = gridMode, onClick = { gridMode = true })
                PillChip(label = "List", selected = !gridMode, onClick = { gridMode = false })
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 132.dp
            )
        ) {
            if (visibleNotes.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (pinnedOnly) "No pinned notes" else "Nothing here yet",
                            style = FT.sectionTitle,
                            color = FN.text
                        )
                        Text(
                            "Tap + to capture something.",
                            style = FT.bodySmall,
                            color = FN.muted
                        )
                    }
                }
            }
            visibleNotes.forEach { entry ->
                if (gridMode) {
                    item(key = "folder-card-${entry.note.id}") {
                        Box(Modifier.height(220.dp)) {
                            HomeNoteCard(
                                entry = entry,
                                dotColor = dotColor,
                                folderName = folderName,
                                modifier = Modifier.fillMaxSize(),
                                playbackState = playback,
                                onOpen = { navController.navigate("editor/${entry.note.id}") },
                                onLongPress = null,
                                onToggleChecklistItem = { blockIndex, itemIndex ->
                                    vm.toggleChecklistItem(entry, blockIndex, itemIndex)
                                },
                                onTogglePlay = { id, path -> container.audioPlayer.toggle(id, path) }
                            )
                        }
                    }
                } else {
                    item(key = "folder-list-${entry.note.id}", span = { GridItemSpan(maxLineSpan) }) {
                        ListRowNote(
                            entry = entry,
                            dotColor = dotColor,
                            onOpen = { navController.navigate("editor/${entry.note.id}") },
                            onLongPress = { }
                        )
                    }
                }
            }
        }
    }

    if (showSort) {
        AlertDialog(
            onDismissRequest = { showSort = false },
            title = { Text("Sort by", style = FT.sectionTitle, color = FN.text) },
            text = {
                Column {
                    SortOptions.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSort = false
                                    vm.setSort(value)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = sort == value,
                                onClick = {
                                    showSort = false
                                    vm.setSort(value)
                                }
                            )
                            Text(label, style = FT.body, color = FN.text)
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { showSort = false }) { Text("Cancel", color = FN.muted) }
            }
        )
    }
}
