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

/** Fired by AlarmManager. Handles visit, daily, evening, weekly, after-visit, overdue and the watchdog. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE) ?: "reminder"

        // The work runs off the main thread (it reads the people list and may build a few notifications),
        // so a slow device can never turn an alarm into an ANR. goAsync() keeps the process alive meanwhile.
        val pending = goAsync()
        val wake = try {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MinistryAssistant:AlarmWake")
                .apply { acquire(30_000L) }
        } catch (e: Throwable) {
            CrashReporter.record("ReminderReceiver", e, "Could not take wake lock")
            null
        }

        val work = Runnable {
            try {
                // 1) Re-arm the next occurrence FIRST, so nothing below can cost us tomorrow's alarm.
                CrashReporter.attempt("ReminderReceiver") { ReminderScheduler.rearmSystem(app, type) }
                // 2) Then deliver.
                if (type == ReminderScheduler.TYPE_HEARTBEAT) {
                    ReminderScheduler.rescheduleAll(app, catchUpMissed = true)
                } else {
                    handleType(app, type, intent)
                }
            } catch (e: Throwable) {
                CrashReporter.record("ReminderReceiver", e, "Failed handling $type")
            } finally {
                if (wake?.isHeld == true) wake.release()
                pending.finish()
            }
        }
        try {
            Thread(work, "MinistryAssistant-alarm").start()
        } catch (e: Throwable) {
            // Could not even start a thread: run inline rather than lose the reminder.
            work.run()
        }
    }

    companion object {
        fun handleType(context: Context, type: String, intent: Intent? = null) {
            NotificationPrefsRepository.init(context)
            val prefs = NotificationPrefsRepository.prefs
            if (!prefs.masterEnabled) return

            when (type) {
                ReminderScheduler.TYPE_DAILY -> if (prefs.dailySummaryActive) handleDaily(context)
                ReminderScheduler.TYPE_DAILY_TEXT -> if (prefs.dailyTextActive) handleDailyText(context)
                ReminderScheduler.TYPE_WEEKLY -> if (prefs.weeklySummaryActive) handleWeekly(context)
                "after_visit" -> handleAfterVisit(context, intent)
                "overdue_visit" -> handleOverdueVisit(context, intent)
                else -> handleReminder(context, intent) // "reminder"
            }
        }

        private fun handleReminder(context: Context, intent: Intent?) {
            if (intent == null) return
            val isExact = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_EXACT, true)
            if (!isExact && !NotificationPrefsRepository.prefs.visitBeforeActive) return

            val id = intent.getStringExtra(ReminderScheduler.EXTRA_PERSON_ID) ?: return
            val person = PeopleRepository.load(context).firstOrNull { it.id == id } ?: return
            if (person.type.equals("inactive", ignoreCase = true)) return

            val visitTime = intent.getLongExtra(ReminderScheduler.EXTRA_VISIT_TIME, 0L)
            // A leftover alarm for a visit that has since been moved or removed must stay silent.
            if (ReminderLogic.isScheduledTimeOutdated(visitTime, ReminderScheduler.triggerAtMillis(person))) return

            val now = System.currentTimeMillis()
            if (!isExact && ReminderLogic.isBeforeStale(now, visitTime)) return
            if (isExact && ReminderLogic.isExactStale(now, visitTime)) return
            // Already delivered (e.g. by catch-up after a reboot): do not buzz twice.
            if (isExact && visitTime > 0 && ReminderScheduler.wasFired(context, id, visitTime)) return

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

            val people = PeopleRepository.load(context)
            var returns = 0
            var studies = 0
            for (p in people) {
                if (p.type.equals("inactive", ignoreCase = true)) continue
                if (p.nextVisitDate == today) {
                    if (p.type.equals("study", ignoreCase = true)) studies++ else returns++
                }
            }
            state.edit().putString("last_daily", today).commit()
            NotificationHelper.showDailySummary(context, returns, studies)
        }

        private fun handleDailyText(context: Context) {
            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            val today = dateFormat().format(Date())
            if (state.getString("last_daily_text", "") == today) return
            state.edit().putString("last_daily_text", today).commit()
            NotificationHelper.showDailyText(context)
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

            state.edit().putString("last_weekly", weekKey).commit()
            NotificationHelper.showWeeklySummary(context, rv, study, hours)
        }

        private fun handleAfterVisit(context: Context, intent: Intent?) {
            if (!NotificationPrefsRepository.prefs.visitAfterActive) return
            val (id, person, visitKey) = resolveFollowUp(context, intent) ?: return

            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            if (state.getString("last_after:$id", "") == visitKey) return
            state.edit().putString("last_after:$id", visitKey).commit()

            NotificationHelper.showAfterVisit(
                context, person, ReminderScheduler.requestCode(id, 2),
            )
        }

        private fun handleOverdueVisit(context: Context, intent: Intent?) {
            val (id, person, visitKey) = resolveFollowUp(context, intent) ?: return

            val state = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
            if (state.getString("last_overdue:$id", "") == visitKey) return
            state.edit().putString("last_overdue:$id", visitKey).commit()

            NotificationHelper.showOverdue(
                context, person, ReminderScheduler.requestCode(id, 3),
            )
        }

        private data class FollowUp(val id: String, val person: com.harsha.myapplication.data.PersonRecord, val visitKey: String)

        /**
         * Shared checks for the after-visit and overdue alarms: the person must still exist, still be active, and
         * their visit must still be the one the alarm was made for (date or time not edited since).
         */
        private fun resolveFollowUp(context: Context, intent: Intent?): FollowUp? {
            if (intent == null) return null
            val id = intent.getStringExtra(ReminderScheduler.EXTRA_PERSON_ID) ?: return null
            val person = PeopleRepository.load(context).firstOrNull { it.id == id } ?: return null
            if (person.type.equals("inactive", ignoreCase = true)) return null

            val visitTime = intent.getLongExtra(ReminderScheduler.EXTRA_VISIT_TIME, 0L)
            if (ReminderLogic.isScheduledTimeOutdated(visitTime, ReminderScheduler.triggerAtMillis(person))) return null
            if (visitTime <= 0L) {
                // Alarm made by an older version: fall back to comparing the visit date text.
                val scheduledDate = intent.getStringExtra(ReminderScheduler.EXTRA_SCHEDULED_DATE) ?: return null
                if (person.nextVisitDate.trim() != scheduledDate.trim()) return null
                return FollowUp(id, person, scheduledDate)
            }
            return FollowUp(id, person, visitTime.toString())
        }
    }
}
