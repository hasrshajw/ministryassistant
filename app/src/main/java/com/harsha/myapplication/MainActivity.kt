package com.harsha.myapplication

import com.harsha.myapplication.telemetry.CrashReporter
import android.os.Bundle
import com.harsha.myapplication.ui.security.SecuritySettingsScreen
import com.harsha.myapplication.ui.security.LockScreen
import com.harsha.myapplication.data.SecurityRepository
import androidx.compose.animation.scaleOut
import androidx.fragment.app.FragmentActivity
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.harsha.myapplication.ui.about.AboutScreen
import com.harsha.myapplication.notifications.AppNotifications
import com.harsha.myapplication.ui.settings.AppearanceSettingsScreen
import com.harsha.myapplication.ui.settings.CustomizeAppScreen
import com.harsha.myapplication.ui.home.DailyTextScreen
import com.harsha.myapplication.ui.settings.GenZModeScreen
import com.harsha.myapplication.ui.home.HomeScreen
import com.harsha.myapplication.ui.home.MonthlyProgressScreen
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.ui.bible.ImportBibleScreen
import com.harsha.myapplication.ui.notes.NoteEditorView
import com.harsha.myapplication.ui.notes.NotesScreen
import com.harsha.myapplication.ui.people.PeopleScreen
import com.harsha.myapplication.ui.legal.PrivacyScreen
import com.harsha.myapplication.ui.profile.ProfileScreen
import com.harsha.myapplication.ui.settings.SettingsScreen
import com.harsha.myapplication.ui.settings.NotificationSettingsScreen
import com.harsha.myapplication.ui.settings.BackupScreen
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.NotificationPrefsRepository
import com.harsha.myapplication.data.HomeLayoutRepository
import com.harsha.myapplication.data.SetupRepository
import com.harsha.myapplication.ui.setup.SetupWizardScreen
import com.harsha.myapplication.ui.legal.TermsScreen
import com.harsha.myapplication.ui.components.AppSharedTransitionHost
import com.harsha.myapplication.ui.components.ProvideAnimatedVisibilityScope
import com.harsha.myapplication.ui.components.predictiveBackPeek
import com.harsha.myapplication.ui.home.sampleDaily
import com.harsha.myapplication.ui.theme.AppearanceRepository
import com.harsha.myapplication.ui.theme.MotionTransitions
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.MyApplicationTheme
import com.harsha.myapplication.ui.theme.StudyBackground
import com.harsha.myapplication.ui.theme.StudyDeepGreen
import com.harsha.myapplication.ui.theme.StudyEmerald
import com.harsha.myapplication.ui.theme.StudyGreenTint
import com.harsha.myapplication.ui.theme.StudyNeutralLight
import com.harsha.myapplication.ui.theme.StudyOnNeutralDark
import com.harsha.myapplication.ui.theme.StudyTextPrimary
import com.harsha.myapplication.ui.theme.StudyTextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Lexend font family
val Lexend = FontFamily(
    Font(R.font.lexend_regular, FontWeight.Normal),
    Font(R.font.lexend_medium, FontWeight.Medium),
    Font(R.font.lexend_bold, FontWeight.SemiBold),
    Font(R.font.lexend_bold, FontWeight.Bold),
)

// ---------- Legacy color aliases (kept for existing screens) ----------
// Mapped onto the new green brand palette so older screens still compile.
// Prefer MaterialTheme.colorScheme in new code.
val Purple = StudyDeepGreen                     // primary brand (was lavender)
val SearchBg = StudyGreenTint                   // soft green container
val CardBg = StudyNeutralLight
val PillBg = StudyNeutralLight
val ScreenBg = StudyBackground
val DotGray = StudyOnNeutralDark
val TextDark = StudyTextPrimary
val TextMuted = StudyTextSecondary

// ---------- Models ----------
data class DailyItem(
    val weekday: String,
    val shortDate: String,
    val title: String,
    val body: String,
)

