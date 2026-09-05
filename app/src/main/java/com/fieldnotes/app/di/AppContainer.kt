package com.fieldnotes.app.di

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import com.fieldnotes.app.data.db.AppDatabase
import com.fieldnotes.app.data.media.AudioPlayer
import com.fieldnotes.app.data.media.AudioRecorder
import com.fieldnotes.app.data.media.ImageStore
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.Seeder
import com.fieldnotes.app.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppContainer(private val appContext: Context) {

    val context: Context get() = appContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "fieldnotes.db"
    ).fallbackToDestructiveMigration().build()

    val noteRepository = NoteRepository(database)
    val settingsRepository = SettingsRepository(context)
    val audioRecorder = AudioRecorder(context)
    val audioPlayer = AudioPlayer(context)
    val imageStore = ImageStore(context)

    val recordingsDir: File = File(context.filesDir, "recordings")

    val biometricAvailable: Boolean = BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

    init {
        appScope.launch { Seeder(database, settingsRepository).seedIfEmpty() }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
