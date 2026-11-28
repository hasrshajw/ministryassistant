@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.DriveBackup
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.data.NotesRepository
import com.harsha.myapplication.telemetry.CrashReporter
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.components.startDriveSignIn
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.rememberAppFeedback
import com.harsha.myapplication.ui.theme.revealOnEnter
import kotlinx.coroutines.launch

// ── Design tokens (same family as About / Notifications / Permissions) ─────
private val ScreenMargin = 16.dp
private val SectionGap = 28.dp
private val CardRadius = 28.dp
private val GroupInner = 4.dp
private val GroupGap = 2.dp

private val ColorEffects = spring<Color>(dampingRatio = 1f, stiffness = 1600f)

// ───────────────────────────── Screen ─────────────────────────────
@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val feedback = rememberAppFeedback()

    // Real Google account state — shared with Backup / Drive
    val prefs = BackupRepository.prefs
    val driveConnected = prefs.driveConnected
    val accountEmail = prefs.driveAccount
    val accountName = remember(driveConnected, accountEmail) {
        DriveBackup.lastSignedInAccount(context)?.displayName
            ?: accountEmail?.substringBefore("@")
            ?: ""
    }

    // Live counts from real repositories
    val noteCount = NotesRepository.notes.size
    val peopleCount = remember(context) {
        CrashReporter.attempt("ProfileScreen") { com.harsha.myapplication.data.PeopleRepository.load(context).size }.getOrDefault(0)
    }
    val totalMinutes = remember { HoursRepository.entries.sumOf { it.minutes } }

    var showSignOutSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BackupRepository.refreshDriveSession(context)
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
                    "Drive permission was not granted. Try again and allow access."
                )
            }
        } catch (e: ApiException) {
            // 12501 = the user closed the sign-in sheet: not an error.
            if (e.statusCode != 12501) CrashReporter.record("ProfileScreen", e, "Google Sign-In failed (${e.statusCode})")
            BackupRepository.onDriveConnectFailed(
                if (e.statusCode == 12501) null // user cancelled
                else (e.message ?: "Google Sign-In failed (${e.statusCode})")
            )
        } catch (e: Exception) {
            CrashReporter.record("ProfileScreen", e)
            BackupRepository.onDriveConnectFailed(e.message ?: "Google Sign-In failed")
        }
    }

    fun signIn() {
        feedback.click()
        startDriveSignIn(context, driveSignInLauncher)
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Profile & Account",
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
                .padding(horizontal = ScreenMargin)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            // ── Hero ──
            Box(Modifier.revealOnEnter(index = 0, playOnceKey = "profile-hero")) {
                if (driveConnected && !accountEmail.isNullOrBlank()) {
                    ProfileHero(name = accountName.ifBlank { "Signed in" }, email = accountEmail)
                } else {
                    SignInHero(connecting = BackupRepository.driveConnecting, onSignIn = ::signIn)
                }
            }

            // ── Activity ──
            Spacer(Modifier.height(SectionGap))
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "profile-stats")) {
                SectionHeader("Your activity")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        icon = Icons.Outlined.EditNote,
                        tileShape = MaterialShapes.Cookie9Sided.toShape(),
                        label = "Notes",
                        target = noteCount,
                        format = { it.toString() },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatTile(
                        icon = Icons.Outlined.Group,
                        tileShape = MaterialShapes.Clover4Leaf.toShape(),
                        label = "People",
                        target = peopleCount,
                        format = { it.toString() },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatTile(
                        icon = Icons.Outlined.Schedule,
                        tileShape = MaterialShapes.Cookie6Sided.toShape(),
                        label = "Hours",
                        target = totalMinutes,
                        format = { minutes ->
                            val h = minutes / 60
                            val m = minutes % 60
                            if (m == 0) "${h}h" else "${h}h ${m}m"
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }

            // ── Cloud backup ──
            Spacer(Modifier.height(SectionGap))
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "profile-account")) {
                SectionHeader("Cloud backup")
                Spacer(Modifier.height(10.dp))

                val rows = buildList {
                    add(
                        BackupRow(
                            icon = if (driveConnected) Icons.Outlined.CloudDone else Icons.Outlined.CloudQueue,
                            tileShape = MaterialShapes.Cookie9Sided.toShape(),
                            title = if (driveConnected) "Google Drive connected" else "Connect Google Drive",
                            subtitle = if (driveConnected) {
                                if (prefs.lastBackupMillis == 0L) "No backup yet"
                                else "Last backup ${prefs.lastBackupLabel()}"
                            } else {
                                "Sign in to back up privately to Drive"
                            },
                            highlight = driveConnected,
                            chevron = !driveConnected,
                            onClick = if (!driveConnected) ({ signIn() }) else null,
                        )
                    )
                    if (driveConnected) {
                        add(
                            BackupRow(
                                icon = Icons.Outlined.CloudUpload,
                                tileShape = MaterialShapes.Clover4Leaf.toShape(),
                                title = "Backup status",
                                subtitle = if (prefs.autoBackup) {
                                    "Auto-syncs after every change"
                                } else {
                                    "Manual backups only"
                                },
                            )
                        )
                    }
                    add(
                        BackupRow(
                            icon = Icons.Outlined.Shield,
                            tileShape = MaterialShapes.Cookie6Sided.toShape(),
                            title = "Private app data",
                            subtitle = "Backups stay in the Drive app folder. Only this app can see them.",
                        )
                    )
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    rows.forEachIndexed { index, row ->
                        BackupListRow(row = row, index = index, count = rows.size)
                    }
                }
            }

            // ── Sign out ──
            if (driveConnected) {
                Spacer(Modifier.height(SectionGap))
                Box(Modifier.revealOnEnter(index = 3, playOnceKey = "profile-signout")) {
                    val height = ButtonDefaults.MediumContainerHeight
                    val source = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = { feedback.click(); showSignOutSheet = true },
                        interactionSource = source,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.errorContainer,
                            contentColor = colors.onErrorContainer,
                        ),
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.contentPaddingFor(height),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = height)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
                        )
                        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                        Text("Sign out", style = ButtonDefaults.textStyleFor(height))
                    }
                }
            }
        }
    }

    if (showSignOutSheet) {
        SignOutSheet(
            onDismiss = { showSignOutSheet = false },
            onConfirm = {
                feedback.confirm()
                BackupRepository.disconnectDrive(context)
            }
        )
    }
}

