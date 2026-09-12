@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.fieldnotes.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.ListRowNote
import com.fieldnotes.app.ui.components.PillChip
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.components.TagChipView
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.noteColor
import com.fieldnotes.app.util.TimeFormat
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(navController: NavHostController, onCreateNote: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory { initializer { LibraryViewModel(container.noteRepository) } }
    )
    val folders by vm.folders.collectAsStateWithLifecycle()
    val noteCount by vm.noteCount.collectAsStateWithLifecycle()
    val visibleNotes by vm.visibleNotes.collectAsStateWithLifecycle()
    val allNotes by vm.notes.collectAsStateWithLifecycle()
    val tagsWithUsage by vm.tagsWithUsage.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()

    var showAllTags by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<NoteWithTags?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Library", style = FT.screenTitle, color = FN.text)
                Text(
                    "${folders.size} folders • $noteCount notes",
                    style = FT.bodySmall.copy(fontSize = 13.sp),
                    color = FN.muted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircleIconButton(Icons.Outlined.Search, "Search") {
                    navController.navigate("search")
                }
                CircleIconButton(Icons.Outlined.Add, "New note", background = FN.strong, tint = FN.onStrong) {
                    onCreateNote()
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LibraryTab.entries.forEach { tabValue ->
                PillChip(
                    label = tabValue.label,
                    selected = tab == tabValue,
                    onClick = { vm.tab.value = tabValue }
                )
            }
        }

        if (tab == LibraryTab.ALL || tab == LibraryTab.FAVORITES) {
            folders.chunked(2).forEach { rowFolders ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowFolders.forEach { folder ->
                        FolderCard(
                            folder = folder,
                            count = allNotes.count { it.note.folderId == folder.id },
                            lastUpdated = allNotes
                                .filter { it.note.folderId == folder.id }
                                .maxOfOrNull { it.note.updatedAt } ?: 0L,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("folder/${folder.id}") }
                        )
                    }
                    if (rowFolders.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (tab != LibraryTab.FAVORITES) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = FN.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tags", style = FT.sectionTitle, color = FN.text)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clickable { showAllTags = true }
                        ) {
                            Text("View all", style = FT.chipSmall, color = FN.muted)
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = FN.muted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        tagsWithUsage.take(7).forEach { usage ->
                            TagChipView(name = usage.tag.name, colorIndex = usage.tag.colorIndex)
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = FN.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recent activity", style = FT.sectionTitle, color = FN.text)
                        Text("Today", style = FT.monoTiny, color = FN.dotGray)
                    }
                    allNotes.take(4).forEach { entry ->
                        ActivityRow(
                            entry = entry,
                            folderName = folders.firstOrNull { it.id == entry.note.folderId }?.name ?: "",
                            folderColor = noteColor(
                                folders.firstOrNull { it.id == entry.note.folderId }?.colorIndex ?: 0
                            ),
                            onClick = { navController.navigate("editor/${entry.note.id}") }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(tab.label.ifBlank { "Notes" })
            if (visibleNotes.isEmpty()) {
                Text(
                    if (tab == LibraryTab.FAVORITES) "Pin notes to find them here." else "No notes yet.",
                    style = FT.bodySmall,
                    color = FN.muted
                )
            }
            visibleNotes.forEach { entry ->
                ListRowNote(
                    entry = entry,
                    dotColor = FN.accent,
                    onOpen = { navController.navigate("editor/${entry.note.id}") },
                    onLongPress = { deleteTarget = entry }
                )
            }
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
    }

    if (showAllTags) {
        ModalBottomSheet(
            onDismissRequest = { showAllTags = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("All tags", style = FT.sectionTitle, color = FN.text)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    tagsWithUsage.forEach { usage ->
                        TagChipView(
                            name = "${usage.tag.name} ${usage.count}",
                            colorIndex = usage.tag.colorIndex
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove note?", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "“${entry.note.title.ifBlank { "Untitled" }}” will be deleted permanently.",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = deleteTarget
                    deleteTarget = null
                    if (target != null) {
                        scope.launch {
                            container.noteRepository.trashNote(target.note)
                            val result = snackbarHostState.showSnackbar(
                                message = "Note moved to trash",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                container.noteRepository.restoreNote(target.note.id)
                            }
                        }
                    }
                }) {
                    Text("Delete", color = FN.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = FN.textSoft) }
            }
        )
    }
}

@Composable
private fun ActivityRow(
    entry: NoteWithTags,
    folderName: String,
    folderColor: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ColoredDot(folderColor, size = 10.dp)
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                entry.note.title.ifBlank { "Untitled" },
                style = FT.cardTitle.copy(fontSize = 13.sp),
                color = FN.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Edited ${TimeFormat.relative(entry.note.updatedAt)} • $folderName",
                style = FT.monoTiny,
                color = FN.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(shape = CircleShape, color = FN.bg) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = FN.muted,
                modifier = Modifier.padding(4.dp).size(14.dp)
            )
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderEntity,
    count: Int,
    lastUpdated: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pastel = folder.colorIndex != 0
    val bg = if (pastel) noteColor(folder.colorIndex) else FN.surface
    val ink = if (pastel) FN.inkFixed else FN.text
    val soft = if (pastel) Color(0xFF6B665F) else FN.muted
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = FN.onStrong) {
                    Box(Modifier.padding(9.dp)) {
                        Icon(
                            imageVector = folderIcon(folder.iconKey),
                            contentDescription = null,
                            tint = FN.inkFixed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                ColoredDot(
                    if (count > 0) FN.accent else FN.dotGray,
                    size = 8.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(folder.name, style = FT.sectionTitle.copy(fontSize = 19.sp), color = FN.text)
            Text(
                if (lastUpdated > 0) "Updated ${TimeFormat.relative(lastUpdated)}" else "No activity yet",
                style = FT.bodySmall.copy(fontSize = 11.5.sp),
                color = soft.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        count.toString(),
                        style = FT.statNumber.copy(fontSize = 26.sp),
                        color = ink
                    )
                    Text(
                        "notes",
                        style = FT.monoTiny,
                        color = soft.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Surface(shape = CircleShape, color = FN.onStrong.copy(alpha = 0.7f)) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Open",
                        tint = FN.inkFixed,
                        modifier = Modifier.padding(6.dp).size(14.dp)
                    )
                }
            }
        }
    }
}

private fun folderIcon(iconKey: String): ImageVector = when (iconKey) {
    "heart" -> Icons.Outlined.FavoriteBorder
    "bulb" -> Icons.Outlined.Lightbulb
    "archive" -> Icons.Outlined.Archive
    else -> Icons.Outlined.Work
}
