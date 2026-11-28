package com.harsha.myapplication.notifications

import android.content.Context
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.harsha.myapplication.data.NotificationPrefsRepository

/**
 * Android 12+ blocks exact alarms until the user allows "Alarms & reminders".
 * Shows an explanation with a shortcut to that setting, and upgrades the already-saved reminders
 * to exact alarms as soon as the user comes back.
 *
 * Only shown while reminders are switched on, and at most once a week after the user says "Not now",
 * so it never nags on every launch.
 */
@Composable
fun ExactAlarmPrompt() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(AppNotifications.canScheduleExactAlarms(context)) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var snoozed by remember { mutableStateOf(isSnoozed(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = AppNotifications.canScheduleExactAlarms(context)
                if (now && !allowed) Thread { ReminderScheduler.rescheduleAll(context) }.start()
                allowed = now
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val remindersOn = NotificationPrefsRepository.prefs.masterEnabled
    if (remindersOn && !allowed && !dismissed && !snoozed) {
        val notNow = {
            dismissed = true
            snoozed = true
            snooze(context)
        }
        AlertDialog(
            onDismissRequest = notNow,
            title = { Text("Allow exact reminders") },
            text = {
                Text(
                    "To notify you at the exact date and time of each visit — even if you haven’t " +
                        "opened the app for weeks — turn on “Alarms & reminders” for this app. " +
                        "Without it, Android may delay reminders by several minutes."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dismissed = true
                    AppNotifications.openExactAlarmSettings(context)
                }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = notNow) { Text("Not now") } },
        )
    }
}

private const val STATE_PREFS = "reminder_state"
private const val KEY_SNOOZED_AT = "exact_prompt_snoozed_at"
private const val SNOOZE_MS = 7L * 24 * 60 * 60 * 1000

private fun isSnoozed(context: Context): Boolean {
    val at = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE).getLong(KEY_SNOOZED_AT, 0L)
    val now = System.currentTimeMillis()
    return at in 1..now && now - at < SNOOZE_MS
}

private fun snooze(context: Context) {
    context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        .edit().putLong(KEY_SNOOZED_AT, System.currentTimeMillis()).apply()
}
