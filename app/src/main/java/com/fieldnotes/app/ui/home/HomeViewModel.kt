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

    val noteCount: StateFlow<Int> = repo.activeNoteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val tags: StateFlow<List<TagEntity>> = repo.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repo.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allNotesRaw: StateFlow<List<NoteWithTags>> = repo.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Non-null while the user is in / has used manual ordering this session. */
    private val customOrder = MutableStateFlow<List<Long>?>(null)

    private val notesWithOrder = combine(repo.activeNotes, customOrder) { notes, order ->
        notes to order
    }

    val query = MutableStateFlow("")
    val sort = MutableStateFlow(NoteSort.RECENT)
    val tagFilter = MutableStateFlow<Set<Long>>(emptySet())
    val pinnedOnly = MutableStateFlow(false)

    val visibleNotes: StateFlow<List<NoteWithTags>> = combine(
        notesWithOrder, query, sort, tagFilter, pinnedOnly
    ) { (notes, order), queryValue, sortValue, tagIds, pinned ->
        filterNotes(notes, queryValue, sortValue, tagIds, pinned, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Enters manual ordering, seeding the order from the current list. */
    fun beginReorder() {
        if (sort.value != NoteSort.CUSTOM || customOrder.value == null) {
            sort.value = NoteSort.CUSTOM
            customOrder.value = allNotesRaw.value.map { it.note.id }
        }
    }

    /** Moves [dragId] to the position [ontoId] held in the manual order. */
    fun moveCard(dragId: Long, ontoId: Long) {
        val order = customOrder.value ?: return
        val from = order.indexOf(dragId)
        val to = order.indexOf(ontoId)
        if (from == -1 || to == -1 || from == to) return
        val updated = order.toMutableList()
        updated.removeAt(from)
        updated.add(to, dragId)
        customOrder.value = updated
    }

    /** Writes the manual order back to the database. */
    fun persistReorder() {
        val order = customOrder.value ?: return
        viewModelScope.launch { repo.saveCustomOrder(order) }
    }

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
        viewModelScope.launch { repo.trashNote(entry.note) }
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
