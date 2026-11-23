@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.components.appSharedBoundsOrNone
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.absoluteValue

private val ScreenMargin = 16.dp
private val RadiusLarge = 28.dp

/** Brand colours used by the screen's "Add Hours" button. */
private object HoursBrand {
    val emeraldGhost = Color(0x143BBF6C)
    val emeraldMuted = Color(0xFF2E9A56)
}

@Composable
fun DailyTextScreen(
    daily: List<DailyItem> = sampleDaily,
    recommended: List<Recommended> = sampleRecommended,
    initialIndex: Int = 0,
    onBack: () -> Unit = {},
    onLogHours: (hours: Double, date: Date) -> Unit = { _, _ -> },
) {
    val colors = MaterialTheme.colorScheme
    val currentItem = daily.getOrNull(initialIndex) ?: daily.firstOrNull()
    val weekday = currentItem?.weekday

    var showAddHoursSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.appSharedBoundsOrNone(key = "daily-card", cornerRadius = RadiusLarge),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Daily Text",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (weekday != null) {
                            AnimatedContent(
                                targetState = weekday,
                                transitionSpec = {
                                    fadeIn(Motion.effectsFast) togetherWith fadeOut(Motion.effectsFast)
                                },
                                label = "weekdaySubtitle"
                            ) { w ->
                                Text(
                                    w,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* Bookmark */ }) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface,
                    navigationIconContentColor = colors.onSurface,
                    actionIconContentColor = colors.onSurfaceVariant
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header row: label + Add Hours button ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenMargin, end = ScreenMargin, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recommended for today",
                    modifier = Modifier.weight(1f).revealOnEnter(index = 2),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                FilledTonalButton(
                    onClick = { showAddHoursSheet = true },
                    modifier = Modifier.revealOnEnter(index = 2),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = HoursBrand.emeraldGhost,
                        contentColor = HoursBrand.emeraldMuted,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Add Hours",
                        fontFamily = Lexend,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Box(Modifier.revealOnEnter(index = 3)) { RecommendedPager(recommended) }
            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Bottom sheet ────────────────────────────────────────────────────
    if (showAddHoursSheet) {
        AddHoursSheet(
            onDismiss = { showAddHoursSheet = false },
            onSave = { hours, date ->
                onLogHours(hours, date)
                showAddHoursSheet = false
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  ADD HOURS SHEET
//  Designed to be easy for everyone, including older people:
//   • big, plain-language labels (no ALL-CAPS, no jargon)
//   • time can be TYPED (big boxes at the top) OR turned with the wheels —
//     both always stay in sync
//   • the day selector has a highlight that slides to the chosen option
//   • large tap targets and one clear Save button
// ═════════════════════════════════════════════════════════════════════════

private val WheelItemHeight = 44.dp
private val WheelHeight = 132.dp

private data class HoursPalette(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
)

@Composable
private fun rememberHoursPalette(): HoursPalette {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(dark) {
        if (dark) {
            val a = Color(0xFF6FD79A)
            HoursPalette(accent = a, onAccent = Color(0xFF0B2A17), accentSoft = a.copy(alpha = 0.16f))
        } else {
            val a = Color(0xFF1E8449)
            HoursPalette(accent = a, onAccent = Color.White, accentSoft = a.copy(alpha = 0.10f))
        }
    }
}

@Composable
fun AddHoursSheet(
    onDismiss: () -> Unit,
    onSave: (hours: Double, date: Date) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val palette = rememberHoursPalette()
    val colors = MaterialTheme.colorScheme

    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val hourListState = rememberLazyListState()
    val minuteListState = rememberLazyListState()

    // The selected value is whichever row sits closest to the wheel's centre.
    val selectedHour by remember { derivedStateOf { hourListState.centeredIndex() } }
    val selectedMinute by remember { derivedStateOf { minuteListState.centeredIndex() } }
    val totalMinutes = selectedHour * 60 + selectedMinute
    val hasTime = totalMinutes > 0

    // Haptic tick on every step (skip the initial emission).
    LaunchedEffect(hourListState) {
        snapshotFlow { hourListState.centeredIndex() }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    LaunchedEffect(minuteListState) {
        snapshotFlow { minuteListState.centeredIndex() }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    // When the person touches a wheel with a finger, close the keyboard so the
    // typed boxes follow the wheel again.
    val hourDragged by hourListState.interactionSource.collectIsDraggedAsState()
    val minuteDragged by minuteListState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(hourDragged, minuteDragged) {
        if (hourDragged || minuteDragged) focusManager.clearFocus()
    }

    // Used by the keyboard boxes: wheels follow what is typed.
    fun onHoursTyped(h: Int) {
        scope.launch { hourListState.animateScrollToItem(h) }
        if (h == 24) scope.launch { minuteListState.animateScrollToItem(0) }
    }

    fun onMinutesTyped(m: Int) {
        scope.launch { minuteListState.animateScrollToItem(m) }
        if (selectedHour == 24) scope.launch { hourListState.animateScrollToItem(23) }
    }

    val isToday = isSameLocalDay(selectedDateMillis, System.currentTimeMillis())
    val isYesterday = isSameLocalDay(selectedDateMillis, daysAgoMillis(1))
    val isCustomDate = !isToday && !isYesterday
    val daySelectedIndex = when {
        isToday -> 0
        isYesterday -> 1
        else -> 2
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
        ) {
            // ── Title row ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Log Ministry Time",
                        fontFamily = Lexend,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp,
                        color = colors.onSurface,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top,
            ) {
                TimeNumberField(
                    value = selectedHour,
                    maxValue = 24,
                    padTwo = false,
                    label = "Hours",
                    palette = palette,
                    imeAction = ImeAction.Next,
                    onTyped = ::onHoursTyped,
                )
                TimeNumberField(
                    value = selectedMinute,
                    maxValue = 59,
                    padTwo = true,
                    label = "Minutes",
                    palette = palette,
                    imeAction = ImeAction.Done,
                    onTyped = ::onMinutesTyped,
                )
            }
            Spacer(Modifier.height(16.dp))

            SlidingDaySelector(
                labels = listOf(
                    "Today",
                    "Yesterday",
                    if (isCustomDate)
                        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(selectedDateMillis))
                    else "Pick date",
                ),
                selectedIndex = daySelectedIndex,
                palette = palette,
                onSelect = { index ->
                    focusManager.clearFocus()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when (index) {
                        0 -> selectedDateMillis = System.currentTimeMillis()
                        1 -> selectedDateMillis = daysAgoMillis(1)
                        else -> showDatePicker = true
                    }
                },
            )

            Spacer(Modifier.height(6.dp))
            Text(
                SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(selectedDateMillis)),
                modifier = Modifier.fillMaxWidth(),
                fontFamily = Lexend,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            val pickerBg = colors.surfaceContainerHigh
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(WheelHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(pickerBg)
            ) {
                // Selection pill behind the centre row
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .height(WheelItemHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentSoft)
                        .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                )
                Row(Modifier.fillMaxSize()) {
                    WheelPicker(
                        listState = hourListState,
                        itemCount = 25,
                        selectedIndex = selectedHour,
                        title = "Hours",
                        unitShort = "hr",
                        accent = palette.accent,
                        onUserTap = { focusManager.clearFocus() },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    WheelPicker(
                        listState = minuteListState,
                        itemCount = 60,
                        selectedIndex = selectedMinute,
                        title = "Minutes",
                        unitShort = "min",
                        accent = palette.accent,
                        onUserTap = { focusManager.clearFocus() },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                // Soft fades at the top and bottom edges
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Brush.verticalGradient(listOf(pickerBg, Color.Transparent)))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, pickerBg)))
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Primary action ──────────────────────────────────────────
            Button(
                onClick = {
                    if (hasTime) {
                        focusManager.clearFocus()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val hours = (totalMinutes / 60.0).coerceAtMost(24.0)
                        onSave(hours, Date(selectedDateMillis))
                    }
                },
                enabled = hasTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.onAccent,
                    disabledContainerColor = colors.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
                ),
            ) {
                Text(
                    if (hasTime) "Save ${formatDuration(selectedHour, selectedMinute)}" else "Enter the time first",
                    fontFamily = Lexend,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
            }
        }
    }

    // ── Date picker dialog ──────────────────────────────────────────────
    if (showDatePicker) {
        val todayUtc = remember { localDayToUtcMillis(System.currentTimeMillis()) }
        val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDayToUtcMillis(selectedDateMillis),
            selectableDates = remember(todayUtc) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
                    override fun isSelectableYear(year: Int) = year <= currentYear
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = utcDayToLocalMillis(it)
                    }
                    showDatePicker = false
                }) { Text("OK", fontFamily = Lexend, fontSize = 16.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", fontFamily = Lexend, fontSize = 16.sp)
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Small building blocks
// ─────────────────────────────────────────────────────────────────────────

/**
 * Three-option control (Today / Yesterday / Pick date).
 * A single coloured pill SLIDES to the chosen option with a soft spring,
 * so it is always obvious which one is selected.
 */
@Composable
private fun SlidingDaySelector(
    labels: List<String>,
    selectedIndex: Int,
    palette: HoursPalette,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val segmentHeight = 44.dp

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / labels.size
        val pillOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "daySlide",
        )

        // The sliding pill
        Box(
            Modifier
                .offset(x = pillOffset)
                .width(segmentWidth)
                .height(segmentHeight)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.accent)
        )

        // Labels on top (they never move — only the pill does)
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    if (selected) palette.onAccent else colors.onSurface,
                    label = "dayText$index",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(segmentHeight)
                        .clip(RoundedCornerShape(22.dp))
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontFamily = Lexend,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Typed time box — big number you can tap and type with the keyboard.
//  While typing, the wheels follow. When not typing, the box follows the
//  wheels. A new digit typed replaces the old value (e.g. "15" then "3"
//  becomes "3" for minutes ≤ 59 rules, "53"→ valid, else last digit).
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun TimeNumberField(
    value: Int,
    maxValue: Int,
    padTwo: Boolean,
    label: String,
    palette: HoursPalette,
    imeAction: ImeAction,
    onTyped: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(18.dp)

    val shown = if (padTwo) value.toString().padStart(2, '0') else value.toString()
    var focused by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(shown)) }

    // Follow the wheels whenever the person is NOT typing in this box.
    LaunchedEffect(shown, focused) {
        if (!focused) field = TextFieldValue(shown)
    }

    val borderColor by animateColorAsState(
        if (focused) palette.accent else colors.outline,
        label = "fieldBorder",
    )
    val numberColor = if (value > 0 || focused) palette.accent else colors.onSurface.copy(alpha = 0.5f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = field,
            onValueChange = { new ->
                val digits = new.text.filter { it.isDigit() }
                if (digits.isEmpty()) {
                    field = TextFieldValue("", TextRange(0))
                    onTyped(0)
                } else {
                    var num = digits.takeLast(2).toInt()
                    if (num > maxValue) num = digits.last().digitToInt()
                    val text = num.toString()
                    field = TextFieldValue(text, TextRange(text.length))
                    onTyped(num)
                }
            },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Lexend,
                fontSize = 38.sp,
                fontWeight = FontWeight.Medium,
                color = numberColor,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(palette.accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier
                .width(100.dp)
                .onFocusChanged { state ->
                    focused = state.isFocused
                    if (state.isFocused) {
                        // Select everything so the first digit typed replaces it.
                        field = field.copy(selection = TextRange(0, field.text.length))
                    }
                }
                .semantics { contentDescription = "$label. Double tap to type a number." },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (focused) palette.accentSoft else colors.surfaceContainerHigh)
                        .border(if (focused) 3.dp else 2.dp, borderColor, shape)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) { inner() }
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontFamily = Lexend,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Wheel picker — one snapping column; rows shrink and fade away from centre.
//  Tap any row to select it. Exposes increase/decrease actions to TalkBack.
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun WheelPicker(
    listState: LazyListState,
    itemCount: Int,
    selectedIndex: Int,
    title: String,
    unitShort: String,
    accent: Color,
    onUserTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val textColor = MaterialTheme.colorScheme.onSurface
    // (WheelHeight - WheelItemHeight) / 2 centres the "selected" row exactly
    val verticalPadding = (WheelHeight - WheelItemHeight) / 2

    fun step(delta: Int): Boolean {
        val target = (selectedIndex + delta).coerceIn(0, itemCount - 1)
        scope.launch { listState.animateScrollToItem(target) }
        return true
    }

    Box(
        modifier.semantics {
            contentDescription = "$title picker"
            stateDescription = "$selectedIndex $unitShort"
            customActions = listOf(
                CustomAccessibilityAction("Increase $title") { step(+1) },
                CustomAccessibilityAction("Decrease $title") { step(-1) },
            )
        }
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = verticalPadding),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count = itemCount) { index ->
                val isSelected = index == selectedIndex
                Box(
                    Modifier
                        .height(WheelItemHeight)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            onUserTap()
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                        .graphicsLayer {
                            val info = listState.layoutInfo
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            if (item != null) {
                                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                val distance = abs(item.offset + item.size / 2f - center) / item.size
                                val focus = 1f - (distance / 2f).coerceIn(0f, 1f)
                                val s = lerp(0.72f, 1f, focus)
                                scaleX = s
                                scaleY = s
                                alpha = lerp(0.18f, 1f, focus)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        index.toString().padStart(2, '0'),
                        modifier = Modifier
                            .offset(x = (-12).dp)
                            .clearAndSetSemantics { },
                        fontFamily = Lexend,
                        fontSize = 26.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) accent else textColor,
                    )
                }
            }
        }

        // Unit label sits beside the selected number
        Text(
            unitShort,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 30.dp)
                .clearAndSetSemantics { },
            fontFamily = Lexend,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────

/** Index of the row whose centre is closest to the viewport centre. */
private fun LazyListState.centeredIndex(): Int {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo
        .minByOrNull { abs(it.offset + it.size / 2 - center) }
        ?.index ?: firstVisibleItemIndex
}

/** Plain-language duration, e.g. "1 hr 30 min". */
private fun formatDuration(hours: Int, minutes: Int): String = when {
    hours > 0 && minutes > 0 -> "$hours hr $minutes min"
    hours > 0 -> "$hours hr"
    else -> "$minutes min"
}

private fun daysAgoMillis(days: Int): Long =
    Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -days) }.timeInMillis

