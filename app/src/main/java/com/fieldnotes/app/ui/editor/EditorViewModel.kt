package com.fieldnotes.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.ChecklistItem
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.repo.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val repo: NoteRepository,
    val noteId: Long,
    folderIdHint: Long = -1L
) : ViewModel() {

    data class EditorState(
        val loading: Boolean = true,
        val missing: Boolean = false,
        val title: String = "",
        val blocks: List<Block> = emptyList(),
        val tagIds: Set<Long> = emptySet(),
        val pinned: Boolean = false,
        val locked: Boolean = false,
        val colorIndex: Int = 0,
        val folderId: Long = NoteEntity.DEFAULT_FOLDER_ID,
        val kind: Int = NoteEntity.KIND_TEXT,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
        val focusedBlock: Int = -1
    )

    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    val tags: StateFlow<List<TagEntity>> = repo.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repo.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var note: NoteEntity? = null
    private var saveJob: Job? = null
    private var dirty = false
    private val draftFolderId: Long = if (folderIdHint > 0) folderIdHint else NoteEntity.DEFAULT_FOLDER_ID

    /** True when the database row was created by this editor session (new drafts). */
    private var createdHere = false

    init {
        if (noteId <= 0L) {
            // New, unsaved draft: nothing touches the database until the user edits.
            val now = System.currentTimeMillis()
            note = null
            _state.value = EditorState(
                loading = false,
                blocks = listOf(Block.Paragraph()),
                folderId = draftFolderId,
                createdAt = now,
                updatedAt = now
            )
        } else {
            viewModelScope.launch {
                val loaded: NoteWithTags? = repo.getNote(noteId)
                if (loaded == null) {
                    _state.update { it.copy(loading = false, missing = true) }
                } else {
                    note = loaded.note
                    var blocks = decodeBlocks(loaded.note.blocksJson)
                    if (blocks.isEmpty() && loaded.note.kind == NoteEntity.KIND_TEXT) {
                        blocks = listOf(Block.Paragraph())
                    }
                    _state.value = EditorState(
                        loading = false,
                        title = loaded.note.title,
                        blocks = blocks,
                        tagIds = loaded.tags.map { it.id }.toSet(),
                        pinned = loaded.note.pinned,
                        locked = loaded.note.locked,
                        colorIndex = loaded.note.colorIndex,
                        folderId = loaded.note.folderId,
                        kind = loaded.note.kind,
                        createdAt = loaded.note.createdAt,
                        updatedAt = loaded.note.updatedAt
                    )
                }
            }
        }
    }

    fun setTitle(value: String) = update { it.copy(title = value) }

    fun setFocusedBlock(index: Int) {
        _state.update { it.copy(focusedBlock = index) }
    }

    fun updateBlock(index: Int, block: Block) = mutateBlocks { list ->
        if (index in list.indices) list[index] = block
    }

    fun addBlock(block: Block) = mutateBlocks { it.add(block) }

    fun removeBlock(index: Int) = mutateBlocks { list ->
        if (index in list.indices) list.removeAt(index)
        // Never leave the editor without a place to type.
        if (list.isEmpty()) list.add(Block.Paragraph())
    }

    /** Toggles bold on the focused paragraph (or the last paragraph). */
    fun toggleEmphasis() {
        val s = _state.value
        val target = if (s.focusedBlock in s.blocks.indices && s.blocks[s.focusedBlock] is Block.Paragraph) {
            s.focusedBlock
        } else {
            s.blocks.indexOfLast { it is Block.Paragraph }
        }
        if (target == -1) return
        val block = s.blocks[target] as Block.Paragraph
        updateBlock(target, block.copy(emphasized = !block.emphasized))
    }

    fun turnIntoChecklist() {
        val s = _state.value
        val target = when {
            s.focusedBlock in s.blocks.indices -> s.focusedBlock
            s.blocks.isNotEmpty() -> s.blocks.lastIndex
            else -> return
        }
        when (val block = s.blocks[target]) {
            is Block.Paragraph -> updateBlock(target, Block.Checklist(listOf(ChecklistItem(block.text))))
            is Block.Heading -> updateBlock(target, Block.Checklist(listOf(ChecklistItem(block.text))))
            else -> addBlock(Block.Checklist(emptyList()))
        }
    }

    fun updateChecklistItem(blockIndex: Int, itemIndex: Int, text: String? = null, done: Boolean? = null) =
        mutateBlocks { list ->
            val checklist = list.getOrNull(blockIndex) as? Block.Checklist ?: return@mutateBlocks
            val items = checklist.items.toMutableList()
            val item = items.getOrNull(itemIndex) ?: return@mutateBlocks
            items[itemIndex] = item.copy(
                text = text ?: item.text,
                done = done ?: item.done
            )
            list[blockIndex] = checklist.copy(items = items)
        }

    fun addChecklistItem(blockIndex: Int) = mutateBlocks { list ->
        val checklist = list.getOrNull(blockIndex) as? Block.Checklist ?: return@mutateBlocks
        list[blockIndex] = checklist.copy(items = checklist.items + ChecklistItem())
    }

    fun removeChecklistItem(blockIndex: Int, itemIndex: Int) = mutateBlocks { list ->
        val checklist = list.getOrNull(blockIndex) as? Block.Checklist ?: return@mutateBlocks
        if (itemIndex in checklist.items.indices) {
            list[blockIndex] = checklist.copy(items = checklist.items.filterIndexed { i, _ -> i != itemIndex })
        }
    }

    fun setTags(ids: Set<Long>) = update { it.copy(tagIds = ids) }

    fun selectTag(id: Long) = update { it.copy(tagIds = it.tagIds + id) }

    fun createTag(name: String, onCreated: (Long) -> Unit = {}) {
        val trimmed = name.trim().removePrefix("#")
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            onCreated(repo.createTag(trimmed, (0..5).random()))
        }
    }
    fun setColor(index: Int) = update { it.copy(colorIndex = index) }
    fun setPinned(value: Boolean) = update { it.copy(pinned = value) }

    fun setLocked(value: Boolean) = update { it.copy(locked = value) }
    fun setFolder(id: Long) = update { it.copy(folderId = id) }

    fun deleteNote(onDone: () -> Unit) {
        viewModelScope.launch {
            note?.let { repo.trashNote(it) }
            onDone()
        }
    }

    private fun update(reducer: (EditorState) -> EditorState) {
        _state.update(reducer)
        markDirtyAndSchedule()
    }

    private fun mutateBlocks(operation: (MutableList<Block>) -> Unit) {
        val list = _state.value.blocks.toMutableList()
        operation(list)
        _state.update { it.copy(blocks = list) }
        markDirtyAndSchedule()
    }

    private fun markDirtyAndSchedule() {
        if (_state.value.loading) return
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            persist()
        }
    }

    /** True when the draft has no content worth keeping. */
    val isEmptyDraft: Boolean
        get() = with(_state.value) {
            title.isBlank() && blocks.all { block ->
                when (block) {
                    is Block.Heading -> block.text.isBlank()
                    is Block.Paragraph -> block.text.isBlank()
                    is Block.Highlight -> block.text.isBlank()
                    is Block.Checklist -> block.items.all { it.text.isBlank() }
                    is Block.Image -> block.path.isBlank()
                    is Block.Audio -> block.path.isBlank()
                }
            }
        }

    /**
     * Finishes editing: saves pending changes, creates the row for new drafts
     * that gained content, and deletes rows that were created here but are
     * still empty. Returns true when a note remains in the database.
     */
    suspend fun finish(): Boolean {
        saveJob?.cancel()
        saveJob = null
        if (dirty) persist()
        if (createdHere && isEmptyDraft) {
            note?.let { repo.deleteNote(it) }
            note = null
            return false
        }
        return note != null
    }

    /** Persists pending edits and returns the note id (null while missing). */
    suspend fun ensureSaved(): Long? {
        saveJob?.cancel()
        saveJob = null
        if (dirty) persist()
        return note?.id
    }

    private suspend fun persist() {
        if (!dirty) return
        val s = _state.value
        var current = note
        if (current == null) {
            val newId = repo.createNote(
                folderId = s.folderId,
                colorIndex = s.colorIndex,
                kind = s.kind
            )
            current = repo.getNote(newId)?.note ?: return
            note = current
            createdHere = true
        }
        repo.saveNote(
            current.copy(
                title = s.title,
                colorIndex = s.colorIndex,
                pinned = s.pinned,
                locked = s.locked,
                folderId = s.folderId
            ),
            s.blocks,
            s.tagIds
        )
        dirty = false
        _state.update { it.copy(updatedAt = System.currentTimeMillis()) }
    }
}
