@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.harsha.myapplication.data.HomeCard
import com.harsha.myapplication.data.HomeLayout
import com.harsha.myapplication.data.HomeLayoutRepository
import com.harsha.myapplication.data.HomeStart
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.StudyDeepGreen
import com.harsha.myapplication.ui.theme.StudyEmerald
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ───────────────────────────── Tokens (same family as Settings / Notifications) ─────────────────────────────
private val CsScreenMargin = 16.dp
private val CsSectionGap = 24.dp
private val CsGroupGap = 2.dp
private val CsGroupOuter = 28.dp
private val CsGroupInner = 4.dp
private val CsPressedCorner = 20.dp

/** Every reorderable row has this exact height, so slots can be computed without measuring. */
private val CsRowHeight = 76.dp

// ───────────────────────────── App icon options ─────────────────────────────

private data class IconOption(val name: String, val background: Color, val accent: Color)

// Only one icon for now. To add more later, add entries here: the picker below lays them out
// in a row and remembers the selected index.
private val CsIconOptions = listOf(
    IconOption("Default", StudyDeepGreen, StudyEmerald),
)

// ───────────────────────────── Home cards ─────────────────────────────

private data class HomeCardMeta(val icon: ImageVector, val title: String, val subtitle: String)

private fun HomeCard.meta(): HomeCardMeta = when (this) {
    HomeCard.DailyText -> HomeCardMeta(Icons.Outlined.Book, "Daily text", "Today's scripture")
    HomeCard.Ministry -> HomeCardMeta(Icons.Outlined.Schedule, "Ministry card", "Monthly hours and goal")
    HomeCard.Notes -> HomeCardMeta(Icons.AutoMirrored.Outlined.Notes, "Notes card", "Your pinned study note")
}

// ───────────────────────────── Screen ─────────────────────────────

@Composable
fun CustomizeAppScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var selectedIconIndex by rememberSaveable { mutableIntStateOf(0) }
    var soundEffectsEnabled by rememberSaveable { mutableStateOf(true) }
    var hapticLevel by rememberSaveable { mutableIntStateOf(2) } // 0 Off, 1 Subtle, 2 Expressive

    val layout = HomeLayoutRepository.layout

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
                .padding(horizontal = CsScreenMargin)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            // ── App icon ──
            Column(Modifier.revealOnEnter(index = 0, playOnceKey = "custom-icon")) {
                CsSectionHeader("App icon")
                Spacer(Modifier.height(8.dp))
                AppIconCard(
                    options = CsIconOptions,
                    selectedIndex = selectedIconIndex,
                    onSelect = { selectedIconIndex = it }
                )
            }

            Spacer(Modifier.height(CsSectionGap))

            // ── Home screen cards ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "custom-home")) {
                CsSectionHeader("Home screen")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Drag the handle to reorder. Home follows this order. At least one card stays on.",
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                )
                Spacer(Modifier.height(10.dp))
                HomeCardsReorderList(layout)
                Spacer(Modifier.height(8.dp))
                StartCardPicker(layout)
            }

            Spacer(Modifier.height(CsSectionGap))

            // ── Sound & feedback ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "custom-sound")) {
                CsSectionHeader("Sound & feedback")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(CsGroupGap)) {
                    CsSwitchRow(
                        first = true,
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        title = "UI sound effects",
                        subtitle = "Subtle sounds on taps and button presses",
                        checked = soundEffectsEnabled,
                        onCheckedChange = { soundEffectsEnabled = it }
                    )
                    HapticCard(level = hapticLevel, onSelect = { hapticLevel = it })
                }
            }
        }
    }
}

@Composable
private fun CsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

// ───────────────────────────── App icon picker ─────────────────────────────

