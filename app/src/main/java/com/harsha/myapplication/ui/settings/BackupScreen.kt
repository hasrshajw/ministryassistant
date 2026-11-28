@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.harsha.myapplication.data.BackupKind
import com.harsha.myapplication.data.BackupPrefs
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.BackupTask
import com.harsha.myapplication.data.DriveBackup
import com.harsha.myapplication.telemetry.CrashReporter
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.components.launchSafely
import com.harsha.myapplication.ui.components.startDriveSignIn
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.RollingCount
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.PI
import kotlin.math.sin

// ── Design tokens (shared rhythm with Settings / About / Import) ─────────────
private val BkMargin = 16.dp
private val BkSectionGap = 28.dp
private val BkGroupGap = 2.dp
private val BkOuter = 28.dp
private val BkInner = 4.dp
private val BkPressed = 20.dp
private val BkHeroCorner = 28.dp

/** A confirmation the user must accept before anything destructive happens. */
private data class BkConfirm(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val runAsTask: Boolean = true,
    val action: () -> Unit,
)

// ───────────────────────────── Screen ─────────────────────────────

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val motion = MaterialTheme.motionScheme

    val prefs = BackupRepository.prefs
    val task = BackupRepository.task
    val localBackup = BackupRepository.localBackup
    val running = task is BackupTask.Running

    var lastAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirm by remember { mutableStateOf<BkConfirm?>(null) }

    LaunchedEffect(Unit) {
        BackupRepository.dismissTask()
        BackupRepository.refreshLocalBackup(context)
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
            confirm = BkConfirm(
                title = "Replace current data?",
                message = "This replaces your current notes, settings and progress with the backup.",
                confirmLabel = "Restore",
            ) { BackupRepository.importFrom(context, uri) }
        }
    }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val signIn = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = signIn.getResult(ApiException::class.java)
            if (account != null && DriveBackup.hasAppDataScope(account)) {
                BackupRepository.onDriveConnected(account.email, account.photoUrl?.toString())
            } else {
                BackupRepository.onDriveConnectFailed(
                    "Drive permission was not granted. Try Connect again and allow access."
                )
            }
        } catch (e: ApiException) {
            // 12501 = the user closed the sign-in sheet: not an error.
            if (e.statusCode != 12501) CrashReporter.record("BackupScreen", e, "Google Sign-In failed (${e.statusCode})")
            BackupRepository.onDriveConnectFailed(
                if (e.statusCode == 12501) null // user cancelled
                else (e.message ?: "Google Sign-In failed (${e.statusCode})")
            )
        } catch (e: Exception) {
            CrashReporter.record("BackupScreen", e)
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
                .padding(top = 8.dp, bottom = 40.dp)
        ) {
            // ── Hero: status + live progress ──
            BackupHero(
                task = task,
                prefs = prefs,
                onBackupNow = { start { BackupRepository.backup(context) } },
                onExport = { exportLauncher.launch("ministry-assistant-backup.zip") },
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

            // ── Google Drive ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "backup-drive")) {
                BkHeader("Google Drive")
                Spacer(Modifier.height(10.dp))
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
                        BkRow(
                            pos = BkPos(first = true, last = true), tone = BkTone.Secondary,
                            icon = Icons.Outlined.CloudQueue, title = "Connect Google Drive",
                            subtitle = "Keep a safe copy in your Drive",
                            enabled = !BackupRepository.driveConnecting,
                            showChevron = !BackupRepository.driveConnecting,
                            onClick = { startDriveSignIn(context, driveSignInLauncher) },
                            trailing = if (BackupRepository.driveConnecting) loadingTrailing else null,
                        )
                    } else {
                        BkAccountPanel(
                            email = prefs.driveAccount,
                            photoUrl = prefs.drivePhoto,
                            corners = BkCorners(tl = true, tr = true, bl = true, br = true),
                            onDisconnect = { BackupRepository.disconnectDrive(context) },
                        )
                    }
                }
                BkFooter(
                    if (prefs.driveConnected)
                        "Backups go to a private app folder on Drive that only this app can see. No copy is kept on this device."
                    else
                        "Without Drive, your backup is kept as a single file on this device."
                )
            }

            Spacer(Modifier.height(BkSectionGap))

            // ── Restore: Drive when connected, otherwise the device backup ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "backup-restore")) {
                BkHeader("Restore")
                Spacer(Modifier.height(10.dp))
                val fromDrive = prefs.driveConnected
                val available = fromDrive || localBackup != null
                BkRow(
                    pos = BkPos(first = true, last = true), tone = BkTone.Tertiary,
                    icon = if (fromDrive) Icons.Outlined.CloudDownload else Icons.Outlined.Backup,
                    title = "Restore backup",
                    subtitle = when {
                        fromDrive -> "From your Google Drive copy"
                        localBackup != null -> "From this device · " +
                            DateUtils.formatDateTime(
                                context, localBackup.time,
                                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
                            ) + " · " + Formatter.formatShortFileSize(context, localBackup.bytes)
                        else -> "No backup to restore yet"
                    },
                    enabled = !running && available, showChevron = available,
                    onClick = {
                        confirm = BkConfirm(
                            title = "Replace current data?",
                            message = if (fromDrive) "This would replace your current data with the copy in Drive."
                            else "This would replace your current data with the backup on this device.",
                            confirmLabel = "Restore",
                        ) { BackupRepository.restoreBackup(context) }
                    },
                )
            }

            Spacer(Modifier.height(BkSectionGap))

            // ── Automatic backup ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "backup-auto")) {
                BkHeader("Automatic backup")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(BkGroupGap)) {
                    BkRow(
                        pos = BkPos(first = true, last = !prefs.autoBackup), tone = BkTone.Primary,
                        icon = Icons.Outlined.Schedule, title = "Back up automatically",
                        subtitle = if (prefs.autoBackup) prefs.autoSyncLabel() else "Off",
                        onClick = { BackupRepository.setAutoBackup(!prefs.autoBackup) },
                        trailing = { BkSwitch(prefs.autoBackup, BackupRepository::setAutoBackup) },
                    )
                    AnimatedVisibility(
                        visible = prefs.autoBackup,
                        enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                        exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(BkGroupGap)) {
                            BkRow(
                                pos = BkPos(first = false, last = true), tone = BkTone.Neutral,
                                icon = Icons.Outlined.Wifi, title = "Only on Wi-Fi",
                                subtitle = "Applies to Google Drive uploads",
                                onClick = { BackupRepository.setWifiOnly(!prefs.wifiOnly) },
                                trailing = { BkSwitch(prefs.wifiOnly, BackupRepository::setWifiOnly) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(BkSectionGap))

            // ── Export & import ──
            Column(Modifier.revealOnEnter(index = 4, playOnceKey = "backup-local")) {
                BkHeader("Export & import")
                Spacer(Modifier.height(10.dp))
                BkGroup {
                    BkRow(
                        pos = BkPos(first = true, last = false), tone = BkTone.Tertiary,
                        icon = Icons.Outlined.FileUpload, title = "Export data",
                        subtitle = "Save a backup file anywhere you like",
                        enabled = !running, showChevron = true,
                        onClick = {
                            launchSafely(context, "BackupScreen", "No file manager found to save the backup") {
                                exportLauncher.launch(BackupRepository.suggestedFileName())
                            }
                        },
                    )
                    BkRow(
                        pos = BkPos(first = false, last = true), tone = BkTone.Tertiary,
                        icon = Icons.Outlined.FileDownload, title = "Import data",
                        subtitle = "Restore from a backup file",
                        enabled = !running, showChevron = true,
                        onClick = {
                            launchSafely(context, "BackupScreen", "No file manager found to pick a backup") {
                                importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            }
                        },
                    )
                }
                BkFooter("Backups include your notes, settings and progress.")
            }
        }
    }

    confirm?.let { request ->
        val destructive = request.confirmLabel == "Delete"
        AlertDialog(
            onDismissRequest = { confirm = null },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = { Text(request.title, textAlign = TextAlign.Center) },
            text = { Text(request.message, textAlign = TextAlign.Center) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = null
                        if (request.runAsTask) start(request.action) else request.action()
                    },
                    colors = if (destructive) {
                        ButtonDefaults.textButtonColors(contentColor = colors.error)
                    } else ButtonDefaults.textButtonColors(),
                    shapes = ButtonDefaults.shapes(),
                ) { Text(request.confirmLabel) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }, shapes = ButtonDefaults.shapes()) { Text("Cancel") } },
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
    onExport: () -> Unit,
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
        // AnimatedContent also animates the card's SIZE between phases, so it grows/shrinks smoothly.
        AnimatedContent(
            targetState = task,
            contentKey = { phaseOf(it) },
            transitionSpec = {
                (fadeIn(Motion.effectsDefault) + scaleIn(Motion.spatialDefault, initialScale = 0.96f)) togetherWith
                    fadeOut(Motion.effectsFast)
            },
            modifier = Modifier.background(
                Brush.verticalGradient(listOf(Color.Transparent, content.copy(alpha = 0.07f)))
            ),
            label = "heroPhase",
        ) { t ->
            when (t) {
                is BackupTask.Idle -> {
                    val hasBackup = prefs.lastBackupMillis != 0L
                    val toDrive = prefs.driveConnected
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {


                            BkStatusGlyph(protected = hasBackup)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (hasBackup) "Your data is backed up" else "No backup yet",
                                    style = typography.titleMediumEmphasized,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (hasBackup) listOfNotNull(
                                        prefs.lastBackupTarget,
                                        prefs.lastBackupLabel(),
                                        Formatter.formatShortFileSize(context, prefs.lastBackupBytes),
                                    ).joinToString(" · ")
                                    else if (toDrive) "Saved privately to your Google Drive"
                                    else "Protect your notes and progress",
                                    style = typography.bodySmall,
                                    color = content.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        // Primary action + secondary options as M3 Expressive SplitButton.
                        // Leading = run backup. Trailing expands Export (and related actions).
                        var splitMenuOpen by remember { mutableStateOf(false) }
                        val size = SplitButtonDefaults.MediumContainerHeight
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SplitButtonLayout(
                                modifier = Modifier.fillMaxWidth(),
                                leadingButton = {
                                    SplitButtonDefaults.LeadingButton(
                                        onClick = onBackupNow,
                                        modifier = Modifier
                                            .heightIn(size),
                                        shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                                        contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
                                    ) {
                                        Icon(
                                            if (toDrive) Icons.Outlined.CloudUpload else Icons.Outlined.Backup,
                                            contentDescription = null,
                                            modifier = Modifier.size(
                                                SplitButtonDefaults.leadingButtonIconSizeFor(size)
                                            ),
                                        )
                                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                        Text(
                                            when {
                                                toDrive -> "Back up to Google Drive"
                                                hasBackup -> "Back up to this device"
                                                else -> "Create first backup"
                                            },
                                            style = typography.labelLargeEmphasized,
                                            maxLines = 1,
                                        )
                                    }
                                },
                                trailingButton = {
                                    SplitButtonDefaults.TrailingButton(
                                        checked = splitMenuOpen,
                                        onCheckedChange = { splitMenuOpen = it },
                                        modifier = Modifier.heightIn(size),
                                        shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                                        contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                                    ) {
                                        val rotation by animateFloatAsState(
                                            targetValue = if (splitMenuOpen) 180f else 0f,
                                            label = "backupSplitRotation",
                                        )
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "More backup options",
                                            modifier = Modifier
                                                .size(SplitButtonDefaults.trailingButtonIconSizeFor(size))
                                                .graphicsLayer { rotationZ = rotation },
                                        )
                                    }
                                },
                            )
                            DropdownMenu(
                                expanded = splitMenuOpen,
                                onDismissRequest = { splitMenuOpen = false },
                                modifier = Modifier.widthIn(min = 220.dp),
                                shape = MaterialTheme.shapes.largeIncreased,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export data") },
                                    onClick = {
                                        splitMenuOpen = false
                                        onExport()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                    },
                                )
                            }
                        }
                    }
                }

                is BackupTask.Running -> RunningContent(t, onCancel)

                is BackupTask.Done -> Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BkPopTile(Icons.Filled.Check, colors.primary, colors.onPrimary, shake = false)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (t.kind == BackupKind.Backup) "Backup complete" else "Restore complete",
                                style = typography.titleMediumEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                t.message,
                                style = typography.bodySmall,
                                color = content.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!t.needsRestart) {
                            Button(
                                onClick = onDismiss,
                                shapes = ButtonDefaults.shapes(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.height(40.dp),
                            ) { Text("Done", style = typography.labelLargeEmphasized) }
                        }
                    }
                    if (t.needsRestart) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = onDismiss,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                            ) { Text("Later") }
                            Button(
                                onClick = onRestart,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                            ) { Text("Restart app", style = typography.labelLargeEmphasized) }
                        }
                    }
                }

                is BackupTask.Failed -> Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BkPopTile(Icons.Outlined.ErrorOutline, colors.error, colors.onError, shake = true)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Something went wrong",
                                style = typography.titleMediumEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                t.message,
                                style = typography.bodySmall,
                                color = content.copy(alpha = 0.8f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shapes = ButtonDefaults.shapes(),
                            border = BorderStroke(1.dp, content.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                        ) { Text("Dismiss") }
                        Button(
                            onClick = onRetry,
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.error,
                                contentColor = colors.onError,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                        ) { Text("Try again", style = typography.labelLargeEmphasized) }
                    }
                }
            }
        }
    }
}

