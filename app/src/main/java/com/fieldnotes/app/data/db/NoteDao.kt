package com.fieldnotes.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY pinned DESC, updatedAt DESC")
    fun observeFolder(folderId: Long): Flow<List<NoteWithTags>>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM notes")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun clearAll()

    @Query("UPDATE notes SET sortIndex = :index WHERE id = :id")
    suspend fun updateSortIndex(id: Long, index: Int)

    @Transaction
    @Query("SELECT * FROM notes WHERE trashed = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE folderId = :folderId AND trashed = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeActiveFolder(folderId: Long): Flow<List<NoteWithTags>>

    @Query("SELECT COUNT(*) FROM notes WHERE trashed = 0")
    fun observeActiveCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashed(): Flow<List<NoteWithTags>>

    @Query("SELECT * FROM notes WHERE trashed = 1 AND trashedAt IS NOT NULL AND trashedAt < :cutoff")
    suspend fun getTrashedBefore(cutoff: Long): List<NoteEntity>

    @Query("UPDATE notes SET trashed = 1, trashedAt = :at WHERE id = :id")
    suspend fun setTrashed(id: Long, at: Long)

    @Query("UPDATE notes SET trashed = 0, trashedAt = NULL WHERE id = :id")
    suspend fun setNotTrashed(id: Long)
}

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY id")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolder(id: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("DELETE FROM folders")
    suspend fun clearAllFolders()

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun count(): Int
}

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query("SELECT COUNT(*) FROM memos WHERE createdAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Insert
    suspend fun insert(memo: MemoEntity): Long

    @Delete
    suspend fun delete(memo: MemoEntity)

    @Query("DELETE FROM memos")
    suspend fun clearAllMemos()
}
