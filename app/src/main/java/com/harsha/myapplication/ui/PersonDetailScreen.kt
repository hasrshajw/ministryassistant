package com.harsha.myapplication.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Amber = Color(0xFFF59E0B)
private val CardShape = RoundedCornerShape(24.dp)

// ─────────────────────────────────────────────────────────────────────────
//  PERSON DETAIL SCREEN — opened when a card in PeopleScreen is tapped
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    recordId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var record by remember(recordId) {
        mutableStateOf(PeopleRepository.load(context).firstOrNull { it.id == recordId })
    }

    var showFullProfile by rememberSaveable { mutableStateOf(false) }
    var showAddress by remember { mutableStateOf(false) }
    var showLogForm by rememberSaveable { mutableStateOf(false) }
    var editingLogId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<VisitLog?>(null) }

    fun persist(updated: PersonRecord): Boolean {
        val ok = PeopleRepository.save(context, updated)
        if (ok) record = updated
        else Toast.makeText(context, "Could not save. Please try again.", Toast.LENGTH_SHORT).show()
        return ok
    }

    fun closeLogForm() {
        showLogForm = false
        editingLogId = null
    }

    fun goBack() {
        when {
            showLogForm -> closeLogForm()
            showFullProfile -> showFullProfile = false
            else -> onDismiss()
        }
    }

    // Back closes the log form first, then "Full Details", then the whole screen.
    BackHandler { goBack() }

    val current = record

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            current == null -> "Profile"
                            showLogForm -> if (editingLogId != null) "Edit Visit" else "Log Visit"
                            showFullProfile -> "Full Details"
                            else -> current.name.ifEmpty { "Profile" }
                        },
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (current != null && !showFullProfile && !showLogForm) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingLogId = null
                        showLogForm = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Log Visit") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "This person could not be found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            val logs = remember(current) { current.visitLogs.sortedByDescending { it.timestamp } }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp),
            ) {
                when {
                    showLogForm -> LogVisitForm(
                        record = current,
                        editing = logs.firstOrNull { it.id == editingLogId },
                        onSave = { draft ->
                            if (persist(applyLogDraft(current, editingLogId, draft))) closeLogForm()
                        },
                    )
                    showFullProfile -> FullProfile(current)
                    else -> Dashboard(
                        record = current,
                        logs = logs,
                        onAddress = { showAddress = true },
                        onDetails = { showFullProfile = true },
                        onEditLog = { log ->
                            editingLogId = log.id
                            showLogForm = true
                        },
                        onDeleteLog = { log -> pendingDelete = log },
                    )
                }
            }
        }
    }

    if (showAddress && current != null) {
        AddressSheet(current.address, onDismiss = { showAddress = false })
    }

    pendingDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this visit?") },
            text = { Text("It will be removed from the activity timeline.") },
            confirmButton = {
                TextButton(onClick = {
                    current?.let { rec ->
                        persist(withLogs(rec, rec.visitLogs.filterNot { it.id == log.id }))
                    }
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  LOG VISIT — add / edit a visit, mirrors _saveData() in the Flutter screen
// ─────────────────────────────────────────────────────────────────────────
private class LogDraft(
    val topic: String,
    val question: String,
    val notes: String,
    val nextDate: String,
    val nextTime: String,
)

/** Re-sorts the logs and refreshes the record's next-visit fields from the newest one. */
private fun withLogs(rec: PersonRecord, logs: List<VisitLog>): PersonRecord {
    val sorted = logs.sortedByDescending { it.timestamp }
    val latest = sorted.firstOrNull()
    return rec.copy(
        nextVisitDate = latest?.nextVisitDate.orEmpty(),
        nextVisitTime = latest?.nextVisitTime?.ifEmpty { null },
        visitLogs = sorted,
    )
}

private fun applyLogDraft(rec: PersonRecord, editingId: String?, d: LogDraft): PersonRecord {
    val logs = if (editingId != null) {
        rec.visitLogs.map {
            if (it.id == editingId) {
                it.copy(topic = d.topic, notes = d.notes, nextVisitDate = d.nextDate, nextVisitTime = d.nextTime)
            } else it
        }
    } else {
        listOf(
            VisitLog(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                nextVisitDate = d.nextDate,
                nextVisitTime = d.nextTime,
                topic = d.topic,
                notes = d.notes,
                logType = if (rec.visitLogs.isEmpty()) "initial" else rec.type,
            )
        ) + rec.visitLogs
    }
    return withLogs(rec, logs).copy(notes = d.question.trim().ifEmpty { null })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogVisitForm(
    record: PersonRecord,
    editing: VisitLog?,
    onSave: (LogDraft) -> Unit,
) {
    val key = editing?.id ?: "new"
    var topic by rememberSaveable(key) { mutableStateOf(editing?.topic.orEmpty()) }
    var question by rememberSaveable(key) { mutableStateOf(record.notes.orEmpty()) }
    var notes by rememberSaveable(key) { mutableStateOf(editing?.notes.orEmpty()) }
    var date by rememberSaveable(key) {
        mutableStateOf(editing?.nextVisitDate ?: dateFormat().format(Date(System.currentTimeMillis() + 7 * DAY_MS)))
    }
    var time by rememberSaveable(key) {
        mutableStateOf(
            editing?.nextVisitTime
                ?: record.visitLogs.maxByOrNull { it.timestamp }?.nextVisitTime?.ifEmpty { null }
                ?: timeFormat().format(Date())
        )
    }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        DetailSectionLabel("Visit Details")
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic or Publication") },
            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Return Visit Question / Topic") },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Observation / Notes") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            minLines = 4,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        DetailSectionLabel("Next Schedule")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PickerField("Date", date, Icons.Default.CalendarToday, Modifier.weight(1f)) { pickDate = true }
            PickerField("Time", time, Icons.Default.Schedule, Modifier.weight(1f)) { pickTime = true }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onSave(LogDraft(topic.trim(), question, notes.trim(), date, time)) },
            enabled = date.isNotEmpty() && time.isNotEmpty(),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Save Visit Activity", fontWeight = FontWeight.SemiBold)
        }
    }

    if (pickDate) {
        // DatePicker works in UTC midnight millis, hence dateFormat(utc = true).
        val utc = dateFormat(utc = true)
        val initial = runCatching { utc.parse(date)?.time }.getOrNull()
            ?: utc.parse(dateFormat().format(Date()))?.time
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = utc.format(Date(it)) }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (pickTime) {
        val parsed = runCatching { timeFormat().parse(time) }.getOrNull()
        val cal = Calendar.getInstance().apply { if (parsed != null) setTime(parsed) }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, state.hour)
                        set(Calendar.MINUTE, state.minute)
                    }
                    time = timeFormat().format(picked.time)
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerField(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = colors.outline)
                Text(
                    value.ifEmpty { "Select" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  DASHBOARD
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun Dashboard(
    record: PersonRecord,
    logs: List<VisitLog>,
    onAddress: () -> Unit,
    onDetails: () -> Unit,
    onEditLog: (VisitLog) -> Unit,
    onDeleteLog: (VisitLog) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val kind = PersonKind.of(record.type)
    val phone = record.mobile.orEmpty()
    val hasPhone = phone.isNotEmpty()
    val hasAddress = record.address.isNotEmpty()

    val nextDate = logs.firstOrNull()?.nextVisitDate?.takeIf { it.isNotEmpty() } ?: record.nextVisitDate
    val nextTime = logs.firstOrNull()?.nextVisitTime?.takeIf { it.isNotEmpty() } ?: record.nextVisitTime.orEmpty()
    val parsedNext = remember(nextDate) { parseDate(nextDate) }

    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))

        // Hero
        Surface(
            shape = CircleShape,
            color = kind.accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, kind.accent.copy(alpha = 0.12f)),
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(kind.icon, contentDescription = null, tint = kind.accent, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            record.name.ifEmpty { "New Person" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = kind.accent.copy(alpha = 0.12f)) {
            Text(
                when (kind) {
                    PersonKind.Study -> "BIBLE STUDY" + (record.studyDay?.let { " · $it" } ?: "")
                    PersonKind.Inactive -> "INACTIVE"
                    PersonKind.ReturnVisit -> "RETURN VISIT"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = kind.accent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Action row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionCircle(Icons.Default.Phone, "Call", Brand2.blue, hasPhone) {
                context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${normalizePhone(phone)}")))
            }
            ActionCircle(Icons.AutoMirrored.Filled.Message, "Message", Brand2.purple, hasPhone) {
                context.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${normalizePhone(phone)}")))
            }
            ActionCircle(Icons.Default.LocationOn, "Address", colors.tertiary, hasAddress, onAddress)
            ActionCircle(Icons.Default.Person, "Details", colors.outline, true, onDetails)
        }

        Spacer(Modifier.height(24.dp))

        // Upcoming appointment
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            DetailSectionLabel("Upcoming Appointment")
            if (nextDate.isNotEmpty()) {
                Card(
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
                    border = BorderStroke(1.5.dp, colors.tertiary.copy(alpha = 0.25f)),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.tertiary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = colors.tertiary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                parsedNext?.let { SimpleDateFormat("EEEE, MMM d", Locale.US).format(it) } ?: nextDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                nextTime.ifEmpty { "Time not set" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No upcoming visit scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.outline,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Activity timeline
            DetailSectionLabel("Activity Timeline")
            if (logs.isEmpty()) {
                Surface(
                    shape = CardShape,
                    color = colors.surfaceContainerHighest,
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.History, null, tint = colors.outline, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No history yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    logs.forEachIndexed { index, log ->
                        VisitLogCard(
                            log = log,
                            visitNumber = logs.size - index,
                            isLatest = index == 0,
                            expanded = expanded[log.id] == true,
                            onToggle = { expanded[log.id] = expanded[log.id] != true },
                            onEdit = { onEditLog(log) },
                            onDelete = { onDeleteLog(log) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCircle(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (enabled) color else colors.outline.copy(alpha = 0.35f)
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceContainerLowest,
            border = BorderStroke(1.dp, if (enabled) colors.outlineVariant else colors.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) colors.onSurfaceVariant else colors.outline.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun VisitLogCard(
    log: VisitLog,
    visitNumber: Int,
    isLatest: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (isLatest) colors.tertiary else colors.outline
    val accentBg = if (isLatest) colors.tertiary.copy(alpha = 0.12f) else colors.surfaceContainerHighest
    val border = if (expanded) accent.copy(alpha = 0.3f) else colors.outlineVariant
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        border = BorderStroke(1.5.dp, border),
    ) {
        Column {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accentBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "#$visitNumber",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(log.timestamp)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (log.isImported) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = colors.tertiary.copy(alpha = 0.12f)) {
                                Text(
                                    "Imported",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.tertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        log.topic.ifEmpty { "General Visit" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = colors.outline,
                    modifier = Modifier.rotate(chevron),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Text(
                            log.notes.ifEmpty { "No detailed notes provided." },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onEdit) { Text("Edit", fontWeight = FontWeight.SemiBold) }
                        TextButton(onClick = onDelete) {
                            Text("Delete", fontWeight = FontWeight.SemiBold, color = colors.error)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  FULL PROFILE
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun FullProfile(record: PersonRecord) {
    val colors = MaterialTheme.colorScheme
    val kind = PersonKind.of(record.type)
    val interest = record.interestLevel.orEmpty()
    val interestColor = when {
        interest.contains("High") -> colors.tertiary
        interest.contains("Medium") -> Amber
        interest.contains("Do Not") -> colors.error
        else -> colors.outline
    }
    val personal = listOfNotNull(record.gender, record.age, record.language)
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
        .ifEmpty { "Not provided" }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        DetailSectionLabel("Classification")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("TYPE", kind.label, kind.accent, Modifier.weight(1f))
            if (interest.isNotEmpty()) {
                StatChip("INTEREST", interest.substringBefore(" ("), interestColor, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(32.dp))
        DetailSectionLabel("Contact Information")
        InfoCard {
            ProfileRow(Icons.Default.Phone, "MOBILE", record.mobile.orEmpty().ifEmpty { "No phone saved" }, Brand2.blue)
            HorizontalDivider(Modifier.padding(start = 56.dp), color = colors.outlineVariant)
            ProfileRow(Icons.Default.LocationOn, "ADDRESS", record.address.ifEmpty { "No address saved" }, colors.tertiary)
        }

        Spacer(Modifier.height(32.dp))
        DetailSectionLabel("Demographics & Spiritual")
        InfoCard {
            ProfileRow(Icons.Default.Person, "PERSONAL", personal, Amber)
            if (kind == PersonKind.Study) {
                HorizontalDivider(Modifier.padding(start = 56.dp), color = colors.outlineVariant)
                ProfileRow(Icons.Default.CalendarToday, "STUDY DAY", record.studyDay ?: "Not set", Brand2.purple)
            }
        }

        if (!record.notes.isNullOrEmpty()) {
            Spacer(Modifier.height(32.dp))
            DetailSectionLabel("Notes")
            InfoCard { ProfileRow(Icons.Default.Info, "RETURN VISIT QUESTION / TOPIC", record.notes, colors.outline) }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(content = content) }
}

@Composable
private fun StatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(6.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  ADDRESS SHEET
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSheet(address: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = colors.tertiary)
                Spacer(Modifier.width(12.dp))
                Text("Address", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainerHighest,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(address, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(20.dp))
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Address", address))
                        Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Address")
                }
                FilledTonalIconButton(
                    onClick = {
                        context.launch(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}"),
                            ),
                        )
                    },
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Open in Maps")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  SMALL HELPERS
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun DetailSectionLabel(text: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f))
    }
}

/** Accent colours matching PersonKind's Study (blue) and Return Visit (purple). */
private object Brand2 {
    val blue = PersonKind.Study.accent
    val purple = PersonKind.ReturnVisit.accent
}

private fun parseDate(value: String): Date? =
    if (value.isEmpty()) null else runCatching { dateFormat().apply { isLenient = false }.parse(value) }.getOrNull()

private fun normalizePhone(phone: String): String {
    val digits = phone.filter(Char::isDigit)
    return if (digits.length == 10) "+91$digits" else digits
}

private fun Context.launch(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app found to handle this action", Toast.LENGTH_SHORT).show()
    }
}