package com.harsha.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Date
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────
//  NEW PERSON SCREEN
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditScreen(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(PersonKind.ReturnVisit) }
    var studyDay by rememberSaveable { mutableStateOf("Monday") }
    var date by rememberSaveable { mutableStateOf(dateFormat().format(Date(System.currentTimeMillis() + 7 * DAY_MS))) }
    var time by rememberSaveable { mutableStateOf(timeFormat().format(Date())) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var observations by rememberSaveable { mutableStateOf("") }
    var logNotes by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var interestLevel by rememberSaveable { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    val mobileInvalid = mobile.isNotEmpty() && mobile.length != 10
    val canSave = name.isNotBlank() && !mobileInvalid && date.isNotEmpty() && time.isNotEmpty()

    fun save() {
        if (!canSave) return
        val log = VisitLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            nextVisitDate = date,
            nextVisitTime = time,
            topic = topic,
            notes = logNotes,
            logType = "initial",
        )
        val record = PersonRecord(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            gender = gender.ifEmpty { null },
            age = age.ifEmpty { null },
            mobile = mobile.ifEmpty { null },
            address = address,
            interestLevel = interestLevel.ifEmpty { null },
            nextVisitDate = date,
            nextVisitTime = time,
            notes = observations.ifEmpty { null },
            type = kind.key,
            studyDay = if (kind == PersonKind.Study) studyDay else null,
            visitLogs = listOf(log),
        )
        if (PeopleRepository.save(context, record)) onSaved()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("New Profile", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("Identity")
            FormField("Full Name", Icons.Default.Person, name, { name = it })

            SectionLabel("Classification", Modifier.padding(top = 8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PersonKind.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = kind == option,
                        onClick = { kind = option },
                        shape = SegmentedButtonDefaults.itemShape(index, PersonKind.entries.size),
                        label = { Text(option.label, maxLines = 1) },
                    )
                }
            }
            AnimatedVisibility(visible = kind == PersonKind.Study) {
                DropdownField(
                    label = "Study Day",
                    icon = Icons.Default.CalendarToday,
                    value = studyDay,
                    options = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
                    onSelect = { studyDay = it },
                )
            }

            SectionLabel("First Visit Schedule", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerField("Initial Date", Icons.Default.CalendarToday, date, { showDatePicker = true }, Modifier.weight(1f))
                PickerField("Initial Time", Icons.Default.Schedule, time, { showTimePicker = true }, Modifier.weight(1f))
            }

            SectionLabel("Contact Details", Modifier.padding(top = 8.dp))
            FormField(
                label = "Mobile Number",
                icon = Icons.Default.Phone,
                value = mobile,
                onValueChange = { mobile = it.filter(Char::isDigit).take(10) },
                keyboardType = KeyboardType.Phone,
                isError = mobileInvalid,
                supportingText = if (mobileInvalid) "Enter a 10-digit number" else null,
            )
            FormField("Address / Location", Icons.Default.LocationOn, address, { address = it })

            SectionLabel("Ministry Start Info", Modifier.padding(top = 8.dp))
            FormField("Initial Topic / Publication", Icons.AutoMirrored.Filled.MenuBook, topic, { topic = it })
            FormField("Return Visit Question / Topic", Icons.AutoMirrored.Filled.MenuBook, observations, { observations = it })
            FormField("Observation / Notes", Icons.Default.Edit, logNotes, { logNotes = it }, maxLines = 4)

            SectionLabel("Additional Info", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField(
                    label = "Gender",
                    icon = Icons.Default.Person,
                    value = gender,
                    options = listOf("Male", "Female", "Others"),
                    onSelect = { gender = it },
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = "Age Range",
                    icon = Icons.Default.Person,
                    value = age,
                    options = listOf("Child", "Teen", "Young Adult", "Adult", "Senior"),
                    onSelect = { age = it },
                    modifier = Modifier.weight(1f),
                )
            }
            DropdownField(
                label = "Interest Level",
                icon = Icons.Default.Person,
                value = interestLevel,
                options = listOf("High (Hot)", "Medium (Warm)", "Low (Cold)", "Do Not Call"),
                onSelect = { interestLevel = it },
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = ::save,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Create Profile", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initial = date,
            onConfirm = { date = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        TimePickerSheet(
            initial = time,
            onConfirm = { time = it; showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  FORM COMPONENTS
// ─────────────────────────────────────────────────────────────────────────
private val FieldShape = RoundedCornerShape(16.dp)

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun FormField(
    label: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = maxLines == 1,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        shape = FieldShape,
    )
}

/** Read-only field whose whole surface is tappable (date/time pickers, dropdown anchors). */
@Composable
private fun PickerField(
    label: String,
    icon: ImageVector,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    role: Role = Role.Button,
) {
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            trailingIcon = trailingIcon?.let { { Icon(it, contentDescription = null) } },
            singleLine = true,
            shape = FieldShape,
        )
        // OutlinedTextField consumes taps, so overlay a click target.
        Box(
            Modifier
                .matchParentSize()
                .clip(FieldShape)
                .clickable(role = role, onClick = onClick)
        )
    }
}

@Composable
private fun DropdownField(
    label: String,
    icon: ImageVector,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        PickerField(
            label = label,
            icon = icon,
            value = value,
            onClick = { expanded = true },
            trailingIcon = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
            role = Role.DropdownList,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    trailingIcon = {
                        if (option == value) Icon(Icons.Default.Check, contentDescription = "Selected")
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // DatePicker works in UTC midnight millis; parse/format in UTC to avoid off-by-one days.
    val utc = remember { dateFormat(utc = true) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = remember { runCatching { utc.parse(initial)?.time }.getOrNull() },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onConfirm(utc.format(Date(it))) } },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val fmt = remember { timeFormat() }
    val start = remember {
        Calendar.getInstance().apply { runCatching { fmt.parse(initial) }.getOrNull()?.let { time = it } }
    }
    val state = rememberTimePickerState(
        initialHour = start.get(Calendar.HOUR_OF_DAY),
        initialMinute = start.get(Calendar.MINUTE),
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, state.hour)
                    set(Calendar.MINUTE, state.minute)
                }
                onConfirm(fmt.format(cal.time))
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}