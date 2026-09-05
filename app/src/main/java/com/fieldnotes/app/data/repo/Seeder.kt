package com.fieldnotes.app.data.repo

import com.fieldnotes.app.data.db.AppDatabase
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteTagCrossRef
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.ChecklistItem
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.data.model.encodeToStringList
import com.fieldnotes.app.util.TimeFormat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Seeds the demo library on first launch (empty database). Clearing app data from
 * Android settings wipes the database and the seed runs again on next start.
 */
class Seeder(private val db: AppDatabase, private val settings: SettingsRepository) {

    private val mutex = Mutex()

    suspend fun seedIfEmpty() {
        mutex.withLock {
            if (db.folderDao().count() > 0 || db.noteDao().count() > 0) return
            runCatching { seed() }
        }
    }

    private suspend fun seed() {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        val folderIds = listOf(
            FolderEntity(name = "Work", iconKey = "work", colorIndex = 5),
            FolderEntity(name = "Personal", iconKey = "heart", colorIndex = 1),
            FolderEntity(name = "Ideas", iconKey = "bulb", colorIndex = 3),
            FolderEntity(name = "Archive", iconKey = "archive", colorIndex = 0)
        ).associate { it.name to db.folderDao().insert(it) }

        val tagIds = listOf(
            TagEntity(name = "inspiration", colorIndex = 4),
            TagEntity(name = "work", colorIndex = 6),
            TagEntity(name = "travel", colorIndex = 5),
            TagEntity(name = "recipes", colorIndex = 2),
            TagEntity(name = "reading", colorIndex = 1),
            TagEntity(name = "meeting", colorIndex = 0),
            TagEntity(name = "ideas", colorIndex = 3),
            TagEntity(name = "photography", colorIndex = 5)
        ).associate { it.name to db.tagDao().insert(it) }

        val notes = listOf(
            SeedNote(
                title = "The warmth of linen light",
                folder = "Personal",
                tagNames = listOf("travel"),
                updatedAgo = 2 * minute,
                blocks = listOf(
                    Block.Paragraph(
                        "Morning in Kyoto arrives softly — the shoji screens filter a pale, " +
                            "honeyed light that settles on linen, wood, and paper. I've been " +
                            "trying to capture how that quiet feels, not just how it looks."
                    ),
                    Block.Heading("Why it stays with you"),
                    Block.Paragraph(
                        "It isn't sharp or loud. It's diffuse. The kind of light that makes you " +
                            "want to write slower, fold your thoughts more carefully."
                    ),
                    Block.Checklist(
                        listOf(
                            ChecklistItem("Linen curtains breathe — wind redraws the room"),
                            ChecklistItem("Paper lanterns hold light like water"),
                            ChecklistItem("Warm wood grounds it; nothing floats away")
                        )
                    ),
                    Block.Highlight(
                        "“Light is not seen, it is felt on the skin of things.” — I wrote this " +
                            "on the train and underlined it twice."
                    ),
                    Block.Paragraph(
                        "Next: print a small zine — four pages, stitched. Linen paper, soft ink. " +
                            "Let the light do most of the work."
                    )
                )
            ),
            SeedNote(
                title = "Summer in Kyoto — photo essay",
                folder = "Personal",
                tagNames = listOf("photography", "travel"),
                colorIndex = 1,
                pinned = true,
                updatedAgo = 6 * day,
                blocks = listOf(
                    Block.Paragraph(
                        "Cherry blossoms at dawn, temple bells and the slow light over " +
                            "Arashiyama. 12 photos + notes."
                    ),
                    Block.Heading("Why it stays with you"),
                    Block.Highlight("“Light is not seen, it is felt on the skin of things.”")
                )
            ),
            SeedNote(
                title = "Weekend Market List",
                folder = "Personal",
                tagNames = emptyList(),
                updatedAgo = 2 * hour,
                blocks = listOf(
                    Block.Checklist(
                        listOf(
                            ChecklistItem("Heirloom tomatoes", done = true),
                            ChecklistItem("Sourdough loaf", done = true),
                            ChecklistItem("Fresh basil & mint"),
                            ChecklistItem("Peaches — 1kg")
                        )
                    )
                )
            ),
            SeedNote(
                title = "Mug study — line work",
                folder = "Ideas",
                tagNames = listOf("ideas"),
                colorIndex = 2,
                kind = NoteEntity.KIND_SKETCH,
                updatedAgo = 5 * day,
                blocks = listOf(
                    Block.Paragraph("Pen study of the stoneware mug — three layers of line work.")
                )
            ),
            SeedNote(
                title = "Voice memo — market sounds",
                folder = "Personal",
                tagNames = emptyList(),
                updatedAgo = 4 * day,
                blocks = listOf(
                    Block.Audio(
                        path = "",
                        durationMs = 42_000,
                        amplitudes = syntheticAmplitudes(36, 7),
                        title = "Voice memo — market sounds"
                    ),
                    Block.Paragraph("Shared with Tom.")
                )
            ),
            SeedNote(
                title = "Reading notes — Eames",
                folder = "Ideas",
                tagNames = listOf("reading"),
                colorIndex = 4,
                updatedAgo = 7 * day,
                blocks = listOf(
                    Block.Paragraph("Notes from the Eames biography, chapter 3."),
                    Block.Highlight(
                        "“Design is art that makes itself useful.” — p. 42 · 3 highlights"
                    )
                )
            ),
            SeedNote(
                title = "Lemon tart — grandma's",
                folder = "Personal",
                tagNames = listOf("recipes"),
                colorIndex = 5,
                updatedAgo = 8 * day,
                blocks = listOf(
                    Block.Checklist(
                        listOf(
                            ChecklistItem("Butter · 120g"),
                            ChecklistItem("Flour · 200g"),
                            ChecklistItem("Lemons · 3")
                        )
                    ),
                    Block.Paragraph("6 steps · chill the dough overnight.")
                )
            ),
            SeedNote(
                title = "Q3 Planning — Roadmap",
                folder = "Work",
                tagNames = listOf("work"),
                updatedAgo = 2 * hour,
                blocks = listOf(
                    Block.Heading("Q3 roadmap"),
                    Block.Checklist(
                        listOf(
                            ChecklistItem("Draft OKRs", done = true),
                            ChecklistItem("Review with the team")
                        )
                    ),
                    Block.Paragraph("Focus: retention, onboarding, and the new capture flow.")
                )
            ),
            SeedNote(
                title = "Book Notes: Atomic Habits",
                folder = "Ideas",
                tagNames = listOf("reading", "ideas"),
                updatedAgo = day + 3 * hour,
                blocks = listOf(
                    Block.Highlight("“You do not rise to the level of your goals.”"),
                    Block.Paragraph("Systems over goals. Make it obvious, attractive, easy, satisfying.")
                )
            ),
            SeedNote(
                title = "Kyoto Trip Itinerary",
                folder = "Personal",
                tagNames = listOf("travel"),
                updatedAgo = 3 * day,
                blocks = listOf(
                    Block.Checklist(
                        listOf(
                            ChecklistItem("Ryokan in Gion — 3 nights", done = true),
                            ChecklistItem("Arashiyama bamboo grove at dawn"),
                            ChecklistItem("Tea ceremony booking")
                        )
                    )
                )
            )
        )

        notes.forEach { seed ->
            val created = now - seed.updatedAgo - day
            val noteId = db.noteDao().insert(
                NoteEntity(
                    title = seed.title,
                    blocksJson = encodeBlocks(seed.blocks),
                    folderId = folderIds[seed.folder] ?: NoteEntity.DEFAULT_FOLDER_ID,
                    colorIndex = seed.colorIndex,
                    kind = seed.kind,
                    pinned = seed.pinned,
                    createdAt = created,
                    updatedAt = now - seed.updatedAgo
                )
            )
            val refs = seed.tagNames.mapNotNull { tagIds[it] }.map { NoteTagCrossRef(noteId, it) }
            if (refs.isNotEmpty()) db.noteDao().insertNoteTags(refs)
        }

        val memos = listOf(
            Triple("Morning idea", 24_000L, 3),
            Triple("Grocery list", 72_000L, 11),
            Triple("Midnight thought", 41_000L, 5)
        )
        memos.forEachIndexed { index, (title, duration, seed) ->
            db.memoDao().insert(
                MemoEntity(
                    title = title,
                    path = "",
                    durationMs = duration,
                    amplitudesJson = encodeToStringList(syntheticAmplitudes(30, seed)),
                    createdAt = now - (index + 1) * 5 * hour
                )
            )
        }

        settings.seedStreak(streak = 3, todayEpochDay = TimeFormat.epochDay())
    }

    private fun syntheticAmplitudes(count: Int, seed: Int): List<Int> =
        List(count) { i -> 5 + ((i * 37 + seed * 13) % 17) }

    private data class SeedNote(
        val title: String,
        val folder: String,
        val tagNames: List<String>,
        val blocks: List<Block>,
        val colorIndex: Int = 0,
        val kind: Int = NoteEntity.KIND_TEXT,
        val pinned: Boolean = false,
        val updatedAgo: Long
    )
}