/** Compact cookie glyph. When protected it breathes gently and wears a small check badge. */
@Composable
private fun BkStatusGlyph(protected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val breathe by rememberInfiniteTransition(label = "glyphBreathe").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glyphScale",
    )
    Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = colors.primary,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    val s = if (protected) breathe else 1f
                    scaleX = s
                    scaleY = s
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (protected) Icons.Outlined.CloudDone else Icons.Outlined.Backup,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        if (protected) {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = colors.tertiary, modifier = Modifier.size(16.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = colors.onTertiary,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact running state: one row (indicator · title + live step label · cancel) and a segmented
 * step bar underneath. Every animation is still here, just packed into a short card.
 */
@Composable
private fun RunningContent(t: BackupTask.Running, onCancel: () -> Unit) {
    val typography = MaterialTheme.typography
    val determinate = t.progress != null
    val animated by animateFloatAsState(
        targetValue = t.progress ?: 0f,
        animationSpec = Motion.effectsDefault,
        label = "heroProgress",
    )
    val stepIndex = t.stepIndex.coerceIn(0, t.steps.lastIndex.coerceAtLeast(0))

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    if (!det) {
                        LoadingIndicator(Modifier.size(56.dp))
                    } else {
                        CircularWavyProgressIndicator(
                            progress = { animated },
                            modifier = Modifier.size(64.dp),
                        )
                        RollingCount(count = (animated * 100).toInt()) { pct ->
                            Text("$pct%", style = typography.labelMediumEmphasized)
                        }
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${t.kind.title} " + (if (t.kind == BackupKind.Backup) "to " else "from ") + t.target,
                    style = typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // The current step swaps with a small vertical slide.
                AnimatedContent(
                    targetState = stepIndex,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn(Motion.effectsFast)) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut(Motion.effectsFast))
                    },
                    label = "stepLabel",
                ) { i ->
                    Text(
                        t.steps.getOrNull(i).orEmpty(),
                        style = typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onCancel, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel")
            }
        }

        Box(Modifier.padding(end = 8.dp)) { BkStepBar(count = t.steps.size, active = stepIndex) }
    }
}

