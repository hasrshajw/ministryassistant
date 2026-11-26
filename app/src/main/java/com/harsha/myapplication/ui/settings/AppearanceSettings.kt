@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.theme.AppearanceRepository
import com.harsha.myapplication.ui.theme.PaletteColors
import com.harsha.myapplication.ui.theme.ThemeMode
import com.harsha.myapplication.ui.theme.ThemePalette
import com.harsha.myapplication.ui.theme.paletteColorScheme
import com.harsha.myapplication.ui.theme.rememberAppColorScheme
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import androidx.core.graphics.ColorUtils

private val ScreenMargin = 16.dp
private val SectionGap = 24.dp
private val GroupGap = 2.dp
private val GroupOuter = 28.dp
private val GroupInner = 4.dp
private val PressedCorner = 20.dp

@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    // Persisted + shared with MainActivity: changing these re-themes the whole app live.
    val prefs = AppearanceRepository.prefs
    val selectedTheme = prefs.themeMode
    val selectedPalette = prefs.palette
    val selectedPaletteIndex = PaletteColors.indexOf(selectedPalette)
    val dynamicColorEnabled = prefs.dynamicColor
    val pureBlackEnabled = prefs.pureBlack

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val preview: @Composable (Modifier) -> Unit = { modifier ->
        ThemePreviewCard(
            modifier = modifier,
            themeMode = selectedTheme,
            pureBlack = pureBlackEnabled,
            palette = selectedPalette,
            dynamicEnabled = dynamicColorEnabled
        )
    }
    val controls: @Composable () -> Unit = {
        AppearanceControls(
            themeMode = selectedTheme,
            onThemeModeChange = AppearanceRepository::setThemeMode,
            selectedPaletteIndex = selectedPaletteIndex,
            onPaletteChange = { AppearanceRepository.setPalette(PaletteColors[it]) },
            pureBlack = pureBlackEnabled,
            onPureBlackChange = AppearanceRepository::setPureBlack,
            dynamicEnabled = dynamicColorEnabled,
            onDynamicChange = AppearanceRepository::setDynamicColor
        )
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Appearance",
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
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(ScreenMargin),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    preview(Modifier.fillMaxWidth().height(140.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(end = ScreenMargin, top = ScreenMargin, bottom = 32.dp)
                ) {
                    controls()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenMargin)
                    .padding(top = 8.dp, bottom = 32.dp)
            ) {
                Column(Modifier.revealOnEnter(index = 0, playOnceKey = "appearance-preview")) {
                    preview(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(160.dp)
                    )
                }
                Spacer(Modifier.height(SectionGap))
                controls()
            }
        }
    }
}

