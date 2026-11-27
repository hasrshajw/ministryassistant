package com.harsha.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager forgets every alarm on reboot, so they are registered again here.
 * Also runs after an app update and when the date, time or time zone changes.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                val pending = goAsync()
                Thread {
                    try {
                        ReminderScheduler.rescheduleAll(
                            context,
                            catchUpMissed = intent.action == Intent.ACTION_BOOT_COMPLETED,
                        )
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
        }
    }
}
