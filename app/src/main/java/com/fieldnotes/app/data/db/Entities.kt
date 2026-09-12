package com.fieldnotes.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val colorIndex: Int
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val blocksJson: String = "[]",
    val folderId: Long = DEFAULT_FOLDER_ID,
    val colorIndex: Int = 0,
    val kind: Int = KIND_TEXT,
    val pinned: Boolean = false,
    val locked: Boolean = false,
    val sortIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val KIND_TEXT = 0
        const val KIND_SKETCH = 1
        const val DEFAULT_FOLDER_ID = 1L
    }
}

@Entity(
    tableName = "note_tag_join",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index("tagId")]
)
data class NoteTagCrossRef(val noteId: Long, val tagId: Long)

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val path: String = "",
    val durationMs: Long = 0L,
    val amplitudesJson: String = "[]",
    val createdAt: Long
)