// ───────────────────────────── Building blocks ─────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun rememberGroupShape(index: Int, count: Int, innerTarget: Dp): Shape {
    val inner by animateDpAsState(innerTarget, Motion.spatialFastDp, label = "groupInner")
    val safeInner = inner.coerceAtLeast(0.dp)
    val top = if (index == 0) CardRadius else safeInner
    val bottom = if (index == count - 1) CardRadius else safeInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun ShapeTile(
    shape: Shape,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Soft gradient used by both hero variants. */
@Composable
private fun heroBrush(): Brush {
    val c = MaterialTheme.colorScheme
    return Brush.linearGradient(
        listOf(c.primaryContainer, lerp(c.primaryContainer, c.tertiaryContainer, 0.65f))
    )
}

/**
 * Avatar: a cookie shape that turns very slowly behind a still initial / icon.
 * Quiet, but it makes the page feel alive.
 */
@Composable
private fun Avatar(size: Dp = 64.dp, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spin by rememberInfiniteTransition(label = "avatarSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "avatarSpinValue"
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        // Halo
        Box(
            Modifier
                .fillMaxSize()
                .rotate(-spin * 0.5f)
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(colors.primary.copy(alpha = 0.16f))
        )
        // Main shape
        Box(
            Modifier
                .size(size * 0.78f)
                .rotate(spin)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(colors.primary)
        )
        content()
    }
}

/** Compact hero: avatar on the left, name + email on the right, a small tick badge at the end. */
@Composable
private fun ProfileHero(name: String, email: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        contentColor = colors.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .background(heroBrush())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(size = 64.dp) {
                Text(
                    name.trim().take(1).uppercase().ifBlank { "?" },
                    style = typography.titleLargeEmphasized,
                    color = colors.onPrimary
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name,
                    style = typography.titleLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        email,
                        style = typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // "Signed in" badge
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Signed in",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SignInHero(connecting: Boolean, onSignIn: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val height = 40.dp

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        contentColor = colors.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .background(heroBrush())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Avatar(size = 64.dp) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Not signed in", style = typography.titleLargeEmphasized, maxLines = 1)
                    Text(
                        "Back up your notes privately with Google.",
                        style = typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Button(
                onClick = onSignIn,
                enabled = !connecting,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
            ) {
                if (connecting) {
                    LoadingIndicator(
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                        color = colors.onPrimary,
                    )
                    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                }
                Text(
                    if (connecting) "Connecting…" else "Sign in with Google",
                    style = ButtonDefaults.textStyleFor(height),
                    maxLines = 1
                )
            }
        }
    }
}

/** Counts up from 0 the first time the page opens. */
@Composable
private fun rememberCountUp(target: Int): Int {
    var start by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) { start = target }
    val value by animateIntAsState(
        targetValue = start,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "countUp"
    )
    return value
}

@Composable
private fun StatTile(
    icon: ImageVector,
    tileShape: Shape,
    label: String,
    target: Int,
    format: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shown = rememberCountUp(target)

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShapeTile(tileShape, colors.secondaryContainer, size = 40.dp) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    format(shown),
                    style = typography.titleLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(label, style = typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

private data class BackupRow(
    val icon: ImageVector,
    val tileShape: Shape,
    val title: String,
    val subtitle: String,
    val highlight: Boolean = false,
    val chevron: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun BackupListRow(row: BackupRow, index: Int, count: Int) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val clickable = row.onClick != null
    val shape = rememberGroupShape(index, count, if (pressed && clickable) 20.dp else GroupInner)

    val tile by animateColorAsState(
        if (row.highlight) colors.primary else colors.secondaryContainer,
        ColorEffects, label = "rowTile"
    )
    val tileContent by animateColorAsState(
        if (row.highlight) colors.onPrimary else colors.onSecondaryContainer,
        ColorEffects, label = "rowTileContent"
    )

    val body: @Composable () -> Unit = {
        ListItem(
            headlineContent = {
                Text(row.title, style = typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(row.subtitle, style = typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = {
                ShapeTile(row.tileShape, tile, size = 44.dp) {
                    Icon(row.icon, contentDescription = null, tint = tileContent, modifier = Modifier.size(22.dp))
                }
            },
            trailingContent = if (row.chevron) {
                {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant
                    )
                }
            } else null,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (row.onClick != null) {
        Surface(
            onClick = row.onClick,
            interactionSource = source,
            shape = shape,
            color = colors.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(source, pressedScale = 0.98f)
        ) { body() }
    } else {
        Surface(
            shape = shape,
            color = colors.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) { body() }
    }
}

/** Sign-out confirmation as a bottom sheet (same pattern as the Gen-Z setup sheet). */
@Composable
private fun SignOutSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val height = ButtonDefaults.MediumContainerHeight

    fun close(after: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss(); after() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShapeTile(MaterialShapes.Cookie9Sided.toShape(), colors.errorContainer, size = 56.dp) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = colors.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Sign out?", style = typography.titleLargeEmphasized)
                    Text(
                        "Disconnect Google Drive from this app.",
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            Text(
                "Your data stays on this device. You can reconnect later to restore or upload backups.",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { close() },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(height),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = height)
                ) { Text("Cancel", style = ButtonDefaults.textStyleFor(height), maxLines = 1) }
                Button(
                    onClick = { close(after = onConfirm) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    ),
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(height),
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = height)
                ) { Text("Sign out", style = ButtonDefaults.textStyleFor(height), maxLines = 1) }
            }
        }
    }
}

