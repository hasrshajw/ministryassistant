package com.harsha.myapplication.notifications

import android.content.Context
import com.harsha.myapplication.data.PersonRecord

/** Back-compat entry used by catch-up paths. Delegates to [NotificationHelper]. */
object ReminderNotifier {
    fun show(ctx: Context, person: PersonRecord, triggerAt: Long) {
        ReminderScheduler.markFired(ctx, person.id, triggerAt)
        NotificationHelper.showVisitReminder(
            ctx,
            person,
            isExact = true,
            notificationId = ReminderScheduler.requestCode(person.id, 0),
            visitTime = triggerAt,
        )
    }
}
