package com.harsha.myapplication.notifications

import com.harsha.myapplication.telemetry.CrashReporter
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object AppNotifications {
    /** Legacy single channel — kept so older scheduled notifications still resolve. */
    const val CHANNEL_ID = "reminders"

    const val DAILY_CHANNEL_ID = "daily_summary_v2"
    const val REMINDER_CHANNEL_ID = "visit_reminders_v2"
    const val UPDATE_CHANNEL_ID = "update_reminders_v2"
    const val OVERDUE_CHANNEL_ID = "overdue_reminders_v2"
    const val WEEKLY_CHANNEL_ID = "weekly_summary_v2"

    const val DAILY_NOTIFICATION_ID = 1001
    const val UPDATE_NOTIFICATION_ID = 1002
    const val OVERDUE_NOTIFICATION_ID = 1003
    const val WEEKLY_NOTIFICATION_ID = 1004

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        // Keep the original channel for backward compatibility.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Return visits, daily text and goal reminders"
                enableVibration(true)
            },
        )

        val audio = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        fun ch(id: String, name: String, desc: String) = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
            description = desc
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setShowBadge(true)
            setSound(sound, audio)
        }

        nm.createNotificationChannel(ch(DAILY_CHANNEL_ID, "Daily Summary", "Morning summary of today's visits"))
        nm.createNotificationChannel(ch(REMINDER_CHANNEL_ID, "Visit Reminders", "Reminders for upcoming visits"))
        nm.createNotificationChannel(ch(UPDATE_CHANNEL_ID, "Update Reminders", "Evening and after-visit update prompts"))
        nm.createNotificationChannel(ch(OVERDUE_CHANNEL_ID, "Overdue Reminders", "Reminders for visits that still need an update"))
        nm.createNotificationChannel(ch(WEEKLY_CHANNEL_ID, "Weekly Summary", "Weekly activity summary"))
    }

    fun areEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            CrashReporter.record("AppNotifications", e)
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
