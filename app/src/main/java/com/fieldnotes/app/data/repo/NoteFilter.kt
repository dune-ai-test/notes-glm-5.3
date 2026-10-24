package com.fieldnotes.app.data.repo

import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.plainText
import java.time.LocalDate
import java.time.ZoneId

enum class NoteSort { RECENT, OLDEST, TITLE }

enum class DateRange(val label: String) {
    ALL("All time"),
    TODAY("Today"),
    WEEK("This week"),
    MONTH("This month")
}

fun filterNotes(
    notes: List<NoteWithTags>,
    query: String,
    sort: NoteSort,
    pinnedOnly: Boolean,
    folders: Set<Long> = emptySet(),
    colours: Set<Int> = emptySet(),
    dateRange: DateRange = DateRange.ALL,
    now: Long = System.currentTimeMillis()
): List<NoteWithTags> {
    val q = query.trim().lowercase()
    val zone = ZoneId.systemDefault()
    val minDate = when (dateRange) {
        DateRange.ALL -> 0L
        DateRange.TODAY -> LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        DateRange.WEEK -> LocalDate.now().minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        DateRange.MONTH -> LocalDate.now().minusDays(29).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val filtered = notes.filter { note ->
        if (pinnedOnly && !note.note.pinned) return@filter false
        if (folders.isNotEmpty() && note.note.folderId !in folders) return@filter false
        if (colours.isNotEmpty() && note.note.colorIndex !in colours) return@filter false
        if (note.note.updatedAt < minDate) return@filter false
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
