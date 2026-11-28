package com.harsha.myapplication

import com.harsha.myapplication.notifications.ReminderLogic
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReminderLogicTest {
    private lateinit var original: TimeZone

    @Before fun fixZone() {
        original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    }

    @After fun restoreZone() = TimeZone.setDefault(original)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int, s: Int = 0): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min, s) }.timeInMillis

    private fun fields(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms }

    // ── nextDaily ─────────────────────────────────────────────────────────────

    @Test fun nextDailyLaterTodayStaysToday() {
        val now = at(2026, 11, 28, 6, 0)
        assertEquals(at(2026, 11, 28, 7, 0), ReminderLogic.nextDaily(now, 7 * 60))
    }

    @Test fun nextDailyAlreadyPassedMovesToTomorrow() {
        val now = at(2026, 11, 28, 8, 0)
        assertEquals(at(2026, 11, 29, 7, 0), ReminderLogic.nextDaily(now, 7 * 60))
    }

    @Test fun nextDailyExactlyNowMovesToTomorrow() {
        val now = at(2026, 11, 28, 7, 0)
        assertEquals(at(2026, 11, 29, 7, 0), ReminderLogic.nextDaily(now, 7 * 60))
    }

    @Test fun rearmNeverLandsOnNowEvenIfAlarmFiredEarly() {
        // Fired 10 s before 07:00: a plain nextDaily would re-arm for 07:00 again and loop.
        val firedEarly = at(2026, 11, 28, 6, 59, 50)
        val plain = ReminderLogic.nextDaily(firedEarly, 7 * 60)
        val rearm = ReminderLogic.nextDaily(firedEarly, 7 * 60, ReminderLogic.REARM_MIN_LEAD_MS)
        assertEquals(at(2026, 11, 28, 7, 0), plain)
        assertEquals(at(2026, 11, 29, 7, 0), rearm)
    }

    @Test fun nextDailyHandlesFirstAndLastMinuteOfDay() {
        val now = at(2026, 11, 28, 12, 0)
        val last = fields(ReminderLogic.nextDaily(now, 1439)) // 23:59, the largest stored value
        assertEquals(23, last.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, last.get(Calendar.MINUTE))
        assertEquals(at(2026, 11, 29, 0, 0), ReminderLogic.nextDaily(now, 0)) // 00:00 has passed today
    }

    // ── nextWeekly ────────────────────────────────────────────────────────────

    @Test fun nextWeeklyFindsComingSunday() {
        // 2026-11-28 is a Saturday.
        val now = at(2026, 11, 28, 10, 0)
        assertEquals(at(2026, 11, 29, 18, 0), ReminderLogic.nextWeekly(now, Calendar.SUNDAY, 18 * 60))
    }

    @Test fun nextWeeklySameDayPassedGoesNextWeek() {
        val now = at(2026, 11, 29, 19, 0) // Sunday evening, after 18:00
        assertEquals(at(2026, 12, 6, 18, 0), ReminderLogic.nextWeekly(now, Calendar.SUNDAY, 18 * 60))
    }

    @Test fun nextWeeklyIsNeverInThePastForAnyDay() {
        val now = at(2026, 11, 28, 12, 0)
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            val t = ReminderLogic.nextWeekly(now, day, 9 * 60)
            assertTrue("day $day", t > now)
            assertTrue("day $day within a week", t - now <= 7 * 24 * ReminderLogic.HOUR_MS)
            assertEquals(day, fields(t).get(Calendar.DAY_OF_WEEK))
        }
    }

    // ── daily catch-up (replaces the old hard-coded "before 11 AM") ───────────

    @Test fun summaryScheduledForEveningIsNoLongerDropped() {
        val scheduled = at(2026, 11, 28, 20, 0) // the app's default summary time
        val now = at(2026, 11, 28, 20, 5)       // well after 11 AM
        assertTrue(ReminderLogic.isDailyStillDeliverable(now, scheduled))
    }

    @Test fun summaryNotDeliveredBeforeItsTime() {
        assertFalse(ReminderLogic.isDailyStillDeliverable(at(2026, 11, 28, 6, 59), at(2026, 11, 28, 7, 0)))
    }

    @Test fun summaryTooLateIsSkipped() {
        val scheduled = at(2026, 11, 28, 7, 0)
        assertTrue(ReminderLogic.isDailyStillDeliverable(scheduled + ReminderLogic.DAILY_CATCH_UP_MS, scheduled))
        assertFalse(ReminderLogic.isDailyStillDeliverable(scheduled + ReminderLogic.DAILY_CATCH_UP_MS + 1, scheduled))
    }

    // ── stale guards ──────────────────────────────────────────────────────────

    @Test fun exactReminderLateByTwentyMinutesIsStillShown() {
        val visit = at(2026, 11, 28, 10, 0)
        assertFalse(ReminderLogic.isExactStale(visit + 20 * ReminderLogic.MINUTE_MS, visit)) // old rule dropped this
        assertTrue(ReminderLogic.isExactStale(visit + ReminderLogic.EXACT_STALE_MS, visit))
    }

    @Test fun beforeReminderDroppedOnlyWhenVisitIsImminent() {
        val visit = at(2026, 11, 28, 10, 0)
        assertFalse(ReminderLogic.isBeforeStale(visit - 30 * ReminderLogic.MINUTE_MS, visit))
        assertTrue(ReminderLogic.isBeforeStale(visit - 4 * ReminderLogic.MINUTE_MS, visit))
    }

    @Test fun movedVisitMakesOldAlarmOutdated() {
        val scheduled = at(2026, 11, 28, 10, 0)
        assertFalse(ReminderLogic.isScheduledTimeOutdated(scheduled, scheduled))
        assertTrue(ReminderLogic.isScheduledTimeOutdated(scheduled, at(2026, 11, 29, 10, 0)))
        assertTrue(ReminderLogic.isScheduledTimeOutdated(scheduled, at(2026, 11, 28, 11, 0)))
        assertTrue("person lost their visit date", ReminderLogic.isScheduledTimeOutdated(scheduled, null))
    }

    // ── request codes ─────────────────────────────────────────────────────────

    @Test fun requestCodesAreStableAndDistinctPerKind() {
        val codes = (0..3).map { ReminderLogic.requestCode("person-1", it) }
        assertEquals(codes, (0..3).map { ReminderLogic.requestCode("person-1", it) })
        assertEquals(4, codes.toSet().size)
        assertTrue(codes.all { it >= 1000 })
    }

    @Test fun requestCodeSurvivesIntMinValueHash() {
        // "polygenelubricants".hashCode() == Int.MIN_VALUE; Math.abs() of that is still negative.
        val id = "polygenelubricants"
        assertEquals(Int.MIN_VALUE, id.hashCode())
        assertTrue(ReminderLogic.requestCode(id, 0) >= 1000)
        assertNotEquals(ReminderLogic.requestCode(id, 0), ReminderLogic.requestCode(id, 1))
    }

    // ── greeting ──────────────────────────────────────────────────────────────

    @Test fun greetingFollowsTheHour() {
        assertEquals("Good Morning", ReminderLogic.greeting(7))
        assertEquals("Good Afternoon", ReminderLogic.greeting(14))
        assertEquals("Good Evening", ReminderLogic.greeting(20))
    }
}
