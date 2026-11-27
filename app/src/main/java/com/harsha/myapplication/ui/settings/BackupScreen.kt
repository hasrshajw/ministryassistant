@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.harsha.myapplication.data.DriveBackup
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.data.BackupFrequency
import com.harsha.myapplication.data.BackupKind
import com.harsha.myapplication.data.BackupPrefs
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.BackupTask
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.RollingCount
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val BkMargin = 16.dp
private val BkSectionGap = 24.dp
private val BkGroupGap = 2.dp
private val BkOuter = 28.dp
private val BkInner = 4.dp
private val BkPressed = 20.dp
private val BkHeroCorner = 32.dp

// ───────────────────────────── Screen ─────────────────────────────

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val prefs = BackupRepository.prefs
    val task = BackupRepository.task
    val snapshots = BackupRepository.snapshots
    val running = task is BackupTask.Running

    var lastAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    LaunchedEffect(Unit) {
        BackupRepository.dismissTask()
        BackupRepository.refreshSnapshots(context)
        BackupRepository.refreshDriveSession(context)
    }

    /** Runs an action and scrolls to the hero card so the progress animation is always visible. */
    fun start(action: () -> Unit) {
        lastAction = action
        scope.launch { scroll.animateScrollTo(0) }
        action()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) start { BackupRepository.exportTo(context, uri) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            confirm = "This replaces your current notes, settings and progress with the backup." to
                { BackupRepository.importFrom(context, uri) }
        }
    }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            if (account != null && DriveBackup.hasAppDataScope(account)) {
                BackupRepository.onDriveConnected(account.email)
            } else {
                BackupRepository.onDriveConnectFailed(
                    "Drive permission was not granted. Try Connect again and allow access."
                )
            }
        } catch (e: ApiException) {
            BackupRepository.onDriveConnectFailed(
                if (e.statusCode == 12501) null // user cancelled
                else (e.message ?: "Google Sign-In failed (${e.statusCode})")
            )
        } catch (e: Exception) {
            BackupRepository.onDriveConnectFailed(e.message ?: "Google Sign-In failed")
        }
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Backup & restore",
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
                .verticalScroll(scroll)
                .padding(horizontal = BkMargin)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            // ── Hero: status + live progress ──
            BackupHero(
                task = task,
                prefs = prefs,
                onBackupNow = { start { BackupRepository.backupNow(context) } },
                onCancel = BackupRepository::cancel,
                onDismiss = BackupRepository::dismissTask,
                onRetry = {
                    BackupRepository.dismissTask()
                    lastAction?.let { start(it) }
                },
                onRestart = { restartApp(context) },
                modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "backup-hero"),
            )

            Spacer(Modifier.height(BkSectionGap))

            // ── On this device ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "backup-local")) {
                BkHeader("Export & import")
                Spacer(Modifier.height(8.dp))
                BkGroup {
                    BkRow(
                        pos = BkPos(first = true, last = false), tone = BkTone.Tertiary,
                        icon = Icons.Outlined.FileUpload, title = "Export data",
                        subtitle = "Save a backup file anywhere you like",
                        enabled = !running, showChevron = true,
                        onClick = { exportLauncher.launch(BackupRepository.suggestedFileName()) },
                    )
                    BkRow(
                        pos = BkPos(first = false, last = true), tone = BkTone.Tertiary,
                        icon = Icons.Outlined.FileDownload, title = "Import data",
                        subtitle = "Restore from a backup file",
                        enabled = !running, showChevron = true,
                        onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    )
                }
            }

            // ── Recent backups on this device ──
            AnimatedVisibility(
                visible = snapshots.isNotEmpty(),
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Column {
                    Spacer(Modifier.height(BkSectionGap))
                    BkHeader("Recent backups")
                    Spacer(Modifier.height(8.dp))
                    val shown = snapshots.take(5)
                    BkGroup {
                        shown.forEachIndexed { i, snap ->
                            BkRow(
                                pos = BkPos(first = i == 0, last = i == shown.lastIndex),
                                tone = BkTone.Neutral,
                                icon = Icons.Outlined.History,
                                title = android.text.format.DateUtils.formatDateTime(
                                    context, snap.time,
                                    android.text.format.DateUtils.FORMAT_SHOW_DATE or
                                        android.text.format.DateUtils.FORMAT_SHOW_TIME or
                                        android.text.format.DateUtils.FORMAT_ABBREV_MONTH
                                ),
                                subtitle = Formatter.formatShortFileSize(context, snap.bytes) + " · tap to restore",
                                enabled = !running,
                                onClick = {
                                    confirm = "Restore the backup from this date? Current data will be replaced." to
                                        { BackupRepository.restoreSnapshot(context, snap) }
                                },
                                trailing = {
                                    IconButton(onClick = { BackupRepository.deleteSnapshot(context, snap) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete backup")
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(BkSectionGap))

            // ── Google Drive ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "backup-drive")) {
                BkHeader("Google Drive")
                Spacer(Modifier.height(8.dp))
                val loadingTrailing: @Composable () -> Unit = { LoadingIndicator(Modifier.size(40.dp)) }
                AnimatedContent(
                    targetState = prefs.driveConnected,
                    transitionSpec = {
                        (fadeIn(Motion.effectsDefault) + scaleIn(Motion.spatialDefault, initialScale = 0.96f)) togetherWith
                            fadeOut(Motion.effectsFast)
                    },
                    label = "driveState",
                ) { connected ->
                    if (!connected) {
                        BkGroup {
                            BkRow(
                                pos = BkPos(first = true, last = true), tone = BkTone.Secondary,
                                icon = Icons.Outlined.CloudQueue, title = "Connect Google Drive",
                                subtitle = "Keep a safe copy in your Drive",
                                enabled = !BackupRepository.driveConnecting,
                                showChevron = !BackupRepository.driveConnecting,
                                onClick = {
                                    BackupRepository.beginDriveConnect()
                                    driveSignInLauncher.launch(DriveBackup.signInIntent(context))
                                },
                                trailing = if (BackupRepository.driveConnecting) loadingTrailing else null,
                            )
                        }
                    } else {
                        BkGroup {
                            BkRow(
                                pos = BkPos(first = true, last = false), tone = BkTone.Secondary,
                                icon = Icons.Outlined.CloudDone, title = "Google Drive",
                                subtitle = prefs.driveAccount ?: "Connected",
                                onClick = {},
                                trailing = {
                                    TextButton(onClick = { BackupRepository.disconnectDrive(context) }) { Text("Disconnect") }
                                },
                            )
                            BkRow(
                                pos = BkPos(first = false, last = false), tone = BkTone.Secondary,
                                icon = Icons.Outlined.CloudUpload, title = "Back up to Drive",
                                enabled = !running, showChevron = true,
                                onClick = {
                                    start {
                                        BackupRepository.backupToDrive(context)
                                    }
                                },
                            )
                            BkRow(
                                pos = BkPos(first = false, last = true), tone = BkTone.Secondary,
                                icon = Icons.Outlined.CloudDownload, title = "Restore from Drive",
                                enabled = !running, showChevron = true,
                                onClick = {
                                    confirm = "This would replace your current data with the copy in Drive." to {
                                        start { BackupRepository.restoreFromDrive(context) }
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Backups are stored in a private app folder on Drive — only this app can see them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(Modifier.height(BkSectionGap))

            // ── Automatic backup ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "backup-auto")) {
                BkHeader("Automatic backup")
                Spacer(Modifier.height(8.dp))
                val motion = MaterialTheme.motionScheme
                Column(verticalArrangement = Arrangement.spacedBy(BkGroupGap)) {
                    BkRow(
                        pos = BkPos(first = true, last = !prefs.autoBackup), tone = BkTone.Primary,
                        icon = Icons.Outlined.Schedule, title = "Auto backup",
                        subtitle = if (prefs.autoBackup) prefs.nextBackupLabel() else "Off",
                        onClick = { BackupRepository.setAutoBackup(!prefs.autoBackup) },
                        trailing = { BkSwitch(prefs.autoBackup, BackupRepository::setAutoBackup) },
                    )
                    AnimatedVisibility(
                        visible = prefs.autoBackup,
                        enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                        exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(BkGroupGap)) {
                            BkPanel(BkPos(first = false, last = false)) {
                                Text("How often", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(10.dp))
                                BkChoiceGroup(
                                    options = BackupFrequency.entries,
                                    selected = prefs.frequency,
                                    label = { it.label },
                                    onSelect = BackupRepository::setFrequency,
                                )
                            }
                            BkRow(
                                pos = BkPos(first = false, last = false), tone = BkTone.Neutral,
                                icon = Icons.Outlined.Wifi, title = "Only on Wi-Fi",
                                subtitle = "Applies to Google Drive uploads",
                                onClick = { BackupRepository.setWifiOnly(!prefs.wifiOnly) },
                                trailing = { BkSwitch(prefs.wifiOnly, BackupRepository::setWifiOnly) },
                            )
                            BkPanel(BkPos(first = false, last = true)) {
                                Text("Keep last", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Older backups on this device are removed automatically",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                BkChoiceGroup(
                                    options = listOf(3, 5, 10),
                                    selected = prefs.keepLast,
                                    label = { "$it backups" },
                                    onSelect = BackupRepository::setKeepLast,
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    confirm?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Replace current data?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { confirm = null; start(action) }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    context.startActivity(Intent.makeRestartActivityTask(intent.component))
    Runtime.getRuntime().exit(0)
}

// ───────────────────────────── Hero card ─────────────────────────────

private fun phaseOf(t: BackupTask) = when (t) {
    is BackupTask.Idle -> 0
    is BackupTask.Running -> 1
    is BackupTask.Done -> 2
    is BackupTask.Failed -> 3
}

@Composable
private fun BackupHero(
    task: BackupTask,
    prefs: BackupPrefs,
    onBackupNow: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val motion = MaterialTheme.motionScheme
    val context = LocalContext.current
    val failed = task is BackupTask.Failed

    val container by animateColorAsState(
        if (failed) colors.errorContainer else colors.primaryContainer,
        motion.defaultEffectsSpec(), label = "heroContainer"
    )
    val content by animateColorAsState(
        if (failed) colors.onErrorContainer else colors.onPrimaryContainer,
        motion.defaultEffectsSpec(), label = "heroContent"
    )

    Surface(
        shape = RoundedCornerShape(BkHeroCorner),
        color = container,
        contentColor = content,
        modifier = modifier.fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = task,
            contentKey = { phaseOf(it) },
            transitionSpec = {
                (fadeIn(Motion.effectsDefault) + scaleIn(Motion.spatialDefault, initialScale = 0.92f)) togetherWith
                    fadeOut(Motion.effectsFast)
            },
            label = "heroPhase",
        ) { t ->
            when (t) {
                is BackupTask.Idle -> Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            shape = MaterialShapes.Cookie9Sided.toShape(),
                            color = colors.primary,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (prefs.lastBackupMillis == 0L) Icons.Outlined.Backup else Icons.Outlined.CloudDone,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (prefs.lastBackupMillis == 0L) "No backup yet" else "Your data is backed up",
                                style = typography.titleLargeEmphasized,
                            )
                            Text(
                                if (prefs.lastBackupMillis == 0L) "Create one to protect your notes and progress"
                                else "Last backup ${prefs.lastBackupLabel()} · " +
                                    Formatter.formatShortFileSize(context, prefs.lastBackupBytes),
                                style = typography.bodyMedium,
                                color = content.copy(alpha = 0.8f),
                            )
                        }
                    }
                    Button(
                        onClick = onBackupNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) { Text("Back up now", style = typography.labelLargeEmphasized) }
                }

                is BackupTask.Running -> RunningContent(t, onCancel)

                is BackupTask.Done -> Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BkPopTile(Icons.Filled.Check, colors.primary, colors.onPrimary, shake = false)
                    Text(
                        if (t.kind == BackupKind.Backup) "Backup complete" else "Restore complete",
                        style = typography.titleLargeEmphasized,
                    )
                    Text(
                        t.message,
                        style = typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = content.copy(alpha = 0.8f),
                    )
                    if (t.needsRestart) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onDismiss) { Text("Later") }
                            Button(onClick = onRestart) { Text("Restart app") }
                        }
                    } else {
                        Button(onClick = onDismiss) { Text("Done") }
                    }
                }

                is BackupTask.Failed -> Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BkPopTile(Icons.Outlined.ErrorOutline, colors.error, colors.onError, shake = true)
                    Text("Something went wrong", style = typography.titleLargeEmphasized)
                    Text(
                        t.message,
                        style = typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = content.copy(alpha = 0.8f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.error,
                                contentColor = colors.onError,
                            ),
                        ) { Text("Try again") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningContent(t: BackupTask.Running, onCancel: () -> Unit) {
    val typography = MaterialTheme.typography
    val determinate = t.progress != null
    val animated by animateFloatAsState(
        targetValue = t.progress ?: 0f,
        animationSpec = Motion.effectsDefault,
        label = "heroProgress",
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Indeterminate: morphing LoadingIndicator. Determinate: wavy ring + rolling percent.
        AnimatedContent(
            targetState = determinate,
            transitionSpec = {
                (fadeIn(Motion.effectsDefault) + scaleIn(Motion.spatialDefault, initialScale = 0.8f)) togetherWith
                    (fadeOut(Motion.effectsFast) + scaleOut(Motion.spatialFast, targetScale = 0.8f))
            },
            label = "indicatorType",
        ) { det ->
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                if (!det) {
                    LoadingIndicator(Modifier.size(96.dp))
                } else {
                    CircularWavyProgressIndicator(
                        progress = { animated },
                        modifier = Modifier.size(120.dp),
                    )
                    RollingCount(count = (animated * 100).toInt()) { pct ->
                        Text("$pct%", style = typography.headlineSmallEmphasized)
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${t.kind.title} ${if (t.demo && t.target == "Demo") "(demo)" else ""}".trim(),
                style = typography.titleLargeEmphasized,
            )
            Text(
                "to ${t.target}".takeIf { t.kind == BackupKind.Backup } ?: "from ${t.target}",
                style = typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.8f),
            )
        }

        AnimatedVisibility(visible = determinate) {
            LinearWavyProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            t.steps.forEachIndexed { i, label ->
                BkStepRow(
                    label = label,
                    state = when {
                        i < t.stepIndex -> 2
                        i == t.stepIndex -> 1
                        else -> 0
                    },
                )
            }
        }

        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

/** 0 = pending, 1 = active, 2 = done. Icon swaps with a bouncy spring. */
@Composable
private fun BkStepRow(label: String, state: Int) {
    val colors = MaterialTheme.colorScheme
    val textAlpha by animateFloatAsState(
        targetValue = if (state == 0) 0.55f else 1f,
        animationSpec = Motion.effectsDefault,
        label = "stepAlpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (scaleIn(Motion.spatialFast, initialScale = 0.4f) + fadeIn(Motion.effectsFast)) togetherWith
                    (scaleOut(Motion.spatialFast, targetScale = 0.4f) + fadeOut(Motion.effectsFast))
            },
            label = "stepIcon",
        ) { s ->
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                when (s) {
                    2 -> Surface(shape = CircleShape, color = colors.primary, modifier = Modifier.size(22.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Check, contentDescription = "Done",
                                tint = colors.onPrimary, modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    1 -> CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                    else -> Surface(
                        shape = CircleShape, color = Color.Transparent,
                        border = BorderStroke(2.dp, colors.onPrimaryContainer.copy(alpha = 0.35f)),
                        modifier = Modifier.size(20.dp),
                    ) {}
                }
            }
        }
        Text(
            label,
            style = if (state == 1) MaterialTheme.typography.bodyLargeEmphasized else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.alpha(textAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Cookie tile that pops in with the spatial spring; optionally wobbles left-right (error). */
@Composable
private fun BkPopTile(icon: ImageVector, container: Color, onContainer: Color, shake: Boolean) {
    val scale = remember { Animatable(0.3f) }
    val wobble = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, Motion.spatialFast) }
        if (shake) wobble.animateTo(1f, tween(450, easing = LinearEasing))
    }
    Surface(
        shape = MaterialShapes.Cookie9Sided.toShape(),
        color = container,
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                val t = wobble.value
                translationX = sin(t * 4f * 2f * PI.toFloat()) * (1f - t) * 10.dp.toPx()
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(36.dp))
        }
    }
}

// ───────────────────────────── List building blocks ─────────────────────────────
// (Same look as SettingsScreen: grouped rows, corners morph on press, cookie icon tile.)

private enum class BkTone { Primary, Secondary, Tertiary, Neutral, Error }

private data class BkPos(val first: Boolean, val last: Boolean)

@Composable
private fun BkTone.tileColors(): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (this) {
        BkTone.Primary -> c.primary to c.onPrimary
        BkTone.Secondary -> c.secondaryContainer to c.onSecondaryContainer
        BkTone.Tertiary -> c.tertiaryContainer to c.onTertiaryContainer
        BkTone.Neutral -> c.surfaceContainerHighest to c.onSurfaceVariant
        BkTone.Error -> c.errorContainer to c.onErrorContainer
    }
}

@Composable
private fun BkHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun BkGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BkGroupGap)) { content() }
}

@Composable
private fun rememberBkShape(pos: BkPos, pressed: Boolean): Shape {
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (pressed) BkPressed else if (pos.first) BkOuter else BkInner, spec, label = "bkTop"
    )
    val bottom by animateDpAsState(
        if (pressed) BkPressed else if (pos.last) BkOuter else BkInner, spec, label = "bkBottom"
    )
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun BkRow(
    pos: BkPos,
    tone: BkTone,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberBkShape(pos, pressed)
    val (tile, tileContent) = tone.tileColors()
    val alpha by animateFloatAsState(
        if (enabled) 1f else 0.5f, Motion.effectsDefault, label = "rowAlpha"
    )
    val chevron: @Composable () -> Unit = {
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = colors.surfaceContainerLow,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .themePressScale(interaction, pressedScale = 0.98f),
    ) {
        ListItem(
            headlineContent = {
                Text(title, style = typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = subtitle?.let {
                { Text(it, style = typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = tile,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = tileContent, modifier = Modifier.size(24.dp))
                    }
                }
            },
            trailingContent = when {
                trailing != null -> trailing
                showChevron -> chevron
                else -> null
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = colors.onSurface,
                supportingColor = colors.onSurfaceVariant,
                trailingIconColor = colors.onSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Non-clickable group row that hosts custom controls (button groups etc.). */
@Composable
private fun BkPanel(pos: BkPos, content: @Composable () -> Unit) {
    val shape = rememberBkShape(pos, pressed = false)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) { content() }
    }
}

@Composable
private fun <T> BkChoiceGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selected == option,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun BkSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}
