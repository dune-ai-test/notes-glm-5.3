package com.fieldnotes.app.ui.quick

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.ReminderEntity
import com.fieldnotes.app.data.db.RepeatMode
import com.fieldnotes.app.data.reminder.ReminderScheduler
import com.fieldnotes.app.data.repo.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val repo: NoteRepository,
    private val appContext: Context
) : ViewModel() {

    val pending: StateFlow<List<ReminderEntity>> = repo.pendingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completed: StateFlow<List<ReminderEntity>> = repo.completedReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String, dueAt: Long, repeat: RepeatMode, noteId: Long? = null) {
        viewModelScope.launch {
            val id = repo.createReminder(title, dueAt, repeat.key, noteId)
            ReminderScheduler.schedule(appContext, id, dueAt)
        }
    }

    fun complete(id: Long) {
        viewModelScope.launch {
            repo.setReminderCompleted(id, true)
            ReminderScheduler.cancel(appContext, id)
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            val reminder = repo.getReminder(id) ?: return@launch
            repo.setReminderCompleted(id, false)
            if (reminder.dueAt > System.currentTimeMillis()) {
                ReminderScheduler.schedule(appContext, id, reminder.dueAt)
            }
        }
    }

    fun snooze(id: Long, toMs: Long) {
        viewModelScope.launch {
            repo.setReminderDueAt(id, toMs)
            ReminderScheduler.schedule(appContext, id, toMs)
        }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, reminder.id)
            repo.deleteReminder(reminder)
        }
    }
}
