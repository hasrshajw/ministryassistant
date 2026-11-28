@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.rememberAppFeedback
import com.harsha.myapplication.ui.theme.revealOnEnter

// ── Design tokens ──────────────────────────────────────────────────────────
private val ScreenMargin = 16.dp
private val SectionGap = 28.dp
private val CardRadius = 28.dp
private val GroupInner = 4.dp
private val GroupGap = 2.dp
private const val TOTAL_PERMISSIONS = 3

private val ColorEffects = spring<Color>(dampingRatio = 1f, stiffness = 1600f)

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
    val feedback = rememberAppFeedback()

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
    val grantedCount = listOf(notifications, exactAlarms, battery).count { it }

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
                .padding(horizontal = ScreenMargin)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            Box(Modifier.revealOnEnter(index = 0, playOnceKey = "perm-0")) {
                HeroCard(
                    requiredMissing = requiredMissing,
                    batteryOk = battery,
                    grantedCount = grantedCount,
                )
            }

            // ── Allow these ──
            Spacer(Modifier.height(SectionGap))
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "perm-1")) {
                SectionHeader("Allow these")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    PermissionRow(
                        index = 0, count = 3,
                        icon = Icons.Outlined.Notifications,
                        tileShape = MaterialShapes.Cookie9Sided.toShape(),
                        title = "Notifications",
                        need = Need.Required,
                        description = "Shows your visit reminders, daily text and summaries.",
                        granted = notifications,
                        onAllow = {
                            feedback.click()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAskedAndDenied) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openAppNotificationSettings(context)
                            }
                        },
                    )
                    PermissionRow(
                        index = 1, count = 3,
                        icon = Icons.Outlined.Alarm,
                        tileShape = MaterialShapes.Clover4Leaf.toShape(),
                        title = "Alarms & reminders",
                        need = Need.Required,
                        description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Lets Android deliver each reminder at the exact minute, even if you haven’t opened the app for weeks."
                        } else {
                            "Built in on your Android version. Nothing to turn on."
                        },
                        granted = exactAlarms,
                        onAllow = { feedback.click(); AppNotifications.openExactAlarmSettings(context) },
                    )
                    BatteryRow(
                        index = 2, count = 3,
                        granted = battery,
                        onOpenSettings = { feedback.click(); AppPermissions.openBatterySettings(context) },
                        onAppSettings = { feedback.click(); AppPermissions.openAppDetails(context) },
                    )
                }
            }

            // ── Granted automatically ──
            Spacer(Modifier.height(SectionGap))
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "perm-2")) {
                SectionHeader("Included with install")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Android gives these to the app automatically. Nothing to turn on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(12.dp))

                val autos = listOf(
                    Triple(Icons.Outlined.RestartAlt, "Run after restart", "Sets your reminders again when the phone restarts or the app updates."),
                    Triple(Icons.Outlined.Vibration, "Vibrate", "Lets reminders vibrate."),
                    Triple(Icons.Outlined.Alarm, "Keep device awake briefly", "Lets a reminder wake the phone for a moment so it can show."),
                    Triple(Icons.Outlined.Wifi, "Internet", "Only for Bible downloads, Google Drive backup and push messages."),
                )
                Surface(
                    shape = MaterialTheme.shapes.extraLargeIncreased,
                    color = colors.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        autos.forEachIndexed { index, (icon, title, description) ->
                            AutoRow(icon, title, description)
                            if (index != autos.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 62.dp, end = 20.dp),
                                    color = colors.outlineVariant.copy(alpha = 0.35f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Building blocks ─────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )
}

@Composable
private fun rememberGroupShape(index: Int, count: Int, innerTarget: Dp = GroupInner): Shape {
    val inner by animateDpAsState(innerTarget, Motion.spatialFastDp, label = "groupInner")
    val safeInner = inner.coerceAtLeast(0.dp)
    val top = if (index == 0) CardRadius else safeInner
    val bottom = if (index == count - 1) CardRadius else safeInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Icon tile. The shape slowly turns when the permission is granted (small "it clicked" moment). */
@Composable
private fun ShapeTile(
    shape: Shape,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    turned: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val rotation by animateFloatAsState(if (turned) 30f else 0f, Motion.spatialDefault, label = "tileTurn")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .rotate(rotation)
                .clip(shape)
                .background(container)
        )
        content()
    }
}

