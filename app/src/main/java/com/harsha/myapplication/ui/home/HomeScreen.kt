package com.harsha.myapplication.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor   // alias: ui.util.lerp (Float) is also imported
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.components.appSharedBoundsOrNone
import com.harsha.myapplication.ui.theme.AppearanceRepository
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.StudyDeepGreen
import com.harsha.myapplication.ui.theme.StudyDeepGreenDarkMode
import com.harsha.myapplication.ui.theme.StudyEmerald
import com.harsha.myapplication.ui.theme.StudyGreenTrack
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.pressScaleClickable
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.data.HomeCard
import com.harsha.myapplication.data.HomeLayoutRepository
import com.harsha.myapplication.data.HoursRepository
import com.harsha.myapplication.data.PeopleRepository
import com.harsha.myapplication.data.SetupRepository
import com.harsha.myapplication.data.PersonRecord
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.people.PersonCard
import com.harsha.myapplication.ui.people.PersonDetailScreen
import com.harsha.myapplication.ui.people.PersonEditScreen
import com.harsha.myapplication.ui.people.daysFromToday
import com.harsha.myapplication.ui.people.visitTimeMinutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ── Design tokens (kept in sync with M3 + brand) ────────────────────────────
private val ScreenMargin = 16.dp
private val RadiusLarge = 28.dp          // matches MaterialTheme.shapes.extraLarge
private val RadiusSmall = 20.dp
private val DailyCardHeight = 168.dp
private val SheetFirstGap = 4.dp
private val HeroCollapseRange = 200.dp
private val SheetSectionGap = 24.dp
private val SheetTitleToContent = 10.dp
private val SheetCardGap = 8.dp
private val MonthlyCardHeight = 128.dp

// ── Palette-aware colors for the Monthly progress + Daily text cards ─────────
@Immutable
private data class HomeCardColors(
    val container: Color,    // card background
    val onContainer: Color,  // main text on the card
    val accent: Color,       // ring progress, "Completed", verse reference
    val track: Color,        // unfilled ring track
)

/**
 * Colors for the Monthly progress and Daily text cards.
 * - "Study" (brand) palette: the original deep green look.
 * - Every other palette (incl. Dynamic): derived from the active ColorScheme, so the cards
 *   change together with the selected palette and in-app theme mode.
 */
