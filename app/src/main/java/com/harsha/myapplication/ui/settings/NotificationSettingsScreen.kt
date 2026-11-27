@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)

package com.harsha.myapplication.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.harsha.myapplication.data.NotificationPrefs
import com.harsha.myapplication.data.NotificationPrefsRepository
import com.harsha.myapplication.notifications.AppNotifications
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import java.util.Calendar
import java.util.Locale

// ───────────────────────────── Tokens (same family as Settings / Import) ─────────────────────────────
private val NsScreenMargin = 16.dp
private val NsSectionGap = 24.dp
private val NsGroupGap = 2.dp
private val NsGroupOuter = 28.dp
private val NsGroupInner = 4.dp
private val NsPressedCorner = 20.dp
private val NsMasterCorner = 32.dp

private const val MAX_LEAD_MINUTES = 7 * 24 * 60

private val BeforeOptions = listOf(5, 10, 15, 30, 60, 120, 1440)
private val AfterOptions = listOf(15, 30, 60, 120, 240, 1440)

private enum class TimeTarget { DailySummary, DailyText, Weekly }
private enum class CustomTarget { Before, After }
private enum class DurationUnit(val label: String, val minutes: Int) {
    Minutes("Minutes", 1), Hours("Hours", 60), Days("Days", 1440)
}

private data class DayOption(val calendarDay: Int, val letter: String, val name: String)

// Monday-first, stored as Calendar constants.
private val WeekDays = listOf(
    DayOption(Calendar.MONDAY, "M", "Monday"),
    DayOption(Calendar.TUESDAY, "T", "Tuesday"),
    DayOption(Calendar.WEDNESDAY, "W", "Wednesday"),
    DayOption(Calendar.THURSDAY, "T", "Thursday"),
    DayOption(Calendar.FRIDAY, "F", "Friday"),
    DayOption(Calendar.SATURDAY, "S", "Saturday"),
    DayOption(Calendar.SUNDAY, "S", "Sunday"),
)

// ───────────────────────────── Formatting ─────────────────────────────

private fun formatTime(minuteOfDay: Int): String {
    val h24 = (minuteOfDay / 60) % 24
    val m = minuteOfDay % 60
    val suffix = if (h24 < 12) "AM" else "PM"
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, suffix)
}

private fun formatDuration(total: Int): String {
    val days = total / 1440
    val hours = (total % 1440) / 60
    val mins = total % 60
    return buildList {
        if (days > 0) add(if (days == 1) "1 day" else "$days days")
        if (hours > 0) add("$hours h")
        if (mins > 0) add("$mins min")
    }.joinToString(" ").ifEmpty { "0 min" }
}

private fun dayName(calendarDay: Int): String =
    WeekDays.firstOrNull { it.calendarDay == calendarDay }?.name ?: "Sunday"

