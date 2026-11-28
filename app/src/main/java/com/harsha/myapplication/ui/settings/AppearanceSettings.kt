@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import android.content.res.Configuration
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.ShapeLargeIncreased
import com.harsha.myapplication.ui.theme.AppShapes
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay

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
            selectedPalette = selectedPalette,
            onPaletteChange = AppearanceRepository::setPalette,
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
    selectedPalette: ThemePalette,
    onPaletteChange: (ThemePalette) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    dynamicEnabled: Boolean,
    onDynamicChange: (Boolean) -> Unit
) {
    val dark = themeMode.isDark(isSystemInDarkTheme())

    // "Default" (brand) and "Adaptive" (dynamic) are featured; every other palette scrolls.
    val defaultPalette = remember { PaletteColors.firstOrNull { it.isBrand } ?: PaletteColors.first() }
    val adaptivePalette = remember { PaletteColors.firstOrNull { it.isDynamic } }
    val morePalettes = remember { PaletteColors.filter { !it.isBrand && !it.isDynamic } }

    Column {
        // ── Theme (light / dark behaviour only) ──
        Column(Modifier.revealOnEnter(index = 1, playOnceKey = "appearance-theme")) {
            AppearanceSectionHeader("Theme")
            Spacer(Modifier.height(8.dp))

            AppearanceGroupedList(items = listOf("mode", "pureBlack")) { id, pos ->
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

                    else -> AppearanceListItem(
                        pos = pos,
                        leadingIcon = Icons.Outlined.Contrast,
                        title = "Pure black mode",
                        subtitle = "Use pure black for dark background",
                        onClick = { onPureBlackChange(!pureBlack) },
                        trailing = { AppearanceSwitch(pureBlack, onPureBlackChange) }
                    )
                }
            }
        }

        Spacer(Modifier.height(SectionGap))

        // ── Palette (colours only) ──
        Column(Modifier.revealOnEnter(index = 2, playOnceKey = "appearance-palette")) {
            AppearanceSectionHeader("Palette")
            Text(
                "Choose the colors used across the app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
            Spacer(Modifier.height(8.dp))

            val rows = buildList {
                add("featured")
                if (morePalettes.isNotEmpty()) add("more")
                if (selectedPalette.isDynamic) add("dynamic")
            }

            AppearanceGroupedList(items = rows) { id, pos ->
                when (id) {
                    "featured" -> AppearanceGroupSurface(pos) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeaturedPaletteTile(
                                palette = defaultPalette,
                                label = "Default",
                                caption = "Study green",
                                dark = dark,
                                selected = selectedPalette == defaultPalette,
                                onClick = { onPaletteChange(defaultPalette) },
                                modifier = Modifier.weight(1f)
                            )
                            if (adaptivePalette != null) {
                                FeaturedPaletteTile(
                                    palette = adaptivePalette,
                                    label = "Adaptive",
                                    caption = "From your wallpaper",
                                    dark = dark,
                                    selected = selectedPalette == adaptivePalette,
                                    onClick = { onPaletteChange(adaptivePalette) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    "more" -> AppearanceGroupSurface(pos) {
                        MorePalettesRow(
                            palettes = morePalettes,
                            dark = dark,
                            selectedPalette = selectedPalette,
                            onPaletteChange = onPaletteChange
                        )
                    }

                    "dynamic" -> AppearanceListItem(
                        pos = pos,
                        leadingIcon = Icons.Outlined.AutoAwesome,
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

// ───────────────────────── Palette pieces ─────────────────────────

/** Plays the "peek" nudge only once per process so revisiting the screen stays calm. */
private var paletteNudgePlayed = false

/** Default + Adaptive: big, side-by-side tiles so they read differently from the scrolling palettes. */
@Composable
private fun FeaturedPaletteTile(
    palette: ThemePalette,
    label: String,
    caption: String,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    // Adaptive shows the real wallpaper colors on Android 12+ (falls back to its seed otherwise).
    val scheme = if (palette.isDynamic) {
        rememberAppColorScheme(palette, dark, pureBlack = false, dynamicColor = true)
    } else remember(palette, dark) { paletteColorScheme(palette, dark) }

    val container by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
        MaterialTheme.motionScheme.defaultEffectsSpec(), label = "featuredTile"
    )
    val interaction = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(2.dp, if (selected) colors.primary else Color.Transparent),
        interactionSource = interaction,
        modifier = modifier
            .themePressScale(interaction, pressedScale = 0.97f)
            .semantics { contentDescription = "$label palette" }
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaletteSwatch(scheme, size = 44.dp)
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = if (selected) colors.onSecondaryContainer else colors.onSurface,
                        maxLines = 1
                    )
                    Text(
                        caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            PaletteCheck(
                visible = selected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * The scrollable palettes. Three cues tell the user this row scrolls:
 *  1. a "Swipe" label with a nudging arrow (hides once there is nothing more to reveal),
 *  2. the last visible item is cut off at the card edge, with a soft fade over it,
 *  3. a one-time nudge animation the first time the screen opens.
 */
@Composable
private fun MorePalettesRow(
    palettes: List<ThemePalette>,
    dark: Boolean,
    selectedPalette: ThemePalette,
    onPaletteChange: (ThemePalette) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }

    LaunchedEffect(Unit) {
        if (!paletteNudgePlayed) {
            paletteNudgePlayed = true
            delay(700)
            listState.animateScrollBy(140f)
            delay(150)
            listState.animateScrollBy(-140f)
        }
    }

    val arrowTransition = rememberInfiniteTransition(label = "swipeHint")
    val arrowShift by arrowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swipeArrow"
    )

    Column(Modifier.padding(vertical = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "More palettes",
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(
                visible = canScrollForward,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Swipe",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { translationX = arrowShift.dp.toPx() }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 40.dp)
            ) {
                items(palettes) { palette ->
                    PaletteItem(
                        palette = palette,
                        dark = dark,
                        isSelected = palette == selectedPalette,
                        onClick = { onPaletteChange(palette) }
                    )
                }
            }

            // Soft fade over the cut-off item; disappears at the end of the list.
            val fadeAlpha by animateFloatAsState(
                if (canScrollForward) 1f else 0f,
                MaterialTheme.motionScheme.defaultEffectsSpec(), label = "paletteFade"
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(44.dp)
                    .height(PaletteItemHeight)
                    .graphicsLayer { alpha = fadeAlpha }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.surfaceContainerLow.copy(alpha = 0f),
                                colors.surfaceContainerLow
                            )
                        )
                    )
            )
        }
    }
}

private val PaletteItemHeight = 96.dp

@Composable
private fun PaletteItem(
    palette: ThemePalette,
    dark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scheme = remember(palette, dark) { paletteColorScheme(palette, dark) }
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(72.dp)
            .height(PaletteItemHeight)
            .themePressScale(interaction, pressedScale = 0.95f)
            .clip(AppShapes.medium)
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .semantics { contentDescription = "${palette.name} palette" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(AppShapes.medium)
                .background(if (isSelected) colors.secondaryContainer else colors.surfaceContainerHigh)
                .border(2.dp, if (isSelected) colors.primary else Color.Transparent, AppShapes.medium),
            contentAlignment = Alignment.Center
        ) {
            PaletteSwatch(scheme, size = 44.dp) {
                PaletteCheck(visible = isSelected)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            palette.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Round three-colour swatch (primary on top, secondary + tertiary below). */
@Composable
private fun PaletteSwatch(
    scheme: ColorScheme,
    size: Dp,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(Modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(scheme.primary, Offset(0f, 0f), Size(this.size.width, this.size.height / 2))
            drawRect(scheme.secondary, Offset(0f, this.size.height / 2), Size(this.size.width / 2, this.size.height / 2))
            drawRect(scheme.tertiary, Offset(this.size.width / 2, this.size.height / 2), Size(this.size.width / 2, this.size.height / 2))
        }
        overlay()
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
        exit = fadeOut() + scaleOut(targetScale = 0.4f, animationSpec = Motion.spatialFast())
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
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

    // Animate the whole preview when palette / mode changes so the swap feels alive.
    val bg by animateColorAsState(previewScheme.surface, Motion.effectsDefault<Color>(), label = "previewBg")
    val border by animateColorAsState(previewScheme.outlineVariant, Motion.effectsDefault<Color>(), label = "previewBorder")
    val chip by animateColorAsState(previewScheme.surfaceContainerHigh, Motion.effectsDefault<Color>(), label = "previewChip")
    val primary by animateColorAsState(previewScheme.primary, Motion.effectsDefault<Color>(), label = "previewPrimary")
    val onPrimary by animateColorAsState(previewScheme.onPrimary, Motion.effectsDefault<Color>(), label = "previewOnPrimary")
    val secondary by animateColorAsState(previewScheme.secondary, Motion.effectsDefault<Color>(), label = "previewSecondary")
    val tertiary by animateColorAsState(previewScheme.tertiary, Motion.effectsDefault<Color>(), label = "previewTertiary")
    val container by animateColorAsState(previewScheme.primaryContainer, Motion.effectsDefault<Color>(), label = "previewContainer")

    Card(
        modifier = modifier,
        shape = ShapeLargeIncreased,
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top bar: title pill + avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(84.dp)
                        .height(12.dp)
                        .clip(AppShapes.extraSmall)
                        .background(chip)
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(24.dp).background(primary, CircleShape))
            }

            // Content tiles
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .clip(AppShapes.medium)
                        .background(primary)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.width(56.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(onPrimary.copy(alpha = 0.9f)))
                        Box(Modifier.width(36.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(onPrimary.copy(alpha = 0.5f)))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(secondary))
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(tertiary))
                }
            }

            // Mini navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(AppShapes.small)
                    .background(chip),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(26.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(container))
                repeat(2) {
                    Box(Modifier.size(8.dp).background(border, CircleShape))
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

/** A plain grouped-list container (same shape / colour as [AppearanceListItem]) for custom content. */
@Composable
private fun AppearanceGroupSurface(
    pos: AppearanceGroupPos,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = rememberAppearanceGroupShape(pos, pressed = false),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) { Column(content = content) }
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