/**
 * Hero: a progress ring (how many of the 3 permissions are on) next to a one-line title and subtitle,
 * on a soft gradient. Turns from red to green as things get allowed.
 */
@Composable
private fun HeroCard(requiredMissing: Int, batteryOk: Boolean, grantedCount: Int) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val allGood = requiredMissing == 0

    val container by animateColorAsState(
        if (allGood) colors.primaryContainer else colors.errorContainer,
        ColorEffects, label = "heroContainer",
    )
    val content by animateColorAsState(
        if (allGood) colors.onPrimaryContainer else colors.onErrorContainer,
        ColorEffects, label = "heroContent",
    )
    val accent by animateColorAsState(
        if (allGood) colors.primary else colors.error,
        ColorEffects, label = "heroAccent",
    )
    val progress by animateFloatAsState(
        grantedCount / TOTAL_PERMISSIONS.toFloat(), Motion.spatialSlow, label = "heroProgress",
    )

    val title = when {
        !allGood -> "$requiredMissing permission${if (requiredMissing > 1) "s" else ""} needed"
        !batteryOk -> "Reminders are working"
        else -> "All set"
    }
    val body = when {
        !allGood -> "Turn these on below"
        !batteryOk -> "Set battery to Unrestricted"
        else -> "Reminders arrive on time"
    }

    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = Color.Transparent,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(container, lerp(container, colors.surface, 0.45f))
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Progress ring
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                val track = content.copy(alpha = 0.14f)
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    if (progress > 0f) {
                        drawArc(
                            color = accent, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Crossfade(targetState = allGood, label = "heroRingCenter") { good ->
                    if (good) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                    } else {
                        Text("$grantedCount/$TOTAL_PERMISSIONS", style = typography.labelLargeEmphasized)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = typography.titleLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    body,
                    style = typography.bodyMedium,
                    color = content.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Description is open while the permission is missing; folds away once granted (chevron reopens it). */
@Composable
private fun rememberDescriptionOpen(granted: Boolean, key: String): MutableState<Boolean> {
    val open = rememberSaveable(key) { mutableStateOf(!granted) }
    LaunchedEffect(granted) { open.value = !granted }
    return open
}

@Composable
private fun Chevron(open: Boolean) {
    val rotation by animateFloatAsState(if (open) 180f else 0f, Motion.spatialDefault, label = "chevron")
    Icon(
        Icons.Filled.ExpandMore,
        contentDescription = if (open) "Hide details" else "Show details",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(22.dp)
            .rotate(rotation),
    )
}

@Composable
private fun AllowButton(onClick: () -> Unit, container: Color = MaterialTheme.colorScheme.primary, content: Color = MaterialTheme.colorScheme.onPrimary) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = source,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
        modifier = Modifier
            .height(36.dp)
            .pressScale(source, pressedScale = 0.95f),
    ) { Text("Allow", style = MaterialTheme.typography.labelLargeEmphasized) }
}

/** Second line under a title: "✓ Allowed" in green, or "Required · Off" in red. */
@Composable
private fun StatusLine(granted: Boolean, need: Need, offColor: Color) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (granted) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
            Text("Allowed", style = typography.labelMediumEmphasized, color = colors.primary)
        } else {
            Text("${need.label} · Off", style = typography.labelMediumEmphasized, color = offColor)
        }
    }
}