@Composable
private fun rememberHomeCardColors(): HomeCardColors {
    val scheme = MaterialTheme.colorScheme
    val prefs = AppearanceRepository.prefs
    val dark = prefs.themeMode.isDark(isSystemInDarkTheme())

    return remember(scheme, prefs.palette, dark) {
        if (prefs.palette.isBrand) {
            HomeCardColors(
                container = if (dark) StudyDeepGreenDarkMode else StudyDeepGreen,
                onContainer = Color.White,
                accent = StudyEmerald,
                track = StudyGreenTrack,
            )
        } else {
            // Light: very dark tinted card (same idea as the deep green). Dark: neutral raised surface.
            val container = if (dark) scheme.surfaceContainerHigh else scheme.onPrimaryContainer
            // The card is dark in both modes, so the accent must be light enough to read on it.
            val accent = if (dark) scheme.primary else lerpColor(scheme.primary, Color.White, 0.35f)
            HomeCardColors(
                container = container,
                onContainer = Color.White,
                accent = accent,
                track = lerpColor(container, accent, 0.25f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    daily: List<DailyItem> = sampleDaily,
    onPersonalStudyClick: () -> Unit = {},
    onNotesTabClick: () -> Unit = {},
    onBibleTabClick: () -> Unit = {},
    onAddNotesClick: () -> Unit = {},
    onAddHoursClick: () -> Unit = {},
    onLogHours: (hours: Double, date: Date) -> Unit = { _, _ -> },
    onMonthlyProgressClick: () -> Unit = {},
    onAddPersonClick: () -> Unit = {},
    onEditLogClick: () -> Unit = {},
    onViewMoreClick: () -> Unit = {},
    onDailyTextClick: (Int) -> Unit = {},
) {
    val todayIndex = remember(daily) {
        val today = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH).format(Date())
        daily.indexOfFirst { it.weekday.equals(today, ignoreCase = true) }
            .takeIf { it >= 0 } ?: (daily.size / 2)
    }
    var dayIndex by remember { mutableIntStateOf(todayIndex) }
    var showAddHoursSheet by remember { mutableStateOf(false) }
    var showAddPerson by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    var people by remember { mutableStateOf<List<PersonRecord>>(emptyList()) }
    var selectedPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(Unit) {
        people = PeopleRepository.load(context)
        onPauseOrDispose {}
    }
    val dueToday = remember(people) {
        people.filter { daysFromToday(it) == 0 }.sortedBy { visitTimeMinutes(it) }
    }
    val upcoming = remember(people) {
        people.filter { daysFromToday(it) in 1..2 }
            .sortedWith(compareBy({ it.nextVisitDate }, { visitTimeMinutes(it) }))
    }

    // Which top cards appear, and in what order, comes from Customize App.
    val cards = HomeLayoutRepository.layout.visibleCards
    val sectionPager = rememberPagerState(
        initialPage = HomeLayoutRepository.layout.startIndex(),
        pageCount = { cards.size },
    )
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    // ── Pull-to-refresh (M3 LoadingIndicator) ───────────────────────────────
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            // Reload people + any other home data
            delay(900) // short wait so the LoadingIndicator is visible
            people = PeopleRepository.load(context)
            isRefreshing = false
        }
    }

    val pageScroll = rememberScrollState()
    var heroHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val collapseRangePx = remember(density) { with(density) { HeroCollapseRange.toPx() } }

    val fullSheetPx by remember {
        derivedStateOf { minOf(heroHeightPx, pageScroll.maxValue) }
    }
    val canScrollSheet by remember { derivedStateOf { fullSheetPx > 0 } }
    val sheetRaised by remember {
        derivedStateOf { fullSheetPx > 0 && pageScroll.value > fullSheetPx / 2 }
    }
    val toggleSheet: () -> Unit = {
        scope.launch {
            pageScroll.animateScrollTo(
                value = if (sheetRaised) 0 else fullSheetPx,
                animationSpec = Motion.spatialDefault,
            )
        }
    }

    // Snap sheet to only two resting positions
    val isDragging by pageScroll.interactionSource.collectIsDraggedAsState()
    var dragStartValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(isDragging) {
        if (isDragging) {
            dragStartValue = pageScroll.value
        } else {
            snapshotFlow { pageScroll.isScrollInProgress }.first { !it }
            val full = fullSheetPx
            val v = pageScroll.value
            if (full > 0 && v in 1 until full) {
                val target = when {
                    v > dragStartValue -> full
                    v < dragStartValue -> 0
                    else -> if (v > full / 2) full else 0
                }
                pageScroll.animateScrollTo(target, Motion.spatialDefault)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            StudyTopBar(
                title = "Home",
                scrollBehavior = scrollBehavior,
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val viewportHeight = maxHeight
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(pageScroll)
                    ) {
                        // ── Hero ────────────────────────────────────────────────
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged { heroHeightPx = it.height }
                                .graphicsLayer {
                                    val y = pageScroll.value.toFloat()
                                    val f = (y / collapseRangePx).coerceIn(0f, 1f)
                                    translationY = y.coerceAtMost(collapseRangePx * 2f) * 0.45f
                                    alpha = 1f - f
                                    val s = lerp(1f, 0.94f, f)
                                    scaleX = s
                                    scaleY = s
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                }
                        ) {
                            HorizontalPager(
                                state = sectionPager,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .revealOnEnter(index = 0, playOnceKey = "home-pager"),
                                verticalAlignment = Alignment.Top,
                            ) { page ->
                                Box(
                                    Modifier.graphicsLayer {
                                        val o = ((sectionPager.currentPage - page) + sectionPager.currentPageOffsetFraction)
                                            .absoluteValue.coerceIn(0f, 1f)
                                        val sc = 1f - 0.06f * o
                                        scaleX = sc
                                        scaleY = sc
                                        alpha = 1f - 0.5f * o
                                    }
                                ) {
                                    when (cards.getOrNull(page) ?: HomeCard.Ministry) {
                                        HomeCard.Notes -> Column(Modifier.fillMaxWidth()) {
                                            SectionTitle("Personal study", icon = Icons.Default.PushPin)
                                            PinnedCard(
                                                height = DailyCardHeight,
                                                icon = Icons.Outlined.PushPin,
                                                message = "Pin a note to keep it always here",
                                                buttonLabel = "Pin a note",
                                                onClick = onPersonalStudyClick,
                                            )
                                        }
                                        HomeCard.Ministry -> Column(Modifier.fillMaxWidth()) {
                                            SectionTitle("Monthly progress")
                                            MonthlyProgressPanel(height = DailyCardHeight, onClick = onMonthlyProgressClick)
                                        }
                                        HomeCard.DailyText -> Column(Modifier.fillMaxWidth()) {
                                            SectionTitle("Daily text")
                                            DailyTextRow(
                                                items = daily,
                                                index = dayIndex,
                                                todayIndex = todayIndex,
                                                height = DailyCardHeight,
                                                onIndexChange = { dayIndex = it },
                                                onDailyTextClick = onDailyTextClick,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                            DotIndicator(
                                count = cards.size,
                                selected = sectionPager.currentPage,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .revealOnEnter(index = 2, playOnceKey = "home-dots"),
                                onDotClick = {
                                    scope.launch {
                                        sectionPager.animateScrollToPage(it, animationSpec = Motion.spatialDefault)
                                    }
                                },
                            )
                            Spacer(Modifier.height(20.dp))

                            Box(Modifier.revealOnEnter(index = 3, playOnceKey = "home-actions")) {
                                ActionButtons(
                                    onAddNotesClick = onAddNotesClick,
                                    onAddHoursClick = { showAddHoursSheet = true },
                                    onAddPersonClick = {
                                        showAddPerson = true
                                        onAddPersonClick()
                                    },
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // ── Bottom sheet ────────────────────────────────────────
                        val hasToday = dueToday.isNotEmpty()
                        val hasUpcoming = upcoming.isNotEmpty()

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = viewportHeight)
                                .revealOnEnter(index = 4, playOnceKey = "home-people-sheet"),
                            shape = RoundedCornerShape(topStart = RadiusLarge, topEnd = RadiusLarge),
                            color = colors.surfaceContainerLowest,
                            tonalElevation = 0.dp,
                            shadowElevation = 8.dp,
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                // Handle
                                val handleSource = remember { MutableInteractionSource() }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .clickable(
                                            interactionSource = handleSource,
                                            indication = null,
                                            enabled = canScrollSheet,
                                            onClickLabel = if (sheetRaised) "Lower sheet" else "Raise sheet",
                                            role = Role.Button,
                                            onClick = toggleSheet,
                                        ),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    Box(
                                        Modifier
                                            .padding(top = 10.dp)
                                            .width(36.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(colors.onSurfaceVariant.copy(alpha = 0.28f))
                                    )
                                }

                                if (hasToday) {
                                    SheetSectionTitle("Today", topGap = SheetFirstGap)
                                    DueCards(
                                        people = dueToday,
                                        emptyText = "Nothing due today",
                                        onPersonClick = { id: String -> selectedPersonId = id },
                                    )
                                }

                                if (hasUpcoming) {
                                    SheetSectionTitle(
                                        "Upcoming",
                                        topGap = if (hasToday) SheetSectionGap else SheetFirstGap,
                                    )
                                    DueCards(
                                        people = upcoming,
                                        emptyText = "Nothing in the next 2 days",
                                        onPersonClick = { id: String -> selectedPersonId = id },
                                    )
                                }

                                SheetSectionTitle(
                                    "Recommended for today",
                                    topGap = if (hasToday || hasUpcoming) SheetSectionGap else SheetFirstGap,
                                )
                                RecommendedPager(sampleRecommended)

                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                } // BoxWithConstraints
            } // PullToRefreshBox
        }

        if (showAddHoursSheet) {
            AddHoursSheet(
                onDismiss = { showAddHoursSheet = false },
                onSave = { hours, date ->
                    HoursRepository.add(context, hours, date)   // feeds the Monthly progress card
                    onLogHours(hours, date)
                    showAddHoursSheet = false
                },
            )
        }

        if (showAddPerson) {
            // Same screen PeopleScreen opens for a new profile.
            PersonEditScreen(
                onDismiss = { showAddPerson = false },
                onSaved = {
                    showAddPerson = false
                    people = PeopleRepository.load(context)   // refresh Today / Upcoming
                },
            )
        }

        selectedPersonId?.let { id ->
            PersonDetailScreen(
                recordId = id,
                onDismiss = {
                    selectedPersonId = null
                    people = PeopleRepository.load(context)
                },
            )
        }
    }
}

// ── Section title ───────────────────────────────────────────────────────────
@Composable
private fun SectionTitle(title: String, icon: ImageVector? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun SheetSectionTitle(title: String, topGap: Dp) {
    Text(
        title,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(start = ScreenMargin, end = ScreenMargin, top = topGap, bottom = SheetTitleToContent),
    )
}

// ── Action buttons (M3 tonal / filled) ──────────────────────────────────────
@Composable
private fun ActionButtons(
    onAddNotesClick: () -> Unit,
    onAddHoursClick: () -> Unit,
    onAddPersonClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionChip(
            label = "Notes",
            icon = Icons.Default.Edit,
            container = colors.surfaceContainerHigh,
            content = colors.onSurface,
            onClick = onAddNotesClick,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            label = "Hours",
            icon = Icons.Default.MoreTime,
            container = colors.surfaceContainerHigh,
            content = colors.onSurface,
            onClick = onAddHoursClick,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            label = "Person",
            icon = Icons.Default.PersonAdd,
            container = colors.primary,
            content = colors.onPrimary,
            onClick = onAddPersonClick,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Expressive button: full pill at rest, squarer corners while pressed (spring morph). */
@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 16.dp else 28.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "actionCorner",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        interactionSource = src,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1,
            )
        }
    }
}

// ── Due cards (uses the shared PersonCard from PeopleScreen) ─────────────────
@Composable
private fun DueCards(
    people: List<PersonRecord>,
    emptyText: String,
    onPersonClick: (String) -> Unit,
) {
    if (people.isEmpty()) {
        Text(
            emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp),
        )
        return
    }
    Column(
        Modifier.padding(horizontal = ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(SheetCardGap),
    ) {
        people.forEach { record ->
            PersonCard(record) { onPersonClick(record.id) }   // shared with PeopleScreen
        }
    }
}


// ── Monthly progress ────────────────────────────────────────────────────────
@Composable
private fun MonthlyGoalCard(
    monthLabel: String,
    completedMinutes: Int,
    goalHours: Int,
    daysLeft: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RadiusLarge),
) {
    val card = rememberHomeCardColors()
    val progress = (completedMinutes / 60f / goalHours).coerceIn(0f, 1f)
    val percentage = (progress * 100).roundToInt()
    val completedText = if (completedMinutes % 60 == 0) {
        "${completedMinutes / 60}"
    } else {
        String.format(Locale.US, "%.1f", completedMinutes / 60.0)
    }
    val remainingHours = (goalHours - completedMinutes / 60.0).coerceAtLeast(0.0)
    val perDayText = if (daysLeft > 0 && remainingHours > 0) {
        String.format(Locale.US, "%.1fh / day left", remainingHours / daysLeft)
    } else {
        "Goal reached"
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress, Motion.spatialDefault)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = card.container,
        contentColor = card.onContainer,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Top-right directed icon (circle background)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.ArrowOutward,
                    contentDescription = "View monthly progress",
                    tint = card.onContainer.copy(alpha = 0.90f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeWidthPx = 8.5.dp.toPx()
                        val diameter = size.minDimension - strokeWidthPx
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f,
                        )
                        val arcSize = Size(diameter, diameter)
                        val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        val progressAngle = (animatedProgress.value * 360f).coerceIn(0f, 360f)
                        val gapAngle = 14f

                        when {
                            progressAngle <= 0f -> {
                                drawArc(
                                    color = card.track,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = stroke,
                                )
                                drawCircle(
                                    color = card.accent,
                                    radius = strokeWidthPx / 2f,
                                    center = Offset(size.width / 2f, topLeft.y),
                                )
                            }
                            progressAngle >= 360f -> drawArc(
                                color = card.accent,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = stroke,
                            )
                            else -> {
                                drawArc(
                                    color = card.accent,
                                    startAngle = -90f,
                                    sweepAngle = progressAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = stroke,
                                )
                                val trackSweep = 360f - progressAngle - 2 * gapAngle
                                if (trackSweep > 0f) {
                                    drawArc(
                                        color = card.track,
                                        startAngle = -90f + progressAngle + gapAngle,
                                        sweepAngle = trackSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = stroke,
                                    )
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = card.onContainer,
                            letterSpacing = (-0.5).sp,
                        )
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmallEmphasized,
                            color = card.accent,
                        )
                    }
                }

                Spacer(Modifier.width(18.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        letterSpacing = 1.8.sp,
                        color = card.onContainer.copy(alpha = 0.60f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$completedText / ${goalHours}h",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        letterSpacing = (-0.8).sp,
                        color = card.onContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x20FFFFFF),
                    ) {
                        Text(
                            text = perDayText,
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            color = card.onContainer.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }  // Box
    }
}

@Composable
private fun MonthlyProgressPanel(height: Dp, onClick: () -> Unit) {
    val panelColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val panelTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Real data: hours logged through AddHoursSheet against the goal chosen in Setup.
    val now = Calendar.getInstance()
    val goalHours = SetupRepository.monthlyGoalHours.coerceAtLeast(1)
    val completedMinutes = HoursRepository.minutesInMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
    val daysLeft = now.getActualMaximum(Calendar.DAY_OF_MONTH) - now.get(Calendar.DAY_OF_MONTH) + 1
    val monthLabel = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(now.time).uppercase(Locale.ENGLISH)

    val remainingMinutes = (goalHours * 60 - completedMinutes).coerceAtLeast(0)
    val message = if (remainingMinutes > 0) {
        val h = remainingMinutes / 60
        val m = remainingMinutes % 60
        val left = when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
        "$left left to reach your ${goalHours}h goal"
    } else {
        "Goal reached. Great work this month!"
    }

    // Press scale must wrap the WHOLE panel (background included). Previously pressScale sat after
    // clip/background, so only the inner green card shrank while the grey panel stayed put.
    val pressSrc = remember { MutableInteractionSource() }

    Column(
        Modifier
            .padding(horizontal = ScreenMargin)
            // Same container-transform as the Daily text card: the card grows into the page.
            .appSharedBoundsOrNone(key = "monthly-card", cornerRadius = RadiusLarge)
            .fillMaxWidth()
            .height(height)
            .pressScale(pressSrc, pressedScale = 0.97f)      // scales everything drawn below
            .clip(RoundedCornerShape(RadiusLarge))
            .background(panelColor)
            .clickable(
                interactionSource = pressSrc,
                indication = LocalIndication.current,
                onClickLabel = "Open monthly progress",
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        MonthlyGoalCard(
            monthLabel = monthLabel,
            completedMinutes = completedMinutes,
            goalHours = goalHours,
            daysLeft = daysLeft,
            modifier = Modifier.height(MonthlyCardHeight),
            shape = RoundedCornerShape(RadiusLarge),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.labelMedium,
                color = panelTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PinnedCard(
    height: Dp,
    icon: ImageVector,
    message: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val pressSrc = remember { MutableInteractionSource() }
    Column(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .height(height)
            .pressScale(pressSrc, pressedScale = 0.97f)      // same fix: scale the whole card
            .clip(RoundedCornerShape(RadiusLarge))
            .background(colors.surfaceContainerHigh)
            .clickable(
                interactionSource = pressSrc,
                indication = LocalIndication.current,
                onClickLabel = buttonLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onClick) {
            Text(buttonLabel, style = MaterialTheme.typography.labelLargeEmphasized)
        }
    }
}

// ── Daily text morphing row ─────────────────────────────────────────────────
private val PillWidth = 36.dp
private val PillGap = 6.dp

@Composable
private fun DailyTextRow(
    items: List<DailyItem>,
    index: Int,
    todayIndex: Int,
    height: Dp,
    onIndexChange: (Int) -> Unit,
    onDailyTextClick: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    BoxWithConstraints(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .height(height)
    ) {
        val rowWidth = maxWidth
        val centerWidth = rowWidth - (PillWidth + PillGap) * 2

        items.forEachIndexed { i, item ->
            key(i) {
                DayShape(
                    item = item,
                    rel = i - index,
                    isToday = i == todayIndex,
                    rowWidth = rowWidth,
                    centerWidth = centerWidth,
                    onClick = {
                        val rel = i - index
                        if (rel == 0) {
                            onDailyTextClick(i)
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIndexChange(i)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DayShape(
    item: DailyItem,
    isToday: Boolean,
    rel: Int,
    rowWidth: Dp,
    centerWidth: Dp,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val card = rememberHomeCardColors()
    val sideFill = colors.surfaceContainerHigh
    val sideText = colors.onSurfaceVariant
    val density = LocalDensity.current
    val pillPx = with(density) { PillWidth.toPx() }
    val gapPx = with(density) { PillGap.toPx() }
    val rowPx = with(density) { rowWidth.toPx() }
    val centerPx = with(density) { centerWidth.toPx() }

    val targetX = when {
        rel <= -2 -> -(pillPx + gapPx)
        rel == -1 -> 0f
        rel == 0 -> pillPx + gapPx
        rel == 1 -> rowPx - pillPx
        else -> rowPx + gapPx
    }
    val targetW = if (rel == 0) centerPx else pillPx

    val sharedBounds = if (rel == 0) {
        Modifier.appSharedBoundsOrNone(key = "daily-card", cornerRadius = RadiusLarge)
    } else Modifier
    val pressSrc = remember { MutableInteractionSource() }

    val xState = animateFloatAsState(targetX, Motion.spatialFast, label = "dayX")
    val wState = animateFloatAsState(targetW, Motion.spatialFast, label = "dayWidth")
    val aState = animateFloatAsState(
        targetValue = if (rel in -1..1) 1f else 0f,
        animationSpec = Motion.effectsFast,
        label = "dayAlpha",
    )
    val fillState = animateColorAsState(
        targetValue = if (isToday) card.container else sideFill,
        animationSpec = motion.defaultEffectsSpec(),
        label = "dayFill",
    )

    val progress = { ((wState.value - pillPx) / (centerPx - pillPx)).coerceIn(0f, 1f) }

    Box(
        Modifier
            .offset { IntOffset(xState.value.roundToInt(), 0) }
            .then(sharedBounds)
            .layout { measurable, constraints ->
                val w = wState.value.roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(Constraints.fixed(w, constraints.maxHeight))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .pressScale(pressSrc, pressedScale = if (rel == 0) 0.97f else 0.92f)
            .graphicsLayer {
                alpha = aState.value
                shape = RoundedCornerShape(
                    (RadiusSmall.value + (RadiusLarge.value - RadiusSmall.value) * progress()).dp
                )
                clip = true
            }
            .drawBehind { drawRect(fillState.value) }
            .clickable(
                interactionSource = pressSrc,
                indication = LocalIndication.current,
                enabled = rel in -1..1,
                role = Role.Button,
                onClickLabel = if (rel == 0) "Open ${item.shortDate}" else "Show ${item.shortDate}",
                onClick = onClick,
            ),
    ) {
        // Pill content
        Box(
            Modifier
                .width(PillWidth)
                .fillMaxHeight()
                .graphicsLayer { alpha = (1f - progress() * 3f).coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.shortDate,
                modifier = Modifier
                    .rotate(90f)
                    .wrapContentSize(unbounded = true),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = if (isToday) card.onContainer else sideText,
                maxLines = 1,
                softWrap = false,
            )
        }

        // Card content
        Column(
            Modifier
                .requiredWidth(centerWidth)
                .fillMaxHeight()
                .graphicsLayer { alpha = ((progress() - 0.45f) * 2.5f).coerceIn(0f, 1f) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                item.weekday,
                style = MaterialTheme.typography.labelLarge,
                color = if (isToday) card.onContainer.copy(alpha = 0.75f) else sideText.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(6.dp))
            val (verse, reference) = splitVerse(item.body)
            var verseSize by remember(verse) { mutableFloatStateOf(14f) }
            Text(
                verse,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontSize = verseSize.sp,
                lineHeight = (verseSize * 1.6f).sp,
                textAlign = TextAlign.Center,
                color = if (isToday) card.onContainer else sideText,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow && verseSize > 11f) verseSize -= 1f
                },
            )
            if (reference.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    reference.replace(' ', '\u00A0'),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isToday) card.accent else colors.secondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun DotIndicator(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
    onDotClick: ((Int) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val inactive = colors.secondaryContainer
    if (count <= 1) return   // a single page needs no dots
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val active = i == selected
            val width by animateDpAsState(
                targetValue = if (active) 21.dp else 8.dp,
                animationSpec = Motion.spatialFastDp,
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (active) colors.primary else inactive,
                animationSpec = motion.defaultEffectsSpec(),
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (onDotClick != null)
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = "Go to page ${i + 1}",
                            ) { onDotClick(i) }
                        else Modifier
                    )
            )
        }
    }
}

// ── Sample data ─────────────────────────────────────────────────────────────
private val dailyTexts = listOf(
    "“‘మీరే నా సాక్షులు’ అని యెహోవా అంటున్నాడు.”—యెష. 43:12.",
    "“తన ఇంటివాళ్లకు ఎలా నాయకత్వం వహించాలో తెలియని వ్యక్తి దేవుని సంఘాన్ని ఎలా చూసుకుంటాడు?”—1 తిమో. 3:5.",
    "“స్నేహితుల కోసం ప్రాణం పెట్టడం కన్నా గొప్ప ప్రేమ లేదు.”—యోహా. 15:13.",
    "“రోజంతా నేను శ్రమ అనుభవించాను.”—కీర్త. 73:14.",
    "“యెహోవా అనే పేరున్న నువ్వు మాత్రమే భూమంతటి పైన మహోన్నతుడివని ప్రజలు తెలుసుకోవాలి.”—కీర్త. 83:18.",
    "“ఆయన . . . అనారోగ్యంతో ఉన్న వాళ్లందర్నీ బాగుచేశాడు.”—మత్త. 8:16.",
    "“చివరి రోజుల్లో ప్రమాదకరమైన, కష్టమైన కాలాలు వస్తాయి.”—2 తిమో. 3:1.",
    "‘ఆయన తన సొంత కుమారుణ్ణి మరణానికి అప్పగించాడు.’—రోమా. 8:32.",
)

val sampleDaily: List<DailyItem> = run {
    val weekdayFormat = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH)
    val shortFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    dailyTexts.map { text ->
        val date = cal.time
        cal.add(Calendar.DAY_OF_YEAR, 1)
        DailyItem(weekdayFormat.format(date), shortFormat.format(date), "", text)
    }
}

// ── Recommended (palette-aware gradients) ───────────────────────────────────
/** Original deep-green gradients, used by the "Study" brand palette. */
private val RecommendedGradients = listOf(
    Color(0xFF1F9D5B) to Color(0xFF001F1E),
    Color(0xFF2FBF71) to Color(0xFF1E3F37),
    Color(0xFF3F6B5C) to Color(0xFF001F1E),
    Color(0xFF2A9D8F) to Color(0xFF1A3A34),
    Color(0xFF40916C) to Color(0xFF1B4332),
)

/**
 * Gradients for the Recommended cards.
 * - "Study" (brand) palette: the original green set.
 * - Every other palette (incl. Dynamic): built from the active ColorScheme (primary / tertiary /
 *   secondary and blends of them) fading into the same deep base the Monthly + Daily cards use, so
 *   the cards follow the selected palette and light/dark mode like the rest of the app.
 */
@Composable
private fun rememberRecommendedGradients(): List<Pair<Color, Color>> {
    val scheme = MaterialTheme.colorScheme
    val prefs = AppearanceRepository.prefs
    val dark = prefs.themeMode.isDark(isSystemInDarkTheme())

    return remember(scheme, prefs.palette, dark) {
        if (prefs.palette.isBrand) {
            RecommendedGradients
        } else {
            val end = if (dark) scheme.surfaceContainerHigh else scheme.onPrimaryContainer
            val starts = listOf(
                scheme.primary,
                scheme.tertiary,
                scheme.secondary,
                lerpColor(scheme.primary, scheme.tertiary, 0.5f),
                lerpColor(scheme.secondary, scheme.primary, 0.5f),
            )
            // In dark mode the roles are light tones; pull them toward the base so the cards stay calm.
            starts.map { start -> (if (dark) lerpColor(start, end, 0.35f) else start) to end }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecommendedPager(items: List<Recommended>) {
    val gradients = rememberRecommendedGradients()
    val pageCount = 1_000_000
    val pagerState = rememberPagerState(
        initialPage = pageCount / 2,
        pageCount = { pageCount },
    )
    val scope = rememberCoroutineScope()
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = Motion.spatialDefault,
    )

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pagerState.settledPage, isDragged) {
        if (isDragged) return@LaunchedEffect
        delay(4000)
        pagerState.animateScrollToPage(
            pagerState.settledPage + 1,
            animationSpec = Motion.spatialDefault,
        )
    }

    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 8.dp,
            flingBehavior = flingBehavior,
        ) { page ->
            val actualPage = page % items.size
            val (start, end) = gradients[actualPage % gradients.size]
            val src = remember { MutableInteractionSource() }
            Card(
                onClick = { /* open recommended */ },
                interactionSource = src,
                shape = RoundedCornerShape(RadiusLarge),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pressScale(src, pressedScale = 0.97f)
                    .graphicsLayer {
                        val offset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                            .absoluteValue.coerceIn(0f, 1f)
                        val s = lerp(0.92f, 1f, 1f - offset)
                        scaleX = s
                        scaleY = s
                        alpha = lerp(0.7f, 1f, 1f - offset)
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(start, end)))
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.5f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.55f),
                                )
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DotIndicator(
            count = items.size,
            selected = pagerState.currentPage % items.size,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onDotClick = { targetIndex ->
                scope.launch {
                    val currentActual = pagerState.currentPage % items.size
                    val diff = (targetIndex - currentActual + items.size) % items.size
                    val targetPage = pagerState.currentPage + diff
                    pagerState.animateScrollToPage(targetPage, animationSpec = Motion.spatialDefault)
                }
            },
        )
    }
}

val sampleRecommended = listOf(
    Recommended("Family Worship Ideas"),
    Recommended("Ministry & Public Speaking"),
    Recommended("Deep Dive: Genesis Creation"),
    Recommended("Understanding Prophecy"),
    Recommended("Building Strong Faith"),
)

// Helper used by DayShape
private fun splitVerse(body: String): Pair<String, String> {
    val idx = body.lastIndexOf("—")
    return if (idx >= 0) {
        body.substring(0, idx).trim() to body.substring(idx + 1).trim()
    } else {
        body to ""
    }
}
