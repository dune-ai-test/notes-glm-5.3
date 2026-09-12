package com.fieldnotes.app.data.repo

import com.fieldnotes.app.data.db.AppDatabase
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteTagCrossRef
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.db.TagIdCount
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.data.model.encodeToStringList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

class NoteRepository(private val db: AppDatabase) {

    val allNotes: Flow<List<NoteWithTags>> = db.noteDao().observeAll()
    val noteCount: Flow<Int> = db.noteDao().observeCount()
    val folders: Flow<List<FolderEntity>> = db.folderDao().observeAll()
    val tags: Flow<List<TagEntity>> = db.tagDao().observeAll()
    val tagCounts: Flow<List<TagIdCount>> = db.tagDao().observeCounts()
    val memos: Flow<List<MemoEntity>> = db.memoDao().observeAll()

    val reminders: Flow<List<com.fieldnotes.app.data.db.ReminderEntity>> = db.reminderDao().observeAll()

    fun memosSince(since: Long): Flow<Int> = db.memoDao().observeCountSince(since)

    suspend fun createReminder(title: String, dueAt: Long): Long =
        db.reminderDao().insert(
            com.fieldnotes.app.data.db.ReminderEntity(
                title = title,
                dueAt = dueAt,
                createdAt = System.currentTimeMillis()
            )
        )

    suspend fun deleteReminder(reminder: com.fieldnotes.app.data.db.ReminderEntity) =
        db.reminderDao().delete(reminder)

    fun folderNotes(folderId: Long): Flow<List<NoteWithTags>> = db.noteDao().observeFolder(folderId)

    suspend fun getNote(id: Long): NoteWithTags? = db.noteDao().getNote(id)
    suspend fun getFolder(id: Long): FolderEntity? = db.folderDao().getFolder(id)

    suspend fun createNote(
        title: String = "",
        folderId: Long = NoteEntity.DEFAULT_FOLDER_ID,
        colorIndex: Int = 0,
        kind: Int = NoteEntity.KIND_TEXT
    ): Long {
        val now = System.currentTimeMillis()
        return db.noteDao().insert(
            NoteEntity(
                title = title,
                folderId = folderId,
                colorIndex = colorIndex,
                kind = kind,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun saveNote(note: NoteEntity, blocks: List<Block>, tagIds: Set<Long>) {
        db.noteDao().update(
            note.copy(blocksJson = encodeBlocks(blocks), updatedAt = System.currentTimeMillis())
        )
        db.noteDao().clearNoteTags(note.id)
        if (tagIds.isNotEmpty()) {
            db.noteDao().insertNoteTags(tagIds.map { NoteTagCrossRef(note.id, it) })
        }
    }

    suspend fun saveBlocks(note: NoteEntity, blocks: List<Block>) {
        db.noteDao().update(
            note.copy(blocksJson = encodeBlocks(blocks), updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun updateNote(note: NoteEntity) = db.noteDao().update(note)
    suspend fun deleteNote(note: NoteEntity) = db.noteDao().delete(note)

    suspend fun createTag(name: String, colorIndex: Int): Long =
        db.tagDao().insert(TagEntity(name = name.trim(), colorIndex = colorIndex))

    suspend fun deleteTag(tag: TagEntity) {
        db.tagDao().clearTagRefs(tag.id)
        db.tagDao().delete(tag)
    }

    suspend fun createMemo(title: String, path: String, durationMs: Long, amplitudes: List<Int>): Long =
        db.memoDao().insert(
            MemoEntity(
                title = title,
                path = path,
                durationMs = durationMs,
                amplitudesJson = encodeToStringList(amplitudes),
                createdAt = System.currentTimeMillis()
            )
        )

    suspend fun deleteMemo(memo: MemoEntity) = db.memoDao().delete(memo)

    /** Persists a manual drag order: ids in display order get sortIndex 0..n. */
    suspend fun saveCustomOrder(order: List<Long>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            order.forEachIndexed { index, id -> db.noteDao().updateSortIndex(id, index) }
        }
    }

    /** Erases everything; used by "Erase all data" and by clearing app data. */
    suspend fun wipeAll() {
        db.noteDao().clearAll()
        db.noteDao().clearAllNoteTags()
        db.tagDao().clearAllTags()
        db.folderDao().clearAllFolders()
        db.memoDao().clearAllMemos()
    }

    /**
     * Seeds the permanent starter structure: default folders and a base tag set.
     * Runs only when the respective tables are empty (first launch or after a wipe).
     */
    suspend fun seedDefaultsIfEmpty() {
        if (db.folderDao().count() == 0) {
            listOf(
                FolderEntity(name = "Work", iconKey = "work", colorIndex = 5),
                FolderEntity(name = "Personal", iconKey = "heart", colorIndex = 1),
                FolderEntity(name = "Ideas", iconKey = "bulb", colorIndex = 3),
                FolderEntity(name = "Archive", iconKey = "archive", colorIndex = 0)
            ).forEach { db.folderDao().insert(it) }
        }
        if (db.tagDao().count() == 0) {
            listOf(
                TagEntity(name = "work", colorIndex = 6),
                TagEntity(name = "personal", colorIndex = 1),
                TagEntity(name = "ideas", colorIndex = 3),
                TagEntity(name = "travel", colorIndex = 5),
                TagEntity(name = "reading", colorIndex = 2),
                TagEntity(name = "recipes", colorIndex = 4),
                TagEntity(name = "inspiration", colorIndex = 0)
            ).forEach { db.tagDao().insert(it) }
        }
    }
}