@Composable
private fun AppearanceControls(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    selectedPaletteIndex: Int,
    onPaletteChange: (Int) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    dynamicEnabled: Boolean,
    onDynamicChange: (Boolean) -> Unit
) {
    // Dynamic-color switch only makes sense while the "Dynamic" palette is selected
    val showDynamicSwitch = PaletteColors[selectedPaletteIndex].isDynamic
    val dark = themeMode.isDark(isSystemInDarkTheme())

    Column {
        // ── Theme / Colors ──
        Column(Modifier.revealOnEnter(index = 1, playOnceKey = "appearance-display")) {
            AppearanceSectionHeader("Theme & Colors")
            Spacer(Modifier.height(8.dp))

            val rows = buildList {
                add("mode"); add("palette"); add("pureBlack")
                if (showDynamicSwitch) add("dynamic")
            }

            AppearanceGroupedList(items = rows) { id, pos ->
                when (id) {
                    "mode" -> AppearanceListItem(
                        pos = pos,
                        leadingIcon = Icons.Outlined.DarkMode,
                        title = "Theme mode",
                        onClick = {},
                        below = {
                            ThemeModeToggleGroup(selected = themeMode, onSelect = onThemeModeChange)
                        }
                    )

                    "palette" -> AppearanceListItem(
                        pos = pos,
                        leadingIcon = Icons.Outlined.Palette,
                        title = "Color palette",
                        onClick = {},
                        below = {
                            LazyRow(
                                modifier = Modifier.height(72.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                itemsIndexed(PaletteColors) { index, palette ->
                                    PaletteItem(
                                        palette = palette,
                                        dark = dark,
                                        isSelected = index == selectedPaletteIndex,
                                        onClick = { onPaletteChange(index) }
                                    )
                                }
                            }
                        }
                    )

                    "pureBlack" -> AppearanceListItem(
                        pos = pos,
                        leadingIcon = Icons.Outlined.Contrast,
                        title = "Pure black mode",
                        subtitle = "Use pure black for dark background",
                        onClick = { onPureBlackChange(!pureBlack) },
                        trailing = { AppearanceSwitch(pureBlack, onPureBlackChange) }
                    )

                    "dynamic" -> AppearanceListItem(
                        pos = pos,
                        leadingIcon = Icons.Outlined.Palette,
                        title = "Dynamic colors",
                        subtitle = "Use system wallpaper colors (Android 12+)",
                        onClick = { onDynamicChange(!dynamicEnabled) },
                        trailing = { AppearanceSwitch(dynamicEnabled, onDynamicChange) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeToggleGroup(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val modes = ThemeMode.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        modes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = selected == mode,
                onCheckedChange = { onSelect(mode) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(mode.shortLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AppearanceSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        }
    )
}

// ───────────────────────── Palette item ─────────────────────────

@Composable
private fun PaletteItem(
    palette: ThemePalette,
    dark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = remember(palette, dark) { paletteColorScheme(palette, dark) }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "paletteScale"
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .semantics { contentDescription = "${palette.name} palette" },
        contentAlignment = Alignment.Center
    ) {
        if (palette.isDynamic) {
            Icon(
                Icons.Outlined.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            PaletteCheck(visible = isSelected)
        } else {
            Box(Modifier.size(44.dp).clip(CircleShape)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(scheme.primary, Offset(0f, 0f), Size(size.width, size.height / 2))
                    drawRect(scheme.secondary, Offset(0f, size.height / 2), Size(size.width / 2, size.height / 2))
                    drawRect(scheme.tertiary, Offset(size.width / 2, size.height / 2), Size(size.width / 2, size.height / 2))
                }
                PaletteCheck(visible = isSelected, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun PaletteCheck(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(
            initialScale = 0.4f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
        ),
        exit = fadeOut() + scaleOut(targetScale = 0.4f, animationSpec = tween(150))
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ───────────────────────── Preview card ─────────────────────────

@Composable
private fun ThemePreviewCard(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    pureBlack: Boolean,
    palette: ThemePalette,
    dynamicEnabled: Boolean
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = themeMode.isDark(isSystemDark)
    val previewScheme = rememberAppColorScheme(palette, useDark, pureBlack, dynamicEnabled)

    MaterialTheme(colorScheme = previewScheme) {
        val cs = MaterialTheme.colorScheme
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            border = BorderStroke(1.dp, cs.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(cs.surfaceContainerHigh)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(28.dp).background(cs.primary, CircleShape))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cs.primary)
                    )
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth().weight(1f)
                                .clip(RoundedCornerShape(12.dp)).background(cs.secondary)
                        )
                        Box(
                            Modifier.fillMaxWidth().weight(1f)
                                .clip(RoundedCornerShape(12.dp)).background(cs.tertiary)
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cs.primaryContainer)
                    )
                }
            }
        }
    }
}

// ───────────────────────── Grouped list primitives ─────────────────────────

@Composable
private fun AppearanceSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun <T> AppearanceGroupedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, pos: AppearanceGroupPos) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupGap)
    ) {
        items.forEachIndexed { index, item ->
            row(item, AppearanceGroupPos(first = index == 0, last = index == items.size - 1))
        }
    }
}

private data class AppearanceGroupPos(val first: Boolean, val last: Boolean)

@Composable
private fun rememberAppearanceGroupShape(pos: AppearanceGroupPos, pressed: Boolean): Shape {
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (pressed) PressedCorner else if (pos.first) GroupOuter else GroupInner,
        spec, label = "appearanceGroupTopCorner"
    )
    val bottom by animateDpAsState(
        if (pressed) PressedCorner else if (pos.last) GroupOuter else GroupInner,
        spec, label = "appearanceGroupBottomCorner"
    )
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * [below] renders custom content (toggle group, palette row) under the headline,
 * like `descriptionBelow = true` in ThemeSettings.
 */
@Composable
private fun AppearanceListItem(
    pos: AppearanceGroupPos,
    leadingIcon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberAppearanceGroupShape(pos, pressed)

    val container by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "appearanceRowContainer"
    )
    val content = if (selected) colors.onSecondaryContainer else colors.onSurface
    val secondary = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant

    val itemContent: @Composable () -> Unit = {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (subtitle != null) 72.dp else 64.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = colors.secondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = colors.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = if (selected) typography.bodyLargeEmphasized else typography.bodyLarge,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = typography.bodyMedium,
                            color = secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(16.dp))
                    trailing()
                }
            }
            if (below != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) { below() }
            }
        }
    }

    if (below != null) {
        // Interactive content inside: the container itself must not be clickable.
        Surface(shape = shape, color = container, modifier = Modifier.fillMaxWidth(), content = itemContent)
    } else {
        Surface(
            onClick = onClick,
            shape = shape,
            color = container,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .themePressScale(interaction, pressedScale = 0.98f),
            content = itemContent
        )
    }
}