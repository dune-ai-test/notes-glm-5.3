package com.fieldnotes.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.fieldnotes.app.MainActivity
import com.fieldnotes.app.R
import com.fieldnotes.app.data.reminder.ReminderContract

/** Home-screen widget: one tap opens a new note draft. */
class QuickCaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pi = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).putExtra(ReminderContract.EXTRA_NEW_NOTE, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_capture)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            manager.updateAppWidget(id, views)
        }
    }
}
