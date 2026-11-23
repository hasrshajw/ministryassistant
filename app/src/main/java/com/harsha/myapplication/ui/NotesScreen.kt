package com.harsha.myapplication.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.*
import com.harsha.myapplication.data.DEFAULT_CATEGORIES
import com.harsha.myapplication.data.DEFAULT_CATEGORY
import com.harsha.myapplication.data.NoteItem
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.StudyDeepGreen
import com.harsha.myapplication.ui.theme.StudyEmerald
import com.harsha.myapplication.ui.theme.StudyNeutralDark
import com.harsha.myapplication.ui.theme.StudyNeutralLight
import com.harsha.myapplication.ui.theme.StudyOnNeutralDark
import com.harsha.myapplication.ui.theme.StudyOnNeutralLight
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.pressScaleClickable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.harsha.myapplication.ui.theme.revealOnEnter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState

/**
 * A folder shown in the side menu. Pass the folders you have stored to `NotesScreen(folders = ...)`.
 * `noteCount` is ignored: the screen counts the notes of each folder itself.
 * `colorArgb` is the folder colour picked in the "New folder" popup (null = default yellow).
 */
@Immutable
data class NotesFolder(val name: String, val noteCount: Int = 0, val colorArgb: Int? = null)

/** Separator between a folder name and its colour in the saved list of folders created here. */
private const val FOLDER_SEP = '\u001F'

/** Every folder for the menu: stored ones, ones created here, and ones only used by a note. */
private fun buildFolderList(
    hosted: List<NotesFolder>,
    created: List<String>,
    rows: List<NoteRow>
): List<NotesFolder> {
    val counts = rows.groupingBy { it.folder.lowercase() }.eachCount()
    val out = LinkedHashMap<String, NotesFolder>()   // key = lowercase name, keeps the order
    fun add(name: String, color: Int?) {
        val n = name.trim()
        if (n.isEmpty() || n.equals(DEFAULT_CATEGORY, ignoreCase = true)) return
        val key = n.lowercase()
        val existing = out[key]
        if (existing == null) out[key] = NotesFolder(n, counts[key] ?: 0, color)
        else if (existing.colorArgb == null && color != null) out[key] = existing.copy(colorArgb = color)
    }
    hosted.forEach { add(it.name, it.colorArgb) }
    // Built-in categories, so every folder offered in the note editor also shows in the menu.
    DEFAULT_CATEGORIES.forEach { add(it, null) }
    created.forEach { e ->
        val i = e.lastIndexOf(FOLDER_SEP)
        if (i > 0) add(e.substring(0, i), e.substring(i + 1).toIntOrNull())
    }
    rows.map { it.folder }.distinct().sortedBy { it.lowercase() }.forEach { add(it, null) }
    return out.values.toList()
}

/** Sort options of the notes list (same menu as the People screen). */
private enum class NotesSort(val label: String, val icon: ImageVector) {
    /** Keeps the order the notes are handed in (newest first if that is how you load them). */
    Date("By Date", Icons.Default.CalendarToday),
    Title("By Title (A–Z)", Icons.Default.SortByAlpha),
}

private val NotesSearchHints = listOf("Title", "Content", "Scripture")

/** Entries of the "Events" section in the side menu. */
enum class NotesEvent(val label: String) {
    Conventions("Conventions"),
    Assemblies("Assemblies"),
    Others("Others"),
}

/** A note plus the text derived from it, computed once per notes change (not per recomposition). */
@Immutable
private class NoteRow(val note: NoteItem) {
    /** Content without the invisible image placeholders (what search and the preview use). */
    val body: String = note.previewText
    /** Only the first lines are ever shown, so never hand a huge string to Text(). */
    val preview: String = body.take(PREVIEW_CHARS)
    val date: String = note.date
    /** The folder (category) the note is in; notes without one are "Uncategorized". */
    val folder: String = (note.category ?: "").trim().ifEmpty { DEFAULT_CATEGORY }