data class Recommended(val title: String, val imageUrl: String? = null)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Returns immediately: saved Bibles are read on a background thread.
        BibleData.init(applicationContext)
        NotesRepository.init(applicationContext)
        // Load the saved theme BEFORE the first frame so there is no flash of the wrong theme.
        AppearanceRepository.init(applicationContext)
        SetupRepository.init(applicationContext)
        HoursRepository.init(applicationContext)
        NotificationPrefsRepository.init(applicationContext)
        HomeLayoutRepository.init(applicationContext)
        AppNotifications.ensureChannel(applicationContext)
        SecurityRepository.init(applicationContext)
        BackupRepository.init(applicationContext)
        BackupRepository.runAutoBackupIfDue(applicationContext)
        enableEdgeToEdge()
        setContent {
            // ---- Appearance settings ----
            // Saved choices (theme mode, palette, dynamic colour, pure black) live in
            // AppearanceRepository; this is Compose state, so changing them in the Appearance
            // screen re-themes the whole app live.
            val appearance = AppearanceRepository.prefs
            val darkTheme = appearance.themeMode.isDark(isSystemInDarkTheme())

            // Status / navigation bar icon colours must follow the IN-APP theme, not the system one
            // (e.g. user forces Light while the phone is in Dark). Only the icon appearance is
            // touched; enableEdgeToEdge() is NOT called again, so the window is left alone.
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            // No Crossfade / key() around the app here: that would re-create MainAppScaffold on every
            // theme change and reset navigation to Home. MyApplicationTheme animates the colours
            // in place instead.
            MyApplicationTheme(
                darkTheme = darkTheme,
                palette = appearance.palette,
                dynamicColor = appearance.dynamicColor,
                pureBlack = appearance.pureBlack,
            ) {
                // Wizard -> app: a short cross-fade (240ms, with a tiny 96% -> 100% settle).
                // It used to be a slow spring (stiffness 300 + 500ms hold), which felt laggy while
                // Home was composing its first frame. Tweens are cheap and always finish on time.
                // ---- App lock gate ----
                // Locked: an opaque overlay swallows every touch. The app content underneath is removed
                // while locked (see below) so no popup or keyboard can show through.
                val locked = SecurityRepository.isLocked
                // Screenshots and screen recording stay allowed: the lock only gates opening the app.
                // While locked, the app content is removed from composition. That closes every dropdown, dialog
                // and sheet (they are separate windows that would otherwise float above the lock screen), and
                // the keyboard is hidden. Navigation state is kept in the holder below and restored on unlock.
                val stateHolder = rememberSaveableStateHolder()
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(locked) {
                    if (locked) {
                        focusManager.clearFocus(force = true)
                        keyboard?.hide()
                        WindowCompat.getInsetsController(window, view).hide(WindowInsetsCompat.Type.ime())
                    }
                }
                Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                if (!locked) stateHolder.SaveableStateProvider("app_content") {
                AnimatedContent(
                    targetState = SetupRepository.isSetupCompleted,
                    transitionSpec = {
                        (fadeIn(tween(240, delayMillis = 40)) +
                            scaleIn(tween(300), initialScale = 0.96f)) togetherWith
                            fadeOut(tween(160))
                    },
                    label = "setup_to_main",
                ) { setupDone ->
                    if (!setupDone) {
                        val context = LocalContext.current
                        SetupWizardScreen(
                            onFinished = {
                                SetupRepository.setSetupCompleted(context, true)
                            }
                        )
                    } else {
                        // ---- SharedTransition host ----
                        // Installs SharedTransitionLayout and publishes the scope to the
                        // CompositionLocal. Any descendant can now use
                        // Modifier.appSharedBounds / appSharedElement.
                        AppSharedTransitionHost {
                            AdaptiveContentFrame {
                                MainAppScaffold()
                            }
                        }
                    }
                }
                } // SaveableStateProvider
                }
                AnimatedVisibility(
                    visible = locked,
                    enter = fadeIn(Motion.effectsDefault()),
                    exit = fadeOut(Motion.effectsDefault()) + scaleOut(Motion.spatialSlow(), targetScale = 1.06f),
                ) {
                    // Registered after the scaffold's own handlers, so it wins: Back on the lock screen sends
                    // the app to the background instead of closing screens hidden underneath.
                    BackHandler { moveTaskToBack(true) }
                    LockScreen(Modifier.pointerInput(Unit) {}) // opaque + consumes taps
                }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        SecurityRepository.onBackgrounded()
    }

    override fun onStart() {
        super.onStart()
        SecurityRepository.onForegrounded()
    }
}

/**
 * Keeps the app readable on big windows (tablets, unfolded foldables, desktop / freeform mode):
 * content is centred and capped at the Material "expanded" width instead of stretching edge to
 * edge. On phones (and phone landscape) the window is narrower than the cap, so nothing changes.
 * The strips beside the content use the app background, so dark / light themes stay consistent.
 */
