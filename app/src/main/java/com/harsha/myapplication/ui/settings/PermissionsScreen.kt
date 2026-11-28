package com.harsha.myapplication.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.harsha.myapplication.notifications.AppNotifications
import com.harsha.myapplication.notifications.AppPermissions
import com.harsha.myapplication.notifications.ReminderScheduler
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.components.openAppNotificationSettings

private enum class Need(val label: String) { Required("Required"), Recommended("Recommended") }

/**
 * Everything reminders need from Android, in one place, each with its live status and a button that either asks
 * for it or opens the exact system screen where the user can turn it on. Statuses refresh when the user returns.
 */
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bumped every time the user comes back from a system screen, so every status below is re-read.
    var refresh by remember { mutableIntStateOf(0) }
    var notificationsAskedAndDenied by rememberSaveable { mutableStateOf(false) }

    val notifications = remember(refresh) { AppPermissions.notificationsAllowed(context) }
    val exactAlarms = remember(refresh) { AppPermissions.exactAlarmsAllowed(context) }
    val battery = remember(refresh) { AppPermissions.batteryUnrestricted(context) }

    var exactWasAllowed by remember { mutableStateOf(exactAlarms) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh++
                // Alarms made while "Alarms & reminders" was off are inexact: upgrade them as soon as it is on.
                val nowExact = AppPermissions.exactAlarmsAllowed(context)
                if (nowExact && !exactWasAllowed) Thread { ReminderScheduler.rescheduleAll(context) }.start()
                exactWasAllowed = nowExact
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refresh++
        if (!granted) notificationsAskedAndDenied = true else AppNotifications.ensureChannel(context)
    }

    val requiredMissing = listOf(notifications, exactAlarms).count { !it }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Permissions",
                navigationIcon = {
                    StudyBarIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                },
            )
        },
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            SummaryCard(requiredMissing = requiredMissing, batteryOk = battery)

            Spacer(Modifier.height(24.dp))
            SectionTitle("Allow these")
            Spacer(Modifier.height(8.dp))

            PermissionCard(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                need = Need.Required,
                description = "Shows your visit reminders, daily text and summaries.",
                granted = notifications,
                actionLabel = "Allow",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAskedAndDenied) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppNotificationSettings(context)
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            PermissionCard(
                icon = Icons.Outlined.Alarm,
                title = "Alarms & reminders",
                need = Need.Required,
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "Lets Android deliver each reminder at the exact minute, even if you haven’t opened the app for weeks."
                } else {
                    "Built in on your Android version. Nothing to turn on."
                },
                granted = exactAlarms,
                actionLabel = "Allow",
                onAction = { AppNotifications.openExactAlarmSettings(context) },
            )
            Spacer(Modifier.height(8.dp))
            PermissionCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "Unrestricted battery",
                need = Need.Recommended,
                description = "Stops the phone from holding reminders back to save power. Choose “Unrestricted” or " +
                    "“Don’t optimise” for this app. On Xiaomi, Oppo, Vivo, Realme, OnePlus and Huawei phones also " +
                    "switch on “Auto-start” in the app’s settings.",
                granted = battery,
                actionLabel = "Open settings",
                onAction = { AppPermissions.openBatterySettings(context) },
                secondaryLabel = "App settings",
                onSecondary = { AppPermissions.openAppDetails(context) },
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Granted automatically")
            Spacer(Modifier.height(4.dp))
            Text(
                "Android gives these to the app when you install it. There is nothing to turn on.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            AutoRow(Icons.Outlined.RestartAlt, "Run after restart", "Sets your reminders again when the phone restarts or the app updates.")
            Spacer(Modifier.height(2.dp))
            AutoRow(Icons.Outlined.Vibration, "Vibrate", "Lets reminders vibrate.")
            Spacer(Modifier.height(2.dp))
            AutoRow(Icons.Outlined.Alarm, "Keep device awake briefly", "Lets a reminder wake the phone for a moment so it can show.")
            Spacer(Modifier.height(2.dp))
            AutoRow(Icons.Outlined.Wifi, "Internet", "Only for Bible downloads, Google Drive backup and push messages.")
        }
    }
}

@Composable
private fun SummaryCard(requiredMissing: Int, batteryOk: Boolean) {
    val colors = MaterialTheme.colorScheme
    val allGood = requiredMissing == 0
    val container = if (allGood) colors.primaryContainer else colors.errorContainer
    val content = if (allGood) colors.onPrimaryContainer else colors.onErrorContainer
    val title = when {
        !allGood -> "$requiredMissing required permission${if (requiredMissing > 1) "s" else ""} needed"
        !batteryOk -> "Reminders are working"
        else -> "All set"
    }
    val body = when {
        !allGood -> "Turn these on below so reminders arrive on time."
        !batteryOk -> "For the most reliable delivery, also set battery to Unrestricted."
        else -> "Your reminders can reach you on time, even when the app is closed."
    }
    Surface(shape = RoundedCornerShape(28.dp), color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                if (allGood) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    need: Need,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon, tint = if (granted) colors.primary else colors.error)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        need.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                StatusPill(granted)
            }
            Spacer(Modifier.height(10.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (!granted) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAction) { Text(actionLabel) }
                    if (secondaryLabel != null && onSecondary != null) {
                        FilledTonalButton(onClick = onSecondary) { Text(secondaryLabel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(granted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (granted) colors.primaryContainer else colors.errorContainer,
        contentColor = if (granted) colors.onPrimaryContainer else colors.onErrorContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (granted) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(if (granted) "Allowed" else "Off", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AutoRow(icon: ImageVector, title: String, description: String) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "Granted",
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
