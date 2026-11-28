package com.harsha.myapplication.notifications

import com.harsha.myapplication.telemetry.CrashReporter
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.harsha.myapplication.MainActivity
import com.harsha.myapplication.data.DAY_MS
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.data.NotificationPrefsRepository
import com.harsha.myapplication.data.PeopleRepository
import com.harsha.myapplication.data.PersonRecord
import com.harsha.myapplication.data.dateFormat
import com.harsha.myapplication.data.timeFormat
import java.util.Calendar
import java.util.Date

/**
 * Schedules OS alarms for visit exact/before/after/overdue, daily morning,
 * daily text, and weekly summary. [BootReceiver] re-registers after reboot.
 *
 * How it stays reliable:
 *  - Alarms live in the system AlarmManager (setAlarmClock), so they fire with the app closed, in Doze and in
 *    battery saver. When "Alarms & reminders" is not allowed the fallback is an inexact Doze-safe alarm.
 *  - A recurring alarm re-arms itself FIRST when it fires, before anything that could fail.
 *  - A watchdog alarm re-syncs everything every few hours (and catches up anything that was missed).
 *  - Every alarm remembers the visit time it was made for, so a leftover alarm for a visit that has since moved
 *    is ignored instead of notifying at the wrong time.
 */
object ReminderScheduler {
    const val ACTION_REMINDER = "com.harsha.myapplication.ACTION_VISIT_REMINDER"
    const val EXTRA_PERSON_ID = "person_id"
    const val EXTRA_TYPE = "type"
    const val EXTRA_IS_EXACT = "is_exact"
    const val EXTRA_VISIT_TIME = "visit_time"
    const val EXTRA_SCHEDULED_DATE = "scheduled_date"

    const val TYPE_DAILY = "daily"
    const val TYPE_DAILY_TEXT = "daily_text"
    const val TYPE_WEEKLY = "weekly"
    const val TYPE_HEARTBEAT = "heartbeat"

    private const val DEFAULT_TIME = "9:00 AM"
    private const val CATCH_UP_WINDOW_MS = DAY_MS
    private const val STATE_PREFS = "reminder_state"

    private const val RC_DAILY = 100
    private const val RC_DAILY_TEXT = 101
    private const val RC_WEEKLY = 102
    private const val RC_HEARTBEAT = 103

    private val SYSTEM_ALARMS = listOf(
        TYPE_DAILY to RC_DAILY,
        TYPE_DAILY_TEXT to RC_DAILY_TEXT,
        TYPE_WEEKLY to RC_WEEKLY,
        TYPE_HEARTBEAT to RC_HEARTBEAT,
    )

    /** Re-registers every alarm. Safe to call any number of times, from any thread. */
    @Synchronized
    fun rescheduleAll(ctx: Context, catchUpMissed: Boolean = false) {
        val app = ctx.applicationContext
        NotificationPrefsRepository.init(app)
        HoursRepository.init(app)
        val people = PeopleRepository.load(app)

        if (!NotificationPrefsRepository.prefs.masterEnabled) {
            cancelSystemAlarms(app)
            people.forEach { cancelAllForPerson(app, it.id) }
            return
        }

        // Each step is isolated: one bad record or one failing alarm must never stop the rest.
        CrashReporter.attempt("ReminderScheduler") { scheduleDaily(app) }
        CrashReporter.attempt("ReminderScheduler") { scheduleDailyText(app) }
        CrashReporter.attempt("ReminderScheduler") { scheduleWeekly(app) }
        CrashReporter.attempt("ReminderScheduler") { scheduleHeartbeat(app) }
        people.forEach { p ->
            CrashReporter.attempt("ReminderScheduler") { schedule(app, p, markPastAsHandled = false) }
        }
        if (catchUpMissed) CrashReporter.attempt("ReminderScheduler") { catchUp(app) }
    }

    /**
     * Called by a recurring system alarm as the very first thing it does, so the next occurrence is already
     * registered even if showing today's notification fails.
     */
    @Synchronized
    fun rearmSystem(ctx: Context, type: String) {
        val app = ctx.applicationContext
        NotificationPrefsRepository.init(app)
        if (!NotificationPrefsRepository.prefs.masterEnabled) return
        val lead = ReminderLogic.REARM_MIN_LEAD_MS
        when (type) {
            TYPE_DAILY -> scheduleDaily(app, lead)
            TYPE_DAILY_TEXT -> scheduleDailyText(app, lead)
            TYPE_WEEKLY -> scheduleWeekly(app, lead)
            TYPE_HEARTBEAT -> scheduleHeartbeat(app)
        }
    }

