package com.harsha.myapplication.notifications

import java.util.Calendar

/**
 * Pure timing rules for reminders. No Android classes, so everything here is unit-tested on the JVM
 * (see ReminderLogicTest). [ReminderScheduler] and [ReminderReceiver] only call these.
 */
object ReminderLogic {
    const val MINUTE_MS = 60_000L
    const val HOUR_MS = 60 * MINUTE_MS

    /** An exact "your visit is now" alarm delivered later than this is dropped as stale. */
    const val EXACT_STALE_MS = 2 * HOUR_MS

    /** A "coming up" alarm delivered this close to the visit (or later) is pointless, the exact one follows. */
    const val BEFORE_STALE_MARGIN_MS = 5 * MINUTE_MS

    /** A daily summary / daily text is still worth showing this long after its scheduled time. */
    const val DAILY_CATCH_UP_MS = 6 * HOUR_MS

    /** How often the watchdog re-syncs every alarm, in case one was lost or a re-arm failed. */
    const val HEARTBEAT_INTERVAL_MS = 6 * HOUR_MS

    /** When a recurring alarm re-arms itself it must land strictly in the future, never on "now". */
    const val REARM_MIN_LEAD_MS = 30_000L

    /** Next wall-clock occurrence of [minutesSinceMidnight] that is later than `nowMs + minLeadMs`. */
    fun nextDaily(nowMs: Long, minutesSinceMidnight: Int, minLeadMs: Long = 0L): Long {
        val cal = timeOfDay(nowMs, minutesSinceMidnight)
        if (cal.timeInMillis <= nowMs + minLeadMs) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** Next [dayOfWeek] ([Calendar.SUNDAY]..[Calendar.SATURDAY]) at [minutesSinceMidnight], after `nowMs + minLeadMs`. */
    fun nextWeekly(nowMs: Long, dayOfWeek: Int, minutesSinceMidnight: Int, minLeadMs: Long = 0L): Long {
        val cal = timeOfDay(nowMs, minutesSinceMidnight)
        // Walk forward to the requested weekday (never backwards), so it does not depend on the locale's first day of week.
        var guard = 0
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek && guard++ < 7) cal.add(Calendar.DAY_OF_YEAR, 1)
        if (cal.timeInMillis <= nowMs + minLeadMs) cal.add(Calendar.DAY_OF_YEAR, 7)
        return cal.timeInMillis
    }

    /** Today's wall-clock instant for [minutesSinceMidnight]. */
    fun todayAt(nowMs: Long, minutesSinceMidnight: Int): Long = timeOfDay(nowMs, minutesSinceMidnight).timeInMillis

    /**
     * Should a daily summary / daily text that was scheduled for [scheduledMs] still be shown at [nowMs]?
     * Replaces the old hard-coded "before 11 AM" rule, which silently dropped any summary set for later in the day.
     */
    fun isDailyStillDeliverable(nowMs: Long, scheduledMs: Long): Boolean =
        nowMs >= scheduledMs && nowMs - scheduledMs <= DAILY_CATCH_UP_MS

    /** An exact visit reminder is stale once it is delivered [EXACT_STALE_MS] after the visit time. */
    fun isExactStale(nowMs: Long, visitMs: Long): Boolean = visitMs > 0 && nowMs >= visitMs + EXACT_STALE_MS

    /** A "coming up" reminder is stale once the visit is about to start (or has started). */
    fun isBeforeStale(nowMs: Long, visitMs: Long): Boolean = visitMs > 0 && nowMs >= visitMs - BEFORE_STALE_MARGIN_MS

    /**
     * An alarm carries the visit time it was scheduled for. If the person's visit has since moved, the alarm is a
     * leftover and must not notify. [currentMs] is null when the person no longer has a valid visit.
     */
    fun isScheduledTimeOutdated(scheduledMs: Long, currentMs: Long?): Boolean =
        currentMs == null || (scheduledMs > 0 && kotlin.math.abs(currentMs - scheduledMs) > MINUTE_MS)

    /** Stable per-person request code. Safe for hashCode() == Int.MIN_VALUE (abs() of that is still negative). */
    fun requestCode(personId: String, kind: Int): Int {
        val h = personId.hashCode()
        val positive = if (h == Int.MIN_VALUE) 0 else kotlin.math.abs(h)
        return (positive % 100_000) * 5 + 1000 + kind
    }

    /** Greeting that matches the hour the summary is actually shown at. */
    fun greeting(hourOfDay: Int): String = when (hourOfDay) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    private fun timeOfDay(nowMs: Long, minutesSinceMidnight: Int): Calendar =
        Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, (minutesSinceMidnight / 60).coerceIn(0, 23))
            set(Calendar.MINUTE, (minutesSinceMidnight % 60).coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}
