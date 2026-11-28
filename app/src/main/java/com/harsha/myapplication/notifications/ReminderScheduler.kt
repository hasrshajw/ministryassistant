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
 */
object ReminderScheduler {
    const val ACTION_REMINDER = "com.harsha.myapplication.ACTION_VISIT_REMINDER"
    const val EXTRA_PERSON_ID = "person_id"
    const val EXTRA_TYPE = "type"
    const val EXTRA_IS_EXACT = "is_exact"
    const val EXTRA_VISIT_TIME = "visit_time"
    const val EXTRA_SCHEDULED_DATE = "scheduled_date"

    private const val DEFAULT_TIME = "9:00 AM"
    private const val CATCH_UP_WINDOW_MS = DAY_MS
    private const val STATE_PREFS = "reminder_state"

    private const val RC_DAILY = 100
    private const val RC_DAILY_TEXT = 101
    private const val RC_WEEKLY = 102

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

        scheduleDaily(app)
        scheduleDailyText(app)
        scheduleWeekly(app)
        people.forEach { schedule(app, it) }
        if (catchUpMissed) catchUp(app)
    }

    fun schedule(ctx: Context, p: PersonRecord) {
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

    fun cancel(ctx: Context, personId: String) = cancelAllForPerson(ctx.applicationContext, personId)

    fun clearState(ctx: Context, personId: String) {
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .edit().remove("fired:$personId").apply()
    }

    fun markFired(ctx: Context, personId: String, at: Long) {
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .edit().putLong("fired:$personId", at).apply()
    }

    fun wasFired(ctx: Context, personId: String, at: Long): Boolean =
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .getLong("fired:$personId", -1L) == at

    fun triggerAtMillis(p: PersonRecord): Long? {
        if (p.id.isBlank() || p.nextVisitDate.isBlank()) return null
        if (p.type == "inactive") return null
        return try {
            val day = dateFormat().parse(p.nextVisitDate.trim()) ?: return null
            val timeText = p.nextVisitTime.orEmpty()
                .replace('\u202f', ' ').replace('\u00a0', ' ').trim()
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

    private fun scheduleDaily(ctx: Context) {
        val prefs = NotificationPrefsRepository.prefs
        if (!prefs.dailySummaryActive) { cancelRequest(ctx, RC_DAILY); return }
        setAlarm(ctx, nextTimeOfDay(prefs.dailySummaryMinutes).timeInMillis, systemPi(ctx, "daily", RC_DAILY))
    }

    private fun scheduleDailyText(ctx: Context) {
        val prefs = NotificationPrefsRepository.prefs
        if (!prefs.dailyTextActive) { cancelRequest(ctx, RC_DAILY_TEXT); return }
        setAlarm(ctx, nextTimeOfDay(prefs.dailyTextMinutes).timeInMillis, systemPi(ctx, "daily_text", RC_DAILY_TEXT))
    }

    private fun scheduleWeekly(ctx: Context) {
        val prefs = NotificationPrefsRepository.prefs
        if (!prefs.weeklySummaryActive) { cancelRequest(ctx, RC_WEEKLY); return }
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, prefs.weeklyDay)
            set(Calendar.HOUR_OF_DAY, prefs.weeklyMinutes / 60)
            set(Calendar.MINUTE, prefs.weeklyMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
        }
        setAlarm(ctx, cal.timeInMillis, systemPi(ctx, "weekly", RC_WEEKLY))
    }

    private fun cancelSystemAlarms(ctx: Context) {
        cancelRequest(ctx, RC_DAILY); cancelRequest(ctx, RC_DAILY_TEXT); cancelRequest(ctx, RC_WEEKLY)
    }

    private fun catchUp(ctx: Context) {
        val prefs = NotificationPrefsRepository.prefs
        val state = ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val today = dateFormat().format(Date(now))
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (prefs.dailySummaryActive && state.getString("last_daily", "") != today && hour < 11) {
            val scheduled = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prefs.dailySummaryMinutes / 60)
                set(Calendar.MINUTE, prefs.dailySummaryMinutes % 60)
                set(Calendar.SECOND, 0)
            }
            if (now >= scheduled.timeInMillis) {
                ReminderReceiver.handleType(ctx, "daily")
            }
        }

        PeopleRepository.load(ctx).forEach { p ->
            val at = triggerAtMillis(p) ?: return@forEach
            if (at <= now && now - at <= CATCH_UP_WINDOW_MS && !wasFired(ctx, p.id, at)) {
                NotificationHelper.showVisitReminder(ctx, p, true, requestCode(p.id, 0), at)
                markFired(ctx, p.id, at)
            }
        }
    }

    private fun schedulePersonAlarm(
        ctx: Context, p: PersonRecord, at: Long, isExact: Boolean, kind: Int, typeOverride: String? = null,
    ) {
        val type = typeOverride ?: "reminder"
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("reminder://person/${Uri.encode(p.id)}/$kind")
            putExtra(EXTRA_PERSON_ID, p.id)
            putExtra(EXTRA_TYPE, type)
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
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("reminder://system/$type")
            putExtra(EXTRA_TYPE, type)
        }
        return PendingIntent.getBroadcast(
            ctx, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (AppNotifications.canScheduleExactAlarms(ctx)) {
                val show = PendingIntent.getActivity(
                    ctx, 0,
                    Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            CrashReporter.record("ReminderScheduler", e)
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        } catch (e: Exception) {
            CrashReporter.record("ReminderScheduler", e)
        }
    }

    private fun cancelAllForPerson(ctx: Context, personId: String) {
        for (kind in 0..3) cancelRequest(ctx, requestCode(personId, kind))
    }

    private fun cancelRequest(ctx: Context, requestCode: Int) {
        val intent = Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_REMINDER)
        val pi = PendingIntent.getBroadcast(
            ctx, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        ctx.getSystemService(AlarmManager::class.java)?.cancel(pi)
        pi.cancel()
    }

    fun requestCode(personId: String, kind: Int): Int {
        val base = (Math.abs(personId.hashCode()) % 100_000) * 5 + 1000
        return base + kind
    }

    private fun nextTimeOfDay(minutesSinceMidnight: Int): Calendar {
        val hour = (minutesSinceMidnight / 60).coerceIn(0, 23)
        val minute = (minutesSinceMidnight % 60).coerceIn(0, 59)
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}
