@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

    var isEditingName by rememberSaveable { mutableStateOf(false) }
    var userName by rememberSaveable { mutableStateOf("HarshaVardhan") }
    var userEmail by rememberSaveable { mutableStateOf("harsha@example.com") }
    var autoSyncEnabled by rememberSaveable { mutableStateOf(true) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

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
            ProfileHeaderCard(
                name = userName,
                email = userEmail,
                onEditClick = { isEditingName = !isEditingName },
                modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "profile-header")
            )

            if (isEditingName) {
                Spacer(Modifier.height(12.dp))
                EditProfileCard(
                    initialName = userName,
                    initialEmail = userEmail,
                    onSave = { newName, newEmail ->
                        userName = newName
                        userEmail = newEmail
                        isEditingName = false
                    },
                    onCancel = { isEditingName = false }
                )
            }

            Spacer(Modifier.height(SectionGap))

            // ── Activity Stats ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "profile-stats")) {
                SectionHeader("Activity Overview")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Notes",
                        value = "24",
                        subtitle = "Created",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Streak",
                        value = "12d",
                        subtitle = "Daily Study",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Hours",
                        value = "48h",
                        subtitle = "Ministry",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Account Settings ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "profile-account")) {
                SectionHeader("Account & Sync")
                Spacer(Modifier.height(8.dp))
                GroupedList(
                    items = listOf(
                        ProfileRowItem(
                            icon = Icons.Outlined.CloudUpload,
                            title = "Cloud Backup & Sync",
                            subtitle = if (autoSyncEnabled) "Auto-sync active" else "Sync disabled",
                            hasSwitch = true,
                            switchState = autoSyncEnabled,
                            onSwitchToggle = { autoSyncEnabled = it }
                        ),
                        ProfileRowItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Push Notifications",
                            subtitle = if (notificationsEnabled) "On" else "Off",
                            hasSwitch = true,
                            switchState = notificationsEnabled,
                            onSwitchToggle = { notificationsEnabled = it }
                        ),
                        ProfileRowItem(
                            icon = Icons.Outlined.Lock,
                            title = "Security & Passcode",
                            subtitle = "Password & biometric lock",
                            showChevron = true
                        )
                    )
                ) { item, pos ->
                    ProfileListItem(
                        pos = pos,
                        leadingIcon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = {
                            if (item.hasSwitch) {
                                item.onSwitchToggle?.invoke(!item.switchState)
                            }
                        },
                        trailing = {
                            if (item.hasSwitch) {
                                Switch(
                                    checked = item.switchState,
                                    onCheckedChange = { item.onSwitchToggle?.invoke(it) }
                                )
                            } else if (item.showChevron) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Membership ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "profile-membership")) {
                SectionHeader("Membership & Plan")
                Spacer(Modifier.height(8.dp))
                GroupedList(
                    items = listOf(
                        ProfileRowItem(
                            icon = Icons.Outlined.Badge,
                            title = "Membership Tier",
                            subtitle = "Gen-Z Premium Pro",
                            showChevron = true
                        )
                    )
                ) { item, pos ->
                    ProfileListItem(
                        pos = pos,
                        leadingIcon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { },
                        trailing = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.primaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "PRO",
                                        style = typography.labelSmall,
                                        color = colors.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Danger Zone / Sign Out ──
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
                    .revealOnEnter(index = 4, playOnceKey = "profile-signout")
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

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out? Your local data will remain saved on this device.") },
                confirmButton = {
                    TextButton(onClick = { showSignOutDialog = false; onBack() }) {
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
private fun ProfileHeaderCard(
    name: String,
    email: String,
    onEditClick: () -> Unit,
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
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.take(2).uppercase(),
                            style = typography.headlineSmall,
                            color = colors.onPrimary
                        )
                    }
                }
                SmallFloatingActionButton(
                    onClick = onEditClick,
                    shape = CircleShape,
                    containerColor = colors.surface,
                    contentColor = colors.primary,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(name, style = typography.titleLarge)
                Text(
                    email,
                    style = typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Member since 2024",
                        style = typography.labelSmall,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileCard(
    initialName: String,
    initialEmail: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit Profile Details", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(name, email) }) {
                    Text("Save Changes")
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

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = typography.headlineMedium, color = colors.primary)
            Text(title, style = typography.labelLarge, color = colors.onSurface)
            Text(subtitle, style = typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

private data class ProfileRowItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val showChevron: Boolean = false,
    val hasSwitch: Boolean = false,
    val switchState: Boolean = false,
    val onSwitchToggle: ((Boolean) -> Unit)? = null
)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

private data class ProfileGroupPos(val first: Boolean, val last: Boolean)

@Composable
private fun rememberGroupShape(pos: ProfileGroupPos, pressed: Boolean): Shape {
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (pressed) PressedCorner else if (pos.first) GroupOuter else GroupInner,
        spec, label = "groupTopCorner"
    )
    val bottom by animateDpAsState(
        if (pressed) PressedCorner else if (pos.last) GroupOuter else GroupInner,
        spec, label = "groupBottomCorner"
    )
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun <T> GroupedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, pos: ProfileGroupPos) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        items.forEachIndexed { index, item ->
            row(item, ProfileGroupPos(first = index == 0, last = index == items.size - 1))
        }
    }
}

@Composable
private fun ProfileListItem(
    pos: ProfileGroupPos,
    leadingIcon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberGroupShape(pos, pressed)

    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainerLow,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.98f)
    ) {
        ListItem(
            headlineContent = {
                Text(title, style = typography.bodyLarge)
            },
            supportingContent = subtitle?.let {
                { Text(it, style = typography.bodyMedium) }
            },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = colors.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = colors.onSurface,
                supportingColor = colors.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