/** One segment per step: finished = solid, active = gently pulsing, pending = faint. */
@Composable
private fun BkStepBar(count: Int, active: Int) {
    val colors = MaterialTheme.colorScheme
    val ink = LocalContentColor.current
    val pulse by rememberInfiniteTransition(label = "stepPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stepPulseAlpha",
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { i ->
            val color by animateColorAsState(
                if (i <= active) colors.primary else ink.copy(alpha = 0.18f),
                MaterialTheme.motionScheme.defaultEffectsSpec(), label = "stepSeg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .alpha(if (i == active) pulse else 1f)
                    .background(color, CircleShape)
            )
        }
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
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                val t = wobble.value
                translationX = sin(t * 4f * 2f * PI.toFloat()) * (1f - t) * 10.dp.toPx()
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(28.dp))
        }
    }
}

// ───────────────────────────── List building blocks ─────────────────────────────
// (Same look as Settings / About: grouped rows, corners morph on press, cookie icon tile.)

private enum class BkTone { Primary, Secondary, Tertiary, Neutral, Error }

private data class BkPos(val first: Boolean, val last: Boolean)

/** Which of the four corners are the group's outer (large) corners. */
private data class BkCorners(val tl: Boolean, val tr: Boolean, val bl: Boolean, val br: Boolean) {
    constructor(pos: BkPos) : this(pos.first, pos.first, pos.last, pos.last)
}

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
        modifier = Modifier.padding(start = 8.dp),
    )
}

