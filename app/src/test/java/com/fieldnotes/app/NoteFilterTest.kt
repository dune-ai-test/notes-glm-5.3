package com.fieldnotes.app

import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteTagCrossRef
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.data.repo.NoteSort
import com.fieldnotes.app.data.repo.filterNotes
import com.fieldnotes.app.util.Streak
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteFilterTest {

    private fun note(
        id: Long,
        title: String,
        body: String = "",
        pinned: Boolean = false,
        updatedAt: Long
    ): NoteWithTags = NoteWithTags(
        note = NoteEntity(
            id = id,
            title = title,
            blocksJson = encodeBlocks(listOf(Block.Paragraph(body))),
            pinned = pinned,
            createdAt = updatedAt - 1_000,
            updatedAt = updatedAt
        ),
        tags = emptyList()
    )

    private val notes = listOf(
        note(1, "Kyoto photo essay", body = "cherry blossoms", pinned = true, updatedAt = 300),
        note(2, "Market list", body = "tomatoes basil", updatedAt = 200),
        note(3, "Book notes", body = "atomic habits kyoto", updatedAt = 100)
    )

    @Test
    fun `recent sort puts newest first`() {
        val result = filterNotes(notes, "", NoteSort.RECENT, emptySet(), false)
        assertEquals(listOf(1L, 2L, 3L), result.map { it.note.id })
    }

    @Test
    fun `query matches title and body`() {
        val byTitle = filterNotes(notes, "kyoto photo", NoteSort.RECENT, emptySet(), false)
        assertEquals(listOf(1L), byTitle.map { it.note.id })

        val byBody = filterNotes(notes, "basil", NoteSort.RECENT, emptySet(), false)
        assertEquals(listOf(2L), byBody.map { it.note.id })
    }

    @Test
    fun `pinned only filters`() {
        val result = filterNotes(notes, "", NoteSort.RECENT, emptySet(), pinnedOnly = true)
        assertEquals(listOf(1L), result.map { it.note.id })
    }

    @Test
    fun `title sort is case insensitive`() {
        val result = filterNotes(notes, "", NoteSort.TITLE, emptySet(), false)
        assertEquals(listOf(3L, 1L, 2L), result.map { it.note.id })
    }
}

class StreakTest {

    @Test
    fun `first activity starts streak at one`() {
        assertEquals(1, Streak.nextStreak(lastActiveEpochDay = null, currentStreak = 0, todayEpochDay = 100))
    }

    @Test
    fun `same day keeps streak`() {
        assertEquals(5, Streak.nextStreak(lastActiveEpochDay = 100, currentStreak = 5, todayEpochDay = 100))
    }

    @Test
    fun `consecutive day extends streak`() {
        assertEquals(6, Streak.nextStreak(lastActiveEpochDay = 99, currentStreak = 5, todayEpochDay = 100))
    }

    @Test
    fun `gap resets streak`() {
        assertEquals(1, Streak.nextStreak(lastActiveEpochDay = 50, currentStreak = 12, todayEpochDay = 100))
    }
}
