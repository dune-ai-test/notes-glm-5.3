package com.fieldnotes.app.ui.quick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.media.AudioRecorder
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.SettingsRepository
import com.fieldnotes.app.util.TimeFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuickTag(val name: String, val colorIndex: Int, val count: Int)

data class FocusState(
    val totalMs: Long = 25 * 60_000L,
    val remainingMs: Long = 25 * 60_000L,
    val running: Boolean = false
) {
    val progress: Float get() = 1f - remainingMs.toFloat() / totalMs
    val clock: String get() = TimeFormat.duration(remainingMs)
}

class QuickViewModel(
    private val repo: NoteRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    val memos: StateFlow<List<com.fieldnotes.app.data.db.MemoEntity>> = repo.memos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val newMemosCount: StateFlow<Int> =
        repo.memosSince(System.currentTimeMillis() - 24 * 60 * 60_000L)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val streak: StateFlow<Int> = settings.settings
        .map { it.streakCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val tags: StateFlow<List<QuickTag>> = combine(repo.tags, repo.tagCounts) { tags, counts ->
        val countById = counts.associate { it.tagId to it.count }
        tags.map { QuickTag(it.name, it.colorIndex, countById[it.id] ?: 0) }
            .sortedByDescending { it.count }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _focus = MutableStateFlow(FocusState())
    val focus = _focus.asStateFlow()

    private var focusJob: Job? = null

    fun toggleFocus() {
        val current = _focus.value
        if (current.running) {
            focusJob?.cancel()
            focusJob = null
            _focus.value = current.copy(running = false)
        } else {
            _focus.value = current.copy(running = true)
            focusJob = viewModelScope.launch {
                while (_focus.value.running && _focus.value.remainingMs > 0) {
                    delay(1_000)
                    val f = _focus.value
                    if (!f.running) break
                    val next = (f.remainingMs - 1_000).coerceAtLeast(0)
                    _focus.value = f.copy(remainingMs = next, running = next > 0)
                }
            }
        }
    }

    fun resetFocus() {
        focusJob?.cancel()
        focusJob = null
        _focus.value = FocusState()
    }

    fun saveMemo(result: AudioRecorder.Result, onSaved: () -> Unit) {
        viewModelScope.launch {
            repo.createMemo(
                title = "Memo · ${TimeFormat.dateShort(System.currentTimeMillis())}",
                path = result.path,
                durationMs = result.durationMs,
                amplitudes = result.amplitudes
            )
            touchStreak()
            onSaved()
        }
    }

    fun deleteMemo(memo: com.fieldnotes.app.data.db.MemoEntity) {
        viewModelScope.launch { repo.deleteMemo(memo) }
    }

    fun createTag(name: String) {
        val trimmed = name.trim().removePrefix("#")
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.createTag(trimmed, (0..5).random()) }
    }

    fun touchStreak() {
        viewModelScope.launch { settings.touchStreak(TimeFormat.epochDay()) }
    }
}
