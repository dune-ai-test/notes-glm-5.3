package com.fieldnotes.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        FolderEntity::class,
        NoteTagCrossRef::class,
        MemoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao
    abstract fun memoDao(): MemoDao
}