    /**
     * (Re)registers the alarms for one person. [markPastAsHandled] is true when the user just saved the record:
     * a visit time that is already in the past is then not "missed", the user is looking at it right now.
     */
    @Synchronized
    fun schedule(ctx: Context, p: PersonRecord, markPastAsHandled: Boolean = true) {
        val app = ctx.applicationContext
        NotificationPrefsRepository.init(app)
        cancelAllForPerson(app, p.id)

        if (!NotificationPrefsRepository.prefs.masterEnabled) return
        if (p.id.isBlank() || p.type.equals("inactive", ignoreCase = true)) return

        val at = triggerAtMillis(p) ?: return
        val now = System.currentTimeMillis()
        val prefs = NotificationPrefsRepository.prefs

        if (at > now) {
            schedulePersonAlarm(app, p, at, isExact = true, kind = 0)
        } else if (markPastAsHandled && !wasFired(app, p.id, at)) {
            markFired(app, p.id, at)
        }
        if (prefs.visitBeforeActive) {
            val before = at - prefs.visitBeforeLeadMinutes * 60_000L
            if (before > now) schedulePersonAlarm(app, p, before, isExact = false, kind = 1)
        }
        if (prefs.visitAfterActive) {
            val after = at + prefs.visitAfterDelayMinutes * 60_000L
            if (after > now) {
                schedulePersonAlarm(app, p, after, isExact = false, kind = 2, typeOverride = "after_visit")
            }
        }
        val overdueDelay = (prefs.visitAfterDelayMinutes * 2).coerceAtLeast(60)
        val overdueAt = at + overdueDelay * 60_000L
        if (overdueAt > now) {
            schedulePersonAlarm(app, p, overdueAt, isExact = false, kind = 3, typeOverride = "overdue_visit")
        }
    }

    /** The person is gone: cancel their alarms and take down any of their notifications still on screen. */
    fun cancel(ctx: Context, personId: String) {
        val app = ctx.applicationContext
        cancelAllForPerson(app, personId)
        NotificationHelper.cancelPerson(app, personId)
    }

    fun clearState(ctx: Context, personId: String) {
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .edit().remove("fired:$personId").apply()
    }

    fun markFired(ctx: Context, personId: String, at: Long) {
        // commit(), not apply(): a receiver can be killed right after it finishes.
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .edit().putLong("fired:$personId", at).commit()
    }

    fun wasFired(ctx: Context, personId: String, at: Long): Boolean =
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .getLong("fired:$personId", -1L) == at

    fun triggerAtMillis(p: PersonRecord): Long? {
        if (p.id.isBlank() || p.nextVisitDate.isBlank()) return null
        if (p.type.equals("inactive", ignoreCase = true)) return null
        return try {
            val day = dateFormat().parse(p.nextVisitDate.trim()) ?: return null
            val timeText = p.nextVisitTime.orEmpty()
                .replace(' ', ' ').replace(' ', ' ').trim()
                .ifEmpty { DEFAULT_TIME }
            val time = CrashReporter.attempt("ReminderScheduler") { timeFormat().parse(timeText) }.getOrNull()
                ?: timeFormat().parse(DEFAULT_TIME)!!
            val t = Calendar.getInstance().apply { this.time = time }
            Calendar.getInstance().apply {
                this.time = day
                set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, t.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (e: Exception) {
            CrashReporter.record("ReminderScheduler", e)
            null
        }
    }

    fun requestCode(personId: String, kind: Int): Int = ReminderLogic.requestCode(personId, kind)

    // ── recurring system alarms ─────────────────────────────────────────────────────────────

    private fun scheduleDaily(ctx: Context, minLeadMs: Long = 0L) {
        val prefs = NotificationPrefsRepository.prefs
        if (!prefs.dailySummaryActive) { cancelRequest(ctx, RC_DAILY, TYPE_DAILY); return }
        val at = ReminderLogic.nextDaily(System.currentTimeMillis(), prefs.dailySummaryMinutes, minLeadMs)
        setAlarm(ctx, at, systemPi(ctx, TYPE_DAILY, RC_DAILY))
    }

    private fun scheduleDailyText(ctx: Context, minLeadMs: Long = 0L) {
        val prefs = NotificationPrefsRepository.prefs
        if (!prefs.dailyTextActive) { cancelRequest(ctx, RC_DAILY_TEXT, TYPE_DAILY_TEXT); return }
        val at = ReminderLogic.nextDaily(System.currentTimeMillis(), prefs.dailyTextMinutes, minLeadMs)
        setAlarm(ctx, at, systemPi(ctx, TYPE_DAILY_TEXT, RC_DAILY_TEXT))
    }

    private fun scheduleWeekly(ctx: Context, minLeadMs: Long = 0L) {
        val prefs = NotificationPrefsRepository.prefs
        if (!prefs.weeklySummaryActive) { cancelRequest(ctx, RC_WEEKLY, TYPE_WEEKLY); return }
        val at = ReminderLogic.nextWeekly(
            System.currentTimeMillis(), prefs.weeklyDay, prefs.weeklyMinutes, minLeadMs,
        )
        setAlarm(ctx, at, systemPi(ctx, TYPE_WEEKLY, RC_WEEKLY))
    }

    /** Watchdog: inexact on purpose (no status-bar alarm icon); it only needs to run "every few hours". */
    private fun scheduleHeartbeat(ctx: Context) {
        val at = System.currentTimeMillis() + ReminderLogic.HEARTBEAT_INTERVAL_MS
        setInexactAlarm(ctx, at, systemPi(ctx, TYPE_HEARTBEAT, RC_HEARTBEAT))
    }

    private fun cancelSystemAlarms(ctx: Context) {
        SYSTEM_ALARMS.forEach { (type, code) -> cancelRequest(ctx, code, type) }
    }

    // ── catch-up for anything that should already have been delivered ───────────────────────

    private fun catchUp(ctx: Context) {
        val prefs = NotificationPrefsRepository.prefs
        val state = ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val today = dateFormat().format(Date(now))

        // Daily summary: only if its time passed recently enough to still be useful, and not yet sent today.
        if (prefs.dailySummaryActive && state.getString("last_daily", "") != today) {
            val scheduled = ReminderLogic.todayAt(now, prefs.dailySummaryMinutes)
            if (ReminderLogic.isDailyStillDeliverable(now, scheduled)) {
                ReminderReceiver.handleType(ctx, TYPE_DAILY)
            }
        }

        // Daily text: deliver if its time already passed today and it has not been sent yet (all-day window).
        if (prefs.dailyTextActive && state.getString("last_daily_text", "") != today) {
            val scheduled = ReminderLogic.todayAt(now, prefs.dailyTextMinutes)
            if (now >= scheduled) {
                ReminderReceiver.handleType(ctx, TYPE_DAILY_TEXT)
            }
        }

        // Missed exact visit within last 24h
        PeopleRepository.load(ctx).forEach { p ->
            val at = triggerAtMillis(p) ?: return@forEach
            if (at <= now && now - at <= CATCH_UP_WINDOW_MS && !wasFired(ctx, p.id, at)) {
                NotificationHelper.showVisitReminder(ctx, p, true, requestCode(p.id, 0), at)
                markFired(ctx, p.id, at)
            }
        }
    }

    // ── per-person alarms ───────────────────────────────────────────────────────────────────

    /**
     * The identity of a person's alarm. Scheduling AND cancelling must build the intent the same way
     * (action + data), otherwise the system cannot match the PendingIntent and the cancel silently does nothing.
     */
    private fun personIntent(ctx: Context, personId: String, kind: Int): Intent =
        Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("reminder://person/${Uri.encode(personId)}/$kind")
        }

    private fun systemIntent(ctx: Context, type: String): Intent =
        Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("reminder://system/$type")
        }

