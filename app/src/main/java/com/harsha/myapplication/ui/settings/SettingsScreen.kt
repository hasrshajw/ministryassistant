@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.notifications.AppPermissions
import com.harsha.myapplication.data.SecurityRepository
import com.harsha.myapplication.data.DriveBackup
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.rememberAppFeedback
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
    onOpenPermissions: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val feedback = rememberAppFeedback()
    fun go(action: () -> Unit): () -> Unit = { feedback.click(); action() }

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
            // ── Profile: compact + quiet (neutral surface) ──
            ProfileSignInCard(
                onClick = go(onOpenProfile),
                modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "settings-profile"),
            )

            Spacer(Modifier.height(20.dp))

            // ── Featured ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "settings-genz")) {
                SectionHeader("Featured")
                Spacer(Modifier.height(8.dp))
                GenZCard(onClick = go(onOpenGenZMode))
            }

            Spacer(Modifier.height(SectionGap))

            // ── Data: import Bible first, then backup ──
            SettingsSection(
                title = "Data",
                modifier = Modifier.revealOnEnter(index = 2, playOnceKey = "settings-data"),
                items = listOf(
                    RowItem(
                        icon = Icons.Outlined.FileDownload,
                        title = "Import Bible",
                        subtitle = if (bibles.isEmpty()) {
                            "Add a Bible to get started"
                        } else {
                            "${bibles.size} translation${if (bibles.size == 1) "" else "s"} installed"
                        },
                        onClick = go(onOpenBibles),
                    ),
                    RowItem(Icons.Outlined.Backup, "Backup & restore", backupSubtitle, go(onOpenBackup)),
                ),
            )

            Spacer(Modifier.height(SectionGap))

            // ── App ──
            SettingsSection(
                title = "App",
                modifier = Modifier.revealOnEnter(index = 3, playOnceKey = "settings-app"),
                items = listOf(
                    RowItem(Icons.Outlined.Palette, "Appearance", onClick = go(onOpenAppearance)),
                    RowItem(Icons.Outlined.Notifications, "Notifications", onClick = go(onOpenNotifications)),
                    RowItem(Icons.Outlined.Tune, "Customize App", onClick = go(onOpenCustomizeApp)),
                    RowItem(
                        Icons.Outlined.VerifiedUser,
                        "Permissions",
                        AppPermissions.summary(LocalContext.current),
                        go(onOpenPermissions),
                    ),
                ),
            )

            Spacer(Modifier.height(SectionGap))

            // ── Privacy (now below App) ──
            SettingsSection(
                title = "Privacy",
                modifier = Modifier.revealOnEnter(index = 4, playOnceKey = "settings-security"),
                items = listOf(
                    RowItem(
                        icon = Icons.Outlined.Lock,
                        title = "App lock",
                        subtitle = if (SecurityRepository.isEnabled) "On · PIN required" else "Protect names, addresses and notes",
                        onClick = go(onOpenSecurity),
                    ),
                ),
            )

            Spacer(Modifier.height(SectionGap))

            // ── Legal ──
            SettingsSection(
                title = "Legal documents",
                modifier = Modifier.revealOnEnter(index = 5, playOnceKey = "settings-legal"),
                items = listOf(
                    RowItem(Icons.Outlined.Description, "Terms and Conditions", onClick = go(onOpenTerms)),
                    RowItem(Icons.Outlined.Security, "Privacy Policy", onClick = go(onOpenPrivacy)),
                ),
            )

            Spacer(Modifier.height(SectionGap))

            // ── About ──
            SettingsSection(
                title = "About",
                modifier = Modifier.revealOnEnter(index = 6, playOnceKey = "settings-about"),
                items = listOf(
                    RowItem(Icons.Outlined.Info, "About", "App version and info", go(onOpenAbout), accent = true),
                ),
            )
        }
    }
}

private data class RowItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit,
    /** Special tile: tertiary colour + slowly spinning sunny shape (used by About). */
    val accent: Boolean = false,
)

/** Where a row sits in its group: decides which corners are large. */
private data class GroupPos(val first: Boolean, val last: Boolean)

// ───────────────────────────── Components ─────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    items: List<RowItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(title)
        Spacer(Modifier.height(8.dp))
        GroupedList(items = items) { item, pos ->
            SettingsListItem(
                pos = pos,
                leadingIcon = item.icon,
                title = item.title,
                subtitle = item.subtitle,
                onClick = item.onClick,
                accent = item.accent,
                showChevron = true,
            )
        }
    }
}

