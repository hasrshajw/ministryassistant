package com.harsha.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.harsha.myapplication.telemetry.CrashReporter

/**
 * AlarmManager forgets every alarm on reboot, so they are registered again here.
 * Also runs after an app update, when the date, time or time zone changes, when "Alarms & reminders"
 * is granted, and after the "quick boot" some phones (HTC, Xiaomi, ...) use instead of a normal boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        val app = context.applicationContext
        val pending = goAsync()
        // Catch up on anything missed while the phone was off / the app was being replaced.
        val catchUp = action in CATCH_UP_ACTIONS
        val work = Runnable {
            try {
                ReminderScheduler.rescheduleAll(app, catchUpMissed = catchUp)
            } catch (e: Throwable) {
                CrashReporter.record("BootReceiver", e, "Failed to reschedule ($action)")
            } finally {
                pending.finish()
            }
        }
        try {
            Thread(work, "MinistryAssistant-boot").start()
        } catch (e: Throwable) {
            work.run()
        }
    }

    private companion object {
        const val EXACT_ALARM_STATE_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        const val QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON"
        const val QUICKBOOT_HTC = "com.htc.intent.action.QUICKBOOT_POWERON"

        val CATCH_UP_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            QUICKBOOT,
            QUICKBOOT_HTC,
        )
        val HANDLED_ACTIONS = CATCH_UP_ACTIONS + setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            EXACT_ALARM_STATE_CHANGED,
        )
    }
}