/** Quiet footnote under a group, in the manner of an inset-grouped list footer. */
@Composable
private fun BkFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp),
    )
}

@Composable
private fun BkGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BkGroupGap)) { content() }
}

@Composable
private fun rememberBkShape(corners: BkCorners, pressed: Boolean): Shape {
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    fun target(outer: Boolean) = if (pressed) BkPressed else if (outer) BkOuter else BkInner
    val tl by animateDpAsState(target(corners.tl), spec, label = "bkTL")
    val tr by animateDpAsState(target(corners.tr), spec, label = "bkTR")
    val bl by animateDpAsState(target(corners.bl), spec, label = "bkBL")
    val br by animateDpAsState(target(corners.br), spec, label = "bkBR")
    // Springs with low damping can overshoot below zero: clamp so the shape stays valid.
    return RoundedCornerShape(
        topStart = tl.coerceAtLeast(0.dp), topEnd = tr.coerceAtLeast(0.dp),
        bottomEnd = br.coerceAtLeast(0.dp), bottomStart = bl.coerceAtLeast(0.dp),
    )
}

@Composable
private fun BkIconTile(icon: ImageVector, tone: BkTone, size: Dp = 44.dp) {
    val (tile, tileContent) = tone.tileColors()
    Surface(
        shape = MaterialShapes.Cookie6Sided.toShape(),
        color = tile,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tileContent, modifier = Modifier.size(size / 2 + 2.dp))
        }
    }
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
    val shape = rememberBkShape(BkCorners(pos), pressed)
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
            leadingContent = { BkIconTile(icon, tone) },
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

