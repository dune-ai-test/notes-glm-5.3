package com.fieldnotes.app.data.repo

import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.plainText

enum class NoteSort { RECENT, OLDEST, TITLE }

fun filterNotes(
    notes: List<NoteWithTags>,
    query: String,
    sort: NoteSort,
    pinnedOnly: Boolean
): List<NoteWithTags> {
    val q = query.trim().lowercase()
    val filtered = notes.filter { note ->
        if (pinnedOnly && !note.note.pinned) return@filter false
        if (q.isEmpty()) return@filter true
        val inTitle = note.note.title.lowercase().contains(q)
        // Locked notes: title stays searchable, the body does not.
        val inBody = !note.note.locked && decodeBlocks(note.note.blocksJson).plainText().lowercase().contains(q)
        inTitle || inBody
    }
    return when (sort) {
        NoteSort.RECENT -> filtered.sortedByDescending { it.note.updatedAt }
        NoteSort.OLDEST -> filtered.sortedBy { it.note.updatedAt }
        NoteSort.TITLE -> filtered.sortedBy { it.note.title.lowercase() }
    }
}
