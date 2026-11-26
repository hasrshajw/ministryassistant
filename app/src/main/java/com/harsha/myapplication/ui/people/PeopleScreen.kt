@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.people

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.data.DAY_MS
import com.harsha.myapplication.data.PeopleRepository
import com.harsha.myapplication.data.PersonRecord
import com.harsha.myapplication.data.dateFormat
import com.harsha.myapplication.data.timeFormat
import com.harsha.myapplication.ui.components.AppSearchField
import com.harsha.myapplication.ui.components.SortMenuButton
import com.harsha.myapplication.ui.components.StudyTopBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────
//  SHARED LAYOUT TOKENS (match SettingsScreen)
// ─────────────────────────────────────────────────────────────────────────
private val ScreenMargin = 16.dp
/** Resting / pressed corner radius of a person card (it morphs on press). */
private val CardCornerRest = 28.dp
private val CardCornerPressed = 18.dp
/** Equal inset on top / bottom / start / end of every person card. */
private val PersonCardPadding = 12.dp


// ─────────────────────────────────────────────────────────────────────────
//  Category accents (semantic, not theme overrides)
// ─────────────────────────────────────────────────────────────────────────
private object CategoryAccent {
    val returnVisit = Color(0xFF7B68EE)
    val study = Color(0xFF3B82F6)
    val inactive = Color(0xFF8E9AAF)
}

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
    ReturnVisit("rv", "Return Visit", Icons.Default.Person, CategoryAccent.returnVisit),
    Study("study", "Study", Icons.AutoMirrored.Filled.MenuBook, CategoryAccent.study),
    Inactive("inactive", "Inactive", Icons.Default.Pause, CategoryAccent.inactive);

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
//  MAIN SCREEN — uses app Material 3 theme (no local color override)
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun PeopleScreen(onPersonClick: (String) -> Unit = {}) {
    PeopleContent(onPersonClick)
}

@Composable
private fun PeopleContent(onPersonClick: (String) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var records by remember { mutableStateOf<List<PersonRecord>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(PersonFilter.All) }
    var sort by rememberSaveable { mutableStateOf(PersonSort.Date) }
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

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
    val fabExpanded by remember {
        derivedStateOf { listState.lastScrolledBackward || !listState.canScrollBackward }
    }

    Box(Modifier.fillMaxSize().background(colors.surface)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                StudyTopBar(title = "Directory")
            },
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
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    expanded = fabExpanded,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add person") },
                    text = { Text("Add person", style = typography.labelLargeEmphasized) },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Header
                Column(Modifier.padding(horizontal = ScreenMargin).padding(top = 4.dp, bottom = 16.dp)) {
                    Text(
                        "${records.size} record${if (records.size == 1) "" else "s"}",
                        style = typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }

                // Search + sort
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenMargin),
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

                // Filter pills: round when idle, squarer when selected (Expressive toggle-button morph)
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = ScreenMargin),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PersonFilter.entries.forEach { f ->
                        FilterPill(
                            label = f.label,
                            count = counts.getValue(f),
                            selected = filter == f,
                            onClick = { filter = f },
                        )
                    }
                }

                if (displayed.isEmpty()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(hasQuery = query.isNotBlank())
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = ScreenMargin,
                            end = ScreenMargin,
                            top = 16.dp,
                            bottom = 88.dp   // clears the extended FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(displayed, key = { it.id }) { record ->
                            PersonCard(
                                record = record,
                                modifier = Modifier.animateItem(),
                            ) {
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
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val motion = MaterialTheme.motionScheme

    val selectedContent = colors.onPrimary
    val container by animateColorAsState(
        if (selected) colors.primary else colors.surfaceContainerLow,
        motion.defaultEffectsSpec(),
        label = "pillBg",
    )
    val content by animateColorAsState(
        if (selected) selectedContent else colors.onSurfaceVariant,
        motion.defaultEffectsSpec(),
        label = "pillFg",
    )
    val badge by animateColorAsState(
        if (selected) selectedContent.copy(alpha = 0.18f) else colors.surfaceContainerHighest,
        motion.defaultEffectsSpec(),
        label = "pillBadge",
    )
    // Pill when idle, squarer when selected; the spring gives the morph its bounce.
    val corner by animateDpAsState(
        if (selected) 14.dp else 20.dp,
        motion.fastSpatialSpec(),
        label = "pillCorner",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                label,
                style = if (selected) typography.labelLargeEmphasized else typography.labelLarge,
                maxLines = 1,
            )
            Box(
                Modifier
                    .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                    .clip(CircleShape)
                    .background(badge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    style = typography.labelMediumEmphasized,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/**
 * Person card shared by People and Home.
 *
 * Built from a plain [Row] instead of [ListItem]: ListItem adds its own minimum height and
 * internal padding, which left extra empty space under the supporting line. Here every side
 * gets exactly [PersonCardPadding]. It is an Expressive card: large corners that morph on press, a cookie-shaped icon well and a tonal chevron button.
 */
@Composable
internal fun PersonCard(
    record: PersonRecord,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,   // last, so `PersonCard(record) { ... }` trailing-lambda calls keep working
) {
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
    val typography = MaterialTheme.typography

    // Like the Settings cards: no outline by default, only a highlight when something is due.
    val border = when (due) {
        DueState.Overdue -> BorderStroke(1.dp, colors.error.copy(alpha = 0.5f))
        DueState.Today -> BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.6f))
        DueState.None -> null
    }

    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) CardCornerPressed else CardCornerRest,
        motion.fastSpatialSpec(),
        label = "cardCorner",
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = border,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PersonCardPadding),          // identical on all four sides
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Leading icon well: Expressive cookie shape
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = kind.accent.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        kind.icon,
                        contentDescription = null,
                        tint = kind.accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Text block
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.name,
                        style = typography.titleMediumEmphasized,
                        color = colors.onSurface,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = kind.accent.copy(alpha = 0.12f),
                    ) {
                        Text(
                            kind.label.uppercase(),
                            style = typography.labelSmallEmphasized,
                            color = kind.accent,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    DueText(record, due, date)
                }
            }

            // Tonal circular chevron
            Surface(
                shape = CircleShape,
                color = colors.surfaceContainerHighest,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}


