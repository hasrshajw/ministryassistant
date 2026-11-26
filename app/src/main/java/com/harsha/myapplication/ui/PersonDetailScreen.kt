@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class,
)
package com.harsha.myapplication.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────
//  DESIGN TOKENS
// ─────────────────────────────────────────────────────────────────────────
private val ScreenMargin = 16.dp
private val CardRadius = 28.dp
private val GroupInner = 4.dp
private val GroupGap = 2.dp

// ─────────────────────────────────────────────────────────────────────────
//  PERSON DETAIL SCREEN
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun PersonDetailScreen(
    recordId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var record by remember(recordId) {
        mutableStateOf(PeopleRepository.load(context).firstOrNull { it.id == recordId })
    }
    var showAddress by remember { mutableStateOf(false) }
    var showLogForm by rememberSaveable { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var editingLogId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<VisitLog?>(null) }
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var confirmInactive by remember { mutableStateOf(false) }
    var confirmReactivate by remember { mutableStateOf(false) }
    var confirmDeleteProfile by remember { mutableStateOf(false) }

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

    BackHandler {
        when {
            showLogForm -> closeLogForm()
            showDetails -> showDetails = false
            else -> onDismiss()
        }
    }

    val current = record
    val scroll = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    LaunchedEffect(showLogForm, showDetails) { scroll.scrollTo(0) }
    val fabExpanded by remember { derivedStateOf { !scroll.canScrollBackward } }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = colors.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                StudyTopBar(
                    title = when {
                        current == null -> "Profile"
                        showLogForm -> if (editingLogId != null) "Edit visit" else "Log visit"
                        showDetails -> "Details"
                        else -> "Profile"
                    },
                    subtitle = when {
                        current == null -> null
                        showLogForm -> if (editingLogId != null) "Update this visit" else "Record what happened"
                        showDetails -> "Contact & ministry"
                        else -> PersonKind.of(current.type).label
                    },
                    navigationIcon = {
                        StudyBarIconButton(
                            onClick = {
                                when {
                                    showLogForm -> closeLogForm()
                                    showDetails -> showDetails = false
                                    else -> onDismiss()
                                }
                            },
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    },
                    actions = {
                        if (current != null && !showLogForm && !showDetails) {
                            val isInactive = PersonKind.of(current.type) == PersonKind.Inactive
                            ProfileMenu(
                                isInactive = isInactive,
                                onEdit = { showEdit = true },
                                onToggleActive = {
                                    if (isInactive) confirmReactivate = true else confirmInactive = true
                                },
                                onDelete = { confirmDeleteProfile = true },
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    windowInsets = TopAppBarDefaults.windowInsets,
                )
            },
            floatingActionButton = {
                if (current != null && !showLogForm && !showDetails) {
                    val fabSrc = remember { MutableInteractionSource() }
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingLogId = null
                            showLogForm = true
                        },
                        interactionSource = fabSrc,
                        modifier = Modifier.pressScale(fabSrc),
                        shape = MaterialTheme.shapes.largeIncreased,
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        expanded = fabExpanded,
                        icon = { Icon(Icons.Default.Add, contentDescription = "Log visit") },
                        text = {
                            Text(
                                "Log visit",
                                style = MaterialTheme.typography.labelLargeEmphasized,
                            )
                        },
                    )
                }
            },
        ) { padding ->
            if (current == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        "This person could not be found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                val logs = remember(current) { current.visitLogs.sortedByDescending { it.timestamp } }
                val expanded = remember { mutableStateMapOf<String, Boolean>() }
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scroll)
                ) {
                    if (showLogForm) {
                        LogVisitForm(
                            record = current,
                            editing = logs.firstOrNull { it.id == editingLogId },
                            onSave = { draft ->
                                if (persist(applyLogDraft(current, editingLogId, draft))) closeLogForm()
                            },
                        )
                    } else if (showDetails) {
                        DetailsFullScreen(
                            record = current,
                            onAddress = { showAddress = true },
                        )
                    } else {
                        Dashboard(
                            record = current,
                            logs = logs,
                            expanded = expanded,
                            onAddress = { showAddress = true },
                            onViewDetails = { showDetails = true },
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

        // Dialogs and Sheets
        if (showAddress && current != null) {
            AddressSheet(current.address, onDismiss = { showAddress = false })
        }
        pendingDelete?.let { log ->
            ConfirmDialog(
                icon = Icons.Default.Delete,
                iconTint = colors.error,
                title = "Delete this visit?",
                message = "This visit will be permanently removed from the activity timeline. This action can’t be undone.",
                confirmLabel = "Delete",
                destructive = true,
                onConfirm = {
                    current?.let { rec ->
                        persist(withLogs(rec, rec.visitLogs.filterNot { it.id == log.id }))
                    }
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null },
            )
        }
        if (confirmInactive && current != null) {
            ConfirmDialog(
                icon = Icons.Default.Pause,
                iconTint = colors.primary,
                title = "Make inactive?",
                message = "${current.name.ifEmpty { "This person" }} will be marked Inactive and won’t appear in Today or Upcoming on the Home screen.\n\nYou can reactivate them anytime from this menu.",
                confirmLabel = "Make inactive",
                onConfirm = {
                    confirmInactive = false
                    if (persist(current.copy(type = PersonKind.Inactive.key))) {
                        Toast.makeText(context, "Marked as inactive", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { confirmInactive = false },
            )
        }
        if (confirmReactivate && current != null) {
            ConfirmDialog(
                icon = Icons.Default.PlayArrow,
                iconTint = colors.primary,
                title = "Reactivate profile?",
                message = "${current.name.ifEmpty { "This person" }} will be marked active again and will appear in Today and Upcoming on the Home screen.\n\nYou can change the type later from Edit profile.",
                confirmLabel = "Reactivate",
                onConfirm = {
                    confirmReactivate = false
                    if (persist(current.copy(type = PersonKind.ReturnVisit.key))) {
                        Toast.makeText(context, "Profile reactivated", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { confirmReactivate = false },
            )
        }
        if (confirmDeleteProfile && current != null) {
            ConfirmDialog(
                icon = Icons.Default.DeleteForever,
                iconTint = colors.error,
                title = "Delete this profile?",
                message = "${current.name.ifEmpty { "This person" }} and all their visit history will be permanently deleted. This action can’t be undone.",
                confirmLabel = "Delete",
                destructive = true,
                onConfirm = {
                    confirmDeleteProfile = false
                    if (PeopleRepository.delete(context, current.id)) {
                        Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Could not delete. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { confirmDeleteProfile = false },
            )
        }
        if (showEdit && current != null) {
            PersonEditScreen(
                existing = current,
                onDismiss = { showEdit = false },
                onSaved = {
                    record = PeopleRepository.load(context).firstOrNull { it.id == recordId }
                    showEdit = false
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  TOP BAR MENU
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileMenu(
    isInactive: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box {
        StudyBarIconButton(
            onClick = { open = true },
            icon = Icons.Default.MoreVert,
            contentDescription = "More options",
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.widthIn(min = 230.dp),
            offset = DpOffset(0.dp, 8.dp),
            shape = MaterialTheme.shapes.large,
            containerColor = colors.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Text(
                "OPTIONS",
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = colors.onSurfaceVariant,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
            )
            MenuRow("Edit profile", Icons.Default.Edit, colors.onSurface, colors.primary) { open = false; onEdit() }
            MenuRow(
                if (isInactive) "Reactivate" else "Make inactive",
                if (isInactive) Icons.Default.PlayArrow else Icons.Default.Pause,
                colors.onSurface,
                colors.primary,
            ) { open = false; onToggleActive() }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = colors.outlineVariant.copy(alpha = 0.6f),
            )
            MenuRow("Delete profile", Icons.Default.Delete, colors.error, colors.error) { open = false; onDelete() }
        }
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, textColor: Color, iconTint: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp)),
        text = { Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor) },
        leadingIcon = {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
        },
        onClick = onClick,
    )
}

// ─────────────────────────────────────────────────────────────────────────
//  DASHBOARD
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun Dashboard(
    record: PersonRecord,
    logs: List<VisitLog>,
    expanded: MutableMap<String, Boolean>,
    onAddress: () -> Unit,
    onViewDetails: () -> Unit,
    onEditLog: (VisitLog) -> Unit,
    onDeleteLog: (VisitLog) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val phone = record.mobile.orEmpty()
    val hasPhone = phone.isNotEmpty()
    val hasAddress = record.address.isNotEmpty()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Simplified Premium Hero
        ProfileHero(record, Modifier.revealOnEnter(0))

        // Quick Actions: Call & Message (light, secondary) + Info (dark, primary emphasis)
        Row(
            Modifier.revealOnEnter(1),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionTile(
                label = "Call",
                icon = Icons.Default.Phone,
                enabled = hasPhone,
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                iconWell = colors.onSecondaryContainer.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f),
            ) {
                context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${normalizePhone(phone)}")))
            }
            ActionTile(
                label = "Message",
                icon = Icons.AutoMirrored.Filled.Message,
                enabled = hasPhone,
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                iconWell = colors.onSecondaryContainer.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f),
            ) {
                context.launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${normalizePhone(phone)}")))
            }
            InfoIconButton(
                icon = Icons.Default.Info,
                enabled = true,
                onClick = onViewDetails,
            )
        }

        NextVisitCard(record, Modifier.revealOnEnter(2))

        // Activity timeline
        Column(Modifier.revealOnEnter(3)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DetailSectionHeader("Activity", Modifier.weight(1f))
                if (logs.isNotEmpty()) {
                    Surface(shape = CircleShape, color = colors.secondaryContainer) {
                        Text(
                            "${logs.size} visit${if (logs.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            color = colors.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (logs.isEmpty()) {
                EmptyActivity()
            } else {
                val railColor = colors.outlineVariant.copy(alpha = 0.7f)
                Column {
                    logs.forEachIndexed { index, log ->
                        val isLast = index == logs.lastIndex
                        Row(
                            Modifier.drawBehind {
                                if (!isLast) {
                                    val x = 8.dp.toPx()
                                    drawLine(
                                        color = railColor,
                                        start = Offset(x, 36.dp.toPx()),
                                        end = Offset(x, size.height),
                                        strokeWidth = 2.dp.toPx(),
                                    )
                                }
                            },
                        ) {
                            Box(
                                Modifier
                                    .padding(top = 24.dp)
                                    .width(16.dp),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Box(
                                    Modifier
                                        .size(if (index == 0) 12.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) colors.primary else colors.outline.copy(alpha = 0.6f)),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            VisitLogCard(
                                log = log,
                                visitNumber = logs.size - index,
                                isLatest = index == 0,
                                expanded = expanded[log.id] == true,
                                onToggle = { expanded[log.id] = expanded[log.id] != true },
                                onEdit = { onEditLog(log) },
                                onDelete = { onDeleteLog(log) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = if (isLast) 0.dp else 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  PREMIUM SIMPLE HERO
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileHero(record: PersonRecord, modifier: Modifier = Modifier) {
    val kind = PersonKind.of(record.type)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = record.name.ifEmpty { "New person" },
            style = MaterialTheme.typography.headlineLargeEmphasized,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = colors.secondaryContainer,
        ) {
            Text(
                text = kind.label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = colors.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  COMPACT ACTION TILES
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    container: Color,
    content: Color,
    iconWell: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 14.dp else 28.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "tileCorner",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .pressScale(src, pressedScale = 0.96f),
        shape = RoundedCornerShape(corner),
        color = if (enabled) container else container.copy(alpha = 0.5f),
        contentColor = if (enabled) content else content.copy(alpha = 0.38f),
        interactionSource = src,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconWell),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  INFO ICON BUTTON (square, icon only)
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun InfoIconButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 14.dp else 28.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "infoCorner",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(56.dp)
            .pressScale(src, pressedScale = 0.96f),
        shape = RoundedCornerShape(corner),
        color = if (enabled) colors.primary else colors.primary.copy(alpha = 0.5f),
        interactionSource = src,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.onPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = "View details",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  NEXT VISIT
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun NextVisitCard(record: PersonRecord, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val kind = PersonKind.of(record.type)
    val date = remember(record.nextVisitDate) { parseDateOnly(record.nextVisitDate) }
    val days = remember(date) {
        date?.let { Math.round((startOfDay(it) - startOfDay(Date())).toDouble() / DAY_MS).toInt() }
    }
    val time = record.nextVisitTime.orEmpty()
    val title = when {
        kind == PersonKind.Inactive -> "Inactive"
        date == null -> "Not scheduled"
        else -> SimpleDateFormat("EEE, MMM d", Locale.US).format(date)
    }
    val status: String? = when {
        kind == PersonKind.Inactive -> "Paused"
        days == null -> null
        days < 0 -> "Overdue · ${-days}d"
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        else -> "In $days days"
    }
    val overdue = kind != PersonKind.Inactive && days != null && days < 0
    val today = kind != PersonKind.Inactive && days == 0
    val pillBg = when {
        overdue -> colors.errorContainer
        today -> colors.primary
        else -> colors.secondaryContainer
    }
    val pillText = when {
        overdue -> colors.onErrorContainer
        today -> colors.onPrimary
        else -> colors.onSecondaryContainer
    }
    val subtitle = when {
        kind == PersonKind.Inactive -> "Reactivate to schedule visits"
        date != null && time.isNotEmpty() -> time
        date != null -> "No time set"
        else -> "Log a visit to schedule one"
    }
    Surface(
        shape = MaterialTheme.shapes.largeIncreased,
        color = colors.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "NEXT VISIT",
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    letterSpacing = 1.2.sp,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (status != null) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = pillBg) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = pillText,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  DETAILS FULL SCREEN
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun DetailsFullScreen(record: PersonRecord, onAddress: () -> Unit) {
    val context = LocalContext.current
    val kind = PersonKind.of(record.type)
    val phone = record.mobile.orEmpty()
    val hasPhone = phone.isNotEmpty()
    val hasAddress = record.address.isNotEmpty()
    val personal = listOfNotNull(record.gender, record.age, record.language)
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
    val interest = record.interestLevel.orEmpty()
    val contactRows = buildList {
        add(InfoRow(Icons.Default.Phone, "Mobile", phone, if (hasPhone) ({
            context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${normalizePhone(phone)}")))
        }) else null))
        add(InfoRow(Icons.Default.LocationOn, "Address", record.address, if (hasAddress) onAddress else null))
        add(InfoRow(Icons.Default.Person, "Personal", personal))
    }
    val ministryRows = buildList {
        if (interest.isNotEmpty()) add(InfoRow(Icons.Default.Star, "Interest level", interest))
        if (!record.placeMet.isNullOrEmpty()) add(InfoRow(Icons.Default.Place, "Place met", record.placeMet.orEmpty()))
        if (!record.bestTime.isNullOrEmpty()) add(InfoRow(Icons.Default.Schedule, "Best time", record.bestTime.orEmpty()))
        if (!record.meetingStatus.isNullOrEmpty()) add(InfoRow(Icons.Default.Groups, "Meetings", record.meetingStatus.orEmpty()))
        if (kind == PersonKind.Study) {
            add(InfoRow(Icons.Default.CalendarToday, "Study day", record.studyDay.orEmpty()))
            if (!record.studyMaterial.isNullOrEmpty()) {
                add(InfoRow(Icons.Default.MenuBook, "Study material", record.studyMaterial.orEmpty(), multiline = true))
            }
        }
        if (!record.notes.isNullOrEmpty()) {
            add(InfoRow(Icons.Default.Book, "Return visit question / topic", record.notes.orEmpty(), multiline = true))
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.revealOnEnter(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailSectionHeader("Contact")
            InfoGroup(contactRows) { row, shape -> InfoListItem(row, shape) }
        }
        if (ministryRows.isNotEmpty()) {
            Column(Modifier.revealOnEnter(1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailSectionHeader("Ministry")
                InfoGroup(ministryRows) { row, shape -> InfoListItem(row, shape) }
            }
        }
    }
}

@Composable
private fun DetailSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMediumEmphasized,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 6.dp),
    )
}

private data class InfoRow(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val onClick: (() -> Unit)? = null,
    val multiline: Boolean = false,
)

private fun infoShape(index: Int, count: Int): Shape {
    val top = if (index == 0) CardRadius else GroupInner
    val bottom = if (index == count - 1) CardRadius else GroupInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun <T> InfoGroup(items: List<T>, row: @Composable (item: T, shape: Shape) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        items.forEachIndexed { index, item -> row(item, infoShape(index, items.size)) }
    }
}

@Composable
private fun InfoListItem(row: InfoRow, shape: Shape) {
    val colors = MaterialTheme.colorScheme
    val empty = row.value.isEmpty()
    Surface(
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            overlineContent = {
                Text(
                    row.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            },
            headlineContent = {
                Text(
                    if (empty) "Not provided" else row.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (empty) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface,
                    maxLines = if (row.multiline) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(colors.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        row.icon,
                        contentDescription = null,
                        tint = colors.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            trailingContent = if (row.onClick != null) {
                {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                    )
                }
            } else null,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (row.onClick != null) Modifier.clickable(role = Role.Button, onClick = row.onClick)
                    else Modifier
                ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  ACTIVITY TIMELINE
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyActivity() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.largeIncreased,
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "No visits logged yet",
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap Log visit to record the first one",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
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
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, Motion.spatialDefault, label = "chevron")
    val nextDate = remember(log.nextVisitDate) { parseDateOnly(log.nextVisitDate) }
    val nextTime = log.nextVisitTime
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.largeIncreased,
        color = colors.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isLatest) colors.primary else colors.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "#$visitNumber",
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = if (isLatest) colors.onPrimary else colors.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(log.timestamp)),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (log.isImported) {
                            Spacer(Modifier.width(8.dp))
                            StatusChip("IMPORTED", colors.tertiary)
                        }
                    }
                    Text(
                        log.topic.ifEmpty { "General visit" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.rotate(chevron),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(Motion.spatialDefault()) + fadeIn(Motion.effectsDefault),
                exit = shrinkVertically(Motion.spatialDefault()) + fadeOut(Motion.effectsDefault),
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = colors.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                log.notes.ifEmpty { "No detailed notes provided." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (log.notes.isEmpty()) colors.onSurfaceVariant else colors.onSurface,
                            )
                            if (nextDate != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Next: " + SimpleDateFormat("MMM d", Locale.US).format(nextDate) +
                                                if (nextTime.isNotEmpty()) " · $nextTime" else "",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        FilledTonalButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                        }
                        FilledTonalButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.errorContainer,
                                contentColor = colors.onErrorContainer,
                            ),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = color.copy(alpha = 0.12f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmallEmphasized,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  LOG VISIT FORM (Compact, Premium)
// ─────────────────────────────────────────────────────────────────────────
private class LogDraft(
    val topic: String,
    val question: String,
    val notes: String,
    val nextDate: String,
    val nextTime: String,
)

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

@Composable
private fun LogVisitForm(
    record: PersonRecord,
    editing: VisitLog?,
    onSave: (LogDraft) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
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
    val clearFocus = rememberClearFocus()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 8.dp, bottom = 24.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { clearFocus() }) },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FormSection(
            title = "Visit details",
            subtitle = "What did you discuss?",
            icon = Icons.Default.Book,
            modifier = Modifier.revealOnEnter(0)
        ) {
            CompactNoteField(
                label = "Topic",
                placeholder = "Publication or discussion",
                icon = Icons.Default.Forum,
                value = topic,
                onValueChange = { topic = it },
                minLines = 2,
                maxLines = 4,
            )
            CompactNoteField(
                label = "Question",
                placeholder = "What to raise next time",
                icon = Icons.Default.QuestionMark,
                value = question,
                onValueChange = { question = it },
                minLines = 2,
                maxLines = 4,
            )
            CompactNoteField(
                label = "Notes",
                placeholder = "Observations, concerns",
                icon = Icons.AutoMirrored.Filled.Notes,
                value = notes,
                onValueChange = { notes = it },
                minLines = 2,
                maxLines = 5,
            )
        }

        FormSection(
            title = "Next visit",
            subtitle = "Set the date and time",
            icon = Icons.Default.Event,
            modifier = Modifier.revealOnEnter(1)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerCard(Icons.Default.CalendarToday, date, { clearFocus(); pickDate = true }, Modifier.weight(1f))
                PickerCard(Icons.Default.Schedule, time, { clearFocus(); pickTime = true }, Modifier.weight(1f))
            }
        }

        Button(
            onClick = {
                clearFocus()
                onSave(LogDraft(topic.trim(), question, notes.trim(), date, time))
            },
            enabled = date.isNotEmpty() && time.isNotEmpty(),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                if (editing != null) "Save changes" else "Save visit",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }
    }

    if (pickDate) {
        DatePickerSheet(
            initial = date,
            onConfirm = { date = it; pickDate = false },
            onDismiss = { pickDate = false },
        )
    }
    if (pickTime) {
        TimePickerSheet(
            initial = time,
            onConfirm = { time = it; pickTime = false },
            onDismiss = { pickTime = false },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  EXPRESSIVE FORM COMPONENTS (Synced with PersonEditScreen)
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun CompactNoteField(
    label: String,
    placeholder: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 2,
    maxLines: Int = 5,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ring by animateColorAsState(
        targetValue = if (focused) colors.primary else colors.outlineVariant.copy(alpha = 0.4f),
        animationSpec = effects(),
        label = "noteRing",
    )
    val container by animateColorAsState(
        targetValue = if (focused) colors.surfaceContainerLowest else colors.surfaceContainer,
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
                Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(colors.secondaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(16.dp))
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
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                AnimatedVisibility(
                    visible = focused && value.isNotEmpty(),
                    enter = scaleIn(Motion.spatialFast) + fadeIn(effects()),
                    exit = scaleOut(Motion.spatialFast) + fadeOut(effects()),
                ) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear $label",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(placeholder, color = colors.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
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
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
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

@Composable
private fun DatePickerSheet(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val utc = remember { dateFormat(utc = true) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = remember { runCatching { utc.parse(initial)?.time }.getOrNull() },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
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
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

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

private fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

// ─────────────────────────────────────────────────────────────────────────
//  DIALOGS & SHEETS
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ConfirmDialog(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp)) }
        },
        title = { Text(title, style = MaterialTheme.typography.headlineSmallEmphasized) },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        },
        confirmButton = {
            if (destructive) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError,
                    ),
                ) { Text(confirmLabel) }
            } else {
                Button(onClick = onConfirm) { Text(confirmLabel) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = MaterialTheme.shapes.extraLargeIncreased,
        containerColor = colors.surfaceContainerHigh,
    )
}

@Composable
private fun AddressSheet(address: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(colors.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colors.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text("Address", style = MaterialTheme.typography.titleLargeEmphasized, color = colors.onSurface)
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    address,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Address", address))
                        Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
                FilledTonalButton(
                    onClick = {
                        context.launch(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}"),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open in Maps")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────
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