@Composable
private fun PermissionRow(
    index: Int,
    count: Int,
    icon: ImageVector,
    tileShape: Shape,
    title: String,
    need: Need,
    description: String,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var open by rememberDescriptionOpen(granted, title)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = rememberGroupShape(index, count, if (pressed && granted) 20.dp else GroupInner)

    val tile by animateColorAsState(
        if (granted) colors.primaryContainer else colors.errorContainer,
        ColorEffects, label = "permTile",
    )
    val tileContent by animateColorAsState(
        if (granted) colors.onPrimaryContainer else colors.onErrorContainer,
        ColorEffects, label = "permTileContent",
    )

    Surface(
        onClick = { if (granted) open = !open },
        enabled = granted,
        interactionSource = source,
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.98f, enabled = granted)
            .animateContentSize(Motion.spatialDefault()),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeTile(tileShape, tile, size = 44.dp, turned = granted) {
                    Icon(icon, contentDescription = null, tint = tileContent, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    StatusLine(granted, need, offColor = colors.error)
                }
                if (granted) Chevron(open) else AllowButton(onAllow)
            }
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>()),
            ) {
                // Indented to line up with the title, not the icon.
                Text(
                    description,
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 58.dp, top = 8.dp, end = 4.dp),
                )
            }
        }
    }
}

/**
 * Battery is "recommended", not required, so it is softer: tertiary tint instead of red, and its two
 * actions sit in a button row under the description instead of one Allow button.
 */
@Composable
private fun BatteryRow(
    index: Int,
    count: Int,
    granted: Boolean,
    onOpenSettings: () -> Unit,
    onAppSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var open by rememberDescriptionOpen(granted, "battery")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = rememberGroupShape(index, count, if (pressed && granted) 20.dp else GroupInner)

    val container by animateColorAsState(
        if (granted) colors.surfaceContainerLow else colors.tertiaryContainer,
        ColorEffects, label = "batteryContainer",
    )
    val content by animateColorAsState(
        if (granted) colors.onSurface else colors.onTertiaryContainer,
        ColorEffects, label = "batteryContent",
    )
    val tile by animateColorAsState(
        if (granted) colors.primaryContainer else colors.tertiary,
        ColorEffects, label = "batteryTile",
    )
    val tileContent by animateColorAsState(
        if (granted) colors.onPrimaryContainer else colors.onTertiary,
        ColorEffects, label = "batteryTileContent",
    )

    Surface(
        onClick = { if (granted) open = !open },
        enabled = granted,
        interactionSource = source,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.98f, enabled = granted)
            .animateContentSize(Motion.spatialDefault()),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeTile(MaterialShapes.Cookie6Sided.toShape(), tile, size = 44.dp, turned = granted) {
                    Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = tileContent, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Unrestricted battery", style = typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    StatusLine(granted, Need.Recommended, offColor = colors.onTertiaryContainer.copy(alpha = 0.8f))
                }
                if (granted) Chevron(open)
            }
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>()),
            ) {
                Column(Modifier.padding(start = 58.dp, top = 8.dp, end = 4.dp)) {
                    Text(
                        "Stops the phone from holding reminders back to save power. Choose “Unrestricted” or " +
                            "“Don’t optimise” for this app. On Xiaomi, Oppo, Vivo, Realme, OnePlus and Huawei phones also " +
                            "switch on “Auto-start” in the app’s settings.",
                        style = typography.bodyMedium,
                        color = content.copy(alpha = 0.8f),
                    )
                    if (!granted) {
                        Row(
                            Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val btnSource = remember { MutableInteractionSource() }
                            Button(
                                onClick = onOpenSettings,
                                interactionSource = btnSource,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.tertiary,
                                    contentColor = colors.onTertiary,
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .pressScale(btnSource, pressedScale = 0.95f),
                            ) { Text("Open settings", style = typography.labelLargeEmphasized) }
                            TextButton(
                                onClick = onAppSettings,
                                colors = ButtonDefaults.textButtonColors(contentColor = colors.onTertiaryContainer),
                            ) { Text("App settings", style = typography.labelLargeEmphasized) }
                        }
                    }
                }
            }
        }
    }
}

/** Plain info row inside the "Included with install" card. */
@Composable
private fun AutoRow(icon: ImageVector, title: String, description: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = typography.titleSmallEmphasized)
            Text(description, style = typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = "Granted",
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}
