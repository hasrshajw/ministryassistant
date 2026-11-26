@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter

private val ScreenMargin = 16.dp
private val SectionGap = 24.dp
private val GroupGap = 2.dp
private val GroupOuter = 28.dp
private val GroupInner = 4.dp
private val PressedCorner = 20.dp

private data class AppIconStyle(
    val name: String,
    val primaryColor: Color,
    val accentColor: Color
)

private val appIconStyles = listOf(
    AppIconStyle("Emerald", Color(0xFF1B5E20), Color(0xFF81C784)),
    AppIconStyle("Obsidian", Color(0xFF212121), Color(0xFF757575)),
    AppIconStyle("Gen-Z Neon", Color(0xFF4A148C), Color(0xFFE040FB)),
    AppIconStyle("Sunset Gold", Color(0xFFE65100), Color(0xFFFFB74D)),
    AppIconStyle("Sapphire", Color(0xFF0D47A1), Color(0xFF64B5F6))
)

@Composable
fun CustomizeAppScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var selectedIconIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDailyTextCard by rememberSaveable { mutableStateOf(true) }
    var showMinistryCard by rememberSaveable { mutableStateOf(true) }
    var showNotesCard by rememberSaveable { mutableStateOf(true) }
    var showRecommendedCard by rememberSaveable { mutableStateOf(true) }
    var soundEffectsEnabled by rememberSaveable { mutableStateOf(true) }
    var hapticFeedbackLevel by rememberSaveable { mutableIntStateOf(2) } // 0:Off, 1:Subtle, 2:Expressive

    val bibles = remember { BibleData.importedBibles }
    var selectedBibleName by remember { mutableStateOf(BibleData.currentTranslationName) }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Customize App",
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
            // ── App Icon Picker ──
            Column(Modifier.revealOnEnter(index = 0, playOnceKey = "custom-icons")) {
                SectionHeader("App Launcher Icon")
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Choose your preferred app icon theme:",
                            style = typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(appIconStyles) { index, style ->
                                val selected = index == selectedIconIndex
                                AppIconItem(
                                    style = style,
                                    selected = selected,
                                    onClick = { selectedIconIndex = index }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Home Screen Cards Customization ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "custom-home")) {
                SectionHeader("Home Screen Cards")
                Spacer(Modifier.height(8.dp))
                CustomizeGroupedList(
                    items = listOf(
                        CustomizeRowItem(
                            icon = Icons.Outlined.Book,
                            title = "Daily Text Card",
                            subtitle = "Show daily scripture text widget on Home",
                            checked = showDailyTextCard,
                            onToggle = { showDailyTextCard = it }
                        ),
                        CustomizeRowItem(
                            icon = Icons.Outlined.Schedule,
                            title = "Ministry Hours Card",
                            subtitle = "Show field service monthly hours summary",
                            checked = showMinistryCard,
                            onToggle = { showMinistryCard = it }
                        ),
                        CustomizeRowItem(
                            icon = Icons.AutoMirrored.Outlined.Notes,
                            title = "Quick Notes Card",
                            subtitle = "Show recently edited study notes",
                            checked = showNotesCard,
                            onToggle = { showNotesCard = it }
                        ),
                        CustomizeRowItem(
                            icon = Icons.Outlined.Dashboard,
                            title = "Recommended Reading",
                            subtitle = "Show recommended Bible study topics",
                            checked = showRecommendedCard,
                            onToggle = { showRecommendedCard = it }
                        )
                    )
                ) { item, pos ->
                    CustomizeListItem(
                        pos = pos,
                        leadingIcon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        checked = item.checked,
                        onCheckedChange = item.onToggle
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Sound & Haptics ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "custom-sound")) {
                SectionHeader("Sound & Feedback")
                Spacer(Modifier.height(8.dp))
                CustomizeGroupedList(
                    items = listOf(
                        CustomizeRowItem(
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                            title = "UI Sound Effects",
                            subtitle = "Play subtle sounds on tab taps & button presses",
                            checked = soundEffectsEnabled,
                            onToggle = { soundEffectsEnabled = it }
                        )
                    )
                ) { item, pos ->
                    CustomizeListItem(
                        pos = pos,
                        leadingIcon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        checked = item.checked,
                        onCheckedChange = item.onToggle
                    )
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Haptic Feedback Intensity", style = typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Off", "Subtle", "Expressive").forEachIndexed { index, label ->
                                val selected = index == hapticFeedbackLevel
                                FilterChip(
                                    selected = selected,
                                    onClick = { hapticFeedbackLevel = index },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Default Translation ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "custom-bible")) {
                SectionHeader("Default Bible Translation")
                Spacer(Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Select default translation for scripture search:",
                            style = typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                        val translations = remember(bibles) { bibles.map { it.name } }
                        if (translations.isEmpty()) {
                            Text(
                                "No imported Bibles. Defaulting to standard reference.",
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        } else {
                            translations.forEach { name ->
                                val selected = name == selectedBibleName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedBibleName = name
                                            BibleData.setActiveBible(name)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, style = typography.bodyLarge)
                                    RadioButton(
                                        selected = selected,
                                        onClick = {
                                            selectedBibleName = name
                                            BibleData.setActiveBible(name)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIconItem(
    style: AppIconStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(style.primaryColor)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) colors.primary else Color.Transparent,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(style.accentColor)
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            style.name,
            style = typography.labelSmall,
            color = if (selected) colors.primary else colors.onSurfaceVariant
        )
    }
}

private data class CustomizeRowItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val checked: Boolean,
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
private fun <T> CustomizeGroupedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, pos: CustomizeGroupPos) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        items.forEachIndexed { index, item ->
            row(item, CustomizeGroupPos(first = index == 0, last = index == items.size - 1))
        }
    }
}

private data class CustomizeGroupPos(val first: Boolean, val last: Boolean)

@Composable
private fun rememberGroupShape(pos: CustomizeGroupPos, pressed: Boolean): Shape {
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (pressed) PressedCorner else if (pos.first) GroupOuter else GroupInner,
        spec, label = "customGroupTopCorner"
    )
    val bottom by animateDpAsState(
        if (pressed) PressedCorner else if (pos.last) GroupOuter else GroupInner,
        spec, label = "customGroupBottomCorner"
    )
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun CustomizeListItem(
    pos: CustomizeGroupPos,
    leadingIcon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberGroupShape(pos, pressed)

    Surface(
        onClick = { onCheckedChange(!checked) },
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
                    color = if (checked) colors.primaryContainer else colors.surfaceContainerHigh,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = if (checked) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
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
