package com.harsha.myapplication.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.harsha.myapplication.MainActivity
import com.harsha.myapplication.data.PersonRecord
import com.harsha.myapplication.data.timeFormat
import java.util.Date

/** Builds and shows the visit notification. */
object ReminderNotifier {

    @SuppressLint("MissingPermission") // guarded by AppNotifications.areEnabled()
    fun show(ctx: Context, person: PersonRecord, triggerAt: Long) {
        // Always record that we handled this trigger, even if notifications are off.
        ReminderScheduler.markFired(ctx, person.id, triggerAt)
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)

        val name = person.name.ifBlank { "your contact" }
        val title = if (person.type == "study") "Bible study with $name" else "Return visit: $name"
        val whenText = timeFormat().format(Date(triggerAt))
        val details = buildList {
            add("Scheduled for $whenText")
            if (person.address.isNotBlank()) add(person.address)
            person.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
        }

        val open = PendingIntent.getActivity(
            ctx,
            person.id.hashCode(),
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val n = NotificationCompat.Builder(ctx, AppNotifications.CHANNEL_ID)
            // Replace with your own monochrome notification icon (R.drawable.ic_notification).
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(details.take(2).joinToString(" · "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(details.joinToString("\n")))
            .setWhen(triggerAt)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(ctx).notify(person.id.hashCode(), n)
    }
}
