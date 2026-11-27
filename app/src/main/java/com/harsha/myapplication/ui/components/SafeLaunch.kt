package com.harsha.myapplication.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.DriveBackup
import com.harsha.myapplication.telemetry.CrashReporter

/**
 * Runs [block] - a file picker / photo picker / intent launch.
 *
 * Some phones (stripped-down Android Go builds, custom ROMs, work profiles) have no app that can
 * handle a picker, and the system throws ActivityNotFoundException. Without this the whole app
 * crashes; with it the problem is reported and the person sees a short message instead.
 */
fun launchSafely(context: Context, tag: String, failMessage: String, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        CrashReporter.record(tag, e, "Could not launch: $failMessage")
        try {
            Toast.makeText(context, failMessage, Toast.LENGTH_LONG).show()
        } catch (_: Throwable) {
        }
    }
}

/**
 * Opens this app's notification settings. The dedicated screen only exists on Android 8+; on older
 * phones (or any ROM without it) this falls back to the app's details page instead of crashing.
 */
fun openAppNotificationSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    } catch (first: Throwable) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        } catch (second: Throwable) {
            CrashReporter.record("openAppNotificationSettings", second, "No settings screen could be opened")
        }
    }
}

/**
 * Starts the Google sign-in for Drive backup. Phones without Google Play services (some custom and
 * low-end ROMs) cannot show it and the launch throws; the "connecting" state is then cleared with a
 * clear message instead of crashing or spinning forever.
 */
fun startDriveSignIn(context: Context, launcher: ActivityResultLauncher<Intent>) {
    BackupRepository.beginDriveConnect()
    try {
        launcher.launch(DriveBackup.signInIntent(context))
    } catch (e: Throwable) {
        CrashReporter.record("DriveSignIn", e, "Google sign-in unavailable")
        BackupRepository.onDriveConnectFailed(
            "Google sign-in isn't available on this device (Google Play services is missing or out of date). " +
                "You can still save a backup file with Export data."
        )
    }
}
