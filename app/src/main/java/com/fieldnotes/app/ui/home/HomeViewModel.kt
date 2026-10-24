package com.fieldnotes.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.DateRange
import com.fieldnotes.app.data.repo.NoteSort
import com.fieldnotes.app.data.repo.filterNotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: NoteRepository) : ViewModel() {

    val noteCount: StateFlow<Int> = repo.activeNoteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val folders: StateFlow<List<FolderEntity>> = repo.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allNotesRaw: StateFlow<List<NoteWithTags>> = repo.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val query = MutableStateFlow("")
    val sort = MutableStateFlow(NoteSort.RECENT)
    val pinnedOnly = MutableStateFlow(false)
    val folderFilter = MutableStateFlow<Set<Long>>(emptySet())
    val colourFilter = MutableStateFlow<Set<Int>>(emptySet())
    val dateRange = MutableStateFlow(DateRange.ALL)

    private data class HomeFilters(
        val query: String,
        val sort: NoteSort,
        val pinnedOnly: Boolean,
        val folders: Set<Long>,
        val colours: Set<Int>
    )

    private val filters = combine(
        query, sort, pinnedOnly, folderFilter, colourFilter
    ) { q, s, p, f, c ->
        HomeFilters(q, s, p, f, c)
    }

    val visibleNotes: StateFlow<List<NoteWithTags>> = combine(
        filters, dateRange, allNotesRaw
    ) { f, range, list ->
        filterNotes(list, f.query, f.sort, f.pinnedOnly, f.folders, f.colours, range)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeFilterCount: StateFlow<Int> = combine(
        folderFilter, colourFilter, dateRange
    ) { folders, colours, range ->
        folders.size + colours.size + if (range == DateRange.ALL) 0 else 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSort(value: NoteSort) {
        sort.value = value
    }

    fun setPinnedOnly(value: Boolean) {
        pinnedOnly.value = value
    }

    fun toggleFolderFilter(folderId: Long) {
        val current = folderFilter.value
        folderFilter.value = if (folderId in current) current - folderId else current + folderId
    }

    fun toggleColourFilter(index: Int) {
        val current = colourFilter.value
        colourFilter.value = if (index in current) current - index else current + index
    }

    fun setDateRange(value: DateRange) {
        dateRange.value = value
    }

    fun resetFilters() {
        query.value = ""
        sort.value = NoteSort.RECENT
        pinnedOnly.value = false
        folderFilter.value = emptySet()
        colourFilter.value = emptySet()
        dateRange.value = DateRange.ALL
    }

    fun togglePin(entry: NoteWithTags) {
        viewModelScope.launch { repo.updateNote(entry.note.copy(pinned = !entry.note.pinned)) }
    }

    fun delete(entry: NoteWithTags) {
        viewModelScope.launch { repo.trashNote(entry.note) }
    }

    fun setColor(entry: NoteWithTags, colorIndex: Int) {
        viewModelScope.launch { repo.updateNote(entry.note.copy(colorIndex = colorIndex)) }
    }

    fun undoDelete(noteId: Long) {
        viewModelScope.launch { repo.restoreNote(noteId) }
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