@Composable
private fun AppIconCard(
    options: List<IconOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(CsGroupOuter),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                options.forEachIndexed { index, option ->
                    AppIconItem(
                        option = option,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) }
                    )
                }
            }
            Text(
                "More icon styles are coming soon.",
                style = typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppIconItem(option: IconOption, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val ring by animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        Motion.effectsDefault(), label = "iconRing"
    )
    val src = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .themePressScale(src, pressedScale = 0.94f)
            .clickable(
                interactionSource = src,
                indication = null,
                role = Role.RadioButton,
                onClickLabel = "Use ${option.name} icon",
                onClick = onClick
            )
            .semantics { this.selected = selected }
    ) {
        Box {
            Box(
                Modifier
                    .border(3.dp, ring, RoundedCornerShape(28.dp))
                    .padding(5.dp)
            ) {
                Box(
                    Modifier
                        .size(68.dp)
                        .background(option.background, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(option.accent, CircleShape)
                    )
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Text(
            option.name,
            style = if (selected) typography.labelLargeEmphasized else typography.labelLarge,
            color = if (selected) colors.primary else colors.onSurfaceVariant
        )
    }
}

// ───────────────────────────── Reorderable Home cards ─────────────────────────────

/**
 * The three Home cards as one grouped list. Each row has a switch (show / hide) and a drag handle.
 *
 * Rows are stacked in a fixed-height Box and positioned by translation, one slot per row. While a row
 * is dragged the others spring into the slot they would take; on release the new order is saved to
 * [HomeLayoutRepository] and the dragged row settles into its slot.
 */
@Composable
private fun HomeCardsReorderList(layout: HomeLayout) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val stepPx = with(density) { (CsRowHeight + CsGroupGap).toPx() }
    val order = layout.order
    val lastIndex = order.lastIndex
    val onlyOneVisible = layout.visibleCards.size == 1

    var draggedCard by remember { mutableStateOf<HomeCard?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var lastTarget by remember { mutableIntStateOf(-1) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    val draggedIndex = draggedCard?.let { order.indexOf(it) } ?: -1
    val targetIndex = if (draggedIndex >= 0) {
        (draggedIndex + (dragY / stepPx).roundToInt()).coerceIn(0, lastIndex)
    } else -1

    Box(
        Modifier
            .fillMaxWidth()
            .height(CsRowHeight * order.size + CsGroupGap * (order.size - 1))
    ) {
        order.forEachIndexed { index, card ->
            key(card) {
                val indexState by rememberUpdatedState(index)
                val isDragged = card == draggedCard
                val visible = layout.isVisible(card)

                // The slot this row currently occupies (differs from `index` while another row is dragged).
                val displayIndex = when {
                    isDragged -> targetIndex
                    draggedIndex < 0 -> index
                    index in (draggedIndex + 1)..targetIndex -> index - 1
                    index in targetIndex..(draggedIndex - 1) -> index + 1
                    else -> index
                }
                val slotY by animateFloatAsState(
                    targetValue = displayIndex * stepPx,
                    animationSpec = Motion.spatialFast,
                    label = "slotY"
                )

                HomeCardRow(
                    meta = card.meta(),
                    first = displayIndex == 0,
                    last = displayIndex == lastIndex,
                    lifted = isDragged,
                    checked = visible,
                    switchEnabled = !(visible && onlyOneVisible),
                    onCheckedChange = { HomeLayoutRepository.setVisible(card, it) },
                    onDragStart = {
                        settleJob?.cancel()
                        settleJob = null
                        dragY = 0f
                        draggedCard = card
                        lastTarget = indexState
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { dy ->
                        dragY += dy
                        val t = (indexState + (dragY / stepPx).roundToInt()).coerceIn(0, lastIndex)
                        if (t != lastTarget) {
                            lastTarget = t
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = {
                        val from = indexState
                        val to = (from + (dragY / stepPx).roundToInt()).coerceIn(0, lastIndex)
                        // Keep the row exactly where it is on screen, then ease it into its slot.
                        val residual = dragY - (to - from) * stepPx
                        if (to != from) HomeLayoutRepository.move(from, to)
                        dragY = residual
                        settleJob?.cancel()
                        settleJob = scope.launch {
                            Animatable(residual).animateTo(0f, Motion.spatialFast) { dragY = value }
                            draggedCard = null
                        }
                    },
                    onMoveBy = { delta ->
                        val to = (indexState + delta).coerceIn(0, lastIndex)
                        if (to != indexState) HomeLayoutRepository.move(indexState, to)
                    },
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) indexState * stepPx + dragY else slotY
                        }
                )
            }
        }
    }
}

@Composable
private fun HomeCardRow(
    meta: HomeCardMeta,
    first: Boolean,
    last: Boolean,
    lifted: Boolean,
    checked: Boolean,
    switchEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveBy: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (lifted || pressed) CsPressedCorner else if (first) CsGroupOuter else CsGroupInner,
        spec, label = "rowTop"
    )
    val bottom by animateDpAsState(
        if (lifted || pressed) CsPressedCorner else if (last) CsGroupOuter else CsGroupInner,
        spec, label = "rowBottom"
    )
    val shape = RoundedCornerShape(
        topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom
    )
    val liftScale by animateFloatAsState(
        if (lifted) 1.03f else 1f, Motion.spatialFast, label = "rowLiftScale"
    )
    val elevation by animateDpAsState(
        if (lifted) 10.dp else 0.dp, spec, label = "rowElevation"
    )
    val container by animateColorAsState(
        if (lifted) colors.surfaceContainerHigh else colors.surfaceContainerLow,
        Motion.effectsDefault(), label = "rowContainer"
    )
    val well by animateColorAsState(
        if (checked) colors.primaryContainer else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "rowWell"
    )
    val tint by animateColorAsState(
        if (checked) colors.onPrimaryContainer else colors.onSurfaceVariant,
        Motion.effectsDefault(), label = "rowTint"
    )

    val dragStartState by rememberUpdatedState(onDragStart)
    val dragState by rememberUpdatedState(onDrag)
    val dragEndState by rememberUpdatedState(onDragEnd)

    Surface(
        onClick = { if (switchEnabled) onCheckedChange(!checked) },
        interactionSource = interaction,
        shape = shape,
        color = container,
        shadowElevation = elevation,
        modifier = modifier
            .fillMaxWidth()
            .height(CsRowHeight)
            .graphicsLayer {
                scaleX = liftScale
                scaleY = liftScale
            }
    ) {
        ListItem(
            headlineContent = {
                Text(
                    meta.title,
                    style = typography.bodyLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    if (checked) meta.subtitle else "Hidden from Home",
                    style = typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = well,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(meta.icon, null, tint = tint, modifier = Modifier.size(24.dp))
                    }
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        enabled = switchEnabled,
                        thumbContent = {
                            Icon(
                                if (checked) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                    // Drag handle. Screen-reader users get Move up / Move down instead.
                    Box(
                        Modifier
                            .size(44.dp)
                            .semantics {
                                contentDescription = "Reorder ${meta.title}"
                                customActions = listOf(
                                    CustomAccessibilityAction("Move up") { onMoveBy(-1); true },
                                    CustomAccessibilityAction("Move down") { onMoveBy(1); true },
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { dragStartState() },
                                    onDragEnd = { dragEndState() },
                                    onDragCancel = { dragEndState() },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragState(amount.y)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = colors.onSurface,
                supportingColor = colors.onSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ───────────────────────────── Start card ─────────────────────────────

/** Chooses which Home card the app opens on (first / center / last of the visible cards). */
@Composable
private fun StartCardPicker(layout: HomeLayout) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val target = layout.visibleCards.getOrNull(layout.startIndex())

    Surface(
        shape = RoundedCornerShape(CsGroupOuter),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Open on", style = typography.bodyLargeEmphasized, color = colors.onSurface)
                Text(
                    if (target != null) "Home starts on the ${target.meta().title.lowercase()}"
                    else "Choose which card Home starts on",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeStart.values().forEach { option ->
                    CsChoiceChip(
                        label = option.label,
                        selected = option == layout.start,
                        onClick = { HomeLayoutRepository.setStart(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Sound & feedback ─────────────────────────────

@Composable
private fun CsSwitchRow(
    first: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val top by animateDpAsState(
        if (pressed) CsPressedCorner else if (first) CsGroupOuter else CsGroupInner,
        spec, label = "switchTop"
    )
    val bottom by animateDpAsState(
        if (pressed) CsPressedCorner else CsGroupInner,
        spec, label = "switchBottom"
    )
    val well by animateColorAsState(
        if (checked) colors.primaryContainer else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "switchWell"
    )
    val tint by animateColorAsState(
        if (checked) colors.onPrimaryContainer else colors.onSurfaceVariant,
        Motion.effectsDefault(), label = "switchTint"
    )

    Surface(
        onClick = { onCheckedChange(!checked) },
        interactionSource = interaction,
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.98f)
    ) {
        ListItem(
            headlineContent = { Text(title, style = typography.bodyLargeEmphasized, maxLines = 1) },
            supportingContent = { Text(subtitle, style = typography.bodyMedium, maxLines = 2) },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = well,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                    }
                }
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = {
                        Icon(
                            if (checked) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = colors.onSurface,
                supportingColor = colors.onSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HapticCard(level: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Surface(
        shape = RoundedCornerShape(
            topStart = CsGroupInner, topEnd = CsGroupInner,
            bottomStart = CsGroupOuter, bottomEnd = CsGroupOuter
        ),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Haptic feedback", style = typography.bodyLargeEmphasized, color = colors.onSurface)
                Text(
                    "How strongly the app vibrates when you interact",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Off", "Subtle", "Expressive").forEachIndexed { index, label ->
                    CsChoiceChip(
                        label = label,
                        selected = index == level,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CsChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val radius by animateDpAsState(
        if (selected) 22.dp else 14.dp, Motion.spatialDefaultDp, label = "chipRadius"
    )
    val container by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "chipContainer"
    )
    val content by animateColorAsState(
        if (selected) colors.onSecondaryContainer else colors.onSurface,
        Motion.effectsDefault(), label = "chipContent"
    )
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = content,
        modifier = modifier
            .height(44.dp)
            .themePressScale(src, pressedScale = 0.94f)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1
            )
        }
    }
}
