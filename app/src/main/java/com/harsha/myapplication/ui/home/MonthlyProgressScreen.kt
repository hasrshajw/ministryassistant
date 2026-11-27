package com.harsha.myapplication.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.data.HourEntry
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.data.SetupRepository
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.components.appSharedBoundsOrNone
import com.harsha.myapplication.ui.theme.revealOnEnter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Full page opened from the Monthly progress card on Home.
 * Shows the month's total against the goal chosen in Setup, plus every logged entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyProgressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // 0 = this month, -1 = last month, ...
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val monthCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = monthCal.get(Calendar.YEAR)
    val month = monthCal.get(Calendar.MONTH)
    val isCurrentMonth = monthOffset == 0

    val goalHours = SetupRepository.monthlyGoalHours.coerceAtLeast(1)
    val goalMinutes = goalHours * 60
    val entries = HoursRepository.entriesInMonth(year, month)   // snapshot state: updates live
    val completedMinutes = entries.sumOf { it.minutes }
    val remainingMinutes = (goalMinutes - completedMinutes).coerceAtLeast(0)
    val progress = (completedMinutes / goalMinutes.toFloat()).coerceIn(0f, 1f)

    val daysLeft = if (isCurrentMonth) {
        val now = Calendar.getInstance()
        now.getActualMaximum(Calendar.DAY_OF_MONTH) - now.get(Calendar.DAY_OF_MONTH) + 1
    } else 0
    val paceText = when {
        remainingMinutes == 0 -> "Goal reached"
        daysLeft > 0 -> "${fmtMinutes((remainingMinutes / daysLeft.toFloat()).roundToInt())} / day to finish"
        else -> "${fmtMinutes(remainingMinutes)} short of goal"
    }

    var deleteTarget by remember { mutableStateOf<HourEntry?>(null) }

    Scaffold(
        // Same key as the card on Home, so the card spreads out into this page.
        modifier = Modifier.appSharedBoundsOrNone(key = "monthly-card", cornerRadius = 28.dp),
        containerColor = colors.background,
        topBar = {
            // Same top bar as Daily Text: title + subtitle line, round back button.
            StudyTopBar(
                title = "Monthly progress",
                subtitle = "${goalHours}h monthly goal",
                navigationIcon = {
                    StudyBarIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Month switcher ──────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().revealOnEnter(index = 2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { monthOffset -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { monthOffset += 1 }, enabled = !isCurrentMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            // ── Summary ─────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth().revealOnEnter(index = 3),
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                            val track = colors.onPrimaryContainer.copy(alpha = 0.15f)
                            val accent = colors.primary
                            Canvas(Modifier.fillMaxSize()) {
                                val sw = 14.dp.toPx()
                                val d = size.minDimension - sw
                                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                                val stroke = Stroke(width = sw, cap = StrokeCap.Round)
                                drawArc(track, -90f, 360f, false, tl, Size(d, d), style = stroke)
                                if (progress > 0f) {
                                    drawArc(accent, -90f, progress * 360f, false, tl, Size(d, d), style = stroke)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(progress * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.headlineLarge,
                                )
                                Text("Completed", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text(
                            "${fmtMinutes(completedMinutes)} of ${goalHours}h",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(paceText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().revealOnEnter(index = 4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile("Logged", fmtMinutes(completedMinutes), Modifier.weight(1f))
                    StatTile("Goal", "${goalHours}h", Modifier.weight(1f))
                    StatTile("Left", fmtMinutes(remainingMinutes), Modifier.weight(1f))
                }
            }

            // ── Entries ─────────────────────────────────────────
            item {
                Text(
                    "Entries",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.revealOnEnter(index = 5).padding(top = 12.dp, bottom = 4.dp),
                )
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        "No hours logged this month yet. Use the Hours button on Home to add some.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.revealOnEnter(index = 6).padding(vertical = 8.dp),
                    )
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                                        .format(Date(entry.dateMillis)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurface,
                                )
                                Text(
                                    fmtMinutes(entry.minutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.primary,
                                )
                            }
                            IconButton(onClick = { deleteTarget = entry }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove ${fmtMinutes(entry.minutes)} from your monthly total?") },
            confirmButton = {
                TextButton(onClick = {
                    HoursRepository.delete(context, entry.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1)
        }
    }
}

/** 90 -> "1h 30m", 120 -> "2h", 45 -> "45m", 0 -> "0m". */
private fun fmtMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