private fun isSameLocalDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

/** Material's DatePicker works in UTC midnight; convert a local day to that form. */
private fun localDayToUtcMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Convert the DatePicker's UTC-midnight result back to local noon of the same calendar day. */
private fun utcDayToLocalMillis(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
    }.timeInMillis
}

// ── Existing recommended-pager section, unchanged ────────────────────────
private val RecommendedGradients = listOf(
    Color(0xFF9C8FC4) to Color(0xFF5A4B86),
    Color(0xFF7FA6C9) to Color(0xFF3F5F86),
    Color(0xFFB48FB8) to Color(0xFF6E4A75),
    Color(0xFF8FB8A8) to Color(0xFF3F6B5C),
    Color(0xFFC9A27F) to Color(0xFF86603F),
)

@Composable
private fun RecommendedPager(items: List<Recommended>) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = Motion.spatialDefault,
    )

    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            flingBehavior = flingBehavior,
        ) { page ->
            val item = items[page]
            val (start, end) = RecommendedGradients[page % RecommendedGradients.size]

            val src = remember { MutableInteractionSource() }

            Card(
                onClick = { /* TODO: open recommended item */ },
                interactionSource = src,
                shape = RoundedCornerShape(RadiusLarge),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp)
                    .pressScale(src, pressedScale = 0.97f)
                    .graphicsLayer {
                        val offset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                            .absoluteValue.coerceIn(0f, 1f)
                        val s = lerp(0.92f, 1f, 1f - offset)
                        scaleX = s
                        scaleY = s
                        alpha = lerp(0.7f, 1f, 1f - offset)
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(start, end)))
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.5f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.55f),
                                )
                            )
                    )
                    Text(
                        item.title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                            .graphicsLayer {
                                translationX = (pagerState.currentPage - page +
                                        pagerState.currentPageOffsetFraction) * 48.dp.toPx()
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        DotIndicator(
            count = items.size,
            selected = pagerState.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onDotClick = {
                scope.launch {
                    pagerState.animateScrollToPage(it, animationSpec = Motion.spatialDefault)
                }
            },
        )
    }
}

val sampleRecommended = listOf(
    Recommended("Family Worship Ideas"),
    Recommended("Ministry & Public Speaking"),
    Recommended("Deep Dive: Genesis Creation"),
    Recommended("Understanding Prophecy"),
    Recommended("Building Strong Faith")
)