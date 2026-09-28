package com.fieldnotes.app.data.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fieldnotes.app.MainActivity
import com.fieldnotes.app.R
import com.fieldnotes.app.data.db.RepeatMode
import com.fieldnotes.app.data.db.ReminderEntity
import java.util.Calendar

/** Shared keys/ids for the reminder alarm + notification pipeline. */
object ReminderContract {
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_OPEN_NOTE = "open_note_id"
    const val ACTION_COMPLETE = "com.fieldnotes.app.action.COMPLETE_REMINDER"
    const val ACTION_SNOOZE_1H = "com.fieldnotes.app.action.SNOOZE_REMINDER"
    const val CHANNEL_ID = "reminders"
}

object ReminderScheduler {

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            ReminderContract.CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Due reminders" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Exact alarm when permitted (Android 12+ asks), inexact fallback otherwise. */
    fun schedule(context: Context, reminderId: Long, triggerAt: Long) {
        if (triggerAt <= System.currentTimeMillis()) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmPendingIntent(context, reminderId)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(alarmPendingIntent(context, reminderId))
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    private fun alarmPendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderContract.EXTRA_ID, reminderId)
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun notify(context: Context, reminder: ReminderEntity) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ReminderContract.EXTRA_OPEN_NOTE, reminder.noteId ?: -1L)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val completePi = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(ReminderContract.ACTION_COMPLETE)
                .putExtra(ReminderContract.EXTRA_ID, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(ReminderContract.ACTION_SNOOZE_1H)
                .putExtra(ReminderContract.EXTRA_ID, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderContract.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText("Tap to open" + if (reminder.noteId != null) " this note" else "")
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(0, "Complete", completePi)
            .addAction(0, "Snooze 1h", snoozePi)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
        }
    }

    /** Next occurrence for a repeating reminder, or null when it doesn't repeat. */
    fun nextOccurrence(reminder: ReminderEntity): Long? = when (RepeatMode.fromKey(reminder.repeat)) {
        RepeatMode.DAILY -> reminder.dueAt + 24L * 60 * 60 * 1000
        RepeatMode.WEEKLY -> reminder.dueAt + 7L * 24 * 60 * 60 * 1000
        RepeatMode.NONE -> null
    }

    fun snoozeUntil1h(): Long = System.currentTimeMillis() + 60L * 60 * 1000

    fun tomorrowAt(hour: Int = 9): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
