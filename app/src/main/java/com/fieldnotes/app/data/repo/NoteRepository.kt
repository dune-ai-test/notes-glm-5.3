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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import java.io.File

class NoteRepository(private val db: AppDatabase) {

    val allNotes: Flow<List<NoteWithTags>> = db.noteDao().observeAll()
    val activeNotes: Flow<List<NoteWithTags>> = db.noteDao().observeActive()
    val activeNoteCount: Flow<Int> = db.noteDao().observeActiveCount()
    val trashedNotes: Flow<List<NoteWithTags>> = db.noteDao().observeTrashed()
    val noteCount: Flow<Int> = db.noteDao().observeCount()
    val folders: Flow<List<FolderEntity>> = db.folderDao().observeAll()
    val tags: Flow<List<TagEntity>> = db.tagDao().observeAll()
    val tagCounts: Flow<List<TagIdCount>> = db.tagDao().observeCounts()
    val memos: Flow<List<MemoEntity>> = db.memoDao().observeAll()

    val pendingReminders: Flow<List<com.fieldnotes.app.data.db.ReminderEntity>> =
        db.reminderDao().observePending()

    val completedReminders: Flow<List<com.fieldnotes.app.data.db.ReminderEntity>> =
        db.reminderDao().observeCompleted()

    fun memosSince(since: Long): Flow<Int> = db.memoDao().observeCountSince(since)

    suspend fun getReminder(id: Long): com.fieldnotes.app.data.db.ReminderEntity? =
        db.reminderDao().getReminder(id)

    suspend fun createReminder(
        title: String,
        dueAt: Long,
        repeat: String = com.fieldnotes.app.data.db.RepeatMode.NONE.key,
        noteId: Long? = null
    ): Long =
        db.reminderDao().insert(
            com.fieldnotes.app.data.db.ReminderEntity(
                title = title,
                dueAt = dueAt,
                createdAt = System.currentTimeMillis(),
                repeat = repeat,
                noteId = noteId
            )
        )

    suspend fun setReminderDueAt(id: Long, dueAt: Long) = db.reminderDao().setDueAt(id, dueAt)

    suspend fun setReminderCompleted(id: Long, completed: Boolean) =
        db.reminderDao().setCompleted(id, completed)

    suspend fun pendingFutureReminders(now: Long): List<com.fieldnotes.app.data.db.ReminderEntity> =
        db.reminderDao().getPendingAfter(now)

    suspend fun deleteReminder(reminder: com.fieldnotes.app.data.db.ReminderEntity) =
        db.reminderDao().delete(reminder)

    fun folderNotes(folderId: Long): Flow<List<NoteWithTags>> = db.noteDao().observeActiveFolder(folderId)

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

    /** Soft-deletes: recoverable from Trash for 30 days. */
    suspend fun trashNote(note: NoteEntity) =
        db.noteDao().setTrashed(note.id, System.currentTimeMillis())

    suspend fun restoreNote(id: Long) = db.noteDao().setNotTrashed(id)

    /** Permanently deletes a note, its tag links, and its media files (best effort). */
    suspend fun hardDelete(note: NoteEntity) {
        db.noteDao().clearNoteTags(note.id)
        decodeBlocks(note.blocksJson).forEach { block ->
            val path = when (block) {
                is Block.Image -> block.path
                is Block.Audio -> block.path
                else -> ""
            }
            if (path.isNotBlank()) {
                runCatching { File(path).takeIf { it.exists() }?.delete() }
            }
        }
        db.noteDao().delete(note)
    }

    suspend fun emptyTrash() {
        db.noteDao().observeTrashed().first().forEach { hardDelete(it.note) }
    }

    /** Permanently removes trashed notes older than [days]. Call on app start. */
    suspend fun purgeOldTrashed(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        db.noteDao().getTrashedBefore(cutoff).forEach { hardDelete(it) }
    }

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
