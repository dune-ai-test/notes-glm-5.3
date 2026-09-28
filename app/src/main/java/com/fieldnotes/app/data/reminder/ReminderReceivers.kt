package com.fieldnotes.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.fieldnotes.app.FieldNotesApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires at a reminder's due time: posts the notification, rolls repeats forward. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderContract.EXTRA_ID, -1L)
        if (id <= 0) return
        val app = context.applicationContext as? FieldNotesApp ?: return
        val repo = app.container.noteRepository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = repo.getReminder(id) ?: return@launch
                if (reminder.completed) return@launch
                ReminderScheduler.notify(context, reminder)

                // Repeat: roll the due date forward and schedule the next alarm.
                val next = ReminderScheduler.nextOccurrence(reminder)
                if (next != null) {
                    repo.setReminderDueAt(id, next)
                    ReminderScheduler.schedule(context, id, next)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Notification actions: complete or snooze for an hour. */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderContract.EXTRA_ID, -1L)
        if (id <= 0) return
        val app = context.applicationContext as? FieldNotesApp ?: return
        val repo = app.container.noteRepository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderContract.ACTION_COMPLETE -> {
                        repo.setReminderCompleted(id, true)
                        ReminderScheduler.cancel(context, id)
                    }
                    ReminderContract.ACTION_SNOOZE_1H -> {
                        val to = ReminderScheduler.snoozeUntil1h()
                        repo.setReminderDueAt(id, to)
                        ReminderScheduler.schedule(context, id, to)
                        NotificationManagerCompat.from(context).cancel(id.toInt())
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Re-registers all future reminders after a reboot. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? FieldNotesApp ?: return
        val repo = app.container.noteRepository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repo.pendingFutureReminders(System.currentTimeMillis()).forEach {
                    ReminderScheduler.schedule(context, it.id, it.dueAt)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
