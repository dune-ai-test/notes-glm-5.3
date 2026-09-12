package com.fieldnotes.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.fieldnotes.app.ui.components.ListRowNote
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderViewModel(repo: NoteRepository, folderId: Long) : ViewModel() {

    val folder: StateFlow<com.fieldnotes.app.data.db.FolderEntity?> = flow {
        emit(repo.getFolder(folderId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notes: StateFlow<List<NoteWithTags>> = repo.folderNotes(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun FolderScreen(folderId: Long, navController: NavHostController) {
    val container = LocalAppContainer.current
    val vm: FolderViewModel = viewModel(
        key = "folder-$folderId",
        factory = viewModelFactory { initializer { FolderViewModel(container.noteRepository, folderId) } }
    )
    val folder by vm.folder.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
                    folder?.name ?: "Folder",
                    style = FT.sectionTitle.copy(fontSize = 20.sp),
                    color = FN.text
                )
                Text("${notes.size} notes", style = FT.monoTiny, color = FN.muted)
            }
            CircleIconButton(Icons.Outlined.Add, "New note", background = FN.strong, tint = FN.onStrong) {
                scope.launch {
                    val id = container.noteRepository.createNote(
                        folderId = folderId,
                        colorIndex = folder?.colorIndex ?: 0
                    )
                    navController.navigate("editor/$id")
                }
            }
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp, top = 8.dp)
        ) {
            if (notes.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nothing in this folder yet.", style = FT.bodySmall, color = FN.muted)
                    }
                }
            }
            items(notes, key = { it.note.id }) { entry ->
                ListRowNote(
                    entry = entry,
                    dotColor = FN.accent,
                    onOpen = { navController.navigate("editor/${entry.note.id}") },
                    onLongPress = { }
                )
            }
        }
    }
}
