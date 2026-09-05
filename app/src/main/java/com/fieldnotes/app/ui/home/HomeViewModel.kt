package com.fieldnotes.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.NoteSort
import com.fieldnotes.app.data.repo.filterNotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: NoteRepository) : ViewModel() {

    val noteCount: StateFlow<Int> = repo.noteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val tags: StateFlow<List<TagEntity>> = repo.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repo.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val query = MutableStateFlow("")
    val sort = MutableStateFlow(NoteSort.RECENT)
    val tagFilter = MutableStateFlow<Set<Long>>(emptySet())
    val pinnedOnly = MutableStateFlow(false)

    val visibleNotes: StateFlow<List<NoteWithTags>> = combine(
        repo.allNotes, query, sort, tagFilter, pinnedOnly
    ) { notes, queryValue, sortValue, tagIds, pinned ->
        filterNotes(notes, queryValue, sortValue, tagIds, pinned)
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

    fun toggleTag(tagId: Long) {
        val current = tagFilter.value
        tagFilter.value = if (tagId in current) current - tagId else current + tagId
    }

    fun resetFilters() {
        sort.value = NoteSort.RECENT
        tagFilter.value = emptySet()
        pinnedOnly.value = false
    }

    fun togglePin(entry: NoteWithTags) {
        viewModelScope.launch { repo.updateNote(entry.note.copy(pinned = !entry.note.pinned)) }
    }

    fun delete(entry: NoteWithTags) {
        viewModelScope.launch { repo.deleteNote(entry.note) }
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
