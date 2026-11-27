package com.harsha.myapplication.notifications

import com.harsha.myapplication.telemetry.CrashReporter
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.harsha.myapplication.data.DAY_MS
import com.harsha.myapplication.data.PeopleRepository
import com.harsha.myapplication.data.PersonRecord
import com.harsha.myapplication.data.dateFormat
import com.harsha.myapplication.data.timeFormat
import java.util.Calendar

/**
 * Schedules one OS-level alarm per person at their next visit date + time.
 *
 * The alarm is stored by Android's AlarmManager, NOT by this app, so it fires even if the app
 * has not been opened for months. It is wiped by a reboot (re-registered by [BootReceiver])
 * and by "Force stop" (re-registered the next time the app is opened).
 */
object ReminderScheduler {
    const val ACTION_REMINDER = "com.harsha.myapplication.ACTION_VISIT_REMINDER"
    const val EXTRA_PERSON_ID = "person_id"

    private const val DEFAULT_TIME = "9:00 AM"            // used when a visit has a date but no time
    private const val CATCH_UP_WINDOW_MS = DAY_MS         // after a reboot, still notify for visits missed in the last 24h
    private const val STATE_PREFS = "reminder_state"

    // ── PendingIntent (unique per person via the data URI, so ids never collide) ──────────
    private fun intentFor(ctx: Context, personId: String): Intent =
        Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .setData(Uri.parse("reminder://person/${Uri.encode(personId)}"))
            .putExtra(EXTRA_PERSON_ID, personId)

    private fun pendingIntent(ctx: Context, personId: String, create: Boolean): PendingIntent? {
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(ctx, 0, intentFor(ctx, personId), flags)
    }

    // ── Trigger time ──────────────────────────────────────────────────────────────────────
    /** Epoch millis of the visit, or null when there is nothing to remind about. */
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

    // ── Public API ────────────────────────────────────────────────────────────────────────
    /** (Re)schedules the reminder for one person. Safe to call repeatedly. */
    fun schedule(ctx: Context, p: PersonRecord) {
        val app = ctx.applicationContext
        cancel(app, p.id)
        val at = triggerAtMillis(p) ?: return
        if (at <= System.currentTimeMillis()) return
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(app, p.id, create = true) ?: return
        try {
            if (AppNotifications.canScheduleExactAlarms(app)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                // Exact-alarm permission not granted: still fires while idle, but may be a few minutes late.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            CrashReporter.record("ReminderScheduler", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(ctx: Context, personId: String) {
        val app = ctx.applicationContext
        pendingIntent(app, personId, create = false)?.let {
            app.getSystemService(AlarmManager::class.java)?.cancel(it)
            it.cancel()
        }
    }

    /**
     * Re-registers every alarm. Call on app start, after reboot / app update / clock change.
     * With [catchUpMissed] (used after a reboot), a visit whose time passed while the phone was
     * off — within the last 24h and never notified — is shown right away.
     */
    fun rescheduleAll(ctx: Context, catchUpMissed: Boolean = false) {
        val app = ctx.applicationContext
        val now = System.currentTimeMillis()
        PeopleRepository.load(app).forEach { p ->
            schedule(app, p)
            if (catchUpMissed) {
                val at = triggerAtMillis(p)
                if (at != null && at <= now && now - at <= CATCH_UP_WINDOW_MS && !wasFired(app, p.id, at)) {
                    ReminderNotifier.show(app, p, at)
                }
            }
        }
    }

    // ── "Already notified" bookkeeping (prevents duplicates after reboot) ─────────────────
    fun markFired(ctx: Context, personId: String, at: Long) {
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .edit().putLong("fired:$personId", at).apply()
    }

    fun wasFired(ctx: Context, personId: String, at: Long): Boolean =
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .getLong("fired:$personId", -1L) == at

    fun clearState(ctx: Context, personId: String) {
        ctx.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            .edit().remove("fired:$personId").apply()
    }
}
