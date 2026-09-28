package com.fieldnotes.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.repo.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(private val repo: NoteRepository) : ViewModel() {

    val folders: StateFlow<List<FolderEntity>> = repo.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteCount: StateFlow<Int> = repo.activeNoteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val notes: StateFlow<List<NoteWithTags>> = repo.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
