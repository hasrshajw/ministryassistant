package com.harsha.myapplication.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.harsha.myapplication.telemetry.CrashReporter

/**
 * Live status of every permission/setting reminders depend on, plus shortcuts to the screens where the user
 * can change them. Used by the Permissions screen in Settings and by the Settings row's summary.
 */
object AppPermissions {
    /** Required: without it Android never shows a reminder. */
    fun notificationsAllowed(ctx: Context): Boolean = AppNotifications.areEnabled(ctx)

    /** Required (Android 12+): "Alarms & reminders". Always true on older versions. */
    fun exactAlarmsAllowed(ctx: Context): Boolean = AppNotifications.canScheduleExactAlarms(ctx)

    /** Recommended: the app is exempt from battery optimisation, so the phone never holds its reminders back. */
    fun batteryUnrestricted(ctx: Context): Boolean = try {
        ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) == true
    } catch (e: Exception) {
        CrashReporter.record("AppPermissions", e)
        false
    }

    /** How many REQUIRED permissions are still missing (0..2). */
    fun requiredMissing(ctx: Context): Int =
        listOf(notificationsAllowed(ctx), exactAlarmsAllowed(ctx)).count { !it }

    /** One-line summary for the Settings row. */
    fun summary(ctx: Context): String {
        val missing = requiredMissing(ctx)
        return when {
            missing > 0 -> "$missing required permission${if (missing > 1) "s" else ""} needed"
            !batteryUnrestricted(ctx) -> "Battery setting recommended"
            else -> "All set"
        }
    }

    /** Opens the system list where the user picks "Don't optimise / Unrestricted" for this app. */
    fun openBatterySettings(ctx: Context) {
        launch(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) { openAppDetails(ctx) }
    }

    /** This app's page in system settings (permissions, battery, auto-start on some phones). */
    fun openAppDetails(ctx: Context) {
        launch(
            ctx,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)),
        ) { }
    }

    private inline fun launch(ctx: Context, intent: Intent, fallback: () -> Unit) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            CrashReporter.record("AppPermissions", e, "Could not open ${intent.action}")
            fallback()
        }
    }
}
