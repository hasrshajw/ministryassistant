package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.pressScaleClickable
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.components.appSharedBoundsOrNone
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ---- Design tokens: one spacing / radius scale used everywhere ----
private val ScreenMargin = 16.dp
private val RadiusLarge = 28.dp   // cards, sheet
private val RadiusSmall = 20.dp   // pills
private val DailyCardHeight = 168.dp   // fixed; shrink/grow this one value to resize the card
// Same height as the SyncSkill hero card: 100dp ring + 14dp top padding + 14dp bottom padding
private val MonthlyCardHeight = 128.dp

// Monthly progress card palette (same colors as the SyncSkill hero card)
private val GoalEmerald = Color(0xFF2FBF71)       // progress + "Completed"
private val GoalCardDark = Color(0xFF001F1E)      // card background (light theme)
private val GoalCardDarkMode = Color(0xFF1A1A1A)  // card background (dark theme)
private val GoalRingTrack = Color(0xFF1E3F37)     // unfilled part of the ring

// Daily text: only TODAY's card/pill gets this dark green background (white text on it)
private val DailyCenterCard = Color(0xFF001F1E)
private val OnDailyCenterCard = Color.White

// Daily text: every other day (side pills and a selected non-today card), neutral grey so the
// dark green center card stands out
private val DailySidePillLight = Color(0xFFE2E5E3)    // soft grey (light theme)
private val OnDailySidePillLight = Color(0xFF3A4140)
private val DailySidePillDark = Color(0xFF2B302F)     // dark grey (dark theme)
private val OnDailySidePillDark = Color(0xFFC9CFCD)

