package com.fieldnotes.app.di

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import com.fieldnotes.app.data.db.AppDatabase
import com.fieldnotes.app.data.media.AudioPlayer
import com.fieldnotes.app.data.media.AudioRecorder
import com.fieldnotes.app.data.media.ImageStore
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppContainer(private val appContext: Context) {

    val context: Context get() = appContext

    val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "fieldnotes.db"
    )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )
        .fallbackToDestructiveMigration()
        .build()

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

    /** Current sounds preference mirror so non-compose code can honor it. */
    @Volatile
    var soundsEnabled: Boolean = true
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        appScope.launch {
            noteRepository.purgeOldTrashed(days = 30)
            noteRepository.seedDefaultsIfEmpty()
            settingsRepository.settings.collect {
                soundsEnabled = it.sounds
                scheduleAutoBackup(context, it.autoBackup)
            }
        }
    }

    /** Short confirmation chime, honoring the Sounds preference. */
    fun playChime() {
        if (!soundsEnabled) return
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 150)
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 400)
        }
    }

    companion object {
        private const val AUTO_BACKUP_WORK = "auto_backup"

        /** Weekly background ZIP backup, battery-not-low; keeps the last 4 files. */
        private fun scheduleAutoBackup(context: Context, enabled: Boolean) {
            val wm = androidx.work.WorkManager.getInstance(context)
            runCatching {
                if (enabled) {
                    val request = androidx.work.PeriodicWorkRequestBuilder<com.fieldnotes.app.data.backup.AutoBackupWorker>(
                        7, java.util.concurrent.TimeUnit.DAYS
                    )
                        .setConstraints(
                            androidx.work.Constraints.Builder()
                                .setRequiresBatteryNotLow(true)
                                .build()
                        )
                        .build()
                    wm.enqueueUniquePeriodicWork(AUTO_BACKUP_WORK, androidx.work.ExistingPeriodicWorkPolicy.KEEP, request)
                } else {
                    wm.cancelUniqueWork(AUTO_BACKUP_WORK)
                }
            }
        }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
