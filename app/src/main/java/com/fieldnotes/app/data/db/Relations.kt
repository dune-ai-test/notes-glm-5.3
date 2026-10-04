package com.fieldnotes.app.data.db

import androidx.room.Embedded

data class NoteWithTags(
    @Embedded val note: NoteEntity
)
