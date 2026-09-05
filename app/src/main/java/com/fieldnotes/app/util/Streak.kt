package com.fieldnotes.app.util

object Streak {

    /**
     * Next streak value given the last active day and the current streak.
     * Consecutive days extend the streak, same day keeps it, a gap resets it.
     */
    fun nextStreak(lastActiveEpochDay: Long?, currentStreak: Int, todayEpochDay: Long): Int =
        when (lastActiveEpochDay) {
            todayEpochDay -> currentStreak.coerceAtLeast(1)
            todayEpochDay - 1 -> currentStreak + 1
            else -> 1
        }
}
