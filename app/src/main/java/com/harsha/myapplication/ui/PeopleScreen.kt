package com.harsha.myapplication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.harsha.myapplication.Lexend
import com.harsha.myapplication.ui.theme.StudyDeepGreen
import com.harsha.myapplication.ui.theme.StudyEmerald
import com.harsha.myapplication.ui.theme.pressScale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────
//  THEME — brand palette mapped onto a Material 3 color scheme
// ─────────────────────────────────────────────────────────────────────────
private object Brand {
    val deep = Color(0xFF1A301D)
    val emerald = Color(0xFF3BBF6C)
    val emeraldDark = Color(0xFF2E9A56)
    val purple = Color(0xFF7B68EE)
    val blue = Color(0xFF3B82F6)
    val muted = Color(0xFF8E9AAF)
    val waveStart = Color(0xFF83F28F)
    val page = Color(0xFFF7F9F8)
    val mint = Color(0xFFD4F2DC)
}

private val PeopleColors = lightColorScheme(
    primary = Brand.deep,
    onPrimary = Color.White,
    primaryContainer = Brand.mint,
    onPrimaryContainer = Brand.deep,
    secondaryContainer = Brand.mint,
    onSecondaryContainer = Brand.deep,
    tertiary = Brand.emerald,
    background = Brand.page,
    surface = Brand.page,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF5E6B7E),
    surfaceContainerLowest = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHighest = Color(0xFFF1F3F4),
    outline = Brand.muted,
    outlineVariant = Color(0xFFE1E4E7),
    error = Color(0xFFE74C3C),
)

private fun Typography.withFont(f: FontFamily) = copy(
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

// ─────────────────────────────────────────────────────────────────────────
//  UI ENUMS
// ─────────────────────────────────────────────────────────────────────────
/** Shared with PersonEditScreen.kt, so internal rather than private. */
internal enum class PersonKind(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color,
) {
    ReturnVisit("rv", "Return Visit", Icons.Default.Person, Brand.purple),
    Study("study", "Study", Icons.AutoMirrored.Filled.MenuBook, Brand.blue),
    Inactive("inactive", "Inactive", Icons.Default.Pause, Brand.muted);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: ReturnVisit
    }
}

private enum class PersonFilter(val label: String, val kind: PersonKind?) {
    All("All", null),
    ReturnVisits("Return Visits", PersonKind.ReturnVisit),
    Studies("Studies", PersonKind.Study),
    Inactive("Inactive", PersonKind.Inactive);

    fun matches(type: String) = kind == null || kind.key == type
}

private enum class PersonSort(val label: String, val icon: ImageVector) {
    Date("By Visit Date", Icons.Default.CalendarToday),
    Name("By Name (A–Z)", Icons.Default.SortByAlpha),
}

private enum class DueState { None, Overdue, Today }

// ─────────────────────────────────────────────────────────────────────────
//  MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun PeopleScreen(onPersonClick: (String) -> Unit = {}) {
    MaterialTheme(
        colorScheme = PeopleColors,
        typography = MaterialTheme.typography.withFont(Lexend),
    ) {
        PeopleContent(onPersonClick)
    }
}

