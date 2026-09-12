package com.fieldnotes.app.ui.quick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.ReminderEntity
import com.fieldnotes.app.data.repo.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(private val repo: NoteRepository) : ViewModel() {

    val reminders: StateFlow<List<ReminderEntity>> = repo.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String, dueAt: Long) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.createReminder(trimmed, dueAt) }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch { repo.deleteReminder(reminder) }
    }
}
