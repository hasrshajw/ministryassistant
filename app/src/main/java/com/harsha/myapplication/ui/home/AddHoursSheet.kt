@file:OptIn(ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ═════════════════════════════════════════════════════════════════════════
//  ADD HOURS SHEET: standard Material 3 modal bottom sheet.
//  Layout (top to bottom, sections split by hairline dividers):
//    Header → Date → Time spent + Quick add → Total + Save
//  Colours, shapes and type all come from MaterialTheme; nothing is hard-coded.
// ═════════════════════════════════════════════════════════════════════════

private const val MaxMinutes = 24 * 60

@Composable
fun AddHoursSheet(
    onDismiss: () -> Unit,
    onSave: (hours: Double, date: Date) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hoursText by rememberSaveable { mutableStateOf("") }
    var minutesText by rememberSaveable { mutableStateOf("") }

    val hours = hoursText.toIntOrNull() ?: 0
    val minutes = minutesText.toIntOrNull() ?: 0
    val totalMinutes = hours * 60 + minutes
    val minutesInvalid = minutes > 59
    val overLimit = totalMinutes > MaxMinutes
    val canSave = totalMinutes in 1..MaxMinutes && !minutesInvalid

    fun addMinutes(delta: Int) {
        val total = (totalMinutes + delta).coerceAtMost(MaxMinutes)
        hoursText = (total / 60).toString()
        minutesText = (total % 60).toString()
    }

    val isToday = isSameLocalDay(selectedDateMillis, System.currentTimeMillis())
    val isYesterday = isSameLocalDay(selectedDateMillis, daysAgoMillis(1))
    val dayIndex = when {
        isYesterday -> 0
        isToday -> 1
        else -> 2
    }
    val dayLabels = listOf(
        "Yesterday",
        "Today",
        if (dayIndex == 2) {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(selectedDateMillis))
        } else {
            "Pick date"
        },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
    ) {
        // Vertical rhythm: 24dp side margins, 20dp between sections, 12dp inside a section.
        // A hairline divider separates each section from the next.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Header ───────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Add hours",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
                Text(
                    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                        .format(Date(selectedDateMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = colors.outlineVariant)

            // ── Date ─────────────────────────────────────────────
            SheetSection("Date") {
                ConnectedDayGroup(
                    labels = dayLabels,
                    selectedIndex = dayIndex,
                    onSelect = { index ->
                        focusManager.clearFocus()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (index) {
                            0 -> selectedDateMillis = daysAgoMillis(1)
                            1 -> selectedDateMillis = System.currentTimeMillis()
                            else -> showDatePicker = true
                        }
                    },
                )
            }

            HorizontalDivider(color = colors.outlineVariant)

            // ── Time spent ───────────────────────────────────────
            SheetSection("Time spent") {
                val fieldColors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
                val numberStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = fieldColors,
                        label = { Text("Hours") },
                        placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        singleLine = true,
                        isError = overLimit,
                        textStyle = numberStyle,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        ),
                        supportingText = if (overLimit) {
                            { Text("Max 24 hr") }
                        } else null,
                    )
                    TextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = fieldColors,
                        label = { Text("Minutes") },
                        placeholder = { Text("0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        singleLine = true,
                        isError = minutesInvalid,
                        textStyle = numberStyle,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                        supportingText = if (minutesInvalid) {
                            { Text("0 to 59") }
                        } else null,
                    )
                }
            }

            // ── Quick add ────────────────────────────────────────
            SheetSection("Quick add") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("+15 min" to 15, "+30 min" to 30, "+1 hr" to 60).forEach { (label, delta) ->
                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                addMinutes(delta)
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.outlineVariant)

            // ── Total + Save ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Total",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    formatDuration(totalMinutes / 60, totalMinutes % 60),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (canSave) colors.onSurface else colors.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(totalMinutes / 60.0, Date(selectedDateMillis))
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("Save", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

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
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = utcDayToLocalMillis(it)
                        }
                        showDatePicker = false
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

/** A labelled block of the sheet: small caption on top, content 12dp below it. */
@Composable
internal fun SheetSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/**
 * M3 connected button group (single-select), small size: equal-width buttons, 2dp gap.
 * Unselected buttons keep small inner corners; the selected one is fully round and filled with
 * the theme's primary colour. Corner radii animate with a spring.
 */
@Composable
internal fun ConnectedDayGroup(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val height = 40.dp
    val full = height / 2
    val inner = 8.dp
    val last = labels.lastIndex

    Row(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val startTarget = if (selected || index == 0) full else inner
            val endTarget = if (selected || index == last) full else inner
            val corner = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.8f, stiffness = 500f)
            val start by animateDpAsState(startTarget, corner, label = "cornerStart$index")
            val end by animateDpAsState(endTarget, corner, label = "cornerEnd$index")
            val container by animateColorAsState(
                if (selected) colors.primary else colors.surfaceContainerHighest,
                label = "dayContainer$index",
            )
            val content by animateColorAsState(
                if (selected) colors.onPrimary else colors.onSurface,
                label = "dayContent$index",
            )
            val shape = RoundedCornerShape(topStart = start, topEnd = end, bottomEnd = end, bottomStart = start)

            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(shape)
                    .background(container)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

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

private fun localDayToUtcMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcDayToLocalMillis(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
    }.timeInMillis
}