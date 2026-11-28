package com.harsha.myapplication.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.harsha.myapplication.MainActivity
import com.harsha.myapplication.data.GenZCategory
import com.harsha.myapplication.data.GenZPrefsRepository
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.data.NotificationPrefsRepository
import com.harsha.myapplication.data.PersonRecord

/**
 * Posts all notification types. Gen-Z tone is applied only when the master switch
 * is on AND the matching per-type flag is enabled in Gen-Z Mode settings.
 * Male / Female pools follow [GenZPrefsRepository.prefs.gender].
 */
object NotificationHelper {

    @SuppressLint("MissingPermission")
    fun showDailySummary(ctx: Context, returnsCount: Int, studiesCount: Int) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        HoursRepository.init(ctx)
        GenZPrefsRepository.init(ctx)

        val (title, body) = if (returnsCount > 0 || studiesCount > 0) {
            val items = mutableListOf<String>()
            if (returnsCount > 0) items += "$returnsCount return visit${if (returnsCount > 1) "s" else ""}"
            if (studiesCount > 0) items += "$studiesCount stud${if (studiesCount > 1) "ies" else "y"}"
            "Good Morning" to "Today we have ${items.joinToString(" & ")}"
        } else if (GenZPrefsRepository.usesGenZ(GenZCategory.DailySummary)) {
            GenZMessages.morningEmpty(ctx)
        } else {
            "Good Morning" to "Have a meaningful day in the ministry."
        }

