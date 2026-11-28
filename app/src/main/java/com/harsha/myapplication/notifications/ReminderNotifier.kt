package com.harsha.myapplication.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.harsha.myapplication.MainActivity
import com.harsha.myapplication.data.GenZPrefsRepository
import com.harsha.myapplication.data.PersonRecord
import com.harsha.myapplication.data.timeFormat
import java.util.Date

/** Builds and shows the visit notification. Supports Gen-Z mode copy when enabled. */
object ReminderNotifier {

    @SuppressLint("MissingPermission") // guarded by AppNotifications.areEnabled()
    fun show(ctx: Context, person: PersonRecord, triggerAt: Long) {
        // Always record that we handled this trigger, even if notifications are off.
        ReminderScheduler.markFired(ctx, person.id, triggerAt)
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)

        GenZPrefsRepository.init(ctx)
        val genZ = GenZPrefsRepository.prefs.enabled
        val name = person.name.ifBlank { "your contact" }
        val isStudy = person.type.equals("study", ignoreCase = true)

        val (title, body) = if (genZ) {
            // Exact-time style (this scheduler fires at the visit time).
            GenZMessages.visitReminder(
                context = ctx,
                contactName = name,
                isStudy = isStudy,
                isExact = true,
            )
        } else {
            val plainTitle = if (isStudy) "Bible study with $name" else "Return visit: $name"
            val whenText = timeFormat().format(Date(triggerAt))
            val details = buildList {
                add("Scheduled for $whenText")
                if (person.address.isNotBlank()) add(person.address)
                person.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            plainTitle to details.joinToString("\n")
        }

        val open = PendingIntent.getActivity(
            ctx,
            person.id.hashCode(),
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val n = NotificationCompat.Builder(ctx, AppNotifications.CHANNEL_ID)
            // Replace with your own monochrome notification icon (R.drawable.ic_notification).
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
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