@Composable
private fun DueText(record: PersonRecord, due: DueState, date: Date?) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.labelMedium
    when (due) {
        DueState.Overdue -> Text(
            "Overdue · ${formatDay(date, withWeekday = false)}",
            style = style,
            color = colors.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        DueState.Today -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.tertiary)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                record.nextVisitTime?.takeIf { it.isNotEmpty() }?.let { "Today at $it" } ?: "Today",
                style = style,
                color = colors.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DueState.None -> Text(
            if (date != null) formatDay(date) else "No date set",
            style = style,
            color = if (date != null) colors.onSurfaceVariant else colors.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = colors.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasQuery) "No matches found" else "No records yet",
            style = typography.titleMediumEmphasized,
            color = colors.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasQuery) "Try a different name or mobile number."
            else "Tap Add person to create your first record.",
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  LIST HELPERS
// ─────────────────────────────────────────────────────────────────────────
private fun formatDay(date: Date?, withWeekday: Boolean = true): String =
    date?.let {
        SimpleDateFormat(if (withWeekday) "MMM d · EEE" else "MMM d", Locale.US).format(it)
    }.orEmpty()

internal fun parseDateOnly(value: String?): Date? {
    if (value.isNullOrEmpty()) return null
    return try {
        dateFormat().apply { isLenient = false }.parse(value)
    } catch (_: Exception) {
        null
    }
}

internal fun startOfDay(date: Date): Long = Calendar.getInstance().apply {
    time = date
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * Whole calendar days from today to the record's next visit date
 * (0 = today, 1 = tomorrow, negative = overdue). Null for inactive people or when no date is set.
 */
internal fun daysFromToday(record: PersonRecord): Int? {
    if (PersonKind.of(record.type) == PersonKind.Inactive) return null
    val date = parseDateOnly(record.nextVisitDate) ?: return null
    val diff = startOfDay(date) - startOfDay(Date())
    return Math.round(diff.toDouble() / DAY_MS).toInt()
}

/** Minutes since midnight for sorting by "h:mm a" visit time; no time sorts last. */
internal fun visitTimeMinutes(record: PersonRecord): Int {
    val t = record.nextVisitTime?.takeIf { it.isNotEmpty() } ?: return Int.MAX_VALUE
    return try {
        val cal = Calendar.getInstance().apply { time = timeFormat().parse(t)!! }
        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    } catch (_: Exception) {
        Int.MAX_VALUE
    }
}

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