/** Loads a remote image (e.g. the Google profile photo) without extra dependencies. */
@Composable
private fun rememberRemoteImage(url: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url) {
        value = if (url.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    return bitmap
}

/** Round-ish icon button: soft tonal background, corners + colour morph and a press scale on tap. */
@Composable
private fun BkIconAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container by animateColorAsState(
        if (pressed) colors.errorContainer else colors.surfaceContainerHighest,
        motion.fastEffectsSpec(), label = "actionBg",
    )
    val tint by animateColorAsState(
        if (pressed) colors.onErrorContainer else colors.onSurfaceVariant,
        motion.fastEffectsSpec(), label = "actionTint",
    )
    val corner by animateDpAsState(if (pressed) 12.dp else 20.dp, motion.fastSpatialSpec(), label = "actionCorner")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        interactionSource = interaction,
        modifier = Modifier
            .size(40.dp)
            .themePressScale(interaction, pressedScale = 0.9f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/** Connected Google account: profile photo in a cookie shape, email on top, status underneath. */
@Composable
private fun BkAccountPanel(email: String?, photoUrl: String?, corners: BkCorners, onDisconnect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shape = rememberBkShape(corners, pressed = false)
    val initial = email?.firstOrNull { it.isLetterOrDigit() }?.uppercase()
    val photo = rememberRemoteImage(photoUrl)

    Surface(shape = shape, color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    email ?: "Google account",
                    style = typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = colors.primary, modifier = Modifier.size(6.dp)) {}
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Google Drive · Connected",
                        style = typography.labelSmall,
                        color = colors.primary,
                        maxLines = 1,
                    )
                }
            },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = colors.secondaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            photo != null -> Image(
                                bitmap = photo,
                                contentDescription = "Google profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            initial != null -> Text(
                                initial,
                                style = typography.titleMediumEmphasized,
                                color = colors.onSecondaryContainer,
                            )
                            else -> Icon(
                                Icons.Outlined.CloudDone,
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            },
            trailingContent = {
                BkIconAction(Icons.Outlined.LinkOff, "Disconnect Google Drive", onDisconnect)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Non-clickable group row that hosts custom controls (button groups etc.). */
@Composable
private fun BkPanel(pos: BkPos, content: @Composable () -> Unit) {
    val shape = rememberBkShape(BkCorners(pos), pressed = false)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) { content() }
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