        post(
            ctx,
            AppNotifications.DAILY_CHANNEL_ID,
            AppNotifications.DAILY_NOTIFICATION_ID,
            title,
            body,
            route = "home",
        )
    }

    @SuppressLint("MissingPermission")
    fun showDailyText(ctx: Context) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        GenZPrefsRepository.init(ctx)

        val (title, body) = if (GenZPrefsRepository.usesGenZ(GenZCategory.DailyText)) {
            val morning = GenZMessages.morningEmpty(ctx)
            "Daily Text 📖" to (morning.second.ifBlank { "Time for today's text — open the app and read it." })
        } else {
            "Daily text" to "A reminder to read today's text."
        }

        post(
            ctx,
            AppNotifications.DAILY_CHANNEL_ID,
            AppNotifications.DAILY_NOTIFICATION_ID + 10,
            title,
            body,
            route = "daily_text",
        )
    }

    @SuppressLint("MissingPermission")
    fun showVisitReminder(
        ctx: Context,
        person: PersonRecord,
        isExact: Boolean,
        notificationId: Int,
        visitTime: Long,
    ) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        GenZPrefsRepository.init(ctx)

        val name = person.name.ifBlank { "your contact" }
        val isStudy = person.type.equals("study", ignoreCase = true)
        val (title, body) = if (GenZPrefsRepository.usesGenZ(GenZCategory.VisitReminder)) {
            GenZMessages.visitReminder(ctx, name, isStudy, isExact)
        } else {
            val typeText = if (isStudy) "study" else "return visit"
            if (isExact) {
                (if (isStudy) "Study" else "Return Visit") to "Your $typeText with $name is now."
            } else {
                "Upcoming ${if (isStudy) "Study" else "Return Visit"}" to
                    "Your $typeText with $name is coming up."
            }
        }

        post(
            ctx,
            AppNotifications.REMINDER_CHANNEL_ID,
            notificationId,
            title,
            body,
            route = "profile",
            recordId = person.id,
            maxPriority = true,
        )
    }

    @SuppressLint("MissingPermission")
    fun showAfterVisit(ctx: Context, person: PersonRecord, notificationId: Int) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        NotificationPrefsRepository.init(ctx)
        GenZPrefsRepository.init(ctx)

        val name = person.name.ifBlank { "your contact" }
        val isStudy = person.type.equals("study", ignoreCase = true)
        val delay = NotificationPrefsRepository.prefs.visitAfterDelayMinutes
        val (title, body) = if (GenZPrefsRepository.usesGenZ(GenZCategory.AfterVisit)) {
            GenZMessages.afterVisit(ctx, name, isStudy, delay)
        } else {
            val t = if (isStudy) "Update Study Details" else "Update Return Visit Details"
            t to "Add the details from your ${if (isStudy) "study" else "return visit"}."
        }

        post(
            ctx,
            AppNotifications.UPDATE_CHANNEL_ID,
            notificationId,
            title,
            body,
            route = "log_visit",
            recordId = person.id,
        )
    }

    @SuppressLint("MissingPermission")
    fun showOverdue(
        ctx: Context,
        person: PersonRecord? = null,
        notificationId: Int = AppNotifications.OVERDUE_NOTIFICATION_ID,
    ) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        NotificationPrefsRepository.init(ctx)
        GenZPrefsRepository.init(ctx)

        val name = person?.name.orEmpty()
        val isStudy = person?.type?.equals("study", ignoreCase = true) == true
        val overdueMins = NotificationPrefsRepository.prefs.visitAfterDelayMinutes.coerceAtLeast(60)
        val (title, body) = if (GenZPrefsRepository.usesGenZ(GenZCategory.Overdue)) {
            GenZMessages.overdue(ctx, name, isStudy, overdueMins)
        } else {
            val typeText = if (isStudy) "study" else "return visit"
            "Follow-Up Pending" to if (name.isNotBlank()) {
                "Follow up with $name for your $typeText."
            } else {
                "You have a return visit that still needs an update."
            }
        }

        post(
            ctx,
            AppNotifications.OVERDUE_CHANNEL_ID,
            notificationId,
            title,
            body,
            route = "log_visit",
            recordId = person?.id.orEmpty(),
        )
    }

    @SuppressLint("MissingPermission")
    fun showEveningUpdate(ctx: Context, returnsCount: Int, studiesCount: Int) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        if (returnsCount <= 0 && studiesCount <= 0) return

        val typeText = when {
            returnsCount > 0 && studiesCount > 0 -> "return visit and study"
            studiesCount > 0 -> "study"
            else -> "return visit"
        }
        post(
            ctx,
            AppNotifications.UPDATE_CHANNEL_ID,
            AppNotifications.UPDATE_NOTIFICATION_ID,
            "Update Your Visit",
            "Have you completed today's $typeText? Add the latest details.",
            route = "home",
        )
    }

    @SuppressLint("MissingPermission")
    fun showWeeklySummary(ctx: Context, rvCount: Int, studyCount: Int, hours: Double) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        GenZPrefsRepository.init(ctx)

        val (title, body) = if (GenZPrefsRepository.usesGenZ(GenZCategory.WeeklySummary)) {
            GenZMessages.weeklySummary(ctx, rvCount, studyCount, hours)
        } else {
            val items = mutableListOf<String>()
            if (rvCount > 0) items += "$rvCount return visit${if (rvCount > 1) "s" else ""}"
            if (studyCount > 0) items += "$studyCount stud${if (studyCount > 1) "ies" else "y"}"
            val hoursStr = if (hours == hours.toInt().toDouble()) hours.toInt().toString() else "%.1f".format(hours)
            val activity = if (items.isNotEmpty()) items.joinToString(", ") + " and " else ""
            "Your Weekly Activity" to "$activity$hoursStr hours were recorded this week."
        }

        post(
            ctx,
            AppNotifications.WEEKLY_CHANNEL_ID,
            AppNotifications.WEEKLY_NOTIFICATION_ID,
            title,
            body,
            route = "reports",
        )
    }

    @SuppressLint("MissingPermission")
    fun showTest(ctx: Context) {
        if (!AppNotifications.areEnabled(ctx)) return
        AppNotifications.ensureChannel(ctx)
        post(
            ctx,
            AppNotifications.DAILY_CHANNEL_ID,
            9999,
            "Test Notification 🔔",
            "Notifications are set up and working on your device!",
            route = "home",
        )
    }

    fun cancelPerson(ctx: Context, recordId: String) {
        if (recordId.isEmpty()) return
        val nm = NotificationManagerCompat.from(ctx)
        val hash = recordId.hashCode()
        listOf(hash, hash + 1000, hash + 2000, hash + 5000).forEach {
            runCatching { nm.cancel(it and 0x7FFFFFFF) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun post(
        ctx: Context,
        channel: String,
        id: Int,
        title: String,
        body: String,
        route: String,
        recordId: String = "",
        maxPriority: Boolean = false,
    ) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            action = "NOTIFICATION_CLICK"
            putExtra("route", route)
            if (recordId.isNotEmpty()) putExtra("recordId", recordId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (maxPriority) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (maxPriority) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
