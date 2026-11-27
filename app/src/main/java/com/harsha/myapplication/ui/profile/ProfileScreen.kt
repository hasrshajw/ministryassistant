@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.profile

import com.harsha.myapplication.ui.components.startDriveSignIn
import com.harsha.myapplication.telemetry.CrashReporter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.DriveBackup
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.data.NotesRepository
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter

private val ScreenMargin = 16.dp
private val SectionGap = 24.dp
private val GroupGap = 2.dp
private val GroupOuter = 28.dp
private val GroupInner = 4.dp
private val PressedCorner = 20.dp

@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val context = LocalContext.current

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
    val hoursLabel = remember {
        val total = HoursRepository.entries.sumOf { it.minutes }
        val h = total / 60
        val m = total % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    }

    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

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
                .padding(bottom = 32.dp)
        ) {
            // ── Profile Header Card ──
            if (driveConnected && !accountEmail.isNullOrBlank()) {
                ProfileHeaderCard(
                    name = accountName.ifBlank { "Signed in" },
                    email = accountEmail,
                    modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "profile-header")
                )
            } else {
                SignInPromptCard(
                    connecting = BackupRepository.driveConnecting,
                    onSignIn = {
                        startDriveSignIn(context, driveSignInLauncher)
                    },
                    modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "profile-signin")
                )
            }

            Spacer(Modifier.height(SectionGap))

            // ── Activity Stats (real data) ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "profile-stats")) {
                SectionHeader("Activity Overview")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Notes",
                        value = noteCount.toString(),
                        subtitle = "Created",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "People",
                        value = peopleCount.toString(),
                        subtitle = "Contacts",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Hours",
                        value = hoursLabel,
                        subtitle = "Ministry",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Account & Backup ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "profile-account")) {
                SectionHeader("Account & Backup")
                Spacer(Modifier.height(8.dp))
                GroupedList(
                    items = buildList {
                        add(
                            ProfileRowItem(
                                icon = if (driveConnected) Icons.Outlined.CloudDone else Icons.Outlined.CloudQueue,
                                title = if (driveConnected) "Google Drive connected" else "Connect Google Drive",
                                subtitle = if (driveConnected) {
                                    if (prefs.lastBackupMillis == 0L) "No backup yet"
                                    else "Last backup ${prefs.lastBackupLabel()}"
                                } else {
                                    "Sign in to back up privately to Drive"
                                },
                                showChevron = !driveConnected,
                            )
                        )
                        if (driveConnected) {
                            add(
                                ProfileRowItem(
                                    icon = Icons.Outlined.CloudUpload,
                                    title = "Backup status",
                                    subtitle = if (prefs.autoBackup) {
                                        "Auto-backup ${prefs.frequency.label.lowercase()}"
                                    } else {
                                        "Manual backups only"
                                    },
                                    showChevron = false,
                                )
                            )
                        }
                        add(
                            ProfileRowItem(
                                icon = Icons.Outlined.Shield,
                                title = "Private app data",
                                subtitle = "Backups stay in Drive app folder — only this app can see them",
                                showChevron = false,
                            )
                        )
                    }
                ) { item, pos ->
                    ProfileListItem(
                        pos = pos,
                        leadingIcon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = {
                            if (!driveConnected && item.title.startsWith("Connect")) {
                                startDriveSignIn(context, driveSignInLauncher)
                            }
                        },
                        trailing = {
                            if (item.showChevron) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }

            if (driveConnected) {
                Spacer(Modifier.height(SectionGap))

                Button(
                    onClick = { showSignOutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.errorContainer,
                        contentColor = colors.onErrorContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .revealOnEnter(index = 3, playOnceKey = "profile-signout")
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", style = typography.labelLarge)
                }
            }
        }

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out") },
                text = {
                    Text(
                        "Disconnect Google Drive? Local data stays on this device. " +
                            "You can reconnect later to restore or upload backups."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            BackupRepository.disconnectDrive(context)
                        }
                    ) {
                        Text("Sign Out", color = colors.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SignInPromptCard(
    connecting: Boolean,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Not signed in", style = typography.titleLargeEmphasized)
                    Text(
                        "Sign in with Google to back up your notes and progress privately",
                        style = typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                    )
                }
            }
            Button(
                onClick = onSignIn,
                enabled = !connecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (connecting) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colors.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (connecting) "Connecting…" else "Sign in with Google",
                    style = typography.labelLargeEmphasized,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String,
    email: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primary,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        name.take(1).uppercase().ifBlank { "?" },
                        style = typography.headlineMediumEmphasized,
                        color = colors.onPrimary
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        modifier = Modifier.size(14.dp),
                        tint = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                    Text(
                        email,
                        style = typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.primary.copy(alpha = 0.15f),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.primary
                    )
                    Text("Signed in", style = typography.labelSmall, color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurface
        ),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = typography.labelMedium, color = colors.onSurfaceVariant)
            Text(value, style = typography.titleLargeEmphasized)
            Text(subtitle, style = typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

private data class ProfileRowItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val showChevron: Boolean = false,
)

@Composable
private fun GroupedList(
    items: List<ProfileRowItem>,
    content: @Composable (ProfileRowItem, GroupPos) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        items.forEachIndexed { i, item ->
            content(
                item,
                GroupPos(first = i == 0, last = i == items.lastIndex)
            )
        }
    }
}

private data class GroupPos(val first: Boolean, val last: Boolean)

@Composable
private fun ProfileListItem(
    pos: GroupPos,
    leadingIcon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = groupShape(pos, pressed)

    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .themePressScale(interaction)
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(leadingIcon, contentDescription = null, tint = colors.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailing()
        }
    }
}

@Composable
private fun groupShape(pos: GroupPos, pressed: Boolean): Shape {
    val outer = GroupOuter
    val inner = if (pressed) PressedCorner else GroupInner
    return RoundedCornerShape(
        topStart = if (pos.first) outer else inner,
        topEnd = if (pos.first) outer else inner,
        bottomStart = if (pos.last) outer else inner,
        bottomEnd = if (pos.last) outer else inner,
    )
}
