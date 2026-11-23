package com.harsha.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.outlined.Note as NoteOutlined
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.data.NotesRepository
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.ui.DailyTextScreen
import com.harsha.myapplication.ui.HomeScreen
import com.harsha.myapplication.ui.ImportBibleScreen
import com.harsha.myapplication.ui.NoteEditorView
import com.harsha.myapplication.ui.NotesScreen
import com.harsha.myapplication.ui.PeopleScreen
import com.harsha.myapplication.ui.SettingsScreen
import com.harsha.myapplication.ui.components.AppSharedTransitionHost
import com.harsha.myapplication.ui.components.ProvideAnimatedVisibilityScope
import com.harsha.myapplication.ui.components.predictiveBackPeek
import com.harsha.myapplication.ui.sampleDaily
import com.harsha.myapplication.ui.theme.MotionTransitions
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- Colors (Package-visible across screens) ----------
val Purple = Color(0xFF6B5B95)
val SearchBg = Color(0xFFEDE7F8)
val CardBg = Color(0xFFE4DBF5)
val PillBg = Color(0xFFE6E1E8)
val ScreenBg = Color(0xFFF1EEF4)
val DotGray = Color(0xFFC9C5CF)
val TextDark = Color(0xFF1B1B1F)
val TextMuted = Color(0xFF5F5B66)

// Lexend font family
val Lexend = FontFamily(
    Font(R.font.lexend_regular, FontWeight.Normal),
    Font(R.font.lexend_medium, FontWeight.Medium),
    Font(R.font.lexend_bold, FontWeight.SemiBold),
    Font(R.font.lexend_bold, FontWeight.Bold),
)

// ---------- Models ----------
data class DailyItem(
    val weekday: String,
    val shortDate: String,
    val title: String,
    val body: String,
)

data class Recommended(val title: String, val imageUrl: String? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Returns immediately: saved Bibles are read on a background thread.
        BibleData.init(applicationContext)
        NotesRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            // ---- Theme-switch crossfade ----
            // When the system theme (or a manual override) flips, the palette morphs
            // over 300 ms instead of flashing. `isSystemInDarkTheme()` is the trigger;
            // passing it down explicitly means MyApplicationTheme's own default is
            // overridden during the transition.
            val darkTheme = isSystemInDarkTheme()
            Crossfade(
                targetState = darkTheme,
                animationSpec = Motion.effectsDefault,
                label = "themeCrossfade",
            ) { dark ->
                MyApplicationTheme(darkTheme = dark) {
                    // ---- SharedTransition host ----
                    // Installs SharedTransitionLayout and publishes the scope to the
                    // CompositionLocal. Any descendant can now use
                    // Modifier.appSharedBounds / appSharedElement.
                    AppSharedTransitionHost {
                        MainAppScaffold()
                    }
                }
            }
        }
    }
}

// The three possible sub-screens that can appear over the tab content.
private enum class SubScreen { Bibles, DailyText, NoteEditor }

