package com.harsha.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.harsha.myapplication.data.PeopleRepository

/** Fired by AlarmManager at the visit date/time. Works even if the app process was dead. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_PERSON_ID) ?: return
        val person = PeopleRepository.load(context).firstOrNull { it.id == id } ?: return
        if (person.type == "inactive") return
        // Use the record's current trigger time (the alarm was just fired for it).
        val at = ReminderScheduler.triggerAtMillis(person) ?: System.currentTimeMillis()
        ReminderNotifier.show(context, person, at)
    }
}