// ───────────────────────────── Screen ─────────────────────────────

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val tick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    val p = NotificationPrefsRepository.prefs
    val master = p.masterEnabled
    fun edit(transform: (NotificationPrefs) -> NotificationPrefs) =
        NotificationPrefsRepository.update(transform)

    // ── System-level permission state (the app switch can be on while Android blocks us) ──
    var systemAllowed by remember { mutableStateOf(AppNotifications.areEnabled(context)) }
    var permissionDenied by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemAllowed = AppNotifications.areEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemAllowed = AppNotifications.areEnabled(context)
        permissionDenied = !granted
    }
    val allowNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionDenied) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }

    // ── Dialog state ──
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var customTarget by remember { mutableStateOf<CustomTarget?>(null) }

    timeTarget?.let { target ->
        val (title, initial) = when (target) {
            TimeTarget.DailySummary -> "Daily summary time" to p.dailySummaryMinutes
            TimeTarget.DailyText -> "Daily text reminder time" to p.dailyTextMinutes
            TimeTarget.Weekly -> "Weekly summary time" to p.weeklyMinutes
        }
        TimeDialog(
            title = title,
            initialMinutes = initial,
            onDismiss = { timeTarget = null },
            onConfirm = { minutes ->
                edit {
                    when (target) {
                        TimeTarget.DailySummary -> it.copy(dailySummaryMinutes = minutes)
                        TimeTarget.DailyText -> it.copy(dailyTextMinutes = minutes)
                        TimeTarget.Weekly -> it.copy(weeklyMinutes = minutes)
                    }
                }
                timeTarget = null
            }
        )
    }

    customTarget?.let { target ->
        CustomDurationDialog(
            title = if (target == CustomTarget.Before) "Custom time before" else "Custom time after",
            initialMinutes = if (target == CustomTarget.Before) p.visitBeforeLeadMinutes
            else p.visitAfterDelayMinutes,
            maxMinutes = MAX_LEAD_MINUTES,
            onDismiss = { customTarget = null },
            onConfirm = { minutes ->
                edit {
                    if (target == CustomTarget.Before) it.copy(visitBeforeLeadMinutes = minutes)
                    else it.copy(visitAfterDelayMinutes = minutes)
                }
                customTarget = null
            }
        )
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Notifications",
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NsScreenMargin)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            // ── System permission warning ──
            AnimatedVisibility(
                visible = !systemAllowed,
                enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>())
            ) {
                Column {
                    SystemBlockedCard(onAllow = allowNotifications)
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Master toggle ──
            MasterToggleCard(
                enabled = master,
                activeCount = p.enabledCount,
                total = NotificationPrefs.TOTAL_OPTIONS,
                onToggle = { on ->
                    tick()
                    edit { it.copy(masterEnabled = on) }
                },
                modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "notif-master")
            )

            Spacer(Modifier.height(NsSectionGap))

            // ── Daily ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "notif-daily")) {
                SectionLabel("Daily")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(NsGroupGap)) {
                    ReminderCard(
                        first = true, last = false,
                        icon = Icons.Outlined.Today,
                        title = "Daily summary",
                        summary = "Every day at ${formatTime(p.dailySummaryMinutes)}",
                        checked = p.dailySummaryEnabled,
                        masterOn = master,
                        onCheckedChange = { tick(); edit { s -> s.copy(dailySummaryEnabled = it) } },
                    ) {
                        PanelHint("A short recap of your hours, notes and visits for the day.")
                        TimeRow("Send at", p.dailySummaryMinutes) { timeTarget = TimeTarget.DailySummary }
                    }
                    ReminderCard(
                        first = false, last = true,
                        icon = Icons.Outlined.Book,
                        title = "Daily text reminder",
                        summary = "Every day at ${formatTime(p.dailyTextMinutes)}",
                        checked = p.dailyTextEnabled,
                        masterOn = master,
                        onCheckedChange = { tick(); edit { s -> s.copy(dailyTextEnabled = it) } },
                    ) {
                        PanelHint("A morning prompt to read today's text.")
                        TimeRow("Remind me at", p.dailyTextMinutes) { timeTarget = TimeTarget.DailyText }
                    }
                }
            }

            Spacer(Modifier.height(NsSectionGap))

            // ── Return visits & studies ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "notif-visits")) {
                SectionLabel("Return visits & studies")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(NsGroupGap)) {
                    ReminderCard(
                        first = true, last = false,
                        icon = Icons.Outlined.Alarm,
                        title = "Before a visit",
                        summary = "${formatDuration(p.visitBeforeLeadMinutes)} before each visit",
                        checked = p.visitBeforeEnabled,
                        masterOn = master,
                        onCheckedChange = { tick(); edit { s -> s.copy(visitBeforeEnabled = it) } },
                    ) {
                        PanelHint("Get a heads-up before a return visit or study starts.")
                        PanelLabel("How long before")
                        DurationChips(
                            options = BeforeOptions,
                            selectedMinutes = p.visitBeforeLeadMinutes,
                            onSelect = { tick(); edit { s -> s.copy(visitBeforeLeadMinutes = it) } },
                            onCustom = { customTarget = CustomTarget.Before }
                        )
                    }
                    ReminderCard(
                        first = false, last = true,
                        icon = Icons.Outlined.EventAvailable,
                        title = "After a visit",
                        summary = "${formatDuration(p.visitAfterDelayMinutes)} after each visit",
                        checked = p.visitAfterEnabled,
                        masterOn = master,
                        onCheckedChange = { tick(); edit { s -> s.copy(visitAfterEnabled = it) } },
                    ) {
                        PanelHint("A nudge to log the visit and plan your next step.")
                        PanelLabel("How long after")
                        DurationChips(
                            options = AfterOptions,
                            selectedMinutes = p.visitAfterDelayMinutes,
                            onSelect = { tick(); edit { s -> s.copy(visitAfterDelayMinutes = it) } },
                            onCustom = { customTarget = CustomTarget.After }
                        )
                    }
                }
            }

            Spacer(Modifier.height(NsSectionGap))

            // ── Weekly ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "notif-weekly")) {
                SectionLabel("Weekly")
                Spacer(Modifier.height(8.dp))
                ReminderCard(
                    first = true, last = true,
                    icon = Icons.Outlined.DateRange,
                    title = "Weekly summary",
                    summary = "Every ${dayName(p.weeklyDay)} at ${formatTime(p.weeklyMinutes)}",
                    checked = p.weeklySummaryEnabled,
                    masterOn = master,
                    onCheckedChange = { tick(); edit { s -> s.copy(weeklySummaryEnabled = it) } },
                ) {
                    PanelHint("Your week at a glance: hours, return visits and studies.")
                    PanelLabel("Day of the week")
                    DayPicker(
                        selectedDay = p.weeklyDay,
                        onSelect = { tick(); edit { s -> s.copy(weeklyDay = it) } }
                    )
                    TimeRow("Send at", p.weeklyMinutes) { timeTarget = TimeTarget.Weekly }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Changes are saved automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// ───────────────────────────── Master + system cards ─────────────────────────────

@Composable
private fun MasterToggleCard(
    enabled: Boolean,
    activeCount: Int,
    total: Int,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val container by animateColorAsState(
        if (enabled) colors.primaryContainer else colors.surfaceContainerHigh,
        Motion.effectsDefault(), label = "masterContainer"
    )
    val content by animateColorAsState(
        if (enabled) colors.onPrimaryContainer else colors.onSurface,
        Motion.effectsDefault(), label = "masterContent"
    )
    val tile by animateColorAsState(
        if (enabled) colors.primary else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "masterTile"
    )
    val tileContent by animateColorAsState(
        if (enabled) colors.onPrimary else colors.onSurfaceVariant,
        Motion.effectsDefault(), label = "masterTileContent"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 24.dp else NsMasterCorner,
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "masterCorner"
    )

    Surface(
        onClick = { onToggle(!enabled) },
        interactionSource = interaction,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        modifier = modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.98f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = tile,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Crossfade(targetState = enabled, label = "masterIcon") { on ->
                        Icon(
                            if (on) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = tileContent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("All notifications", style = typography.titleLargeEmphasized)
                Text(
                    if (enabled) "$activeCount of $total reminders on" else "Everything is paused",
                    style = typography.bodyMedium,
                    color = content.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                thumbContent = {
                    Icon(
                        if (enabled) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
private fun SystemBlockedCard(onAllow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Surface(
        shape = RoundedCornerShape(NsGroupOuter),
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = colors.error,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = colors.onError,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Blocked by Android", style = typography.titleSmallEmphasized)
                Text(
                    "Reminders can't be delivered until you allow notifications.",
                    style = typography.bodySmall,
                    color = colors.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            FilledTonalButton(
                onClick = onAllow,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.error,
                    contentColor = colors.onError
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { Text("Allow", style = typography.labelLargeEmphasized) }
        }
    }
}

// ───────────────────────────── Section pieces ─────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun PanelHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PanelLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * One notification option: a grouped-list card (big outer corners, tight joins, corners morph on
 * press) with a cookie tile, title, summary and a switch. When it is on, an inner panel expands with
 * the option's settings. When the master switch is off the card dims and its controls are disabled,
 * but the choices stay saved.
 */
@Composable
private fun ReminderCard(
    first: Boolean,
    last: Boolean,
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    masterOn: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val active = checked && masterOn

    // The settings panel stays closed until the row is tapped. Tapping the row toggles it; the
    // switch only turns the reminder on / off (turning it off also closes the panel).
    var expanded by rememberSaveable { mutableStateOf(false) }
    val showPanel = expanded && active
    val chevronRotation by animateFloatAsState(
        if (showPanel) 180f else 0f, Motion.spatialDefault, label = "chevron"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (pressed) NsPressedCorner else if (first) NsGroupOuter else NsGroupInner,
        spec, label = "cardTop"
    )
    val bottom by animateDpAsState(
        if (pressed) NsPressedCorner else if (last) NsGroupOuter else NsGroupInner,
        spec, label = "cardBottom"
    )
    val shape = RoundedCornerShape(
        topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom
    )

    val well by animateColorAsState(
        if (active) colors.primaryContainer else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "cardWell"
    )
    val tint by animateColorAsState(
        if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
        Motion.effectsDefault(), label = "cardTint"
    )
    val dim by animateFloatAsState(
        if (masterOn) 1f else 0.5f, Motion.effectsDefault, label = "cardDim"
    )

    Surface(
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = dim }
            .themePressScale(interaction, pressedScale = 0.98f, enabled = masterOn)
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(title, style = typography.bodyLargeEmphasized, maxLines = 1)
                },
                supportingContent = {
                    Text(
                        if (checked) summary else "Off",
                        style = typography.bodyMedium,
                        maxLines = 2
                    )
                },
                leadingContent = {
                    Surface(
                        shape = MaterialShapes.Cookie6Sided.toShape(),
                        color = well,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                        }
                    }
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Chevron = "tap the row to open its settings". Only while the reminder is on.
                        AnimatedVisibility(
                            visible = active,
                            enter = fadeIn(Motion.effectsFast) + expandHorizontally(Motion.spatialFast<IntSize>()),
                            exit = fadeOut(Motion.effectsFast) + shrinkHorizontally(Motion.spatialFast<IntSize>())
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showPanel) "Hide settings" else "Show settings",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = chevronRotation }
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = {
                                if (!it) expanded = false
                                onCheckedChange(it)
                            },
                            enabled = masterOn,
                            thumbContent = {
                                Icon(
                                    if (checked) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = colors.onSurface,
                    supportingColor = if (active) colors.primary else colors.onSurfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        enabled = active,
                        role = Role.Button,
                        onClickLabel = if (showPanel) "Hide $title settings" else "Show $title settings",
                    ) { expanded = !expanded }
            )

            AnimatedVisibility(
                visible = showPanel,
                enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>())
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            content = content
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Controls ─────────────────────────────

@Composable
private fun TimeRow(label: String, minutes: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(formatTime(minutes), style = MaterialTheme.typography.labelLargeEmphasized)
        }
    }
}

/** Preset durations + a "Custom" chip. A value that is not a preset shows up on the Custom chip. */
@Composable
private fun DurationChips(
    options: List<Int>,
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val isCustom = selectedMinutes !in options
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { minutes ->
            ChoiceChip(
                label = formatDuration(minutes),
                selected = minutes == selectedMinutes,
                onClick = { onSelect(minutes) }
            )
        }
        ChoiceChip(
            label = if (isCustom) "Custom · ${formatDuration(selectedMinutes)}" else "Custom",
            selected = isCustom,
            onClick = onCustom
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val radius by animateDpAsState(
        if (selected) 20.dp else 12.dp, Motion.spatialDefaultDp, label = "chipRadius"
    )
    val container by animateColorAsState(
        if (selected) colors.primary else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "chipContainer"
    )
    val content by animateColorAsState(
        if (selected) colors.onPrimary else colors.onSurface,
        Motion.effectsDefault(), label = "chipContent"
    )
    val src = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = content,
        modifier = Modifier
            .height(40.dp)
            .themePressScale(src, pressedScale = 0.94f)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(Motion.spatialFast<IntSize>()) + fadeIn(Motion.effectsFast),
                exit = shrinkHorizontally(Motion.spatialFast<IntSize>()) + fadeOut(Motion.effectsFast)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(16.dp)
                )
            }
            Text(label, style = MaterialTheme.typography.labelLargeEmphasized, maxLines = 1)
        }
    }
}

@Composable
private fun DayPicker(selectedDay: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        WeekDays.forEach { day ->
            DayBubble(
                day = day,
                selected = day.calendarDay == selectedDay,
                onClick = { onSelect(day.calendarDay) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayBubble(
    day: DayOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val radius by animateDpAsState(
        if (selected) 24.dp else 12.dp, Motion.spatialDefaultDp, label = "dayRadius"
    )
    val container by animateColorAsState(
        if (selected) colors.primary else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "dayContainer"
    )
    val content by animateColorAsState(
        if (selected) colors.onPrimary else colors.onSurface,
        Motion.effectsDefault(), label = "dayContent"
    )
    val src = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = content,
        modifier = modifier
            .height(48.dp)
            .themePressScale(src, pressedScale = 0.92f)
            .semantics {
                contentDescription = day.name
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(day.letter, style = MaterialTheme.typography.labelLargeEmphasized)
        }
    }
}

// ───────────────────────────── Dialogs ─────────────────────────────

@Composable
private fun TimeDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    title,
                    style = typography.titleMediumEmphasized,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(20.dp))
                TimePicker(state = state, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", style = typography.labelLarge)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                        Text("OK", style = typography.labelLargeEmphasized)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomDurationDialog(
    title: String,
    initialMinutes: Int,
    maxMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val initialUnit = when {
        initialMinutes % 1440 == 0 -> DurationUnit.Days
        initialMinutes % 60 == 0 -> DurationUnit.Hours
        else -> DurationUnit.Minutes
    }
    var unit by remember { mutableStateOf(initialUnit) }
    var text by remember { mutableStateOf((initialMinutes / initialUnit.minutes).toString()) }

    val total = text.toIntOrNull()?.times(unit.minutes)
    val valid = total != null && total in 1..maxMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = typography.headlineSmallEmphasized) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) text = v },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valid,
                    supportingText = {
                        Text(
                            if (valid) "= ${formatDuration(total!!)}"
                            else "Enter up to ${formatDuration(maxMinutes)}"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val units = DurationUnit.values()
                    units.forEachIndexed { index, u ->
                        SegmentedButton(
                            selected = unit == u,
                            onClick = { unit = u },
                            shape = SegmentedButtonDefaults.itemShape(index, units.size),
                            label = { Text(u.label, maxLines = 1) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(total!!) }, enabled = valid) {
                Text("Set", style = typography.labelLargeEmphasized)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = typography.labelLarge) }
        },
        shape = MaterialTheme.shapes.extraLargeIncreased,
        containerColor = colors.surfaceContainerHigh
    )
}
