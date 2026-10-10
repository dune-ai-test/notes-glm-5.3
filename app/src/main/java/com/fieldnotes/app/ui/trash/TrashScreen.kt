package com.fieldnotes.app.ui.trash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.util.TimeFormat
import com.fieldnotes.app.util.plural
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TrashViewModel(private val repo: NoteRepository) : ViewModel() {

    val trashed: StateFlow<List<NoteWithTags>> = repo.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: Long) {
        viewModelScope.launch { repo.restoreNote(id) }
    }

    fun deleteForever(note: com.fieldnotes.app.data.db.NoteEntity) {
        viewModelScope.launch { repo.hardDelete(note) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repo.emptyTrash() }
    }
}

private const val TRASH_DAYS = 30

@Composable
fun TrashScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val vm: TrashViewModel = viewModel(
        factory = viewModelFactory { initializer { TrashViewModel(container.noteRepository) } }
    )
    val trashed by vm.trashed.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showEmptyDialog by remember { mutableStateOf(false) }
    var deleteForeverTarget by remember { mutableStateOf<NoteWithTags?>(null) }

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
                Text("Trash", style = FT.sectionTitle.copy(fontSize = 20.sp), color = FN.text)
                Text(
                    "${trashed.size} ${plural(trashed.size, "item", "items")} • deleted after $TRASH_DAYS days",
                    style = FT.monoTiny,
                    color = FN.muted
                )
            }
            Text(
                "Empty",
                style = FT.button,
                color = FN.accent,
                modifier = Modifier.clickable(enabled = trashed.isNotEmpty()) { showEmptyDialog = true }
            )
        }

        if (trashed.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Trash is empty", style = FT.sectionTitle, color = FN.text)
                Text("Deleted notes rest here for $TRASH_DAYS days.", style = FT.bodySmall, color = FN.muted)
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                items(trashed, key = { it.note.id }) { entry ->
                    TrashedRow(
                        entry = entry,
                        onRestore = { vm.restore(entry.note.id) },
                        onDeleteForever = { deleteForeverTarget = entry }
                    )
                }
            }
        }
    }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("Empty trash?", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "All ${trashed.size} notes in the trash will be permanently deleted.",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyDialog = false
                    vm.emptyTrash()
                }) {
                    Text("Empty", color = FN.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) { Text("Cancel", color = FN.muted) }
            }
        )
    }

    deleteForeverTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteForeverTarget = null },
            title = { Text("Delete forever?", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "“${entry.note.title.ifBlank { "Untitled" }}” will be permanently deleted. This can't be undone.",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = deleteForeverTarget
                    deleteForeverTarget = null
                    if (target != null) vm.deleteForever(target.note)
                }) {
                    Text("Delete", color = FN.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteForeverTarget = null }) { Text("Cancel", color = FN.muted) }
            }
        )
    }
}

@Composable
private fun TrashedRow(
    entry: NoteWithTags,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val daysGone = remember(entry.note.trashedAt) {
        ((System.currentTimeMillis() - (entry.note.trashedAt ?: 0L)) / TimeUnit.DAYS.toMillis(1)).toInt()
    }
    val daysLeft = (TRASH_DAYS - daysGone).coerceAtLeast(0)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = entry.note.title.ifBlank { "Untitled" },
                    style = FT.cardTitle,
                    color = FN.text,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Deleted " + TimeFormat.relative(entry.note.trashedAt ?: 0L) +
                        " • $daysLeft d left",
                    style = FT.monoTiny,
                    color = FN.muted
                )
            }
            Text(
                "Restore",
                style = FT.button,
                color = FN.text,
                modifier = Modifier.clickable(onClick = onRestore)
            )
            Text(
                "Delete",
                style = FT.button,
                color = FN.accent,
                modifier = Modifier.clickable(onClick = onDeleteForever)
            )
        }
    }
}