@Composable
private fun AdaptiveContentFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

/**
 * Lets any full-screen editor shown inside a tab (e.g. PersonEditScreen) hide the bottom bar
 * while it is on screen. Call with true when it appears and false when it leaves.
 */
val LocalSetBottomBarHidden = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

// The sub-screens that can appear over the tab content.
private enum class SubScreen {
    Bibles, DailyText, NoteEditor, About, Appearance, Profile, GenZMode, CustomizeApp, Terms, Privacy, MonthlyProgress, Notifications, Backup, Security
}

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
    // Folder that a NEW note should start in (the folder selected in the Notes list, if any).
    var newNoteFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showGenZMode by rememberSaveable { mutableStateOf(false) }
    var showCustomizeApp by rememberSaveable { mutableStateOf(false) }
    var showTerms by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var showMonthlyProgress by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showSecurity by rememberSaveable { mutableStateOf(false) }

    // True while the Notes side menu (push drawer) is open: the bottom bar hides and the notes
    // screen grows into the space it leaves.
    var notesDrawerOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedScreen) { if (selectedScreen != 1) notesDrawerOpen = false }
    // True while a full-screen editor (new / edit person) is open over a tab.
    var bottomBarHidden by remember { mutableStateOf(false) }

    val tabStateHolder = rememberSaveableStateHolder()

    // Which sub-screen (if any) is currently overlaying the tabs?
    val subScreen: SubScreen? = when {
        showBibles -> SubScreen.Bibles
        showDailyText -> SubScreen.DailyText
        showNoteEditor -> SubScreen.NoteEditor
        showAbout -> SubScreen.About
        showAppearance -> SubScreen.Appearance
        showProfile -> SubScreen.Profile
        showGenZMode -> SubScreen.GenZMode
        showCustomizeApp -> SubScreen.CustomizeApp
        showTerms -> SubScreen.Terms
        showPrivacy -> SubScreen.Privacy
        showMonthlyProgress -> SubScreen.MonthlyProgress
        showNotifications -> SubScreen.Notifications
        showBackup -> SubScreen.Backup
        showSecurity -> SubScreen.Security
        else -> null
    }

    // Tell Firebase which screen the user is on, so every crash / handled error shows where it happened.
    LaunchedEffect(selectedScreen, subScreen) {
        val tab = when (selectedScreen) { 0 -> "Home"; 1 -> "Notes"; 2 -> "People"; 3 -> "Settings"; else -> "Tab$selectedScreen" }
        CrashReporter.setScreen(if (subScreen != null) "$tab > ${subScreen.name}" else tab)
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
            SubScreen.About -> showAbout = false
            SubScreen.Appearance -> showAppearance = false
            SubScreen.Profile -> showProfile = false
            SubScreen.GenZMode -> showGenZMode = false
            SubScreen.CustomizeApp -> showCustomizeApp = false
            SubScreen.Terms -> showTerms = false
            SubScreen.Privacy -> showPrivacy = false
            SubScreen.MonthlyProgress -> showMonthlyProgress = false
            SubScreen.Notifications -> showNotifications = false
            SubScreen.Backup -> showBackup = false
            SubScreen.Security -> showSecurity = false
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

    CompositionLocalProvider(LocalSetBottomBarHidden provides { bottomBarHidden = it }) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Bottom bar rises with a spring as a sub-screen exits, drops the same way
                // as one opens. Was using default AnimatedVisibility tween (240ms linear-ish).
                AnimatedVisibility(
                    visible = subScreen == null && !notesDrawerOpen && !bottomBarHidden,
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
                targetValue = if (notesDrawerOpen || bottomBarHidden) navInset else scaffoldPadding.calculateBottomPadding(),
                animationSpec = Motion.spatialDefaultDp,
                label = "contentBottomPadding",
            )
            // A spring animation can overshoot below its target (e.g. toward 0.dp when the system
            // navigation inset is 0), and Compose throws "Padding must be non-negative" on that.
            val innerPadding = PaddingValues(
                start = scaffoldPadding.calculateStartPadding(layoutDirection).coerceAtLeast(0.dp),
                top = scaffoldPadding.calculateTopPadding().coerceAtLeast(0.dp),
                end = scaffoldPadding.calculateEndPadding(layoutDirection).coerceAtLeast(0.dp),
                bottom = bottomPadding.coerceAtLeast(0.dp),
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

            // Sub-screens (Bibles, Daily text, Note editor, About) hide the bottom bar, so they get
            // a FIXED bottom padding = the system navigation inset. It is never animated: using the
            // animated `innerPadding` made anything pinned to the bottom of a sub-screen start at the
            // bottom bar's height and slide down while the bar left.
            val subScreenPadding = PaddingValues(
                start = scaffoldPadding.calculateStartPadding(layoutDirection),
                top = scaffoldPadding.calculateTopPadding(),
                end = scaffoldPadding.calculateEndPadding(layoutDirection),
                bottom = navInset,
            )
            val subScreenInsets = Modifier
                .fillMaxSize()
                .padding(subScreenPadding)
                .consumeWindowInsets(subScreenPadding)

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
                        showAbout -> 4
                        showAppearance -> 5
                        showProfile -> 6
                        showGenZMode -> 7
                        showCustomizeApp -> 8
                        showTerms -> 9
                        showPrivacy -> 10
                        showMonthlyProgress -> 11
                        showNotifications -> 12
                        showBackup -> 13
                        showSecurity -> 14
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
                                                onMonthlyProgressClick = { showMonthlyProgress = true },
                                                onDailyTextClick = { index ->
                                                    selectedDayIndex = index
                                                    showDailyText = true
                                                },
                                                onAddNotesClick = {
                                                    editingNoteId = null
                                                    newNoteFolder = null
                                                    showNoteEditor = true
                                                }
                                            )
                                        }
                                        1 -> NotesScreen(
                                            notes = NotesRepository.notes,
                                            searchQuery = searchQuery,
                                            onSearchQueryChange = { searchQuery = it },
                                            onAddNoteClick = { folder ->
                                                editingNoteId = null
                                                newNoteFolder = folder
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
                                        3 -> Box(screenInsets) {
                                            SettingsScreen(
                                                onOpenBibles = { showBibles = true },
                                                onOpenAppearance = { showAppearance = true },
                                                onOpenProfile = { showProfile = true },
                                                onOpenGenZMode = { showGenZMode = true },
                                                onOpenCustomizeApp = { showCustomizeApp = true },
                                                onOpenTerms = { showTerms = true },
                                                onOpenPrivacy = { showPrivacy = true },
                                                onOpenNotifications = { showNotifications = true },
                                                onOpenBackup = { showBackup = true },
                                                onOpenSecurity = { showSecurity = true },
                                                onOpenAbout = { showAbout = true }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // ---- Sub-screens: carry the live predictive-back peek ----
                            Box(subScreenInsets) {
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
                                            initialFolder = newNoteFolder,
                                            // The editor keeps its own note id after the first save, so saving a
                                            // new note twice updates the same note. Do NOT close the screen here:
                                            // after saving, the editor switches to view mode and closes through onBack.
                                            onSave = { draft, existingId -> NotesRepository.save(draft, existingId) },
                                            onBack = { showNoteEditor = false }
                                        )
                                        4 -> AboutScreen(onBack = { showAbout = false })
                                        5 -> AppearanceSettingsScreen(onBack = { showAppearance = false })
                                        6 -> ProfileScreen(onBack = { showProfile = false })
                                        7 -> GenZModeScreen(onBack = { showGenZMode = false })
                                        8 -> CustomizeAppScreen(onBack = { showCustomizeApp = false })
                                        9 -> TermsScreen(onBack = { showTerms = false })
                                        10 -> PrivacyScreen(onBack = { showPrivacy = false })
                                        11 -> MonthlyProgressScreen(onBack = { showMonthlyProgress = false })
                                        12 -> NotificationSettingsScreen(onBack = { showNotifications = false })
                                        13 -> BackupScreen(onBack = { showBackup = false })
                                        14 -> SecuritySettingsScreen(onBack = { showSecurity = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } // CompositionLocalProvider
}

// ---------- Bottom navigation ----------
// Material 3 Expressive ShortNavigationBar. Selected pill, icon and label colors come from the
// theme (secondaryContainer / onSecondaryContainer), so light and dark need no special-casing,
// and the label font comes from Typography (Lexend).

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppBottomNavBar(
    currentIndex: Int,
    onTap: (Int) -> Unit,
) {
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        navTabs.forEachIndexed { i, tab ->
            val selected = currentIndex == i
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onTap(i) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = null,   // the label is the accessible name
                    )
                },
                label = { Text(text = tab.label, maxLines = 1) },
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainAppScaffoldPreview() {
    MyApplicationTheme { MainAppScaffold() }
}
