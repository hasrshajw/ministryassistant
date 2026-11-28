package com.harsha.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.data.NotificationPrefsRepository
import com.harsha.myapplication.data.PeopleRepository
import com.harsha.myapplication.data.dateFormat
import com.harsha.myapplication.telemetry.CrashReporter
import java.util.Calendar
import java.util.Date

/** Fired by AlarmManager. Handles visit, daily, evening, weekly, after-visit, overdue. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE) ?: "reminder"
        val pending = goAsync()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MinistryAssistant:AlarmWake")
        wake.acquire(15_000L)
        try {
            // Re-arm recurring system alarms immediately
            when (type) {
                "daily" -> ReminderScheduler.rescheduleAll(context)
                "evening" -> ReminderScheduler.rescheduleAll(context)
                "weekly" -> ReminderScheduler.rescheduleAll(context)
            }
            handleType(context, type, intent)
        } catch (e: Throwable) {
            CrashReporter.record("ReminderReceiver", e, "Failed handling $type")
        } finally {
            if (wake.isHeld) wake.release()
            pending.finish()
        }
    }

    companion object {
        fun handleType(context: Context, type: String, intent: Intent? = null) {
            NotificationPrefsRepository.init(context)
            val prefs = NotificationPrefsRepository.prefs
            if (!prefs.masterEnabled) return

            when (type) {
                "daily" -> if (prefs.dailySummaryActive) handleDaily(context)
                "evening" -> handleEvening(context)
                "weekly" -> if (prefs.weeklySummaryActive) handleWeekly(context)
                "after_visit" -> handleAfterVisit(context, intent)
                "overdue_visit" -> handleOverdueVisit(context, intent)
                else -> handleReminder(context, intent) // "reminder"
            }
        }

        private fun handleReminder(context: Context, intent: Intent?) {
            if (!NotificationPrefsRepository.prefs.visitBeforeActive &&
                intent?.getBooleanExtra(ReminderScheduler.EXTRA_IS_EXACT, true) != true
            ) return

            val id = intent?.getStringExtra(ReminderScheduler.EXTRA_PERSON_ID) ?: return
            val person = PeopleRepository.load(context).firstOrNull { it.id == id } ?: return
            if (person.type.equals("inactive", ignoreCase = true)) return

            val isExact = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_EXACT, true)
            val visitTime = intent.getLongExtra(ReminderScheduler.EXTRA_VISIT_TIME, 0L)
            val now = System.currentTimeMillis()

            // Stale guards (mirror Flutter)
            if (visitTime > 0) {
                if (!isExact && now >= visitTime - 5 * 60_000L) return
                if (isExact && now >= visitTime + 15 * 60_000L) return
            }

            val kind = if (isExact) 0 else 1
            val notifId = ReminderScheduler.requestCode(id, kind)
            NotificationHelper.showVisitReminder(context, person, isExact, notifId, visitTime)
            if (isExact && visitTime > 0) {
                ReminderScheduler.markFired(context, id, visitTime)
            }
        }

        private fun handleDaily(context: Context) {
            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            val today = dateFormat().format(Date())
            if (state.getString("last_daily", "") == today) return
            if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 11) {
                state.edit().putString("last_daily", today).apply()
                return
            }

            val people = PeopleRepository.load(context)
            var returns = 0
            var studies = 0
            for (p in people) {
                if (p.type.equals("inactive", ignoreCase = true)) continue
                if (p.nextVisitDate == today) {
                    if (p.type.equals("study", ignoreCase = true)) studies++ else returns++
                }
            }
            state.edit().putString("last_daily", today).apply()
            NotificationHelper.showDailySummary(context, returns, studies)
        }

        private fun handleEvening(context: Context) {
            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            val today = dateFormat().format(Date())
            if (state.getString("last_evening", "") == today) return

            val people = PeopleRepository.load(context)
            var returns = 0
            var studies = 0
            for (p in people) {
                if (p.type.equals("inactive", ignoreCase = true)) continue
                if (p.nextVisitDate == today) {
                    if (p.type.equals("study", ignoreCase = true)) studies++ else returns++
                }
            }
            state.edit().putString("last_evening", today).apply()
            if (returns > 0 || studies > 0) {
                NotificationHelper.showEveningUpdate(context, returns, studies)
            }
        }

        private fun handleWeekly(context: Context) {
            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            val now = Calendar.getInstance()
            val weekKey = "${now.get(Calendar.YEAR)}_${now.get(Calendar.WEEK_OF_YEAR)}"
            if (state.getString("last_weekly", "") == weekKey) return

            HoursRepository.init(context)
            val start = (now.clone() as Calendar).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis > now.timeInMillis) add(Calendar.WEEK_OF_YEAR, -1)
            }
            val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }

            var rv = 0
            var study = 0
            for (p in PeopleRepository.load(context)) {
                for (log in p.visitLogs) {
                    if (log.timestamp in start.timeInMillis until end.timeInMillis) {
                        if (log.logType.contains("study", ignoreCase = true)) study++ else rv++
                    }
                }
            }
            val hours = HoursRepository.entries
                .filter { it.dateMillis in start.timeInMillis until end.timeInMillis }
                .sumOf { it.minutes } / 60.0

            state.edit().putString("last_weekly", weekKey).apply()
            NotificationHelper.showWeeklySummary(context, rv, study, hours)
        }

        private fun handleAfterVisit(context: Context, intent: Intent?) {
            if (!NotificationPrefsRepository.prefs.visitAfterActive) return
            val id = intent?.getStringExtra(ReminderScheduler.EXTRA_PERSON_ID) ?: return
            val scheduledDate = intent.getStringExtra(ReminderScheduler.EXTRA_SCHEDULED_DATE) ?: return
            val person = PeopleRepository.load(context).firstOrNull { it.id == id } ?: return
            if (person.nextVisitDate.trim() != scheduledDate.trim()) return // date changed

            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            if (state.getString("last_after:$id", "") == scheduledDate) return
            state.edit().putString("last_after:$id", scheduledDate).apply()

            NotificationHelper.showAfterVisit(
                context, person, ReminderScheduler.requestCode(id, 2),
            )
        }

        private fun handleOverdueVisit(context: Context, intent: Intent?) {
            val id = intent?.getStringExtra(ReminderScheduler.EXTRA_PERSON_ID) ?: return
            val scheduledDate = intent.getStringExtra(ReminderScheduler.EXTRA_SCHEDULED_DATE) ?: return
            val person = PeopleRepository.load(context).firstOrNull { it.id == id } ?: return
            if (person.nextVisitDate.trim() != scheduledDate.trim()) return

            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            if (state.getString("last_overdue:$id", "") == scheduledDate) return
            state.edit().putString("last_overdue:$id", scheduledDate).apply()

            NotificationHelper.showOverdue(
                context, person, ReminderScheduler.requestCode(id, 3),
            )
        }
    }
}
