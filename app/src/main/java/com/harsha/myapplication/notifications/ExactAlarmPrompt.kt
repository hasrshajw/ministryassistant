package com.harsha.myapplication.notifications

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
import androidx.lifecycle.LifecycleOwner

/**
 * Android 12+ blocks exact alarms until the user allows "Alarms & reminders".
 * Shows a one-time explanation with a shortcut to that setting, and upgrades the
 * already-saved reminders to exact alarms as soon as the user comes back.
 */
@Composable
fun ExactAlarmPrompt() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(AppNotifications.canScheduleExactAlarms(context)) }
    var dismissed by rememberSaveable { mutableStateOf(false) }

    val owner = context as? LifecycleOwner
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = AppNotifications.canScheduleExactAlarms(context)
                if (now && !allowed) Thread { ReminderScheduler.rescheduleAll(context) }.start()
                allowed = now
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    if (!allowed && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("Allow exact reminders") },
            text = {
                Text(
                    "To notify you at the exact date and time of each visit — even if you haven’t " +
                        "opened the app for weeks — turn on “Alarms & reminders” for this app."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dismissed = true
                    AppNotifications.openExactAlarmSettings(context)
                }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = { dismissed = true }) { Text("Not now") } },
        )
    }
}
