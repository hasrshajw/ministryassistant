@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.harsha.myapplication.ui.notes

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.data.DEFAULT_CATEGORIES
import com.harsha.myapplication.data.DEFAULT_CATEGORY
import com.harsha.myapplication.data.FolderRepository
import com.harsha.myapplication.data.NotesRepository
import com.harsha.myapplication.data.NoteItem
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.theme.rememberAppFeedback
import com.harsha.myapplication.ui.components.AppSearchField
import com.harsha.myapplication.ui.components.SortMenuButton
import com.harsha.myapplication.ui.components.StudyTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


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
    /**
     * New note. [folder] is the folder currently selected in the side menu, so the editor can open
     * with it preselected (null = "All" / Uncategorized selected).
     */
    onAddNoteClick: (folder: String?) -> Unit = {},
    /** No longer shown (the Quick Notes button was removed from the side menu); kept so existing callers still compile. */
    onQuickNotesClick: (folder: String?) -> Unit = onAddNoteClick,
    onNoteClick: (NoteItem) -> Unit = {},
    folders: List<NotesFolder> = emptyList(),
    onFolderClick: (NotesFolder) -> Unit = {},
    /** A folder was created in the side menu. Store it (name + ARGB colour) and pass it back via `folders`. */
    onCreateFolder: (name: String, colorArgb: Int) -> Unit = { _, _ -> },
    /**
     * A folder was edited from the side menu (pencil on its row). Already saved by the screen itself
     * (FolderRepository + the notes' category); this is only a hook for the host. For a colour-only
     * edit oldName == newName.
     */
    onEditFolder: (oldName: String, newName: String, colorArgb: Int) -> Unit = { _, _, _ -> },
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
    var editingFolder by remember { mutableStateOf<NotesFolder?>(null) }

    // Load the saved folders (and make sure the notes store is open). Both calls are no-ops after the first.
    val appContext = LocalContext.current
    remember { NotesRepository.init(appContext); FolderRepository.init(appContext) }
    val savedFolders = FolderRepository.folders

    // Derived data is rebuilt only when the notes change, not on every keystroke / recomposition.
    val rows = remember(notes) { notes.map { NoteRow(it) } }
    // Folders handed in by the host + every folder saved on disk (they survive closing the app).
    val hostedFolders = remember(folders, savedFolders) {
        folders + savedFolders.map { NotesFolder(it.name, 0, it.colorArgb) }
    }
    val allFolders = remember(hostedFolders, createdFolders, rows) { buildFolderList(hostedFolders, createdFolders, rows) }
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
        else -> "Notes"
    }
    // Folder to preselect when a new note is started from here.
    val selectedFolderName: String? =
        if (drawerSelection.startsWith("folder:")) drawerSelection.removePrefix("folder:") else null
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val typography = MaterialTheme.typography
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val fabExpanded by remember {
        derivedStateOf { listState.lastScrolledBackward || !listState.canScrollBackward }
    }
    // Folder colour per (lower-cased) name, so each note card can tint itself with its folder.
    val folderColors = remember(allFolders) {
        allFolders.associate { it.name.lowercase() to (it.colorArgb?.let { c -> Color(c) } ?: DefaultFolderColor) }
    }
    // Quick-switch pills: same selection as the side menu.
    val pillEntries = remember(allFolders, notes.size, uncategorizedCount) {
        buildList {
            add(NotesPillEntry("all", "All", notes.size, null))
            add(NotesPillEntry("uncategorized", DEFAULT_CATEGORY, uncategorizedCount, null))
            allFolders.forEach { add(NotesPillEntry("folder:${it.name}", it.name, it.noteCount, it)) }
        }
    }

    // ---- Push drawer ------------------------------------------------------------------------
    // The side menu does NOT overlay the notes: the whole screen slides right by the drawer width
    // while the menu stays underneath (with a small parallax). `drawerProgress` (0 = closed,
    // 1 = open) is the single source of truth; the finger, the menu button and the predictive
    // back gesture all drive it, and everything else is derived from it in the draw phase only.
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val feedback = rememberAppFeedback()
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
        scope.launch {
            drawerProgress.animateTo(if (open) 1f else 0f, Motion.spatialDefault)
            feedback.tick()
        }
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
            .background(drawerBackground())                  // the menu's own background
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
                onCreateFolderClick = { showNewFolder = true },
                onEditFolderClick = { editingFolder = it },
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
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
                topBar = {
                    StudyTopBar(
                        title = screenTitle,
                        // Note count lives under the title; the title itself doesn't move.
                        subtitle = "${visible.size} note${if (visible.size == 1) "" else "s"}",
                        keepTitleInPlace = true,
                        scrollBehavior = scrollBehavior,
                    )
                },
                floatingActionButton = {
                    val fabSrc = remember { MutableInteractionSource() }
                    ExtendedFloatingActionButton(
                        onClick = {
                            feedback.click()
                            onAddNoteClick(selectedFolderName)   // opens the editor in the selected folder
                        },
                        interactionSource = fabSrc,
                        modifier = Modifier.pressScale(fabSrc),
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        expanded = fabExpanded,
                        icon = { Icon(Icons.Default.Add, contentDescription = "New note") },
                        text = { Text("New note", style = typography.labelLargeEmphasized) },
                    )
                },
                containerColor = colors.surface
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Search bar + sort button (same components as the People screen)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
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
                            onSelect = {
                                feedback.select()
                                sort = it
                            },
                            label = { it.label },
                            icon = { it.icon },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Left: a pinned "Folders" button that opens the side menu (a navigation action,
                    // so it is a squarer, outlined-free button with a chevron and stays put).
                    // Right: the scrolling folder filter pills. A divider separates the two so the
                    // button is not mistaken for another filter.
                    // Scroll state is hoisted so the button can shrink to icon-only once the
                    // pills are swiped away from their start position.
                    val pillScroll = rememberScrollState()
                    // Collapsing the button widens the pill viewport by roughly the label + chevron
                    // width. If the pills only overflow by less than that, collapsing makes them fit
                    // entirely, the scroll snaps back to 0, the button re-expands, and the pills
                    // flicker with an empty gap on the right. So: only collapse when there is enough
                    // overflow to survive the widening, and stay collapsed (latched) until the pills
                    // are scrolled back to the start.
                    val collapseWidthPx = with(density) { 96.dp.toPx() }
                    var foldersCollapsed by remember { mutableStateOf(false) }
                    LaunchedEffect(pillScroll, collapseWidthPx) {
                        snapshotFlow { pillScroll.value to pillScroll.maxValue }
                            .collect { (value, max) ->
                                if (!foldersCollapsed) {
                                    if (value > 0 && max > collapseWidthPx) foldersCollapsed = true
                                } else if (value == 0) {
                                    foldersCollapsed = false
                                }
                            }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .revealOnEnter(index = 1, playOnceKey = "notes-actions"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FoldersMenuButton(
                            collapsed = foldersCollapsed,
                            onClick = {
                                feedback.click()
                                setDrawer(true)
                            }
                        )
                        // Tight spacing around the divider while the button is just an icon.
                        val dividerGap by animateDpAsState(
                            if (foldersCollapsed) 6.dp else 12.dp,
                            MaterialTheme.motionScheme.fastSpatialSpec(),
                            label = "dividerGap",
                        )
                        Box(
                            Modifier
                                .padding(horizontal = dividerGap)
                                .width(1.dp)
                                .height(24.dp)
                                .background(colors.outlineVariant)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(pillScroll)
                                .padding(end = 16.dp),   // last pill stops 16dp from the edge, but scrolls right up to it
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pillEntries.forEach { entry ->
                                NotesPill(
                                    label = entry.label,
                                    count = entry.count,
                                    selected = drawerSelection == entry.key,
                                    onClick = {
                                        feedback.select()
                                        drawerSelection = entry.key
                                        entry.folder?.let(onFolderClick)
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            .padding(horizontal = 16.dp)
                            .weight(1f),
                        label = "notesEmptyState"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = MaterialShapes.Cookie9Sided.toShape(),
                                        color = colors.primaryContainer,
                                        modifier = Modifier.size(96.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Note,
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = when {
                                            searchQuery.isNotBlank() -> "No matching notes"
                                            drawerSelection != "all" -> "No notes in this folder"
                                            else -> "No notes yet"
                                        },
                                        style = typography.titleMediumEmphasized,
                                        color = colors.onSurface
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Tap New note to start your first study note",
                                        style = typography.bodyMedium,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp),   // clears the extended FAB
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = visible,
                                    key = { _, it -> it.note.id },          // stable identity: no re-creation when the list changes
                                    contentType = { _, _ -> "note" }
                                ) { index, row ->
                                    NoteCard(
                                        row = row,
                                        folderColor = folderColors[row.folder.lowercase()] ?: colors.primary,
                                        onClick = { feedback.click(); onNoteClick(row.note) },
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

    editingFolder?.let { folder ->
        EditFolderDialog(
            folder = folder,
            existing = listOf(DEFAULT_CATEGORY) + allFolders.map { it.name }.filterNot { it.equals(folder.name, true) },
            // Built-in categories are re-added by the app on every start, so only their colour is editable.
            renameable = DEFAULT_CATEGORIES.none { it.equals(folder.name, ignoreCase = true) },
            onDismiss = { editingFolder = null },
            onSave = { newName, color ->
                FolderRepository.update(folder.name, newName, color.toArgb())   // name + colour, saved to disk
                NotesRepository.renameCategory(folder.name, newName)            // notes follow the new name
                // Forget the old name in this session's created list so it doesn't reappear as a folder.
                createdFolders = createdFolders.filterNot {
                    it.substringBeforeLast(FOLDER_SEP).equals(folder.name, ignoreCase = true)
                }
                if (drawerSelection.equals("folder:${folder.name}", ignoreCase = true)) {
                    drawerSelection = "folder:$newName"     // keep the renamed folder selected
                }
                editingFolder = null
                onEditFolder(folder.name, newName, color.toArgb())
            }
        )
    }

    if (showNewFolder) {
        NewFolderDialog(
            existing = listOf(DEFAULT_CATEGORY) + allFolders.map { it.name },
            onDismiss = { showNewFolder = false },
            onCreate = { name, color ->
                createdFolders = createdFolders + "$name$FOLDER_SEP${color.toArgb()}"
                FolderRepository.add(name, color.toArgb())      // saved to disk
                showNewFolder = false
                onCreateFolder(name, color.toArgb())
            }
        )
    }
}

private val NoteCardCornerRest = 28.dp
private val NoteCardCornerPressed = 18.dp
private val NoteCardPadding = 16.dp

private class NotesPillEntry(val key: String, val label: String, val count: Int, val folder: NotesFolder?)

/**
 * Expressive note card: tonal card whose corners morph on press, a cookie-shaped tile tinted with the
 * note's folder colour, an emphasized title, and a small folder badge + date row.
 */
@Composable
private fun NoteCard(
    row: NoteRow,
    folderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) NoteCardCornerPressed else NoteCardCornerRest,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "noteCardCorner",
    )
    // Blended toward onSurface so pastel folder colours stay readable in light and dark.
    val tint = lerp(folderColor, colors.onSurface, 0.35f)
    // Card background: a light wash of the note's folder colour over the normal card colour, so notes
    // from different folders read as different soft colours (works in light and dark). Text keeps
    // onSurface / onSurfaceVariant, which stay well above AA contrast at this low blend.
    val base = colors.surfaceContainerLow
    val container = remember(folderColor, base) { lerp(base, folderColor, 0.20f) }
    // Thin folder-tinted edge so pale / grey folder colours never melt into the app background.
    val edge = remember(folderColor, colors.outlineVariant) { lerp(colors.outlineVariant, folderColor, 0.6f) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, edge),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NoteCardPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = folderColor.copy(alpha = 0.42f),
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Note,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = row.note.title,
                    style = typography.titleMediumEmphasized,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.preview.isNotBlank()) {
                    Text(
                        text = row.preview,
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = folderColor.copy(alpha = 0.42f),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            row.folder,
                            style = typography.labelSmallEmphasized,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = row.date,
                        style = typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Opens the full side menu (folders, events, new folder).
 * Deliberately NOT pill-shaped like the filter pills: a 12dp rounded square in a neutral
 * container, folder icon + label + chevron, so it reads as "go to folders", not as a filter.
 */
@Composable
private fun FoldersMenuButton(collapsed: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // 18dp icon + 11dp each side = a 40dp square when collapsed (icon centred).
    val startPad by animateDpAsState(if (collapsed) 11.dp else 12.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "foldersStartPad")
    val endPad by animateDpAsState(if (collapsed) 11.dp else 8.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "foldersEndPad")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 20.dp else 12.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "foldersButtonCorner",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .semantics { contentDescription = "Open folders menu" },
        shape = RoundedCornerShape(corner),
        color = colors.surfaceContainerHighest,
        contentColor = colors.onSurface,
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(start = startPad, end = endPad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            // Label + chevron slide away while the filter pills are scrolled.
            AnimatedVisibility(
                visible = !collapsed,
                enter = expandHorizontally() + fadeIn(Motion.effectsDefault),
                exit = shrinkHorizontally() + fadeOut(Motion.effectsDefault),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(6.dp))
                    Text("Folders", style = MaterialTheme.typography.labelLargeEmphasized, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Folder quick-switch pill: round when idle, squarer when selected (spring morph). */
@Composable
private fun NotesPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val motion = MaterialTheme.motionScheme
    val selectedContent = colors.onPrimary
    val container by animateColorAsState(
        if (selected) colors.primary else colors.surfaceContainerLow,
        motion.defaultEffectsSpec(),
        label = "notesPillBg",
    )
    val content by animateColorAsState(
        if (selected) selectedContent else colors.onSurfaceVariant,
        motion.defaultEffectsSpec(),
        label = "notesPillFg",
    )
    val badge by animateColorAsState(
        if (selected) selectedContent.copy(alpha = 0.18f) else colors.surfaceContainerHighest,
        motion.defaultEffectsSpec(),
        label = "notesPillBadge",
    )
    val corner by animateDpAsState(
        if (selected) 14.dp else 20.dp,
        motion.fastSpatialSpec(),
        label = "notesPillCorner",
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

// ============================================================================
//  Push-drawer menu — M3 grouped lists (same pattern as the Settings screen)
// ============================================================================

private val DrawerGroupOuter = 28.dp
private val DrawerGroupInner = 4.dp
private val DrawerGroupGap = 2.dp

/** Items fade in and slide in from the left one after another while the menu opens. */
private fun Modifier.drawerReveal(progress: () -> Float, index: Int): Modifier = graphicsLayer {
    val t = ((progress() - index * 0.07f) / 0.55f).coerceIn(0f, 1f)
    alpha = t
    translationX = (1f - t) * -28.dp.toPx()
}

private fun drawerGroupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) DrawerGroupOuter else DrawerGroupInner
    val bottom = if (index == count - 1) DrawerGroupOuter else DrawerGroupInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun <T> DrawerGroup(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, shape: Shape) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DrawerGroupGap)
    ) {
        items.forEachIndexed { index, item -> row(item, drawerGroupShape(index, items.size)) }
    }
}

@Composable
private fun DrawerSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp)
    )
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
    onCreateFolderClick: () -> Unit,
    onEditFolderClick: (NotesFolder) -> Unit,
    onEventClick: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Title
        Text(
            "Notes",
            style = typography.headlineSmallEmphasized,
            color = colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 16.dp, bottom = 16.dp)
                .drawerReveal(progress, 0)
        )

        // All / Uncategorized
        DrawerGroup(
            items = listOf("all", "uncategorized"),
            modifier = Modifier.drawerReveal(progress, 2)
        ) { key, shape ->
            if (key == "all") {
                DrawerRow(
                    label = "All",
                    count = noteCount,
                    selected = selection == "all",
                    shape = shape,
                    icon = Icons.AutoMirrored.Filled.Note,
                    onClick = { onSelect("all") },
                )
            } else {
                DrawerRow(
                    label = "Uncategorized",
                    count = uncategorizedCount,
                    selected = selection == "uncategorized",
                    shape = shape,
                    icon = Icons.Outlined.Inbox,
                    onClick = { onSelect("uncategorized") },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Events
        DrawerSectionHeader("Events", Modifier.drawerReveal(progress, 3))
        Spacer(Modifier.height(8.dp))
        DrawerGroup(
            items = NotesEvent.entries.toList(),
            modifier = Modifier.drawerReveal(progress, 4)
        ) { event, shape ->
            DrawerRow(
                label = event.label,
                count = null,                       // events have no note count
                selected = false,
                shape = shape,
                icon = when (event) {
                    NotesEvent.Conventions -> Icons.Default.Stadium
                    NotesEvent.Assemblies -> Icons.Default.Groups
                    NotesEvent.Others -> Icons.Outlined.Category
                },
                onClick = { onEventClick(event) },
                tileColor = when (event) {
                    NotesEvent.Conventions -> colors.tertiaryContainer
                    NotesEvent.Assemblies -> colors.primaryContainer
                    NotesEvent.Others -> colors.surfaceContainerHighest
                },
                iconTint = when (event) {
                    NotesEvent.Conventions -> colors.onTertiaryContainer
                    NotesEvent.Assemblies -> colors.onPrimaryContainer
                    NotesEvent.Others -> colors.onSurfaceVariant
                },
            )
        }

        Spacer(Modifier.height(24.dp))

        // My folders + new folder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawerReveal(progress, 5),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerSectionHeader("Other folders", Modifier.weight(1f))
            FilledTonalIconButton(onClick = onCreateFolderClick) {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = "New folder",
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        if (folders.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawerReveal(progress, 6),
                shape = RoundedCornerShape(DrawerGroupOuter),
                color = drawerRowColor(),
            ) {
                Text(
                    "No folders yet. Tap the folder button to create one.",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            DrawerGroup(
                items = folders,
                modifier = Modifier.drawerReveal(progress, 6)
            ) { folder, shape ->
                val folderColor = folder.colorArgb?.let { Color(it) } ?: DefaultFolderColor
                DrawerRow(
                    label = folder.name,
                    count = folder.noteCount,
                    selected = selection == "folder:${folder.name}",
                    shape = shape,
                    icon = Icons.Default.Folder,
                    onClick = { onFolderClick(folder) },
                    accent = folderColor,
                    onEditClick = { onEditFolderClick(folder) },
                    tileColor = folderColor.copy(alpha = 0.42f),
                    // Blended toward onSurface so the pastel folder stays readable in light and dark.
                    iconTint = lerp(folderColor, colors.onSurface, 0.35f),
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** Drawer background: a soft neutral so the white / raised rows on top of it stand out. */
@Composable
private fun drawerBackground(): Color {
    val c = MaterialTheme.colorScheme
    return if (c.background.luminance() > 0.5f) c.surfaceContainerHigh else c.surfaceContainer
}

/** Resting colour of a drawer row: white on the grey menu in light mode, a raised tone in dark mode. */
@Composable
private fun drawerRowColor(): Color {
    val c = MaterialTheme.colorScheme
    return if (c.background.luminance() > 0.5f) c.surfaceContainerLowest else c.surfaceContainerHigh
}

/**
 * M3 list row: [Surface] + [ListItem]. Selected rows use secondaryContainer, or a wash of the folder
 * colour when [accent] is given. When [onEditClick] is set the trailing chevron becomes a pencil.
 */
@Composable
private fun DrawerRow(
    label: String,
    count: Int?,
    selected: Boolean,
    shape: Shape,
    icon: ImageVector,
    onClick: () -> Unit,
    tileColor: Color? = null,
    iconTint: Color? = null,
    accent: Color? = null,
    onEditClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val rowBase = drawerRowColor()

    val selectedContainer = if (accent != null) lerp(rowBase, accent, 0.38f) else colors.secondaryContainer
    val selectedContent = if (accent != null) colors.onSurface else colors.onSecondaryContainer
    val container by animateColorAsState(
        targetValue = if (selected) selectedContainer else rowBase,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "drawerRowContainer"
    )
    val content = if (selected) selectedContent else colors.onSurface
    val trailing = if (selected) selectedContent else colors.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    label,
                    style = if (selected) typography.bodyLargeEmphasized else typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = tileColor ?: colors.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconTint ?: colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (count != null) {
                        Text("$count", style = typography.labelLarge)
                        Spacer(Modifier.width(4.dp))
                    }
                    if (onEditClick != null) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit folder $label",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = content,
                trailingIconColor = trailing,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
//  Edit folder dialog (name + colour)
// ============================================================================

/** Soft pastel swatches offered when editing a folder (the folder's current colour is added if missing). */
private val FolderEditPalette = listOf(
    Color(0xFFF4C95D), // yellow
    Color(0xFFF2A65A), // orange
    Color(0xFFEF8F8F), // red
    Color(0xFFE79BC7), // pink
    Color(0xFFB79CE8), // purple
    Color(0xFF8FA8F0), // blue
    Color(0xFF7CCBE0), // cyan
    Color(0xFF6FD1B0), // teal
    Color(0xFF8FD18A), // green
    Color(0xFFB5BDB9), // grey
)

@Composable
private fun EditFolderDialog(
    folder: NotesFolder,
    existing: List<String>,
    renameable: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Color) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val initialColor = folder.colorArgb?.let { Color(it) } ?: DefaultFolderColor
    var name by rememberSaveable { mutableStateOf(folder.name) }
    var selectedArgb by rememberSaveable { mutableIntStateOf(initialColor.toArgb()) }
    val swatches = remember(initialColor) {
        if (FolderEditPalette.any { it.toArgb() == initialColor.toArgb() }) FolderEditPalette
        else listOf(initialColor) + FolderEditPalette
    }

    val trimmed = name.trim()
    val duplicate = existing.any { it.equals(trimmed, ignoreCase = true) }
    val error = when {
        trimmed.isEmpty() -> "Enter a folder name"
        duplicate -> "A folder with this name already exists"
        else -> null
    }
    val unchanged = trimmed == folder.name && selectedArgb == initialColor.toArgb()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Edit folder", style = typography.titleLargeEmphasized) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Folder name") },
                    singleLine = true,
                    enabled = renameable,
                    isError = error != null,
                    supportingText = {
                        if (!renameable) Text("Built-in folder: only the colour can be changed")
                        else error?.let { Text(it) }
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Colour", style = typography.labelLargeEmphasized, color = colors.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    swatches.forEach { swatch ->
                        val isSelected = swatch.toArgb() == selectedArgb
                        Surface(
                            onClick = { selectedArgb = swatch.toArgb() },
                            shape = CircleShape,
                            color = swatch,
                            border = if (isSelected) BorderStroke(3.dp, colors.onSurface) else null,
                            modifier = Modifier.size(40.dp),
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF191C1B),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmed, Color(selectedArgb)) },
                enabled = error == null && !unchanged,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
