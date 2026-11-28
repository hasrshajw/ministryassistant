@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class,
)

package com.harsha.myapplication.ui.people

import com.harsha.myapplication.telemetry.CrashReporter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.LocalSetBottomBarHidden
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.data.DAY_MS
import com.harsha.myapplication.data.PeopleRepository
import com.harsha.myapplication.data.PersonRecord
import com.harsha.myapplication.data.VisitLog
import com.harsha.myapplication.data.dateFormat
import com.harsha.myapplication.data.timeFormat
import com.harsha.myapplication.ui.components.StudyBarIconButton
import java.util.Calendar
import java.util.Date
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────
//  PERSON EDIT SCREEN  (Material 3 Expressive)
//
//  Record keeping for return visits and Bible studies.
//
//  Layout, top to bottom:
//    • LargeFlexibleTopAppBar (collapses on scroll)
//    • Name
//    • Visit type (Return visit / Bible study; "inactive" only offered when editing)
//    • "When is your next visit?" question card with compact date + time tiles
//    • Contact details
//    • Conversation notes (topic, question to prepare, observations; grow with the text)
//    • Optional details about the person
//    • Bottom save bar
//
//  Needs material3 >= 1.4.0 (expressive APIs).
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun PersonEditScreen(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    /** Null = create a new profile; non-null = edit this person. */
    existing: PersonRecord? = null,
) {
    val context = LocalContext.current
    val isEdit = existing != null

    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(existing?.let { PersonKind.of(it.type) } ?: PersonKind.ReturnVisit) }
    var studyDay by rememberSaveable { mutableStateOf(existing?.studyDay ?: "Monday") }
    var date by rememberSaveable {
        mutableStateOf(existing?.nextVisitDate ?: dateFormat().format(Date(System.currentTimeMillis() + 7 * DAY_MS)))
    }
    var time by rememberSaveable {
        mutableStateOf(if (existing != null) existing.nextVisitTime.orEmpty() else timeFormat().format(Date()))
    }
    var mobile by rememberSaveable { mutableStateOf(existing?.mobile.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(existing?.address.orEmpty()) }
    var topic by rememberSaveable { mutableStateOf("") }
    var observations by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var logNotes by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(existing?.gender.orEmpty()) }
    var age by rememberSaveable { mutableStateOf(existing?.age.orEmpty()) }
    var interestLevel by rememberSaveable { mutableStateOf(existing?.interestLevel.orEmpty()) }
    var language by rememberSaveable { mutableStateOf(existing?.language.orEmpty()) }
    var placeMet by rememberSaveable { mutableStateOf(existing?.placeMet.orEmpty()) }
    var bestTime by rememberSaveable { mutableStateOf(existing?.bestTime.orEmpty()) }
    var studyMaterial by rememberSaveable { mutableStateOf(existing?.studyMaterial.orEmpty()) }
    var meetingStatus by rememberSaveable { mutableStateOf(existing?.meetingStatus.orEmpty()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    // Hide the bottom navigation bar while this screen is shown (from Home or People).
    val setBottomBarHidden = LocalSetBottomBarHidden.current
    DisposableEffect(Unit) {
        setBottomBarHidden(true)
        onDispose { setBottomBarHidden(false) }
    }

    val mobileInvalid = mobile.isNotEmpty() && mobile.length != 10
    val canSave = name.isNotBlank() && !mobileInvalid && (isEdit || (date.isNotEmpty() && time.isNotEmpty()))

    // New contacts are either a return visit or a study. Other types (e.g. inactive) are only
    // offered while editing an existing person.
    val kinds = if (isEdit) {
        PersonKind.entries.toList()
    } else {
        PersonKind.entries.filter { it == PersonKind.ReturnVisit || it == PersonKind.Study }
    }

    val visitQuestion = when {
        kind == PersonKind.Study -> if (isEdit) "When is your next study?" else "When is your Bible study?"
        isEdit -> "When is your next return visit?"
        else -> "When is your first return visit?"
    }

    fun save() {
        if (!canSave) return
        if (existing != null) {
            val updated = existing.copy(
                name = name.trim(),
                gender = gender.ifEmpty { null },
                age = age.ifEmpty { null },
                mobile = mobile.ifEmpty { null },
                address = address,
                interestLevel = interestLevel.ifEmpty { null },
                nextVisitDate = date,
                nextVisitTime = time.ifEmpty { null },
                notes = observations.ifEmpty { null },
                type = kind.key,
                studyDay = if (kind == PersonKind.Study) studyDay else null,
                language = language.ifEmpty { null },
                placeMet = placeMet.ifEmpty { null },
                bestTime = bestTime.ifEmpty { null },
                studyMaterial = if (kind == PersonKind.Study) studyMaterial.trim().ifEmpty { null } else null,
                meetingStatus = meetingStatus.ifEmpty { null },
            )
            if (PeopleRepository.save(context, updated)) onSaved()
            return
        }
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
            language = language.ifEmpty { null },
            placeMet = placeMet.ifEmpty { null },
            bestTime = bestTime.ifEmpty { null },
            studyMaterial = if (kind == PersonKind.Study) studyMaterial.trim().ifEmpty { null } else null,
            meetingStatus = meetingStatus.ifEmpty { null },
        )
        if (PeopleRepository.save(context, record)) onSaved()
    }

    val colors = MaterialTheme.colorScheme
    val clearFocus = rememberClearFocus()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(if (isEdit) "Edit details" else "New contact") },
                subtitle = {
                    Text(if (isEdit) existing.name else "Record a return visit or Bible study")
                },
                navigationIcon = {
                    StudyBarIconButton(
                        onClick = onDismiss,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            SaveBar(
                label = when {
                    name.isBlank() -> "Add a name to continue"
                    mobileInvalid -> "Check the mobile number"
                    isEdit -> "Save changes"
                    else -> "Save contact"
                },
                enabled = canSave,
                onClick = { save() },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                // Tap on any empty area: un-highlight the field and hide the keyboard.
                .pointerInput(Unit) { detectTapGestures(onTap = { clearFocus() }) }
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormSection(
                title = "Name",
                subtitle = "Who did you meet?",
                icon = Icons.Default.Person,
                modifier = Modifier.revealOnEnter(index = 0),
            ) {
                ExpressiveField(
                    label = "Full name",
                    icon = Icons.Default.Person,
                    value = name,
                    onValueChange = { name = it },
                    capitalization = KeyboardCapitalization.Words,
                )
            }

            FormSection(
                title = "Visit type",
                subtitle = "Return visit or Bible study?",
                icon = Icons.Default.Category,
                modifier = Modifier.revealOnEnter(index = 1),
            ) {
                ConnectedToggleGroup(
                    labels = kinds.map { displayLabel(it) },
                    selectedIndex = kinds.indexOf(kind),
                    onToggle = { index, checked -> if (checked) kind = kinds[index] },
                )
                AnimatedVisibility(
                    visible = kind == PersonKind.Study,
                    enter = expandVertically(Motion.spatialDefault()) + fadeIn(effects()),
                    exit = shrinkVertically(Motion.spatialDefault()) + fadeOut(effects()),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Study day",
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            weekDays.forEach { day ->
                                ChoiceChip(
                                    label = day.take(3),
                                    selected = studyDay == day,
                                    onClick = { studyDay = day },
                                )
                            }
                        }
                        ExpressiveField(
                            label = "Study material / lesson",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            value = studyMaterial,
                            onValueChange = { studyMaterial = it },
                            capitalization = KeyboardCapitalization.Sentences,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            FormSection(
                title = visitQuestion,
                subtitle = "Set the date and time",
                icon = Icons.Default.Event,
                modifier = Modifier.revealOnEnter(index = 2),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerCard(Icons.Default.CalendarToday, date, { clearFocus(); showDatePicker = true }, Modifier.weight(1f))
                    PickerCard(Icons.Default.Schedule, time, { clearFocus(); showTimePicker = true }, Modifier.weight(1f))
                }
            }

            FormSection(
                title = "Contact",
                subtitle = "How to reach them",
                icon = Icons.Default.Call,
                modifier = Modifier.revealOnEnter(index = 3),
            ) {
                ExpressiveField(
                    label = "Mobile number",
                    icon = Icons.Default.Phone,
                    value = mobile,
                    onValueChange = { mobile = it.filter(Char::isDigit).take(10) },
                    keyboardType = KeyboardType.Phone,
                    isError = mobileInvalid,
                    supportingText = if (mobileInvalid) "Enter a 10-digit number (${mobile.length}/10)" else null,
                )
                ExpressiveField(
                    label = "Address / location",
                    icon = Icons.Default.LocationOn,
                    value = address,
                    onValueChange = { address = it },
                    capitalization = KeyboardCapitalization.Sentences,
                )
            }

            FormSection(
                title = "How you met",
                subtitle = "Optional. Where and when to find them",
                icon = Icons.Default.Explore,
                modifier = Modifier.revealOnEnter(index = 4),
            ) {
                GroupLabel("Where did you meet?")
                SingleChoicePills(placeOptions, placeMet) { placeMet = it }

                GroupLabel("Best time to visit", Modifier.padding(top = 4.dp))
                SingleChoicePills(bestTimeOptions, bestTime) { bestTime = it }
            }

            FormSection(
                title = if (isEdit) "Follow-up notes" else "First conversation",
                subtitle = if (isEdit) "What to prepare for next time" else "What you shared and what to prepare",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                modifier = Modifier.revealOnEnter(index = 5),
            ) {
                // The topic and observations of the first conversation belong to the first visit log,
                // so they are only asked for when creating.
                if (!isEdit) {
                    NoteField(
                        label = "Topic / publication shared",
                        placeholder = "What did you talk about or leave with them?",
                        icon = Icons.Default.Forum,
                        value = topic,
                        onValueChange = { topic = it },
                    )
                }
                NoteField(
                    label = "Question for next visit",
                    placeholder = "What question will you raise next time?",
                    icon = Icons.Default.QuestionMark,
                    value = observations,
                    onValueChange = { observations = it },
                )
                if (!isEdit) {
                    NoteField(
                        label = "Observations",
                        placeholder = "Interest, family, concerns…",
                        icon = Icons.AutoMirrored.Filled.Notes,
                        value = logNotes,
                        onValueChange = { logNotes = it },
                    )
                }
            }

            FormSection(
                title = "About them",
                subtitle = "Optional. Tap a selection again to clear it",
                icon = Icons.Default.Info,
                modifier = Modifier.revealOnEnter(index = 6),
            ) {
                GroupLabel("Gender")
                val genders = listOf("Male", "Female", "Others")
                ConnectedToggleGroup(
                    labels = genders,
                    selectedIndex = genders.indexOf(gender),
                    onToggle = { index, checked -> gender = if (checked) genders[index] else "" },
                )

                GroupLabel("Age group", Modifier.padding(top = 4.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                ) {
                    ageOptions.forEach { option ->
                        OptionPill(
                            label = option.label,
                            caption = option.range,
                            selected = age == option.label,
                            onClick = { age = if (age == option.label) "" else option.label },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                GroupLabel("Interest level", Modifier.padding(top = 4.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                ) {
                    interestOptions.forEach { option ->
                        InterestTile(
                            option = option,
                            selected = interestLevel == option.value,
                            onClick = { interestLevel = if (interestLevel == option.value) "" else option.value },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                GroupLabel("Language", Modifier.padding(top = 4.dp))
                // Keep a previously saved language visible even if it is not one of the presets.
                val languageChoices = remember(language) {
                    if (language.isEmpty() || language in languageOptions) languageOptions else languageOptions + language
                }
                SingleChoicePills(languageChoices, language) { language = it }

                GroupLabel("Meetings", Modifier.padding(top = 4.dp))
                SingleChoicePills(meetingOptions, meetingStatus) { meetingStatus = it }
            }
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
//  DATA
// ─────────────────────────────────────────────────────────────────────────
private val weekDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Full, readable names for the visit type buttons. */
private fun displayLabel(kind: PersonKind): String = when (kind) {
    PersonKind.ReturnVisit -> "Return visit"
    PersonKind.Study -> "Bible study"
    else -> kind.label
}

/** [value] is what gets stored (unchanged from before); [short] is the tile title, [hint] the caption. */
private data class InterestOption(
    val value: String,
    val short: String,
    val hint: String,
    val icon: ImageVector,
    val accent: Color,
)

private val interestOptions = listOf(
    InterestOption("High (Hot)", "Hot", "Very keen", Icons.Default.LocalFireDepartment, Color(0xFFE5484D)),
    InterestOption("Medium (Warm)", "Warm", "Some interest", Icons.Default.WbSunny, Color(0xFFF08C00)),
    InterestOption("Low (Cold)", "Cold", "Not much yet", Icons.Default.AcUnit, Color(0xFF2D9CDB)),
    InterestOption("Do Not Call", "Do not call", "Don't visit", Icons.Default.DoNotDisturbOn, Color(0xFF7A7F7D)),
)

/** [label] is what gets stored; [range] is only a hint under the label. */
private data class AgeOption(val label: String, val range: String)

private val ageOptions = listOf(
    AgeOption("Child", "Under 13"),
    AgeOption("Teen", "13–17"),
    AgeOption("Young Adult", "18–29"),
    AgeOption("Adult", "30–59"),
    AgeOption("Senior", "60+"),
)

private val placeOptions = listOf(
    "House to house", "Street", "Public witnessing", "Informal", "Phone / letter", "Business", "Referral",
)
private val bestTimeOptions = listOf("Morning", "Afternoon", "Evening", "Weekend")
private val languageOptions = listOf("Telugu", "English", "Hindi", "Urdu", "Tamil", "Other")
private val meetingOptions = listOf("Not yet", "Invited", "Attended once", "Attends regularly")

/** Returns a function that drops focus (removing the field highlight) and hides the keyboard. */
@Composable
private fun rememberClearFocus(): () -> Unit {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focus, keyboard) {
        {
            focus.clearFocus()
            keyboard?.hide()
        }
    }
}

/** Critically damped spring for fades / colour (Material "effects" family, never bounces). */
private fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

// ─────────────────────────────────────────────────────────────────────────
//  SECTION CARD
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun FormSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .animateContentSize(Motion.spatialDefault()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(colors.primaryContainer, MaterialShapes.Cookie6Sided.toShape()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    // The title can be a full question ("When is your first return visit?"), so let it wrap.
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = { fadeIn(effects()) togetherWith fadeOut(effects()) },
                        label = "sectionTitle",
                    ) { text ->
                        Text(text, style = MaterialTheme.typography.titleMediumEmphasized, color = colors.onSurface)
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// ─────────────────────────────────────────────────────────────────────────
//  INPUTS
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun fieldColors(): TextFieldColors {
    val c = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        unfocusedContainerColor = c.surfaceContainerHighest,
        focusedContainerColor = c.surfaceContainerLowest,
        errorContainerColor = c.surfaceContainerHighest,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = c.primary,
        focusedLabelColor = c.primary,
        focusedLeadingIconColor = c.primary,
    )
}

/**
 * Filled text field with an animated focus ring and a rounded Expressive shape.
 *
 * Single line (maxLines == 1): leading icon + clear button.
 * Multi line (maxLines > 1): no icons, so long text wraps cleanly and the field grows with it
 * (Enter inserts a new line).
 */
@Composable
private fun ExpressiveField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    minLines: Int = 1,
    maxLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val multiLine = maxLines > 1
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ring by animateColorAsState(
        targetValue = when {
            isError -> colors.error
            focused -> colors.primary
            else -> colors.outlineVariant
        },
        animationSpec = effects(),
        label = "fieldRing",
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, ring, shape),
        label = { Text(label) },
        leadingIcon = if (icon != null && !multiLine) {
            { Icon(icon, contentDescription = null) }
        } else null,
        trailingIcon = if (!multiLine) {
            {
                AnimatedVisibility(
                    visible = focused && value.isNotEmpty(),
                    enter = scaleIn(Motion.spatialFast) + fadeIn(effects()),
                    exit = scaleOut(Motion.spatialFast) + fadeOut(effects()),
                ) {
                    IconButton(onClick = { onValueChange("") }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Default.Close, contentDescription = "Clear $label")
                    }
                }
            }
        } else null,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = !multiLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = if (multiLine) ImeAction.Default else ImeAction.Next,
        ),
        interactionSource = interaction,
        shape = shape,
        colors = fieldColors(),
    )
}

/**
 * Connected button group: expressive ToggleButtons that share edges and morph their corners when
 * checked. The selected button is shown by its colour and shape (no tick). [onToggle] gets the
 * tapped index and its new checked state.
 */
@Composable
private fun ConnectedToggleGroup(
    labels: List<String>,
    selectedIndex: Int,
    onToggle: (index: Int, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clearFocus = rememberClearFocus()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        labels.forEachIndexed { index, label ->
            val checked = index == selectedIndex
            val shapes = when {
                labels.size == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = checked,
                onCheckedChange = { clearFocus(); onToggle(index, it) },
                modifier = Modifier.weight(1f),
                shapes = shapes,
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val clearFocus = rememberClearFocus()
    FilterChip(
        selected = selected,
        onClick = { clearFocus(); onClick() },
        label = { Text(label) },
        modifier = modifier.pressScale(source, pressedScale = 0.94f),
        leadingIcon = icon?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        },
        shape = MaterialTheme.shapes.medium,
        interactionSource = source,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceContainerHighest,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            selectedLeadingIconColor = colors.onPrimary,
        ),
        border = null,
    )
}

/**
 * One note card. All three conversation notes use it, so they look and behave the same:
 * icon badge + title, a character count and clear button in the header, and a roomy body that
 * grows with the text (up to [maxLines], then scrolls inside the card). The border and fill
 * animate on focus.
 */
@Composable
private fun NoteField(
    label: String,
    placeholder: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    maxLines: Int = 10,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ring by animateColorAsState(
        targetValue = if (focused) colors.primary else colors.outlineVariant,
        animationSpec = effects(),
        label = "noteRing",
    )
    val container by animateColorAsState(
        targetValue = if (focused) colors.surfaceContainerLowest else colors.surfaceContainerHighest,
        animationSpec = effects(),
        label = "noteContainer",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        border = BorderStroke(2.dp, ring),
    ) {
        Column(Modifier.animateContentSize(Motion.spatialDefault())) {
            Row(
                Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(colors.secondaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(
                    visible = value.isNotEmpty(),
                    enter = scaleIn(Motion.spatialFast) + fadeIn(effects()),
                    exit = scaleOut(Motion.spatialFast) + fadeOut(effects()),
                ) {
                    Text(
                        value.length.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                AnimatedVisibility(
                    visible = focused && value.isNotEmpty(),
                    enter = scaleIn(Motion.spatialFast) + fadeIn(effects()),
                    exit = scaleOut(Motion.spatialFast) + fadeOut(effects()),
                ) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp), shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear $label",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(placeholder, color = colors.onSurfaceVariant.copy(alpha = 0.7f))
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                minLines = minLines,
                maxLines = maxLines,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                interactionSource = interaction,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = colors.primary,
                ),
            )
        }
    }
}

/**
 * Larger selectable pill without an icon. The shape morphs from a rounded rectangle to a full pill
 * with a spring when selected, colours cross-fade, and the pill dips on press.
 * Give it Modifier.weight(1f) inside a FlowRow to make a neat grid.
 */
@Composable
private fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) colors.primary else colors.surfaceContainerHighest, effects(), label = "pillContainer",
    )
    val content by animateColorAsState(
        if (selected) colors.onPrimary else colors.onSurface, effects(), label = "pillContent",
    )
    val captionColor by animateColorAsState(
        if (selected) colors.onPrimary.copy(alpha = 0.75f) else colors.onSurfaceVariant, effects(), label = "pillCaption",
    )
    val corner by animateDpAsState(if (selected) 30.dp else 14.dp, Motion.spatialDefaultDp, label = "pillCorner")
    val clearFocus = rememberClearFocus()

    Surface(
        selected = selected,
        onClick = { clearFocus(); onClick() },
        modifier = modifier
            .heightIn(min = if (caption != null) 60.dp else 48.dp)
            .pressScale(source, pressedScale = 0.95f),
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        contentColor = content,
        interactionSource = source,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = captionColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Wrapping group of [OptionPill]s. [onSelect] gets the new value, or "" when the same pill is tapped again. */
@Composable
private fun SingleChoicePills(
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            OptionPill(
                label = option,
                selected = selected == option,
                onClick = { onSelect(if (selected == option) "" else option) },
            )
        }
    }
}

/**
 * Interest level card: tinted icon badge, title and a short caption. Icons stay here because each
 * level has a different one. Same morph / colour / press motion as [OptionPill].
 */
@Composable
private fun InterestTile(
    option: InterestOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) colors.primary else colors.surfaceContainerHighest, effects(), label = "tileContainer",
    )
    val content by animateColorAsState(
        if (selected) colors.onPrimary else colors.onSurface, effects(), label = "tileContent",
    )
    val captionColor by animateColorAsState(
        if (selected) colors.onPrimary.copy(alpha = 0.75f) else colors.onSurfaceVariant, effects(), label = "tileCaption",
    )
    val badge by animateColorAsState(
        if (selected) colors.onPrimary.copy(alpha = 0.18f) else option.accent.copy(alpha = 0.16f),
        effects(), label = "tileBadge",
    )
    val tint by animateColorAsState(
        if (selected) colors.onPrimary else option.accent, effects(), label = "tileTint",
    )
    val corner by animateDpAsState(if (selected) 32.dp else 18.dp, Motion.spatialDefaultDp, label = "tileCorner")
    val clearFocus = rememberClearFocus()

    Surface(
        selected = selected,
        onClick = { clearFocus(); onClick() },
        modifier = modifier
            .heightIn(min = 100.dp)
            .pressScale(source, pressedScale = 0.95f),
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        contentColor = content,
        interactionSource = source,
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(badge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(option.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    option.short,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    option.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = captionColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Compact date / time tile: just an icon and the value. The value rolls vertically when it changes.
 */
@Composable
private fun PickerCard(
    icon: ImageVector,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val clearFocus = rememberClearFocus()
    Surface(
        onClick = { clearFocus(); onClick() },
        modifier = modifier.pressScale(source, pressedScale = 0.95f),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerHighest,
        border = BorderStroke(1.dp, colors.outlineVariant),
        interactionSource = source,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
            }
            AnimatedContent(
                targetState = value,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInVertically(Motion.spatialDefault<IntOffset>()) { it / 2 } + fadeIn(effects())) togetherWith
                            (slideOutVertically(Motion.spatialDefault<IntOffset>()) { -it / 2 } + fadeOut(effects()))
                },
                label = "pickerValue",
            ) { text ->
                Text(
                    text.ifEmpty { "Not set" },
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = if (text.isEmpty()) colors.onSurfaceVariant else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  BOTTOM ACTION
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun SaveBar(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val height = ButtonDefaults.MediumContainerHeight
    Surface(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = colors.surfaceContainerLow,
        shadowElevation = 8.dp,
    ) {
        Box(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height),
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
            ) {
                AnimatedVisibility(
                    visible = enabled,
                    enter = expandHorizontally(Motion.spatialFast()) + fadeIn(effects()),
                    exit = shrinkHorizontally(Motion.spatialFast()) + fadeOut(effects()),
                ) {
                    Row {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                        )
                        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                    }
                }
                AnimatedContent(
                    targetState = label,
                    transitionSpec = { fadeIn(effects()) togetherWith fadeOut(effects()) },
                    label = "saveLabel",
                ) { text ->
                    Text(text, style = ButtonDefaults.textStyleFor(height), maxLines = 1)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  PICKER DIALOGS
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun DatePickerSheet(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // DatePicker works in UTC midnight millis; parse/format in UTC to avoid off-by-one days.
    val utc = remember { dateFormat(utc = true) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = remember { if (initial.isBlank()) null else CrashReporter.attempt("PersonEditScreen") { utc.parse(initial)?.time }.getOrNull() },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onConfirm(utc.format(Date(it))) } },
                shapes = ButtonDefaults.shapes(),
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun TimePickerSheet(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val fmt = remember { timeFormat() }
    val start = remember {
        Calendar.getInstance().apply {
            // An empty initial value (new record) is expected, so it is not reported as a parse failure.
            initial.takeIf { it.isNotBlank() }
                ?.let { text -> CrashReporter.attempt("PersonEditScreen") { fmt.parse(text) }.getOrNull() }
                ?.let { time = it }
        }
    }
    val state = rememberTimePickerState(
        initialHour = start.get(Calendar.HOUR_OF_DAY),
        initialMinute = start.get(Calendar.MINUTE),
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Select time", style = MaterialTheme.typography.labelLargeEmphasized) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, state.hour)
                    set(Calendar.MINUTE, state.minute)
                }
                onConfirm(fmt.format(cal.time))
            },
                shapes = ButtonDefaults.shapes()) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") } },
    )
}