    private companion object { const val PREVIEW_CHARS = 200 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<NoteItem> = emptyList(),
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onAddNoteClick: () -> Unit = {},
    /** "Quick Notes" button at the top of the side menu. Wire this to open NotesEditorScreen. */
    onQuickNotesClick: () -> Unit = onAddNoteClick,
    onNoteClick: (NoteItem) -> Unit = {},
    folders: List<NotesFolder> = emptyList(),
    onFolderClick: (NotesFolder) -> Unit = {},
    /** A folder was created in the side menu. Store it (name + ARGB colour) and pass it back via `folders`. */
    onCreateFolder: (name: String, colorArgb: Int) -> Unit = { _, _ -> },
    onEventClick: (NotesEvent) -> Unit = {},
    /** Called whenever the side menu opens / closes (MainActivity hides the bottom bar while it is open). */
    onDrawerOpenChange: (Boolean) -> Unit = {},
    /** Space taken by the status bar / bottom bar. Backgrounds are still painted edge-to-edge. */
    contentPadding: PaddingValues = PaddingValues(),
) {
    var drawerSelection by rememberSaveable { mutableStateOf("all") }   // "all", "uncategorized" or "folder:<name>"
    // Folders created from the side menu in this session, stored as "name<US>argb".
    var createdFolders by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showNewFolder by rememberSaveable { mutableStateOf(false) }

    // Derived data is rebuilt only when the notes change, not on every keystroke / recomposition.
    val rows = remember(notes) { notes.map { NoteRow(it) } }
    val allFolders = remember(folders, createdFolders, rows) { buildFolderList(folders, createdFolders, rows) }
    val uncategorizedCount = remember(rows) {
        rows.count { it.folder.equals(DEFAULT_CATEGORY, ignoreCase = true) }
    }
    // The menu selection decides which notes the main list shows; search narrows it further.
    val selectedRows = remember(rows, drawerSelection) {
        when {
            drawerSelection == "uncategorized" ->
                rows.filter { it.folder.equals(DEFAULT_CATEGORY, ignoreCase = true) }
            drawerSelection.startsWith("folder:") -> {
                val name = drawerSelection.removePrefix("folder:")
                rows.filter { it.folder.equals(name, ignoreCase = true) }
            }
            else -> rows
        }
    }
    val filtered = remember(selectedRows, searchQuery) {
        if (searchQuery.isBlank()) selectedRows
        else selectedRows.filter {
            it.note.title.contains(searchQuery, ignoreCase = true) ||
                    it.body.contains(searchQuery, ignoreCase = true)
        }
    }
    var sort by rememberSaveable { mutableStateOf(NotesSort.Date) }
    val visible = remember(filtered, sort) {
        when (sort) {
            NotesSort.Date -> filtered
            NotesSort.Title -> filtered.sortedBy { it.note.title.lowercase() }
        }
    }
    val screenTitle = when {
        drawerSelection == "uncategorized" -> DEFAULT_CATEGORY
        drawerSelection.startsWith("folder:") -> drawerSelection.removePrefix("folder:")
        else -> "Notes & Studies"
    }
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    // The FAB collapses to an icon while the list scrolls down and extends again on scroll up / at top.
    val fabExpanded by remember {
        derivedStateOf { listState.lastScrolledBackward || !listState.canScrollBackward }
    }

    // ---- Push drawer ------------------------------------------------------------------------
    // The side menu does NOT overlay the notes: the whole screen slides right by the drawer width
    // while the menu stays underneath (with a small parallax). `drawerProgress` (0 = closed,
    // 1 = open) is the single source of truth; the finger, the menu button and the predictive
    // back gesture all drive it, and everything else is derived from it in the draw phase only.
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val drawerProgress = remember { Animatable(if (drawerOpen) 1f else 0f) }
    // While a finger is down the position lives in `dragProgress` (a plain state read only in the
    // draw phase), so following the finger needs no coroutine and no recomposition per frame.
    // When the finger lifts it is handed over to `drawerProgress`, which animates the settle.
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val progress: () -> Float = { if (isDragging) dragProgress else drawerProgress.value }
    var rootWidthPx by remember { mutableIntStateOf(0) }
    val drawerWidthPx = minOf(rootWidthPx * 0.78f, with(density) { 370.dp.toPx() })
    val drawerWidth = with(density) { drawerWidthPx.toDp() }
    // True while the menu is (partly) on screen. The panel itself stays composed so the first frame
    // of a swipe never has to compose it; it is just hidden from accessibility while closed.
    val drawerVisible by remember { derivedStateOf { progress() > 0.001f } }

    // Tell the host so it can hide the bottom bar while the menu is open.
    val currentOnDrawerOpenChange by rememberUpdatedState(onDrawerOpenChange)
    LaunchedEffect(drawerOpen) { currentOnDrawerOpenChange(drawerOpen) }
    DisposableEffect(Unit) { onDispose { currentOnDrawerOpenChange(false) } }

    fun setDrawer(open: Boolean) {
        drawerOpen = open
        scope.launch { drawerProgress.animateTo(if (open) 1f else 0f, Motion.spatialDefault) }
    }

    // Back closes the menu first. While the finger drags from the edge the menu closes with it;
    // cancelling springs it back open.
    PredictiveBackHandler(enabled = drawerOpen) { backEvents ->
        try {
            backEvents.collect { drawerProgress.snapTo(1f - it.progress) }
            drawerOpen = false
            drawerProgress.animateTo(0f, Motion.spatialDefault)
        } catch (e: CancellationException) {
            scope.launch { drawerProgress.animateTo(1f, Motion.spatialDefault) }
            throw e
        }
    }

    // Horizontal drag anywhere on the screen opens / closes the menu and follows the finger.
    val dragState = rememberDraggableState { delta ->
        if (drawerWidthPx > 0f) {
            dragProgress = (dragProgress + delta / drawerWidthPx).coerceIn(0f, 1f)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { rootWidthPx = it.width }
            .background(DrawerBackground)                  // the menu's own background (white)
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    // Catch the menu mid-animation: continue from where it is and stop the spring.
                    dragProgress = drawerProgress.value
                    isDragging = true
                    drawerProgress.stop()
                },
                onDragStopped = { velocity ->
                    // Same rule as Material 3's ModalNavigationDrawer: a fling faster than 400dp/s
                    // decides first, otherwise settle to the nearest side (50% threshold).
                    val flingThreshold = with(density) { 400.dp.toPx() }
                    val target = when {
                        velocity > flingThreshold -> 1f
                        velocity < -flingThreshold -> 0f
                        dragProgress > 0.5f -> 1f
                        else -> 0f
                    }
                    drawerOpen = target == 1f
                    // Hand over to the animation at the exact finger position, in the same frame,
                    // then spring to the target carrying the finger's velocity (no stop-and-go).
                    drawerProgress.snapTo(dragProgress)
                    isDragging = false
                    drawerProgress.animateTo(
                        targetValue = target,
                        animationSpec = Motion.spatialDefault,
                        initialVelocity = if (drawerWidthPx > 0f) velocity / drawerWidthPx else 0f,
                    )
                }
            )
    ) {
        // ---- Layer 1: the side menu (stays put, drifts in slightly) ----
        run {
            NotesDrawerPanel(
                progress = progress,
                noteCount = notes.size,
                uncategorizedCount = uncategorizedCount,
                folders = allFolders,
                selection = drawerSelection,
                onSelect = { key ->
                    drawerSelection = key
                    setDrawer(false)
                },
                onFolderClick = { folder ->
                    drawerSelection = "folder:${folder.name}"
                    setDrawer(false)
                    onFolderClick(folder)
                },
                onNewNoteClick = {
                    setDrawer(false)
                    onAddNoteClick()
                },
                onQuickNotesClick = {
                    setDrawer(false)
                    onQuickNotesClick()
                },
                onCreateFolderClick = { showNewFolder = true },
                onEventClick = { event ->
                    setDrawer(false)
                    onEventClick(event)
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(drawerWidth)
                    .graphicsLayer {
                        val p = progress()
                        translationX = -(1f - p) * drawerWidthPx * 0.25f   // parallax
                        alpha = (p * 1.5f).coerceIn(0f, 1f)
                    }
                    .then(if (drawerVisible) Modifier else Modifier.clearAndSetSemantics { })
                    .padding(contentPadding),   // menu content sits below the status bar
            )
        }

        // ---- Layer 2: the notes screen, pushed to the right ----
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress()
                    translationX = p * drawerWidthPx
                    // Straight edge (no rounded corners) with a soft shadow, like the reference.
                    // Set unconditionally so the layer never changes type in the middle of a swipe.
                    clip = true
                    shadowElevation = 10.dp.toPx() * p
                }
                // Painted edge-to-edge (behind the status bar and the navigation bar) so the system
                // bar areas show the notes colour on the pushed side and the menu colour on the other.
                .background(colors.surface)
        ) {
            Scaffold(
                modifier = Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                screenTitle,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface,
                            titleContentColor = colors.onSurface
                        )
                    )
                },
                floatingActionButton = {
                    // Shared interaction source so the FAB's own ripple and press-scale
                    // animate in sync. FABs across Material 3 Expressive shrink on press.
                    val fabSrc = remember { MutableInteractionSource() }
                    val dark = isSystemInDarkTheme()
                    ExtendedFloatingActionButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            setDrawer(true)
                        },
                        interactionSource = fabSrc,
                        modifier = Modifier.pressScale(fabSrc),
                        containerColor = if (dark) StudyEmerald else StudyDeepGreen,
                        contentColor = if (dark) StudyDeepGreen else Color.White,
                        expanded = fabExpanded,
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Note") },
                        text = { Text("New note", fontFamily = Lexend) },
                    )
                },
                // Dark-mode fix: was hard-coded white. colors.surface maps to StudyBackground
                // in light mode (visually identical) and StudyDarkBackground in dark mode.
                containerColor = colors.surface
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Search bar + sort button (same components as the People screen)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .revealOnEnter(index = 0, playOnceKey = "notes-search"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppSearchField(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            onSearch = { focusManager.clearFocus() },
                            hints = NotesSearchHints,
                            modifier = Modifier.weight(1f)
                        )
                        SortMenuButton(
                            options = NotesSort.entries,
                            selected = sort,
                            onSelect = { sort = it },
                            label = { it.label },
                            icon = { it.icon },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Empty state ↔ populated list: crossfade. Keying on `isEmpty()` (not on the
                    // full list contents) means typing into the search field doesn't re-animate
                    // the whole screen on every keystroke — only the empty↔list boundary moves.
                    AnimatedContent(
                        targetState = visible.isEmpty(),
                        transitionSpec = {
                            fadeIn(Motion.effectsDefault) togetherWith fadeOut(Motion.effectsDefault)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        label = "notesEmptyState"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = when {
                                            searchQuery.isNotBlank() -> "No matching notes"
                                            drawerSelection != "all" -> "No notes in this folder"
                                            else -> "No notes yet"
                                        },
                                        fontFamily = Lexend,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap + to create your first study note",
                                        fontFamily = Lexend,
                                        fontSize = 14.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                // Room under the last card so the FAB (56dp + 16dp margin) never covers it (M3).
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = visible,
                                    key = { _, it -> it.note.id },          // stable identity: no re-creation when the list changes
                                    contentType = { _, _ -> "note" }
                                ) { index, row ->
                                    NoteCard(
                                        row = row,
                                        onClick = { onNoteClick(row.note) },
                                        // Cards glide to their new slot when search filters the list and
                                        // fade in/out instead of popping. The first screenful also staggers in.
                                        modifier = Modifier
                                            .animateItem(
                                                fadeInSpec = Motion.effectsDefault,
                                                placementSpec = Motion.spatialDefault<IntOffset>(),
                                                fadeOutSpec = Motion.effectsFast,
                                            )
                                            // Stagger only the first screenful; later items must not animate while scrolling.
                                            .then(
                                                if (index < 8) Modifier.revealOnEnter(index = index, playOnceKey = "note-$index")
                                                else Modifier
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // While the menu is open a tap on the pushed screen closes it (and does nothing else).
            if (drawerVisible) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { setDrawer(false) } }
                )
            }
        }
    }

    if (showNewFolder) {
        NewFolderDialog(
            existing = listOf(DEFAULT_CATEGORY) + allFolders.map { it.name },
            onDismiss = { showNewFolder = false },
            onCreate = { name, color ->
                createdFolders = createdFolders + "$name$FOLDER_SEP${color.toArgb()}"
                showNewFolder = false
                onCreateFolder(name, color.toArgb())
            }
        )
    }
}

@Composable
private fun NoteCard(row: NoteRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    // Same grey as the Home screen's Daily text side pills / Monthly progress panel.
    val cardColor = if (isSystemInDarkTheme()) StudyNeutralDark else StudyNeutralLight
    // Shared interaction source: ripple + press-scale stay in sync on the graphics layer.
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = src,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(src, pressedScale = 0.97f),   // subtle shrink: cards are large
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.note.title,
                    fontFamily = Lexend,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = row.date,
                    fontFamily = Lexend,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = row.preview,
                fontFamily = Lexend,
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
//  Push-drawer menu (layout follows the reference notes app)
// ============================================================================

private val UncategorizedTile = StudyOnNeutralLight
private val DrawerBackground = Color(0xFFFFFFFF)
private val DrawerCardColor = StudyNeutralLight          // same grey as the Home pills, on the white menu
private val DrawerCardBorder = StudyOnNeutralDark

/** Items fade in and slide in from the left one after another while the menu opens. */
private fun Modifier.drawerReveal(progress: () -> Float, index: Int): Modifier = graphicsLayer {
    val t = ((progress() - index * 0.07f) / 0.55f).coerceIn(0f, 1f)
    alpha = t
    translationX = (1f - t) * -28.dp.toPx()
}

@Composable
private fun NotesDrawerPanel(
    progress: () -> Float,
    noteCount: Int,
    uncategorizedCount: Int,
    folders: List<NotesFolder>,
    selection: String,
    onSelect: (String) -> Unit,
    onFolderClick: (NotesFolder) -> Unit,
    onNewNoteClick: () -> Unit,
    onQuickNotesClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onEventClick: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // "Notes" title + new note (pencil, top right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 12.dp)
                .drawerReveal(progress, 0),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notes",
                fontFamily = Lexend,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNewNoteClick) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "New note",
                    tint = colors.onSurfaceVariant
                )
            }
        }

        // "Quick Notes": full-width Material 3 button that opens the note editor
        val dark = isSystemInDarkTheme()
        Button(
            onClick = onQuickNotesClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 0.dp)
                .drawerReveal(progress, 1),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                // Same colours as the "New note" FAB
                containerColor = if (dark) StudyEmerald else StudyDeepGreen,
                contentColor = if (dark) StudyDeepGreen else Color.White
            ),
        ) {
            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Quick Notes", fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        // One rounded white card holding "All" and "Uncategorized"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .drawerReveal(progress, 2),
            shape = RoundedCornerShape(20.dp),
            color = DrawerCardColor,
            border = BorderStroke(1.dp, DrawerCardBorder),
        ) {
            Column {
                DrawerRow(
                    label = "All",
                    count = noteCount,
                    selected = selection == "all",
                    tileColor = StudyOnNeutralLight,
                    onClick = { onSelect("all") },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null, tint = StudyOnNeutralLight, modifier = Modifier.size(18.dp))
                }
                DrawerRow(
                    label = "Uncategorized",
                    count = uncategorizedCount,
                    selected = selection == "uncategorized",
                    tileColor = UncategorizedTile,
                    onClick = { onSelect("uncategorized") },
                ) {
                    Text("?", fontFamily = Lexend, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = UncategorizedTile)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // "Events" section: Conventions, Assemblies, Daily text
        Text(
            "Events",
            fontFamily = Lexend,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 6.dp, bottom = 8.dp)
                .drawerReveal(progress, 3)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .drawerReveal(progress, 4),
            shape = RoundedCornerShape(20.dp),
            color = DrawerCardColor,
            border = BorderStroke(1.dp, DrawerCardBorder),
        ) {
            Column {
                NotesEvent.entries.forEachIndexed { i, event ->
                    if (i > 0) HorizontalDivider(color = DrawerCardBorder)
                    DrawerRow(
                        label = event.label,
                        count = null,                       // events have no note count
                        selected = false,
                        tileColor = StudyOnNeutralLight,
                        onClick = { onEventClick(event) },
                        tileSize = 34.dp,
                        verticalPadding = 12.dp,
                    ) {
                        Icon(
                            imageVector = when (event) {
                                NotesEvent.Conventions -> Icons.Default.Stadium
                                NotesEvent.Assemblies -> Icons.Default.Groups
                                NotesEvent.Others -> Icons.Outlined.Category
                            },
                            contentDescription = null,
                            tint = StudyOnNeutralLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // "My folders" + new folder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp)
                .drawerReveal(progress, 5),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My folders",
                fontFamily = Lexend,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCreateFolderClick) {
                Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New folder", tint = colors.onSurface)
            }
        }

        if (folders.isEmpty()) {
            // Empty state: outlined rounded square with a dot, "No folders"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .drawerReveal(progress, 6),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .border(6.dp, colors.onSurface.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(StudyOnNeutralLight.copy(alpha = 0.35f))
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No folders",
                        fontFamily = Lexend,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = colors.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawerReveal(progress, 6),
                shape = RoundedCornerShape(20.dp),
                color = DrawerCardColor,
                border = BorderStroke(1.dp, DrawerCardBorder),
            ) {
                Column {
                    folders.forEachIndexed { i, folder ->
                        if (i > 0) HorizontalDivider(color = DrawerCardBorder)
                        val folderColor = folder.colorArgb?.let { Color(it) } ?: DefaultFolderColor
                        DrawerRow(
                            label = folder.name,
                            count = folder.noteCount,
                            selected = selection == "folder:${folder.name}",
                            tileColor = folderColor,
                            tileAlpha = 0.45f,
                            onClick = { onFolderClick(folder) },
                            tileSize = 34.dp,
                            verticalPadding = 12.dp,
                        ) {
                            // Darkened a little so the pastel folder stays readable on the grey card.
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = lerp(folderColor, Color.Black, 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DrawerRow(
    label: String,
    count: Int?,
    selected: Boolean,
    tileColor: Color,
    onClick: () -> Unit,
    tileAlpha: Float = 0.16f,
    tileSize: Dp = 30.dp,
    verticalPadding: Dp = 12.dp,
    leading: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) StudyOnNeutralLight.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(200),
        label = "drawerRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(pressedScale = 0.98f, role = Role.Button, onClick = onClick)
            .background(background)
            .padding(horizontal = 14.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(tileSize)
                .clip(RoundedCornerShape(if (tileSize > 36.dp) 14.dp else 8.dp))
                .background(tileColor.copy(alpha = tileAlpha)),
            contentAlignment = Alignment.Center
        ) { leading() }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            fontFamily = Lexend,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                "$count",
                fontFamily = Lexend,
                fontSize = 15.sp,
                color = colors.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}