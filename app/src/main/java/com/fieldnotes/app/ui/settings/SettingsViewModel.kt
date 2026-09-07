package com.fieldnotes.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.export.NoteExporter
import com.fieldnotes.app.data.repo.AppSettings
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.SettingsRepository
import com.fieldnotes.app.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settingsRepo: SettingsRepository = container.settingsRepository
    private val noteRepo: NoteRepository = container.noteRepository

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val noteCount: StateFlow<Int> = noteRepo.noteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val biometricAvailable: Boolean = container.biometricAvailable

    fun setUserName(value: String) {
        if (value.isBlank()) return
        viewModelScope.launch { settingsRepo.setUserName(value.trim()) }
    }

    fun setHaptics(value: Boolean) = viewModelScope.launch { settingsRepo.setHaptics(value) }
    fun setReduceMotion(value: Boolean) = viewModelScope.launch { settingsRepo.setReduceMotion(value) }
    fun setAutoTranscribe(value: Boolean) = viewModelScope.launch { settingsRepo.setAutoTranscribe(value) }
    fun setPushNotifications(value: Boolean) = viewModelScope.launch { settingsRepo.setPushNotifications(value) }
    fun setSounds(value: Boolean) = viewModelScope.launch { settingsRepo.setSounds(value) }
    fun setBiometricLock(value: Boolean) = viewModelScope.launch { settingsRepo.setBiometricLock(value) }
    fun setDefaultCapture(value: String) = viewModelScope.launch { settingsRepo.setDefaultCapture(value) }
    fun setAudioQuality(value: String) = viewModelScope.launch { settingsRepo.setAudioQuality(value) }

    suspend fun cacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val dir = container.context.cacheDir
        if (dir == null || !dir.exists()) 0L else dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearCache(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            container.context.cacheDir?.deleteRecursively()
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    /** Writes a ZIP backup (Markdown + JSON + media) to the picked location. */
    fun exportAll(target: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val notes = noteRepo.allNotes.first()
                    val folders = noteRepo.folders.first()
                    val memos: List<MemoEntity> = noteRepo.memos.first()
                    container.context.contentResolver.openOutputStream(target)?.use { out ->
                        NoteExporter.exportZip(notes, folders, memos, out)
                    } ?: return@withContext false
                    true
                } catch (_: Exception) {
                    false
                }
            }
            onDone(ok)
        }
    }

    /** Erases everything: notes, memos, media and settings. */
    fun wipeAll(onDone: () -> Unit) {
        viewModelScope.launch {
            noteRepo.wipeAll()
            settingsRepo.clearAll()
            onDone()
        }
    }
}