// Page dots: emerald when active, soft green-gray when inactive
private val DotActive = Color(0xFF2FBF71)
private val DotInactiveLight = Color(0xFFC5DDD0)
private val DotInactiveDark = Color(0xFF1E3F37)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    daily: List<DailyItem> = sampleDaily,
    onMenuClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onPersonalStudyClick: () -> Unit = {},
    onNotesTabClick: () -> Unit = {},
    onBibleTabClick: () -> Unit = {},
    onAddNotesClick: () -> Unit = {},
    onAddHoursClick: () -> Unit = {},
    onEditLogClick: () -> Unit = {},
    onViewMoreClick: () -> Unit = {},
    onDailyTextClick: (Int) -> Unit = {}
) {
    // Index of today's entry (matched by "Sunday, November 22" style text); falls back to the middle item.
    val todayIndex = remember(daily) {
        val today = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH).format(Date())
        daily.indexOfFirst { it.weekday.equals(today, ignoreCase = true) }
            .takeIf { it >= 0 } ?: (daily.size / 2)
    }
    var dayIndex by remember { mutableIntStateOf(todayIndex) }
    // 0 = Personal study, 1 = Monthly progress (center), 2 = Daily text (right)
    val sectionPager = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val colors = MaterialTheme.colorScheme

    // ---- Continuously-coupled button weight -----------------------------------
    // `currentPage + currentPageOffsetFraction` is the true sub-pixel position of
    // the pager, from 0 (Personal study) to 2 (Daily text). When on page 1
    // (Monthly progress, center), progressToHours = 1f (hours big); on page 0
    // or page 2, progressToHours = 0f (notes big).
    val combined = sectionPager.currentPage + sectionPager.currentPageOffsetFraction
    val progressToHours = 1f - (combined - 1f).absoluteValue.coerceIn(0f, 1f)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Home",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Open menu")
                }
            },
            actions = {
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.surface,
                scrolledContainerColor = colors.surfaceContainer,
                titleContentColor = colors.onSurface,
                navigationIconContentColor = colors.onSurfaceVariant,
                actionIconContentColor = colors.onSurfaceVariant,
            ),
            scrollBehavior = scrollBehavior,
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Swipe sideways to switch between sections
                HorizontalPager(
                    state = sectionPager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .revealOnEnter(index = 0, playOnceKey = "home-pager"),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    // Depth: the page leaving / arriving shrinks to 94% and fades to 50% while the
                    // finger drags. graphicsLayer only, so it costs no recomposition.
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
                        when (page) {
                            0 -> Column(Modifier.fillMaxWidth()) {
                                SectionTitle("Personal study", icon = Icons.Default.PushPin)
                                PinnedCard(
                                    height = DailyCardHeight,
                                    icon = Icons.Outlined.PushPin,
                                    message = "Pin a note to keep it always here",
                                    buttonLabel = "Pin a note",
                                    onClick = onPersonalStudyClick,
                                )
                            }
                            1 -> Column(Modifier.fillMaxWidth()) {
                                SectionTitle("Monthly progress")
                                MonthlyProgressPanel(height = DailyCardHeight)
                            }
                            else -> Column(Modifier.fillMaxWidth()) {
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
                // These dots belong to the two sections above, not to the individual days
                DotIndicator(
                    count = 3,
                    selected = sectionPager.currentPage,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .revealOnEnter(index = 2, playOnceKey = "home-dots"),
                    onDotClick = { scope.launch { sectionPager.animateScrollToPage(it, animationSpec = Motion.spatialDefault) } },
                )
                Spacer(Modifier.height(20.dp))

                // 0f (Personal study / Daily text) -> notes is big.
                // 1f (Monthly progress) -> hours is big.
                // Interpolated continuously from pager offset; spring smooths snaps.
                Box(Modifier.revealOnEnter(index = 3, playOnceKey = "home-actions")) {
                    ActionButtons(
                        progressToHours = progressToHours,
                        onAddNotesClick = onAddNotesClick,
                        onAddHoursClick = onAddHoursClick,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** Splits "verse text"—reference into its two parts (reference is empty if there is no dash). */
private fun splitVerse(body: String): Pair<String, String> {
    val cut = body.lastIndexOf('—')
    return if (cut > 0) body.substring(0, cut) to body.substring(cut + 1).trim() else body to ""
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(
        Modifier.padding(start = ScreenMargin, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Two action buttons that always share the row in a 3 : 1 ratio.
 *
 * The active one is big (weight 3, full label); the other shrinks to weight 1
 * (icon only). [progressToHours] drives the interpolation continuously:
 *   0f → notes at weight 3, hours at weight 1
 *   1f → notes at weight 1, hours at weight 3
 *
 * Because the parent computes this from the pager's live offset, the buttons
 * morph WITH the finger during a swipe. The spring here is only smoothing the
 * pager's snap so the settle has a tiny bounce instead of a hard stop.
 */
@Composable
private fun ActionButtons(
    progressToHours: Float,
    onAddNotesClick: () -> Unit,
    onAddHoursClick: () -> Unit,
) {
    val notesWeight by animateFloatAsState(
        targetValue = lerp(3f, 1f, progressToHours),
        animationSpec = Motion.spatialDefault,
        label = "notesWeight",
    )
    val hoursWeight by animateFloatAsState(
        targetValue = lerp(1f, 3f, progressToHours),
        animationSpec = Motion.spatialDefault,
        label = "hoursWeight",
    )

    // Label threshold: whichever button owns more than 50% of the row shows its text.
    val notesExpanded = progressToHours < 0.5f

    Row(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpandingActionButton(
            label = "Quick notes",
            icon = Icons.Default.Edit,
            expanded = notesExpanded,
            onClick = onAddNotesClick,
            modifier = Modifier.weight(notesWeight),
        )
        ExpandingActionButton(
            label = "Add hours",
            icon = Icons.Default.MoreTime,
            expanded = !notesExpanded,
            onClick = onAddHoursClick,
            modifier = Modifier.weight(hoursWeight),
        )
    }
}

@Composable
private fun ExpandingActionButton(
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Same palette as the top cards: active = dark green center card, inactive = grey side card.
    val dark = isSystemInDarkTheme()
    val inactiveFill = if (dark) DailySidePillDark else DailySidePillLight
    val inactiveText = if (dark) OnDailySidePillDark else OnDailySidePillLight
    // Colour change is a binary "expanded" state; 200ms reads as crisp without a flash.
    val container by animateColorAsState(
        targetValue = if (expanded) DailyCenterCard else inactiveFill,
        animationSpec = tween(200),
        label = "btnContainer",
    )
    val content by animateColorAsState(
        targetValue = if (expanded) OnDailyCenterCard else inactiveText,
        animationSpec = tween(200),
        label = "btnContent",
    )

    val src = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        interactionSource = src,
        modifier = modifier
            .fillMaxHeight()
            .pressScale(src),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Icon(icon, contentDescription = if (expanded) null else label, modifier = Modifier.size(20.dp))
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(Motion.effectsFast) + expandHorizontally(Motion.spatialDefault<IntSize>()),
            exit = fadeOut(Motion.effectsFast) + shrinkHorizontally(Motion.spatialDefault<IntSize>()),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * Monthly goal card: dark card, green progress ring, month / time goal / per-day pill.
 *
 * Ring animation: the arc fills from 0 to [progress] with a 150-stiffness spring
 * on entry (and on any data change). The percentage counter is derived from the
 * same [Animatable] so it ticks up in step with the ring — one source of truth,
 * one animation.
 */
@Composable
private fun MonthlyGoalCard(
    monthLabel: String,
    completedMinutes: Int,
    goalHours: Int,
    daysLeft: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RadiusSmall),
) {
    val goalMinutes = goalHours * 60
    val progress = if (goalMinutes > 0) {
        (completedMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
    } else 0f

    // Entry + data-change animation for the ring. Also drives the percentage text.
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        progressAnim.animateTo(
            targetValue = progress,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 150f),
        )
    }
    val animatedProgress = progressAnim.value
    val percentage = (animatedProgress * 100).roundToInt()

    val remainingMinutes = (goalMinutes - completedMinutes).coerceAtLeast(0)
    val perDayMinutes = if (daysLeft > 0) remainingMinutes / daysLeft else remainingMinutes
    val completedText = if (completedMinutes < 60) {
        "${completedMinutes}m"
    } else {
        "${completedMinutes / 60}h ${completedMinutes % 60}m"
    }
    val perDayText = "${perDayMinutes / 60}h ${perDayMinutes % 60}m/day · $daysLeft days left"

    val cardColor = if (isSystemInDarkTheme()) GoalCardDarkMode else GoalCardDark

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, GoalEmerald.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Progress ring — uses animatedProgress, not raw progress.
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = 8.5.dp.toPx()
                    val diameter = size.minDimension - strokeWidthPx
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)
                    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

                    val progressAngle = (animatedProgress * 360f).coerceIn(0f, 360f)
                    val gapAngle = 14f

                    when {
                        progressAngle <= 0f -> {
                            drawArc(
                                color = GoalRingTrack,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = stroke
                            )
                            // Start marker dot at the top when nothing is completed yet
                            drawCircle(
                                color = GoalEmerald,
                                radius = strokeWidthPx / 2f,
                                center = Offset(size.width / 2f, topLeft.y)
                            )
                        }
                        progressAngle >= 360f -> drawArc(
                            color = GoalEmerald,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                        else -> {
                            drawArc(
                                color = GoalEmerald,
                                startAngle = -90f,
                                sweepAngle = progressAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = stroke
                            )
                            val trackSweep = 360f - progressAngle - 2 * gapAngle
                            if (trackSweep > 0f) {
                                drawArc(
                                    color = GoalRingTrack,
                                    startAngle = -90f + progressAngle + gapAngle,
                                    sweepAngle = trackSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = stroke
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        text = "Completed",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoalEmerald,
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monthLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.8.sp,
                    color = Color.White.copy(alpha = 0.60f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$completedText / ${goalHours}h",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.8).sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x20FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = perDayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Monthly progress section: one rounded container in the same tone as the Personal study card.
 * The progress card on top is exactly [MonthlyCardHeight] (same as the SyncSkill hero card);
 * the space left under it holds a short motivational line. The container height equals [height],
 * so the pager keeps the same size on every page.
 */
@Composable
private fun MonthlyProgressPanel(
    height: Dp,
) {
    // Same grey as the Daily text side date cards
    val dark = isSystemInDarkTheme()
    val panelColor = if (dark) DailySidePillDark else DailySidePillLight
    val panelTextColor = if (dark) OnDailySidePillDark else OnDailySidePillLight

    val completedMinutes = 180   // 3h of 30h = 10%
    val goalHours = 30
    val remainingHours = (goalHours - completedMinutes / 60).coerceAtLeast(0)
    val message = if (remainingHours > 0) {
        "${remainingHours}h left to reach your ${goalHours}h goal"
    } else {
        "Goal reached. Great work this month!"
    }

    Column(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(RadiusLarge))
            .background(panelColor),   // same grey as the Daily text side cards
    ) {
        // Flush with the container's top, left and right edges (no padding on those three sides):
        // all four corners of the card use the same RadiusLarge.
        MonthlyGoalCard(
            monthLabel = "NOV 2026",
            completedMinutes = completedMinutes,
            goalHours = goalHours,
            daysLeft = 8,
            modifier = Modifier.height(MonthlyCardHeight),
            shape = RoundedCornerShape(RadiusLarge),   // same radius on all four corners
        )
        // Remaining space under the card
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                message,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
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
    Column(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(RadiusLarge))
            .background(colors.surfaceContainerHigh)
            .pressScaleClickable(
                pressedScale = 0.97f,
                onClickLabel = buttonLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon, contentDescription = null,
            tint = colors.onSurface, modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onClick) {
            Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val PillWidth = 36.dp   // narrower side date pills (was 48.dp)
private val PillGap = 6.dp

/**
 * Three-slot carousel: [prev pill] [big card] [next pill].
 * Every day is ONE shape that glides between slots, so tapping a side date makes that pill
 * grow into the center card while the old center card shrinks into the opposite pill.
 */
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
    isToday: Boolean,    // only today gets the highlight color, in any slot
    rel: Int,            // -1 = left pill, 0 = center card, 1 = right pill, others = parked off-screen
    rowWidth: Dp,
    centerWidth: Dp,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Non-today days use a green tint; only today is the dark #001F1E card
    val dark = isSystemInDarkTheme()
    val sideFill = if (dark) DailySidePillDark else DailySidePillLight
    val sideText = if (dark) OnDailySidePillDark else OnDailySidePillLight
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

    // Only the card in the centre slot is the container that morphs into the Daily text screen.
    val sharedBounds = if (rel == 0) {
        Modifier.appSharedBoundsOrNone(key = "daily-card", cornerRadius = RadiusLarge)
    } else Modifier
    val pressSrc = remember { MutableInteractionSource() }

    // Motion.spatialFast: M3 Expressive spring. Slight overshoot on the shape morph
    // gives the pill→card transition a subtle "pop" without feeling rubbery.
    val xState = animateFloatAsState(targetX, Motion.spatialFast, label = "dayX")
    val wState = animateFloatAsState(targetW, Motion.spatialFast, label = "dayWidth")
    val aState = animateFloatAsState(
        targetValue = if (rel in -1..1) 1f else 0f,
        animationSpec = Motion.effectsFast,
        label = "dayAlpha",
    )
    val fillState = animateColorAsState(
        targetValue = if (isToday) DailyCenterCard else sideFill,
        animationSpec = tween(200),
        label = "dayFill",
    )

    // 0 = pill, 1 = full card
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
                shape = RoundedCornerShape((RadiusSmall.value + (RadiusLarge.value - RadiusSmall.value) * progress()).dp)
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
        // Pill content: rotated date, fades out quickly as the shape grows
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isToday) OnDailyCenterCard else sideText,
                maxLines = 1, softWrap = false,
            )
        }

        // Card content: fixed width so the text never re-wraps while the shape is growing
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
                color = if (isToday) OnDailyCenterCard.copy(alpha = 0.75f) else sideText.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(6.dp))
            // verse—reference, split so the reference never breaks across lines
            val (verse, reference) = splitVerse(item.body)
            // Card height is fixed: start at 14sp and shrink 1sp at a time until the verse fits.
            var verseSize by remember(verse) { mutableFloatStateOf(14f) }
            Text(
                verse,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = verseSize.sp,
                lineHeight = (verseSize * 1.6f).sp,   // Telugu needs extra line height
                textAlign = TextAlign.Center,
                color = if (isToday) OnDailyCenterCard else sideText,
                maxLines = 4, overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow && verseSize > 11f) verseSize -= 1f
                },
            )
            if (reference.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    reference.replace(' ', '\u00A0'),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isToday) GoalEmerald else colors.secondary,
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
    val inactive = if (isSystemInDarkTheme()) DotInactiveDark else DotInactiveLight
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val active = i == selected
            // Width morph: spring for the shape; colour stays quick but not instant.
            val width by animateDpAsState(
                targetValue = if (active) 21.dp else 8.dp,
                animationSpec = Motion.spatialFastDp,
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (active) DotActive else inactive,
                animationSpec = tween(200),
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
                            Modifier.clickable(role = Role.Button, onClickLabel = "Go to page ${i + 1}") { onDotClick(i) }
                        else Modifier
                    )
            )
        }
    }
}

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

// First entry is yesterday, second is today, then one entry per following day.
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