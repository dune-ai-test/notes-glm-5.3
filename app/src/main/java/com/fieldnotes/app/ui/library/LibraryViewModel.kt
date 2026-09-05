package com.fieldnotes.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.db.TagIdCount
import com.fieldnotes.app.data.repo.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class LibraryTab(val label: String) {
    ALL("All"),
    RECENT("Recent"),
    FAVORITES("Favorites")
}

data class TagWithUsage(val tag: TagEntity, val count: Int)

class LibraryViewModel(private val repo: NoteRepository) : ViewModel() {

    val folders: StateFlow<List<FolderEntity>> = repo.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteCount: StateFlow<Int> = repo.noteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val notes: StateFlow<List<NoteWithTags>> = repo.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tab = MutableStateFlow(LibraryTab.ALL)

    val visibleNotes: StateFlow<List<NoteWithTags>> = combine(notes, tab) { list, tabValue ->
        when (tabValue) {
            LibraryTab.ALL -> list
            LibraryTab.RECENT -> list.sortedByDescending { it.note.updatedAt }.take(12)
            LibraryTab.FAVORITES -> list.filter { it.note.pinned }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tagsWithUsage: StateFlow<List<TagWithUsage>> = combine(repo.tags, repo.tagCounts) { tags, counts ->
        val countById = counts.associate { it.tagId to it.count }
        tags.map { TagWithUsage(it, countById[it.id] ?: 0) }
            .sortedByDescending { it.count }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