// ---------- Main Scaffold with Bottom Navigation ----------
@Composable
fun MainAppScaffold() {
    var selectedScreen by rememberSaveable { mutableIntStateOf(0) } // 0:Home 1:Notes 2:People 3:Settings
    var showBibles by rememberSaveable { mutableStateOf(false) }
    var showDailyText by rememberSaveable { mutableStateOf(false) }
    var selectedDayIndex by rememberSaveable { mutableIntStateOf(0) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showNoteEditor by rememberSaveable { mutableStateOf(false) }
    var editingNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    // True while the Notes side menu (push drawer) is open: the bottom bar hides and the notes
    // screen grows into the space it leaves.
    var notesDrawerOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedScreen) { if (selectedScreen != 1) notesDrawerOpen = false }

    val tabStateHolder = rememberSaveableStateHolder()

    // Which sub-screen (if any) is currently overlaying the tabs?
    val subScreen: SubScreen? = when {
        showBibles -> SubScreen.Bibles
        showDailyText -> SubScreen.DailyText
        showNoteEditor -> SubScreen.NoteEditor
        else -> null
    }

    // ---- Predictive back, driven in-app ----
    // While the finger drags from the screen edge, the sub-screen itself shrinks, drifts toward
    // the finger and gains rounded corners (Android 14+ style peek). On commit the progress is
    // left where it is so the exit slide continues from the shrunken state instead of snapping
    // back to full size; on cancel it springs back to 0.
    val scope = rememberCoroutineScope()
    val backProgress = remember { Animatable(0f) }
    var backEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    fun closeSubScreen() {
        when (subScreen) {
            SubScreen.Bibles -> showBibles = false
            SubScreen.DailyText -> showDailyText = false
            SubScreen.NoteEditor -> showNoteEditor = false
            null -> {}
        }
    }

    // NoteEditor registers its own BackHandlers for the unsaved-changes dialog; those are
    // registered later, so they win and this handler never discards unsaved edits.
    PredictiveBackHandler(enabled = subScreen != null) { progressFlow ->
        try {
            progressFlow.collect { event ->
                backEdge = event.swipeEdge
                backProgress.snapTo(event.progress)
            }
            closeSubScreen()          // committed
        } catch (e: CancellationException) {
            scope.launch { backProgress.animateTo(0f, Motion.spatialDefault) }
            throw e
        }
    }

    // Reset the peek after the exit finished (and as a safety net when a sub-screen opens).
    LaunchedEffect(subScreen) {
        if (subScreen == null) {
            delay(600)
            backProgress.snapTo(0f)
        } else if (backProgress.value != 0f && !backProgress.isRunning) {
            backProgress.snapTo(0f)
        }
    }

    // On a tab other than Home, back returns to Home (animated by the tab fade-through)
    // instead of closing the app.
    BackHandler(enabled = subScreen == null && selectedScreen != 0) { selectedScreen = 0 }

    val haptics = LocalHapticFeedback.current
    fun selectTab(i: Int) {
        if (selectedScreen != i) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            selectedScreen = i
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Bottom bar rises with a spring as a sub-screen exits, drops the same way
            // as one opens. Was using default AnimatedVisibility tween (240ms linear-ish).
            AnimatedVisibility(
                visible = subScreen == null && !notesDrawerOpen,
                enter = slideInVertically(Motion.spatialDefault<IntOffset>()) { it } +
                        fadeIn(Motion.effectsFast),
                exit = slideOutVertically(Motion.spatialDefault<IntOffset>()) { it } +
                        fadeOut(Motion.effectsFast),
            ) {
                AppBottomNavBar(
                    currentIndex = selectedScreen,
                    onTap = { selectTab(it) },
                )
            }
        }
    ) { scaffoldPadding ->
        // While the Notes menu is open the bottom bar is gone, so the content grows down to the
        // system navigation inset; the padding springs in step with the bar sliding away / back.
        val layoutDirection = LocalLayoutDirection.current
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomPadding by animateDpAsState(
            targetValue = if (notesDrawerOpen) navInset else scaffoldPadding.calculateBottomPadding(),
            animationSpec = Motion.spatialDefaultDp,
            label = "contentBottomPadding",
        )
        val innerPadding = PaddingValues(
            start = scaffoldPadding.calculateStartPadding(layoutDirection),
            top = scaffoldPadding.calculateTopPadding(),
            end = scaffoldPadding.calculateEndPadding(layoutDirection),
            bottom = bottomPadding,
        )
        // The Scaffold already reserves the status bar and the bottom bar (which handles the
        // system navigation inset itself). Each screen applies this modifier itself, which pads it
        // and marks those insets as consumed so screens that call navigationBarsPadding() /
        // statusBarsPadding() don't add the same space a second time.
        // The Notes tab is the exception: it receives the padding as `contentPadding` and paints
        // its own background edge-to-edge, so the status bar and navigation bar areas always match
        // the side menu / notes colors.
        val screenInsets = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)

        Box(modifier = Modifier.fillMaxSize()) {
            // ---- Navigation transitions ----
            //  tabs  ↔ Daily text / Note editor : container fade (Daily text also morphs from the
            //                                     card through appSharedBoundsOrNone)
            //  tabs  ↔ Bibles                   : push / pop (hierarchical, parallax)
            // On pop the closing screen stays on top (zIndex) so you actually see it leave.
            AnimatedContent(
                targetState = when {
                    showBibles -> 1
                    showDailyText -> 2
                    showNoteEditor -> 3
                    else -> 0
                },
                transitionSpec = {
                    val from = initialState
                    val to = targetState
                    val forward = to > from
                    if (to >= 2 || from >= 2) MotionTransitions.containerFade(forward)
                    else if (forward) MotionTransitions.push()
                    else MotionTransitions.pop()
                },
                label = "sub_screens"
            ) { state ->
                ProvideAnimatedVisibilityScope(this) {
                    if (state == 0) {
                        // ---- Tabs: siblings → Material fade-through (fade + 92% → 100% scale) ----
                        AnimatedContent(
                            targetState = selectedScreen,
                            transitionSpec = { MotionTransitions.fadeThrough() },
                            label = "tabs"
                        ) { screen ->
                            tabStateHolder.SaveableStateProvider(screen) {
                                when (screen) {
                                    0 -> Box(screenInsets) {
                                        HomeScreen(
                                            onNotesTabClick = { selectTab(1) },
                                            onBibleTabClick = { selectTab(2) },
                                            onDailyTextClick = { index ->
                                                selectedDayIndex = index
                                                showDailyText = true
                                            },
                                            onAddNotesClick = {
                                                editingNoteId = null
                                                showNoteEditor = true
                                            }
                                        )
                                    }
                                    1 -> NotesScreen(
                                        notes = NotesRepository.notes,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        onAddNoteClick = {
                                            editingNoteId = null
                                            showNoteEditor = true
                                        },
                                        onNoteClick = { note ->
                                            editingNoteId = note.id
                                            showNoteEditor = true
                                        },
                                        onDrawerOpenChange = { notesDrawerOpen = it },
                                        contentPadding = innerPadding,
                                    )
                                    2 -> Box(screenInsets) { PeopleScreen() }
                                    3 -> Box(screenInsets) { SettingsScreen(onOpenBibles = { showBibles = true }) }
                                }
                            }
                        }
                    } else {
                        // ---- Sub-screens: carry the live predictive-back peek ----
                        Box(screenInsets) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .predictiveBackPeek(
                                        progress = { backProgress.value },
                                        edgeSign = { if (backEdge == BackEventCompat.EDGE_LEFT) 1f else -1f },
                                    )
                            ) {
                                when (state) {
                                    1 -> ImportBibleScreen(onBack = { showBibles = false })
                                    2 -> DailyTextScreen(
                                        daily = sampleDaily,
                                        initialIndex = selectedDayIndex,
                                        onBack = { showDailyText = false }
                                    )
                                    3 -> NoteEditorView(
                                        initialNote = editingNoteId?.let { id ->
                                            NotesRepository.notes.firstOrNull { it.id == id }
                                        },
                                        categories = NotesRepository.categories,
                                        // The editor keeps its own note id after the first save, so saving a
                                        // new note twice updates the same note. Do NOT close the screen here:
                                        // after saving, the editor switches to view mode and closes through onBack.
                                        onSave = { draft, existingId -> NotesRepository.save(draft, existingId) },
                                        onBack = { showNoteEditor = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Bottom navigation (SyncSkill-style) ----------
// Selected tab: a highlight pill spreads out behind the icon, the icon pops (1.0 → 1.10 → 1.0),
// the filled icon replaces the outlined one and the label turns bold. Colors match SyncSkill. Every tile also squishes to
// 90% while pressed. Haptics still come from selectTab() in MainAppScaffold.
private val NavEase = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f)

// Same palette as SyncSkill's AppColors (surfaceCard, surfaceStroke, navActive, navInactive, navHighlight)
private val NavBarBgLight = Color(0xFFFFFFFF)
private val NavBarBgDark = Color(0xFF161616)
private val NavStrokeLight = Color(0xFFE8EAEE)
private val NavStrokeDark = Color(0xFF2C2C2C)
private val NavActiveLight = Color(0xFF111111)
private val NavActiveDark = Color(0xFF2FBF71)      // emerald
private val NavInactiveLight = Color(0xFF757575)
private val NavInactiveDark = Color(0xFF9E9E9E)
private val NavHighlightLight = Color(0xFFD8FDD2)  // mint pill
private val NavHighlightDark = Color(0xFF1B3D2F)

private data class NavTab(
    val label: String,
    val icon: ImageVector,           // unselected (outlined)
    val selectedIcon: ImageVector,   // selected (filled)
)

private val navTabs = listOf(
    NavTab("Home", Icons.Outlined.Home, Icons.Default.Home),
    NavTab("Notes", Icons.AutoMirrored.Outlined.NoteOutlined, Icons.AutoMirrored.Filled.Note),
    NavTab("People", Icons.Outlined.People, Icons.Default.People),
    NavTab("Settings", Icons.Outlined.Settings, Icons.Default.Settings),
)

@Composable
private fun AppBottomNavBar(
    currentIndex: Int,
    onTap: (Int) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) NavBarBgDark else NavBarBgLight)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = if (isDark) NavStrokeDark else NavStrokeLight,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            navTabs.forEachIndexed { i, tab ->
                Box(modifier = Modifier.weight(1f)) {
                    NavTabTile(
                        tab = tab,
                        isSelected = currentIndex == i,
                        onTap = { onTap(i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabTile(
    tab: NavTab,
    isSelected: Boolean,
    onTap: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val press = remember { Animatable(1f) }
    val spread = remember { Animatable(if (isSelected) 1f else 0f) }
    val pop = remember { Animatable(1f) }

    var prevSelected by remember { mutableStateOf(isSelected) }
    LaunchedEffect(isSelected) {
        if (isSelected != prevSelected) {
            if (isSelected) {
                coroutineScope {
                    launch {
                        spread.snapTo(0f)
                        spread.animateTo(1f, tween(260, easing = NavEase))
                    }
                    launch {
                        pop.snapTo(1f)
                        pop.animateTo(1.10f, tween(192, easing = NavEase))
                        pop.animateTo(1.0f, tween(288, easing = NavEase))
                    }
                }
            } else {
                spread.snapTo(0f)
                pop.snapTo(1f)
            }
            prevSelected = isSelected
        }
    }

    val activeColor = if (isDark) NavActiveDark else NavActiveLight
    val inactiveColor = if (isDark) NavInactiveDark else NavInactiveLight
    val highlightColor = if (isDark) NavHighlightDark else NavHighlightLight
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = isSelected
                onClick { onTap(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { press.animateTo(0.90f, tween(100, easing = NavEase)) }
                        val released = tryAwaitRelease()
                        scope.launch { press.animateTo(1f, tween(250, easing = NavEase)) }
                        if (released) onTap()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val padH = if (isSelected) (2f + (20f - 2f) * spread.value).dp else 0.dp
        val iconScale = if (isSelected) pop.value else 1f
        val pillShape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = press.value; scaleY = press.value }
                .then(
                    if (isSelected) Modifier.shadow(
                        elevation = 6.dp,
                        shape = pillShape,
                        ambientColor = highlightColor.copy(alpha = if (isDark) 0.35f else 0.45f),
                        spotColor = highlightColor.copy(alpha = if (isDark) 0.35f else 0.45f),
                    ) else Modifier
                )
                .background(if (isSelected) highlightColor else Color.Transparent, pillShape)
                .padding(horizontal = padH, vertical = 3.dp),
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                contentDescription = null,   // the label below is the accessible name
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
            )
        }

        Spacer(Modifier.height(5.dp))

        val spacing by animateFloatAsState(
            targetValue = if (isSelected) -0.2f else 0f,
            animationSpec = tween(180, easing = NavEase),
            label = "navLetterSpacing",
        )
        val textColor by animateColorAsState(
            targetValue = if (isSelected) activeColor else inactiveColor,
            animationSpec = tween(180, easing = NavEase),
            label = "navTextColor",
        )
        Text(
            text = tab.label,
            style = TextStyle(
                fontFamily = Lexend,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
                letterSpacing = spacing.sp,
            ),
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainAppScaffoldPreview() {
    MyApplicationTheme { MainAppScaffold() }
}