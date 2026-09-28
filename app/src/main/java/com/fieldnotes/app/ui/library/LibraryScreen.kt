@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fieldnotes.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
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
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.noteColor
import com.fieldnotes.app.util.TimeFormat
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory { initializer { LibraryViewModel(container.noteRepository) } }
    )
    val folders by vm.folders.collectAsStateWithLifecycle()
    val allNotes by vm.notes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showNewFolder by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Library", style = FT.screenTitle, color = FN.text)
                    Text(
                        "${folders.size} folders • ${allNotes.size} notes",
                        style = FT.bodySmall.copy(fontSize = 13.sp),
                        color = FN.muted
                    )
                }
                CircleIconButton(Icons.Outlined.Add, "New folder", background = FN.strong, tint = FN.onStrong) {
                    showNewFolder = true
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (folders.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 70.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("No folders yet", style = FT.sectionTitle, color = FN.text)
                        Text("Tap + to create your first one.", style = FT.bodySmall, color = FN.muted)
                    }
                }
                folders.chunked(2).forEach { rowFolders ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowFolders.forEach { folder ->
                            val folderNotes = allNotes.filter { it.note.folderId == folder.id }
                            FolderCard(
                                folder = folder,
                                count = folderNotes.size,
                                lastUpdated = folderNotes.maxOfOrNull { it.note.updatedAt } ?: 0L,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("folder/${folder.id}") }
                            )
                        }
                        if (rowFolders.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        NewFolderDialog(
            onDismiss = { showNewFolder = false },
            onCreate = { name, iconKey, colorIndex ->
                showNewFolder = false
                scope.launch {
                    container.noteRepository.createFolder(name, iconKey, colorIndex)
                }
            }
        )
    }
}

private val FolderIconChoices: List<Pair<String, ImageVector>> = listOf(
    "work" to Icons.Outlined.Work,
    "heart" to Icons.Outlined.FavoriteBorder,
    "bulb" to Icons.Outlined.Lightbulb,
    "archive" to Icons.Outlined.Archive
)

@Composable
private fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, iconKey: String, colorIndex: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("work") }
    var colorIndex by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder", style = FT.sectionTitle, color = FN.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = FT.body.copy(color = FN.text),
                    cursorBrush = SolidColor(FN.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FN.surfaceAlt, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (name.isEmpty()) {
                                Text("Folder name", style = FT.body, color = FN.muted)
                            }
                            inner()
                        }
                    }
                )
                SectionLabel("Icon", color = FN.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FolderIconChoices.forEach { (key, icon) ->
                        val selected = iconKey == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) FN.strong else FN.surfaceAlt,
                            border = if (selected) {
                                BorderStroke(2.dp, FN.accent)
                            } else {
                                BorderStroke(1.dp, FN.line)
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                Modifier.clickable { iconKey = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = key,
                                    tint = if (selected) FN.onStrong else FN.textSoft,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
                SectionLabel("Color", color = FN.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (1..5).forEach { index ->
                        val selected = colorIndex == index
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(noteColor(index))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) FN.accent else FN.line,
                                    shape = CircleShape
                                )
                                .clickable { colorIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name, iconKey, colorIndex) }) {
                Text("Create", color = FN.text)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FN.muted) }
        }
    )
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
            Text(folder.name, style = FT.sectionTitle.copy(fontSize = 19.sp), color = ink)
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

private fun folderIcon(iconKey: String): ImageVector =
    FolderIconChoices.firstOrNull { it.first == iconKey }?.second ?: Icons.Outlined.Work