    private fun schedulePersonAlarm(
        ctx: Context, p: PersonRecord, at: Long, isExact: Boolean, kind: Int, typeOverride: String? = null,
    ) {
        val intent = personIntent(ctx, p.id, kind).apply {
            putExtra(EXTRA_PERSON_ID, p.id)
            putExtra(EXTRA_TYPE, typeOverride ?: "reminder")
            putExtra(EXTRA_IS_EXACT, isExact)
            putExtra(EXTRA_VISIT_TIME, triggerAtMillis(p) ?: at)
            putExtra(EXTRA_SCHEDULED_DATE, p.nextVisitDate)
        }
        val pi = PendingIntent.getBroadcast(
            ctx, requestCode(p.id, kind), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(ctx, at, pi)
    }

    private fun systemPi(ctx: Context, type: String, code: Int): PendingIntent {
        val intent = systemIntent(ctx, type).apply { putExtra(EXTRA_TYPE, type) }
        return PendingIntent.getBroadcast(
            ctx, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelAllForPerson(ctx: Context, personId: String) {
        for (kind in 0..3) cancelPi(ctx, requestCode(personId, kind), personIntent(ctx, personId, kind))
    }

    private fun cancelRequest(ctx: Context, requestCode: Int, systemType: String) =
        cancelPi(ctx, requestCode, systemIntent(ctx, systemType))

    private fun cancelPi(ctx: Context, requestCode: Int, intent: Intent) {
        val pi = PendingIntent.getBroadcast(
            ctx, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        ctx.getSystemService(AlarmManager::class.java)?.cancel(pi)
        pi.cancel()
    }

    // ── the actual AlarmManager calls ───────────────────────────────────────────────────────

    /**
     * Strongest first: setAlarmClock (exempt from Doze and battery saver) → setExactAndAllowWhileIdle →
     * setAndAllowWhileIdle (inexact, used when "Alarms & reminders" is not allowed). Each step only runs if the
     * previous one threw, so a reminder is always registered with the best option this device allows.
     */
    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        if (AppNotifications.canScheduleExactAlarms(ctx)) {
            try {
                val show = PendingIntent.getActivity(
                    ctx, 0,
                    Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
                return
            } catch (e: Exception) {
                CrashReporter.record("ReminderScheduler", e, "setAlarmClock failed")
            }
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                return
            } catch (e: Exception) {
                CrashReporter.record("ReminderScheduler", e, "setExactAndAllowWhileIdle failed")
            }
        }
        setInexactAlarm(ctx, at, pi)
    }

    private fun setInexactAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: Exception) {
            CrashReporter.record("ReminderScheduler", e, "setAndAllowWhileIdle failed")
        }
    }
}
