@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.SecurityRepository
import com.harsha.myapplication.data.DriveBackup
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar

// ───────────────────────────── Expressive spacing & shape tokens ─────────────────────────────
private val ScreenMargin = 16.dp
private val SectionGap = 24.dp
private val GroupGap = 2.dp

// Grouped lists: big outer corners, tight inner joins. On press every corner morphs to PressedCorner.
private val GroupOuter = 28.dp
private val GroupInner = 4.dp
private val PressedCorner = 20.dp
private val ProfileCorner = 32.dp

/**
 * Kept for screens that still call `Modifier.pressScale(source)` from this package; it now shares the
 * app-wide spring from Motion.kt (the same one the Notes FAB uses).
 */
@Composable
internal fun Modifier.pressScale(source: MutableInteractionSource): Modifier = themePressScale(source)

@Composable
fun SettingsScreen(
    onOpenBibles: () -> Unit,
    onOpenAppearance: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenGenZMode: () -> Unit = {},
    onOpenCustomizeApp: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    val dataVersion = BibleData.version
    val bibles = remember(dataVersion) { BibleData.importedBibles }

    val backupPrefs = BackupRepository.prefs
    val backupSubtitle = if (backupPrefs.lastBackupMillis == 0L) {
        "Protect your notes and progress"
    } else {
        "Last backup ${backupPrefs.lastBackupLabel()}"
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Settings",
                navigationIcon = {
                    if (onClose != null) {
                        StudyBarIconButton(
                            onClick = onClose,
                            icon = Icons.Default.Close,
                            contentDescription = "Close",
                        )
                    }
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
            // ── Profile / Sign-in ──
            ProfileSignInCard(
                onClick = onOpenProfile,
                modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "settings-profile"),
            )

            Spacer(Modifier.height(SectionGap))

            // ── Featured (premium upsell) ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "settings-premium")) {
                SectionHeader("Featured")
                Spacer(Modifier.height(8.dp))
                PremiumUpgradeCard(
                    onClick = onOpenGenZMode,
                )
            }

            Spacer(Modifier.height(SectionGap))

            // ── Bibles (active translation + import, merged) ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "settings-bibles")) {
                SectionHeader("Bibles")
                Spacer(Modifier.height(8.dp))

                // Only the import entry lives here; installed Bibles are managed on the Import screen.
                GroupedList(
                    items = listOf(Unit),
                    modifier = Modifier.animateContentSize()
                ) { _, pos ->
                    SettingsListItem(
                        pos = pos,
                        tone = Tone.Tertiary,
                        leadingIcon = Icons.Outlined.FileDownload,
                        title = "Import Bible",
                        subtitle = if (bibles.isEmpty()) {
                            "Add a Bible to get started"
                        } else {
                            "${bibles.size} translation${if (bibles.size == 1) "" else "s"} installed"
                        },
                        onClick = onOpenBibles,
                        showChevron = true
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Data & backup ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "settings-data")) {
                SectionHeader("Data")
                Spacer(Modifier.height(8.dp))
                GroupedList(items = listOf(Unit)) { _, pos ->
                    SettingsListItem(
                        pos = pos,
                        tone = Tone.Secondary,
                        leadingIcon = Icons.Outlined.Backup,
                        title = "Backup & restore",
                        subtitle = backupSubtitle,
                        onClick = onOpenBackup,
                        showChevron = true
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Security (PIN lock) ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "settings-security")) {
                SectionHeader("Privacy")
                Spacer(Modifier.height(8.dp))
                GroupedList(items = listOf(Unit)) { _, pos ->
                    SettingsListItem(
                        pos = pos,
                        tone = Tone.Primary,
                        leadingIcon = Icons.Outlined.Lock,
                        title = "App lock",
                        subtitle = if (SecurityRepository.isEnabled) "On · PIN required" else "Protect names, addresses and notes",
                        onClick = onOpenSecurity,
                        showChevron = true
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── App ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "settings-app")) {
                SectionHeader("App")
                Spacer(Modifier.height(8.dp))
                GroupedList(
                    items = listOf(
                        RowItem(Icons.Outlined.Palette, "Appearance", Tone.Secondary),
                        RowItem(Icons.Outlined.Notifications, "Notifications", Tone.Tertiary),
                        RowItem(Icons.Outlined.Tune, "Customize App", Tone.Tertiary),
                    )
                ) { item, pos ->
                    SettingsListItem(
                        pos = pos,
                        tone = item.tone,
                        leadingIcon = item.icon,
                        title = item.title,
                        onClick = {
                            when (item.title) {
                                "Appearance" -> onOpenAppearance()
                                "Notifications" -> onOpenNotifications()
                                "Customize App" -> onOpenCustomizeApp()
                            }
                        },
                        showChevron = true
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Legal ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "settings-legal")) {
                SectionHeader("Legal documents")
                Spacer(Modifier.height(8.dp))
                GroupedList(
                    items = listOf(
                        RowItem(Icons.Outlined.Description, "Terms and Conditions", Tone.Neutral),
                        RowItem(Icons.Outlined.Security, "Privacy Policy", Tone.Neutral),
                    )
                ) { item, pos ->
                    SettingsListItem(
                        pos = pos,
                        tone = item.tone,
                        leadingIcon = item.icon,
                        title = item.title,
                        onClick = {
                            when (item.title) {
                                "Terms and Conditions" -> onOpenTerms()
                                "Privacy Policy" -> onOpenPrivacy()
                            }
                        },
                        showChevron = true
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── About ──
            Column(Modifier.revealOnEnter(index = 4, playOnceKey = "settings-about")) {
                SectionHeader("About")
                Spacer(Modifier.height(8.dp))
                GroupedList(items = listOf(Unit)) { _, pos ->
                    SettingsListItem(
                        pos = pos,
                        tone = Tone.Primary,
                        leadingIcon = Icons.Outlined.Info,
                        title = "About",
                        subtitle = "App version and info",
                        onClick = onOpenAbout,
                        showChevron = true
                    )
                }
            }
        }
    }
}

/** Colour family of a row's cookie-shaped icon tile. All pairs come from the theme (light + dark safe). */
private enum class Tone { Primary, Secondary, Tertiary, Neutral }

private data class RowItem(val icon: ImageVector, val title: String, val tone: Tone)

/** Where a row sits in its group: decides which corners are large. */
private data class GroupPos(val first: Boolean, val last: Boolean)

@Composable
private fun Tone.tileColors(): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (this) {
        Tone.Primary -> c.primary to c.onPrimary
        Tone.Secondary -> c.secondaryContainer to c.onSecondaryContainer
        Tone.Tertiary -> c.tertiaryContainer to c.onTertiaryContainer
        Tone.Neutral -> c.surfaceContainerHighest to c.onSurfaceVariant
    }
}

// ───────────────────────────── Components ─────────────────────────────


/**
 * Premium upgrade card — Material 3 Expressive.
 * Same height language as Bible list rows (44dp cookie + compact padding).
 * No shadows — stands out via primaryContainer + gradient wash.
 */
@Composable
private fun PremiumUpgradeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 20.dp else 28.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "premiumCorner",
    )
    val shape = RoundedCornerShape(corner)

    val baseWash = Brush.linearGradient(
        colors = listOf(
            colors.primary.copy(alpha = 0.18f),
            colors.primaryContainer.copy(alpha = 0.70f),
            colors.tertiaryContainer.copy(alpha = 0.32f),
        ),
        start = Offset.Zero,
        end = Offset(480f, 200f),
    )

    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.primaryContainer,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.97f),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(baseWash)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Match Bible row cookie size (44dp)
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = colors.primary,
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Gen-Z Mode",
                        style = typography.bodyLargeEmphasized,
                        color = colors.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Get your reminders with a little more fun",
                        style = typography.bodyMedium,
                        color = colors.onPrimaryContainer.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer.copy(alpha = 0.70f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Hero card: shows real Google account state from [BackupRepository] / Drive.
 * Signed-in: name + email from Google; tap opens Profile.
 * Signed-out: Sign in opens Profile (where real Google Sign-In runs).
 */
@Composable
private fun ProfileSignInCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        BackupRepository.refreshDriveSession(context)
    }

    val prefs = BackupRepository.prefs
    val signedIn = prefs.driveConnected && !prefs.driveAccount.isNullOrBlank()
    val email = prefs.driveAccount.orEmpty()
    val name = remember(signedIn, email) {
        if (!signedIn) ""
        else DriveBackup.lastSignedInAccount(context)?.displayName
            ?: email.substringBefore("@").ifBlank { "Signed in" }
    }
    val initials = remember(name, email) {
        when {
            name.isNotBlank() -> name.trim().split(Regex("\\s+")).take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                .joinToString("")
                .ifBlank { name.take(1).uppercase() }
            email.isNotBlank() -> email.take(1).uppercase()
            else -> "?"
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ProfileCorner),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
        ),
    ) {
        if (signedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            initials,
                            style = typography.titleMediumEmphasized,
                            color = colors.onPrimary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(name, style = typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        email,
                        style = typography.bodyMedium,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(20.dp),
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
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("Welcome", style = typography.titleLargeEmphasized)
                        Text(
                            "Sign in with Google to back up privately to Drive",
                            style = typography.bodyMedium,
                            color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Sign in", style = typography.labelLargeEmphasized)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun <T> GroupedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, pos: GroupPos) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        items.forEachIndexed { index, item ->
            row(item, GroupPos(first = index == 0, last = index == items.size - 1))
        }
    }
}

/** Group shape whose corners spring to [PressedCorner] while the row is pressed. */
@Composable
private fun rememberGroupShape(pos: GroupPos, pressed: Boolean): Shape {
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

/**
 * Expressive settings row: a [ListItem] in a [Surface] whose corners morph and which scales on press,
 * with a cookie-shaped tonal icon tile. [selected] rows switch to secondaryContainer.
 */
@Composable
private fun SettingsListItem(
    pos: GroupPos,
    tone: Tone,
    leadingIcon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberGroupShape(pos, pressed)
    val (tileColor, tileContent) = tone.tileColors()

    val container by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "settingsRowContainer"
    )
    val content = if (selected) colors.onSecondaryContainer else colors.onSurface
    val secondary = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.98f)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    title,
                    style = if (selected) typography.bodyLargeEmphasized else typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = subtitle?.let {
                {
                    Text(
                        it,
                        style = typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = tileColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = tileContent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = {
                when {
                    trailing != null -> trailing()
                    showChevron -> Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = content,
                supportingColor = secondary,
                trailingIconColor = secondary,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Selection indicator: ring that fills with primary, check pops in with a spring. */
@Composable
private fun SelectionMark(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val fill by animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "markFill"
    )
    val ring by animateColorAsState(
        if (selected) colors.primary else colors.outline,
        animationSpec = motion.defaultEffectsSpec(),
        label = "markRing"
    )
    Surface(
        shape = CircleShape,
        color = fill,
        border = BorderStroke(2.dp, ring),
        modifier = Modifier.size(26.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
                exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec())
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Active translation",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
