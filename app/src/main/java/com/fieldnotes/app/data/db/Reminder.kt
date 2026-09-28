package com.fieldnotes.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class RepeatMode(val key: String, val label: String) {
    NONE("none", "Once"),
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly");

    companion object {
        fun fromKey(key: String): RepeatMode = entries.firstOrNull { it.key == key } ?: NONE
    }
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueAt: Long,
    val createdAt: Long,
    val completed: Boolean = false,
    val repeat: String = RepeatMode.NONE.key,
    val noteId: Long? = null
)

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE completed = 0 ORDER BY dueAt")
    fun observePending(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE completed = 1 ORDER BY dueAt DESC")
    fun observeCompleted(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminder(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE completed = 0 AND dueAt > :now")
    suspend fun getPendingAfter(now: Long): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("UPDATE reminders SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE reminders SET dueAt = :dueAt WHERE id = :id")
    suspend fun setDueAt(id: Long, dueAt: Long)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()
}
