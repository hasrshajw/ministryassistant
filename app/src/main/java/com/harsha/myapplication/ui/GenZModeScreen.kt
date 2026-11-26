@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter

private val ScreenMargin = 16.dp
private val SectionGap = 24.dp
private val GroupGap = 2.dp
private val GroupOuter = 28.dp
private val GroupInner = 4.dp
private val PressedCorner = 20.dp

private val sampleSlangQuotes = listOf(
    "Fr fr this daily text is lowkey bussin 🗿🔥",
    "Don't sleep on today's ministry study, stay real 💀⚡️",
    "Streak on 14 days? That's major flex, keep cookin 👨‍🍳🔥",
    "No cap, your note organization is elite level 🧠✨",
    "Main character energy in your scripture reading today 📖💥",
    "Locked in for the daily text grind, zero distractions 🎯⚡️"
)

@Composable
fun GenZModeScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var masterGenZEnabled by rememberSaveable { mutableStateOf(true) }
    var noCapReminders by rememberSaveable { mutableStateOf(true) }
    var slangDailyText by rememberSaveable { mutableStateOf(true) }
    var ultraHaptics by rememberSaveable { mutableStateOf(true) }
    var flexStreakBadge by rememberSaveable { mutableStateOf(true) }

    var currentQuoteIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Gen-Z Mode ⚡️",
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
            // ── Hero Banner Card ──
            GenZHeroCard(
                enabled = masterGenZEnabled,
                onToggle = { masterGenZEnabled = !masterGenZEnabled },
                modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "genz-hero")
            )

            Spacer(Modifier.height(SectionGap))

            // ── Live Slang Preview ──
            AnimatedVisibility(visible = masterGenZEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .revealOnEnter(index = 1, playOnceKey = "genz-preview")
                ) {
                    SectionHeader("Live Vibe Preview 🎲")
                    Spacer(Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Gen-Z Slang Generator",
                                    style = typography.titleMedium,
                                    color = colors.onSecondaryContainer
                                )
                            }

                            Text(
                                text = sampleSlangQuotes[currentQuoteIndex],
                                style = typography.bodyLarge,
                                color = colors.onSecondaryContainer
                            )

                            Button(
                                onClick = {
                                    currentQuoteIndex = (currentQuoteIndex + 1) % sampleSlangQuotes.size
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(
                                    Icons.Default.Casino,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Cycle Quote")
                            }
                        }
                    }

                    Spacer(Modifier.height(SectionGap))
                }
            }

            // ── Gen-Z Feature Toggles ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "genz-toggles")) {
                SectionHeader("Vibe Settings")
                Spacer(Modifier.height(8.dp))
                GenZGroupedList(
                    items = listOf(
                        GenZRowItem(
                            icon = Icons.Outlined.NotificationsActive,
                            title = "No Cap Reminders",
                            subtitle = "Daily notifications in pure Gen-Z slang (fr fr 🧢)",
                            enabled = masterGenZEnabled && noCapReminders,
                            onToggle = { noCapReminders = it }
                        ),
                        GenZRowItem(
                            icon = Icons.Outlined.Forum,
                            title = "Slang Daily Text",
                            subtitle = "Expressive scripture captions & daily motivation",
                            enabled = masterGenZEnabled && slangDailyText,
                            onToggle = { slangDailyText = it }
                        ),
                        GenZRowItem(
                            icon = Icons.Outlined.Vibration,
                            title = "Ultra Haptic Vibrations",
                            subtitle = "Feel hyper-expressive tactile feedback",
                            enabled = masterGenZEnabled && ultraHaptics,
                            onToggle = { ultraHaptics = it }
                        ),
                        GenZRowItem(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Flex Streak Badges",
                            subtitle = "Show flame 🔥 and 🗿 badges on study streak",
                            enabled = masterGenZEnabled && flexStreakBadge,
                            onToggle = { flexStreakBadge = it }
                        ),
                    )
                ) { item, pos ->
                    GenZListItem(
                        pos = pos,
                        leadingIcon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        checked = item.enabled,
                        onCheckedChange = { if (masterGenZEnabled) item.onToggle(it) },
                        enabled = masterGenZEnabled
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Badges & Achievements ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "genz-badges")) {
                SectionHeader("Unlocked Vibe Badges 🏆")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VibeBadgeCard(
                        title = "Vibe Master",
                        emoji = "🗿",
                        description = "100% Locked In",
                        modifier = Modifier.weight(1f)
                    )
                    VibeBadgeCard(
                        title = "Study Grind",
                        emoji = "⚡️",
                        description = "14 Day Streak",
                        modifier = Modifier.weight(1f)
                    )
                    VibeBadgeCard(
                        title = "No Slump Zone",
                        emoji = "🔥",
                        description = "Elite Focus",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GenZHeroCard(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val baseWash = Brush.linearGradient(
        colors = listOf(
            colors.primaryContainer,
            colors.tertiaryContainer.copy(alpha = 0.8f),
            colors.secondaryContainer.copy(alpha = 0.6f)
        ),
        start = Offset.Zero,
        end = Offset(500f, 300f)
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, colors.primary.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(baseWash)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = MaterialShapes.Cookie9Sided.toShape(),
                        color = colors.primary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.ElectricBolt,
                                contentDescription = null,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Gen-Z Mode ⚡️",
                            style = typography.headlineSmall,
                            color = colors.onPrimaryContainer
                        )
                        Text(
                            if (enabled) "Mode Active: Fully Loaded 🔥" else "Mode Paused",
                            style = typography.bodyMedium,
                            color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggle() }
                    )
                }

                Text(
                    "Turn your ministry & scripture study reminders into an expressive, slang-loaded experience with streak badges, custom haptics, and daily vibe checks. No cap!",
                    style = typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun VibeBadgeCard(
    title: String,
    emoji: String,
    description: String,
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, style = typography.headlineMedium)
            Text(title, style = typography.labelLarge, color = colors.onSurface)
            Text(description, style = typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

private data class GenZRowItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val enabled: Boolean,
    val onToggle: (Boolean) -> Unit
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

@Composable
private fun <T> GenZGroupedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, pos: GenZGroupPos) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        items.forEachIndexed { index, item ->
            row(item, GenZGroupPos(first = index == 0, last = index == items.size - 1))
        }
    }
}

private data class GenZGroupPos(val first: Boolean, val last: Boolean)

@Composable
private fun rememberGroupShape(pos: GenZGroupPos, pressed: Boolean): Shape {
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (pressed) PressedCorner else if (pos.first) GroupOuter else GroupInner,
        spec, label = "genZGroupTopCorner"
    )
    val bottom by animateDpAsState(
        if (pressed) PressedCorner else if (pos.last) GroupOuter else GroupInner,
        spec, label = "genZGroupBottomCorner"
    )
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun GenZListItem(
    pos: GenZGroupPos,
    leadingIcon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberGroupShape(pos, pressed)

    Surface(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
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
            supportingContent = {
                Text(subtitle, style = typography.bodyMedium)
            },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = if (checked) colors.tertiaryContainer else colors.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = if (checked) colors.onTertiaryContainer else colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = colors.onSurface,
                supportingColor = colors.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
