package com.fieldnotes.app.data.repo

import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.plainText

enum class NoteSort { RECENT, OLDEST, TITLE, CUSTOM }

fun filterNotes(
    notes: List<NoteWithTags>,
    query: String,
    sort: NoteSort,
    tagFilter: Set<Long>,
    pinnedOnly: Boolean,
    customOrder: List<Long>? = null
): List<NoteWithTags> {
    val q = query.trim().lowercase()
    val filtered = notes.filter { note ->
        if (pinnedOnly && !note.note.pinned) return@filter false
        if (tagFilter.isNotEmpty() && note.tags.none { it.id in tagFilter }) return@filter false
        if (q.isEmpty()) return@filter true
        val inTitle = note.note.title.lowercase().contains(q)
        val inBody = decodeBlocks(note.note.blocksJson).plainText().lowercase().contains(q)
        val inTags = note.tags.any { it.name.lowercase().contains(q) }
        inTitle || inBody || inTags
    }
    return when (sort) {
        NoteSort.RECENT -> filtered.sortedByDescending { it.note.updatedAt }
        NoteSort.OLDEST -> filtered.sortedBy { it.note.updatedAt }
        NoteSort.TITLE -> filtered.sortedBy { it.note.title.lowercase() }
        NoteSort.CUSTOM -> {
            if (customOrder.isNullOrEmpty()) {
                filtered
            } else {
                val position = customOrder.withIndex().associate { (index, id) -> id to index }
                filtered.sortedWith(
                    compareBy(
                        { position[it.note.id] ?: Int.MAX_VALUE },
                        { -it.note.updatedAt }
                    )
                )
            }
        }
    }
}