/** Gen-Z icon (from icon_genz.svg): rounded pentagon with two "eye" cut-outs. Tinted by the caller. */
private val GenZIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "GenZ",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(
            "M9.5255,2.0919C10.7171,0.3535 13.283,0.3535 14.4747,2.0919L16.5665,5.1446C16.6964,5.3341 16.8882,5.4731 17.1085,5.5382L20.6583,6.5851C22.68,7.1811 23.4728,9.6215 22.1876,11.2921L19.9317,14.2247C19.7916,14.4069 19.7184,14.6326 19.7247,14.8624L19.8263,18.5616C19.8838,20.6682 17.808,22.1764 15.8223,21.4708L12.335,20.2306C12.1184,20.1536 11.8817,20.1536 11.6651,20.2306L8.1778,21.4708C6.1921,22.1766 4.1163,20.6683 4.1739,18.5616L4.2755,14.8624C4.2817,14.6327 4.2085,14.4069 4.0684,14.2247L1.8126,11.2921C0.5274,9.6215 1.3201,7.1810 3.3419,6.5851L6.8917,5.5382C7.1121,5.4731 7.3037,5.3341 7.4337,5.1446L9.5255,2.0919ZM9,9.5V13.5H11V9.5H9ZM13,9.5V13.5H15V9.5H13Z"
        ).toNodes(),
        pathFillType = PathFillType.EvenOdd,
        fill = SolidColor(Color.Black),
    ).build()
}

/**
 * Icon tile whose background shape slowly rotates while the icon stays upright
 * (only the shape layer is transformed, in the draw phase: no recomposition).
 */
@Composable
private fun SpinningShapeTile(
    icon: ImageVector,
    shape: Shape,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    brush: Brush? = null,
    iconSize: Dp = 24.dp,
    spin: Boolean = true,
    clockwise: Boolean = true,
    periodMillis: Int = 24_000,
) {
    val transition = rememberInfiniteTransition(label = "tileSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (clockwise) 360f else -360f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "tileAngle",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { if (spin) rotationZ = angle }
                .then(if (brush != null) Modifier.background(brush, shape) else Modifier.background(container, shape))
        )
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
    }
}

/**
 * Gen-Z Mode: row-sized, but premium: soft gradient wash, hairline gradient border, and a
 * solid dark pentagon tile (static) holding the Gen-Z glyph.
 * Everything is theme-derived (light, dark, any palette).
 */
@Composable
private fun GenZCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) PressedCorner else GroupOuter,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "genZCorner",
    )

    // Solid dark tile: deep primary in light theme, deep green container + emerald glyph in dark theme.
    val isDark = colors.background.luminance() < 0.5f
    val tileBg = if (isDark) colors.primaryContainer else colors.primary
    val tileFg = if (isDark) colors.primary else colors.onPrimary
    val border = BorderStroke(
        1.dp,
        Brush.linearGradient(
            listOf(
                colors.primary.copy(alpha = 0.55f),
                colors.tertiary.copy(alpha = 0.55f),
            )
        )
    )
    val wash = Brush.linearGradient(
        colors = listOf(
            colors.primaryContainer.copy(alpha = 0.65f),
            colors.surfaceContainerLow,
            colors.tertiaryContainer.copy(alpha = 0.55f),
        ),
        start = Offset.Zero,
        end = Offset(900f, 260f),
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = colors.surfaceContainerLow,
        border = border,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.98f),
    ) {
        Box(Modifier.background(wash)) {
            ListItem(
                headlineContent = {
                    Text(
                        "Gen-Z Mode",
                        style = typography.bodyLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Text(
                        "Get your reminders with a little more fun",
                        style = typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = {
                    // Static pentagon tile (no rotation) holding the Gen-Z glyph
                    SpinningShapeTile(
                        icon = GenZIcon,
                        shape = MaterialShapes.Pentagon.toShape(),
                        container = tileBg,
                        content = tileFg,
                        size = 48.dp,
                        iconSize = 28.dp,
                        spin = false,
                    )
                },
                trailingContent = {
                    Surface(shape = CircleShape, color = colors.surfaceContainerLowest, modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = colors.onSurface,
                    supportingColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Compact account row on a neutral surface so it never competes with the Gen-Z hero.
 * Signed-in: avatar + name + email. Signed-out: short pitch + tonal Sign-in button.
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

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) PressedCorner else GroupOuter,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "profileCorner",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = colors.surfaceContainerLow,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.98f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (signedIn) {
                Surface(
                    shape = CircleShape,
                    color = colors.secondaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            initials,
                            style = typography.titleMediumEmphasized,
                            color = colors.onSecondaryContainer,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        name,
                        style = typography.titleMediumEmphasized,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        email,
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = colors.secondaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = colors.onSecondaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text("Welcome", style = typography.titleMediumEmphasized, color = colors.onSurface)
                    Text(
                        "Back up privately to Google Drive",
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 16.dp),
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
        style = MaterialTheme.typography.labelMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp)
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
    leadingIcon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    selected: Boolean = false,
    accent: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberGroupShape(pos, pressed)
    // One tile colour for every row: icons read as a family, colour is reserved for Gen-Z.
    val tileColor = colors.secondaryContainer
    val tileContent = colors.onSecondaryContainer

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
                        style = typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                if (accent) {
                    SpinningShapeTile(
                        icon = leadingIcon,
                        shape = MaterialShapes.Sunny.toShape(),
                        container = colors.tertiaryContainer,
                        content = colors.onTertiaryContainer,
                        clockwise = false,
                        periodMillis = 30_000,
                    )
                } else {
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
