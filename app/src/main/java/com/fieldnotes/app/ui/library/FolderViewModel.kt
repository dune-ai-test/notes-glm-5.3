package com.fieldnotes.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.repo.NoteSort
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.filterNotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderViewModel(
    private val repo: NoteRepository,
    folderId: Long
) : ViewModel() {

    val folder: StateFlow<FolderEntity?> = repo.folders
        .map { folders -> folders.firstOrNull { it.id == folderId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notes: StateFlow<List<NoteWithTags>> = repo.folderNotes(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val query = MutableStateFlow("")
    val sort = MutableStateFlow(NoteSort.RECENT)
    val pinnedOnly = MutableStateFlow(false)

    val visibleNotes: StateFlow<List<NoteWithTags>> = combine(
        notes, query, sort, pinnedOnly
    ) { list, q, s, p ->
        filterNotes(list, q, s, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSort(value: NoteSort) {
        sort.value = value
    }

    fun setPinnedOnly(value: Boolean) {
        pinnedOnly.value = value
    }

    fun toggleChecklistItem(entry: NoteWithTags, blockIndex: Int, itemIndex: Int) {
        viewModelScope.launch {
            val blocks = decodeBlocks(entry.note.blocksJson).toMutableList()
            val checklist = blocks.getOrNull(blockIndex) as? Block.Checklist ?: return@launch
            val items = checklist.items.toMutableList()
            val item = items.getOrNull(itemIndex) ?: return@launch
            items[itemIndex] = item.copy(done = !item.done)
            blocks[blockIndex] = checklist.copy(items = items)
            repo.saveBlocks(entry.note, blocks)
        }
    }
}
