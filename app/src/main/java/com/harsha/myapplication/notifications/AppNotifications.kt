package com.harsha.myapplication.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object AppNotifications {
    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Return visits, daily text and goal reminders" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** True only when the permission is granted AND notifications are not switched off in system settings. */
    fun areEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}