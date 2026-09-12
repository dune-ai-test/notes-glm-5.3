package com.fieldnotes.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fieldnotes.app.util.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "fieldnotes_settings")

data class AppSettings(
    val userName: String = "You",
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val autoTranscribe: Boolean = true,
    val pushNotifications: Boolean = true,
    val sounds: Boolean = true,
    val biometricLock: Boolean = false,
    val autoLockMinutes: Int = 1,
    val autoBackup: Boolean = true,
    val defaultCapture: String = "voice",
    val audioQuality: String = "high",
    val appearance: String = "light",
    val streakCount: Int = 0,
    val lastActiveEpochDay: Long? = null
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val haptics = booleanPreferencesKey("haptics")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val autoTranscribe = booleanPreferencesKey("auto_transcribe")
        val pushNotifications = booleanPreferencesKey("push_notifications")
        val sounds = booleanPreferencesKey("sounds")
        val biometricLock = booleanPreferencesKey("biometric_lock")
        val autoLockMinutes = intPreferencesKey("auto_lock_minutes")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val defaultCapture = stringPreferencesKey("default_capture")
        val audioQuality = stringPreferencesKey("audio_quality")
        val appearance = stringPreferencesKey("appearance")
        val streakCount = intPreferencesKey("streak_count")
        val lastActiveEpochDay = longPreferencesKey("last_active_epoch_day")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            userName = p[Keys.userName] ?: "You",
            haptics = p[Keys.haptics] ?: true,
            reduceMotion = p[Keys.reduceMotion] ?: false,
            autoTranscribe = p[Keys.autoTranscribe] ?: true,
            pushNotifications = p[Keys.pushNotifications] ?: true,
            sounds = p[Keys.sounds] ?: true,
            biometricLock = p[Keys.biometricLock] ?: false,
            autoLockMinutes = p[Keys.autoLockMinutes] ?: 1,
            autoBackup = p[Keys.autoBackup] ?: true,
            defaultCapture = p[Keys.defaultCapture] ?: "voice",
            audioQuality = p[Keys.audioQuality] ?: "high",
            appearance = p[Keys.appearance] ?: "light",
            streakCount = p[Keys.streakCount] ?: 0,
            lastActiveEpochDay = p[Keys.lastActiveEpochDay]
        )
    }

    suspend fun setUserName(value: String) = edit { it[Keys.userName] = value }
    suspend fun setHaptics(value: Boolean) = edit { it[Keys.haptics] = value }
    suspend fun setReduceMotion(value: Boolean) = edit { it[Keys.reduceMotion] = value }
    suspend fun setAutoTranscribe(value: Boolean) = edit { it[Keys.autoTranscribe] = value }
    suspend fun setPushNotifications(value: Boolean) = edit { it[Keys.pushNotifications] = value }
    suspend fun setSounds(value: Boolean) = edit { it[Keys.sounds] = value }
    suspend fun setBiometricLock(value: Boolean) = edit { it[Keys.biometricLock] = value }
    suspend fun setAutoLockMinutes(value: Int) = edit { it[Keys.autoLockMinutes] = value }
    suspend fun setAutoBackup(value: Boolean) = edit { it[Keys.autoBackup] = value }
    suspend fun setDefaultCapture(value: String) = edit { it[Keys.defaultCapture] = value }
    suspend fun setAudioQuality(value: String) = edit { it[Keys.audioQuality] = value }
    suspend fun setAppearance(value: String) = edit { it[Keys.appearance] = value }

    /** Marks today as active and returns the updated streak count. */
    suspend fun touchStreak(todayEpochDay: Long): Int {
        var result = 0
        context.settingsDataStore.edit { p ->
            val last = p[Keys.lastActiveEpochDay]
            val current = p[Keys.streakCount] ?: 0
            val next = Streak.nextStreak(last, current, todayEpochDay)
            p[Keys.streakCount] = next
            p[Keys.lastActiveEpochDay] = todayEpochDay
            result = next
        }
        return result
    }

    suspend fun clearAll() {
        context.settingsDataStore.edit { it.clear() }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit { p -> block(p) }
    }
}
