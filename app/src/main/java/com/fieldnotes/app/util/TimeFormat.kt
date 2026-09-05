package com.fieldnotes.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

object TimeFormat {

    fun duration(ms: Long): String {
        val totalSeconds = ms / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%d:%02d".format(m, s)
    }

    fun relative(ms: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - ms
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        return when {
            diff < minute -> "just now"
            diff < hour -> "${diff / minute}m ago"
            diff < day -> "${diff / hour}h ago"
            diff < 7 * day -> "${diff / day}d ago"
            else -> dateShort(ms, now)
        }
    }

    fun smartDate(ms: Long, now: Long = System.currentTimeMillis()): String {
        val date = localDate(ms)
        val today = localDate(now)
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> dateShort(ms, now)
        }
    }

    fun dateShort(ms: Long, now: Long = System.currentTimeMillis()): String {
        val date = localDate(ms)
        val month = date.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
        return "$month ${date.dayOfMonth}"
    }

    fun headerDate(now: Long = System.currentTimeMillis()): String {
        val date = localDate(now)
        val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
        val month = date.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
        return "$weekday · $month ${date.dayOfMonth}"
    }

    fun greeting(hourOfDay: Int): String = when {
        hourOfDay < 12 -> "Good morning"
        hourOfDay < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    fun epochDay(ms: Long = System.currentTimeMillis()): Long = localDate(ms).toEpochDay()

    private fun localDate(ms: Long): LocalDate =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
}