@Composable
private fun PeopleContent(onPersonClick: (String) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    // Same colours as the "New note" FAB on the Notes screen; the selected filter pill reuses them.
    val dark = isSystemInDarkTheme()
    val accentContainer = if (dark) StudyEmerald else StudyDeepGreen
    val accentContent = if (dark) StudyDeepGreen else Color.White

    var records by remember { mutableStateOf<List<PersonRecord>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(PersonFilter.All) }
    var sort by rememberSaveable { mutableStateOf(PersonSort.Date) }
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    // Reload on first composition and every resume.
    LifecycleResumeEffect(Unit) {
        records = PeopleRepository.load(context)
        onPauseOrDispose {}
    }

    val counts = remember(records) {
        PersonFilter.entries.associateWith { f -> records.count { f.matches(it.type) } }
    }
    val displayed = remember(records, query, filter, sort) {
        filterAndSort(records, query, filter, sort)
    }
    // Collapses to an icon while scrolling down, extends again on scroll up / at top (same as Notes).
    val fabExpanded by remember {
        derivedStateOf { listState.lastScrolledBackward || !listState.canScrollBackward }
    }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        WavyHeader(
            modifier = Modifier.fillMaxWidth().height(statusBarTop + 190.dp),
            startColor = Brand.waveStart,
            endColor = MaterialTheme.colorScheme.background,
        )

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                val fabSrc = remember { MutableInteractionSource() }
                ExtendedFloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        focusManager.clearFocus()
                        showAddForm = true
                    },
                    interactionSource = fabSrc,
                    modifier = Modifier.pressScale(fabSrc),
                    containerColor = accentContainer,
                    contentColor = accentContent,
                    expanded = fabExpanded,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add person") },
                    text = { Text("Add person") },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("Directory", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                    Text(
                        "${records.size} record${if (records.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { focusManager.clearFocus() },
                        hints = SearchHints,
                        modifier = Modifier.weight(1f),
                    )
                    SortMenuButton(
                        options = PersonSort.entries,
                        selected = sort,
                        onSelect = { sort = it },
                        label = { it.label },
                        icon = { it.icon },
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PersonFilter.entries.forEach { f ->
                        FilterPill(
                            label = f.label,
                            count = counts.getValue(f),
                            selected = filter == f,
                            selectedContainer = accentContainer,
                            selectedContent = accentContent,
                            onClick = { filter = f },
                        )
                    }
                }

                if (displayed.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyState(hasQuery = query.isNotBlank())
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(displayed, key = { it.id }) { record ->
                            PersonCard(record) {
                                focusManager.clearFocus()
                                selectedId = record.id
                                onPersonClick(record.id)
                            }
                        }
                    }
                }
            }
        }

        if (showAddForm) {
            PersonEditScreen(
                onDismiss = { showAddForm = false },
                onSaved = {
                    showAddForm = false
                    records = PeopleRepository.load(context)
                },
            )
        }

        selectedId?.let { id ->
            PersonDetailScreen(
                recordId = id,
                onDismiss = {
                    selectedId = null
                    records = PeopleRepository.load(context)
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  LIST COMPONENTS
// ─────────────────────────────────────────────────────────────────────────
private val SearchHints = listOf("Name", "Number", "Address")


@Composable
private fun FilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    selectedContainer: Color,
    selectedContent: Color,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) selectedContainer else colors.surfaceContainerLowest, tween(200), label = "pillBg",
    )
    val content by animateColorAsState(
        if (selected) selectedContent else colors.onSurfaceVariant, tween(200), label = "pillFg",
    )
    val badge by animateColorAsState(
        if (selected) selectedContent.copy(alpha = 0.18f) else colors.surfaceContainerHighest, tween(200), label = "pillBadge",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
            Box(
                Modifier
                    .defaultMinSize(minWidth = 26.dp, minHeight = 26.dp)
                    .clip(CircleShape)
                    .background(badge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun PersonCard(record: PersonRecord, onClick: () -> Unit) {
    val kind = PersonKind.of(record.type)
    val date = remember(record.nextVisitDate) { parseDateOnly(record.nextVisitDate) }
    val due = remember(date, kind) {
        if (kind == PersonKind.Inactive || date == null) DueState.None
        else {
            val diff = startOfDay(date).compareTo(startOfDay(Date()))
            when {
                diff < 0 -> DueState.Overdue
                diff == 0 -> DueState.Today
                else -> DueState.None
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val borderColor = when (due) {
        DueState.Overdue -> colors.error.copy(alpha = 0.3f)
        DueState.Today -> colors.tertiary.copy(alpha = 0.4f)
        DueState.None -> colors.outlineVariant
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        border = BorderStroke(1.dp, borderColor),
    ) {
        // Plain Row instead of ListItem: ListItem forces a minimum height and top-aligns its
        // text, which left a big empty gap under the content. Equal padding + centred content
        // keeps top and bottom spacing identical on every card.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(kind.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(kind.icon, contentDescription = null, tint = kind.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (record.hasPhone) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Has phone number",
                            tint = colors.tertiary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = kind.accent.copy(alpha = 0.12f)) {
                        Text(
                            kind.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = kind.accent,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    DueText(record, due, date)
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.outline,
            )
        }
    }
}

@Composable
private fun DueText(record: PersonRecord, due: DueState, date: Date?) {
    val style = MaterialTheme.typography.labelMedium
    when (due) {
        DueState.Overdue -> Text(
            "Overdue · ${formatDay(date, withWeekday = false)}",
            style = style, color = MaterialTheme.colorScheme.error,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        DueState.Today -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
            Spacer(Modifier.width(4.dp))
            Text(
                record.nextVisitTime?.takeIf { it.isNotEmpty() }?.let { "Today at $it" } ?: "Today",
                style = style, color = Brand.emeraldDark,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        DueState.None -> Text(
            if (date != null) formatDay(date) else "No date set",
            style = style,
            color = if (date != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasQuery) "No matches found" else "No records yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasQuery) "Try a different name or mobile number." else "Tap Add person to create your first record.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  WAVY HEADER
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun WavyHeader(modifier: Modifier = Modifier, startColor: Color, endColor: Color) {
    Canvas(modifier) {
        val waveHeight = 6.dp.toPx()
        val waveLength = size.width / 4.5f
        val lineSpacing = 3.3.dp.toPx()
        val stroke = Stroke(width = 0.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val extra = size.width * 0.3f
        val fadeStart = 0.55f

        rotate(degrees = -2.86f, pivot = center) {
            var y = -extra
            while (y < size.height + extra) {
                val t = (y / size.height).coerceIn(0f, 1f)
                val alpha = if (t < fadeStart) 1f else ((1f - t) / (1f - fadeStart)).coerceIn(0f, 1f)
                val path = Path().apply {
                    var x = -extra
                    var first = true
                    while (x <= size.width + extra) {
                        val py = y + sin((x / waveLength) * 2 * PI).toFloat() * waveHeight
                        if (first) { moveTo(x, py); first = false } else lineTo(x, py)
                        x += 6f
                    }
                }
                drawPath(path, lerp(startColor, endColor, t).copy(alpha = alpha), style = stroke)
                y += lineSpacing
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  LIST HELPERS
// ─────────────────────────────────────────────────────────────────────────
private fun formatDay(date: Date?, withWeekday: Boolean = true): String =
    date?.let { SimpleDateFormat(if (withWeekday) "MMM d · EEE" else "MMM d", Locale.US).format(it) }.orEmpty()

private fun parseDateOnly(value: String?): Date? {
    if (value.isNullOrEmpty()) return null
    return try {
        dateFormat().apply { isLenient = false }.parse(value)
    } catch (_: Exception) {
        null
    }
}

private fun startOfDay(date: Date): Long = Calendar.getInstance().apply {
    time = date
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun filterAndSort(
    records: List<PersonRecord>,
    query: String,
    filter: PersonFilter,
    sort: PersonSort,
): List<PersonRecord> {
    val q = query.trim()
    val matches = records.filter { r ->
        filter.matches(r.type) && (
                q.isEmpty() ||
                        r.name.contains(q, ignoreCase = true) ||
                        r.address.contains(q, ignoreCase = true) ||
                        r.mobile.orEmpty().contains(q, ignoreCase = true)
                )
    }
    return when (sort) {
        PersonSort.Name -> matches.sortedBy { it.name.lowercase() }
        PersonSort.Date -> matches.sortedBy { it.nextVisitDate.ifEmpty { "9999-99-99" } }
    }
}