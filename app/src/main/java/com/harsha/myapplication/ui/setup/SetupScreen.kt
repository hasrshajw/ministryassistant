/**
 * Ministry Assistant — Setup Wizard (Material 3 Expressive)
 *
 * 5 focused steps:  Welcome  →  Notifications  →  Bible download  →  Monthly goal  →  Color palette
 *
 * Motion comes from ui/theme/Motion.kt (spatial / effects springs, MotionTransitions,
 * revealOnEnter, pressScale), shapes from MaterialShapes + AppShapes, type from Lexend Typography.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.setup

import com.harsha.myapplication.ui.components.openAppNotificationSettings
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.harsha.myapplication.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.harsha.myapplication.ui.components.ExpressiveBackButton
import com.harsha.myapplication.ui.theme.AppearanceRepository
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.MotionTransitions
import com.harsha.myapplication.ui.theme.PaletteColors
import com.harsha.myapplication.ui.theme.ThemePalette
import com.harsha.myapplication.ui.theme.paletteColorScheme
import com.harsha.myapplication.ui.theme.StudyDeepGreen
import com.harsha.myapplication.ui.theme.StudyDeepGreenDarkMode
import com.harsha.myapplication.ui.theme.StudyEmerald
import com.harsha.myapplication.ui.theme.StudyGreenTrack
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.notifications.AppNotifications
import com.harsha.myapplication.data.SetupRepository
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.BibleDownloads
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ── Persistence ─────────────────────────────────────────────────────────────

// ── Constants ───────────────────────────────────────────────────────────────

private const val STEP_WELCOME = 0
private const val STEP_NOTIFICATIONS = 1
private const val STEP_BIBLE = 2
private const val STEP_GOAL = 3
private const val STEP_PALETTE = 4
private const val STEP_COUNT = 5

private const val GOAL_MIN = 1
private const val GOAL_MAX = 300
private val GoalPresets = listOf(10 to "Light", 15 to "Easy", 30 to "Steady", 50 to "Focused", 70 to "Driven", 100 to "All in")

// ── Wavy ring path ──────────────────────────────────────────────────────────

private fun buildWavyRingPath(
    cx: Float,
    cy: Float,
    radius: Float,
    amplitude: Float,
    ripples: Int,
    phase: Float = 0f,
    startAngleDeg: Float = 0f,
    sweepAngleDeg: Float = 360f,
    isClosed: Boolean = true,
): Path {
    val path = Path()
    val steps = (sweepAngleDeg * 1.5f).toInt().coerceAtLeast(30)
    val degToRad = (Math.PI / 180.0).toFloat()
    val startRad = startAngleDeg * degToRad
    val sweepRad = sweepAngleDeg * degToRad
    for (i in 0..steps) {
        val fraction = i.toFloat() / steps
        val theta = startRad + fraction * sweepRad
        val r = radius + amplitude * kotlin.math.sin(ripples * theta + phase)
        val x = cx + r * kotlin.math.cos(theta)
        val y = cy + r * kotlin.math.sin(theta)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (isClosed) path.close()
    return path
}

// ── Wizard ──────────────────────────────────────────────────────────────────

@Composable
fun SetupWizardScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    /** Called with the chosen monthly hours right before [onFinished]. Also saved to [SetupRepository]. */
    onGoalChosen: (hours: Int) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    var currentStep by rememberSaveable { mutableIntStateOf(STEP_WELCOME) }
    var monthlyGoal by rememberSaveable { mutableIntStateOf(SetupRepository.monthlyGoalHours) }
    // True from the tap on "Start using the app" until the wizard hands over to Home (blocks double taps).
    var finishing by remember { mutableStateOf(false) }

    // areNotificationsEnabled() covers BOTH the runtime permission and the system switch.
    var notificationsOn by remember { mutableStateOf(AppNotifications.areEnabled(context)) }
    // Set once the user has tapped "Don't allow"; from then on the switch opens system settings.
    var notificationsBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { AppNotifications.ensureChannel(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsOn = AppNotifications.areEnabled(context)
                if (notificationsOn) notificationsBlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsOn = AppNotifications.areEnabled(context)
        notificationsBlocked = !granted
    }

    fun openNotificationSettings() {
        openAppNotificationSettings(context)
    }

    BackHandler(enabled = currentStep > STEP_WELCOME && !finishing) { currentStep-- }
    BackHandler(enabled = finishing) { /* ignore back while handing over to Home */ }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative living rings, only on the welcome step.
        AnimatedVisibility(
            visible = currentStep == STEP_WELCOME,
            enter = fadeIn(Motion.effectsSlow),
            exit = fadeOut(Motion.effectsDefault),
        ) {
            WelcomeRings(Modifier.fillMaxSize())
        }

        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            SetupTopBar(step = currentStep, hidden = finishing, onBack = { currentStep-- })

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) MotionTransitions.push() else MotionTransitions.pop()
                },
                label = "SetupStep",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) { step ->
                when (step) {
                    STEP_WELCOME -> WelcomeStep(
                        onGetStarted = { currentStep = STEP_NOTIFICATIONS },
                    )

                    STEP_NOTIFICATIONS -> NotificationsStep(
                        notificationsOn = notificationsOn,
                        notificationsBlocked = notificationsBlocked,
                        onToggle = {
                            // Granted -> the user wants it off; blocked / pre-Android 13 -> only the
                            // system settings can change it. Otherwise show the permission dialog.
                            if (notificationsOn || notificationsBlocked ||
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                            ) {
                                openNotificationSettings()
                            } else {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onContinue = { currentStep = STEP_BIBLE },
                    )

                    STEP_BIBLE -> BibleStep(
                        onContinue = { currentStep = STEP_GOAL },
                    )

                    STEP_GOAL -> GoalStep(
                        goal = monthlyGoal,
                        onGoalChange = { v ->
                            val next = v.coerceIn(GOAL_MIN, GOAL_MAX)
                            if (next != monthlyGoal) {
                                monthlyGoal = next
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onContinue = { currentStep = STEP_PALETTE },
                    )

                    STEP_PALETTE -> PaletteStep(
                        finishing = finishing,
                        onFinish = {
                            if (!finishing) {
                                finishing = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                SetupRepository.setMonthlyGoal(context, monthlyGoal)
                                onGoalChosen(monthlyGoal)
                                // MainActivity cross-fades the wizard into Home (short tween, no spring).
                                onFinished()
                            }
                        },
                    )
                }
            }
        }
    }
}

// ── Top bar + step indicator ────────────────────────────────────────────────

@Composable
private fun SetupTopBar(step: Int, hidden: Boolean, onBack: () -> Unit) {
    val barAlpha by animateFloatAsState(
        if (hidden) 0f else 1f, Motion.effectsDefault, label = "topBarAlpha"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 16.dp)
            .graphicsLayer { alpha = barAlpha }
    ) {
        AnimatedVisibility(
            visible = step > STEP_WELCOME,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = scaleIn(Motion.spatialFast) + fadeIn(Motion.effectsDefault),
            exit = scaleOut(Motion.spatialFast) + fadeOut(Motion.effectsFast),
        ) {
            ExpressiveBackButton(onClick = onBack)
        }
        StepIndicator(step, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun StepIndicator(step: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(STEP_COUNT) { i ->
            val width by animateDpAsState(
                if (i == step) 36.dp else 10.dp,
                Motion.spatialDefaultDp,
                label = "dotWidth$i"
            )
            val color by animateColorAsState(
                when {
                    i == step -> primary
                    i < step -> primary.copy(alpha = 0.5f)
                    else -> inactive
                },
                Motion.effectsDefault(),
                label = "dotColor$i"
            )
            Box(
                Modifier
                    .height(10.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// ── Welcome ─────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeRings(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "rings")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "ringPhase"
    )
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Canvas(
        modifier.pointerInput(Unit) {
            detectTapGestures {
                scope.launch {
                    bounce.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
                    bounce.animateTo(
                        0f,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
                    )
                }
            }
        }
    ) {
        val baseWidth = 390f
        val scale = if (size.width > size.height) size.height / baseWidth else size.width / baseWidth
        val b = bounce.value
        val bounceOffset = 45f * scale * b

        val topRing = buildWavyRingPath(
            cx = size.width - (65f * scale),
            cy = (106f * scale) - bounceOffset,
            radius = 160f * scale,
            amplitude = (8f + 3f * b) * scale,
            ripples = 12,
            phase = phase,
        )
        val bottomRing = buildWavyRingPath(
            cx = 139f * scale,
            cy = size.height - (215f * scale) + bounceOffset,
            radius = 276f * scale,
            amplitude = (12f + 4f * b) * scale,
            ripples = 14,
            phase = -phase,
            startAngleDeg = 180f,
            sweepAngleDeg = 180f,
            isClosed = false,
        )
        val stroke = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
        drawPath(topRing, ringColor, style = stroke)
        drawPath(bottomRing, ringColor, style = stroke)
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = screenHeight),
                verticalArrangement = Arrangement.Bottom
            ) {
                Spacer(Modifier.height(24.dp))

                HeroBadge(painterResource(R.drawable.logo), Modifier.revealOnEnter(0))
                Spacer(Modifier.height(24.dp))

                Text(
                    "Hi there",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.revealOnEnter(1)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    buildAnnotatedString {
                        append("Welcome to\n")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Ministry Assistant")
                        }
                    },
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 44.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.revealOnEnter(2)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Track field service, study notes, return visits and the daily text — all offline.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.revealOnEnter(3)
                )

                Spacer(Modifier.height(32.dp))
                BigCtaButton("Get started", onGetStarted, Modifier.revealOnEnter(4))
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Notifications ───────────────────────────────────────────────────────────

@Composable
private fun NotificationsStep(
    notificationsOn: Boolean,
    notificationsBlocked: Boolean,
    onToggle: () -> Unit,
    onContinue: () -> Unit,
) {
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(notificationsOn) {
        if (notificationsOn) {
            listOf(18f, -16f, 12f, -8f, 0f).forEach {
                wiggle.animateTo(it, spring(dampingRatio = 0.5f, stiffness = 700f))
            }
        }
    }

    StepScaffold(ctaLabel = "Continue", onCta = onContinue) {
        Spacer(Modifier.height(8.dp))
        HeroBadge(
            icon = Icons.Default.Notifications,
            iconRotation = { wiggle.value },
            modifier = Modifier.revealOnEnter(0)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Stay on top of",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.revealOnEnter(1)
        )
        Text(
            "your visits",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.revealOnEnter(2)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "A gentle nudge at the right moment. Nothing else.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.revealOnEnter(3)
        )

        // ── Master switch ──
        Spacer(Modifier.height(24.dp))
        SetupSwitchRow(
            icon = Icons.Default.Notifications,
            title = if (notificationsOn) "Notifications on" else "Turn on notifications",
            description = if (notificationsOn) "You can change this any time in Settings"
            else "Allow Ministry Assistant to send reminders",
            checked = notificationsOn,
            onClick = onToggle,
            modifier = Modifier.revealOnEnter(4)
        )
        AnimatedVisibility(
            visible = notificationsBlocked && !notificationsOn,
            enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
            exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialDefault<IntSize>()),
        ) {
            Text(
                "Notifications are blocked for this app. Tap the switch to open system settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp)
            )
        }
        // ── What you'll get: one connected group (below the switch), outer corners round, inner corners tight ──
        Spacer(Modifier.height(16.dp))
        val outer = 28.dp
        val inner = 6.dp
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            NotificationFeatureRow(
                icon = Icons.Default.Person,
                title = "Return visits",
                description = "Reminded on the day someone is due",
                enabled = notificationsOn,
                shape = RoundedCornerShape(outer, outer, inner, inner),
                modifier = Modifier.revealOnEnter(5)
            )
            NotificationFeatureRow(
                icon = Icons.Outlined.Book,
                title = "Daily text",
                description = "A morning prompt to read today's text",
                enabled = notificationsOn,
                shape = RoundedCornerShape(inner),
                modifier = Modifier.revealOnEnter(6)
            )
            NotificationFeatureRow(
                icon = Icons.Outlined.Timer,
                title = "Monthly goal",
                description = "Progress updates as you log hours",
                enabled = notificationsOn,
                shape = RoundedCornerShape(inner, inner, outer, outer),
                modifier = Modifier.revealOnEnter(7)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** One line of the "what you'll get" group. The icon well and check fill in when notifications are on. */
@Composable
private fun NotificationFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val well by animateColorAsState(
        if (enabled) scheme.primaryContainer else scheme.surfaceContainerHighest,
        Motion.effectsDefault(), label = "featureWell"
    )
    val tint by animateColorAsState(
        if (enabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        Motion.effectsDefault(), label = "featureTint"
    )
    Surface(
        shape = shape,
        color = scheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = well,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = scheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = enabled,
                enter = scaleIn(Motion.spatialFast) + fadeIn(Motion.effectsFast),
                exit = scaleOut(Motion.spatialFast) + fadeOut(Motion.effectsFast),
            ) {
                Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ── Bible download ──────────────────────────────────────────────────────────

/**
 * Optional step: start downloading a Bible. The work lives in [BibleDownloads] (process scoped, with a
 * foreground service), so it keeps running while the user moves on through the wizard.
 */
@Composable
private fun BibleStep(onContinue: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val dataVersion = BibleData.version
    val imported = remember(dataVersion) { BibleData.importedBibles }
    val catalog = BibleDownloads.catalog

    val anyStarted = catalog.any { src ->
        BibleDownloads.states[src.id] != null || imported.any { it.name == src.language }
    }

    StepScaffold(
        ctaLabel = if (anyStarted) "Continue" else "Skip for now",
        onCta = onContinue
    ) {
        Spacer(Modifier.height(8.dp))
        HeroBadge(icon = Icons.Default.Download, modifier = Modifier.revealOnEnter(0))
        Spacer(Modifier.height(24.dp))
        Text(
            "Download your",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.revealOnEnter(1)
        )
        Text(
            "Bible",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.revealOnEnter(2)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Read scriptures offline, right inside the app. You can add more Bibles later in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.revealOnEnter(3)
        )

        Spacer(Modifier.height(24.dp))
        val outer = 28.dp
        val inner = 6.dp
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            catalog.forEachIndexed { index, source ->
                val shape = when {
                    catalog.size == 1 -> RoundedCornerShape(outer)
                    index == 0 -> RoundedCornerShape(outer, outer, inner, inner)
                    index == catalog.lastIndex -> RoundedCornerShape(inner, inner, outer, outer)
                    else -> RoundedCornerShape(inner)
                }
                BibleSourceRow(
                    source = source,
                    imported = imported.any { it.name == source.language },
                    state = BibleDownloads.states[source.id],
                    shape = shape,
                    onStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        BibleDownloads.start(context, source)
                    },
                    modifier = Modifier.revealOnEnter(4 + index)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Downloads keep going in the background, so you can carry on with setup.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .revealOnEnter(6)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BibleSourceRow(
    source: BibleDownloads.Source,
    imported: Boolean,
    state: BibleDownloads.State?,
    shape: RoundedCornerShape,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val done = imported && state == null
    val well by animateColorAsState(
        if (done) scheme.primaryContainer else scheme.surfaceContainerHighest,
        Motion.effectsDefault(), label = "bibleWell"
    )
    val tint by animateColorAsState(
        if (done) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        Motion.effectsDefault(), label = "bibleTint"
    )

    val subtitle = when {
        state is BibleDownloads.State.Downloading ->
            if (state.progress >= 0f) "Downloading… ${(state.progress * 100).toInt()}%" else "Downloading…"
        state is BibleDownloads.State.Importing -> "Importing…"
        state is BibleDownloads.State.Failed -> state.message
        done -> "Ready to read"
        else -> source.subtitle
    }

    Surface(shape = shape, color = scheme.surfaceContainerLow, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = well,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Book, null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(source.language)
                        source.nativeName?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = scheme.onSurface,
                    maxLines = 1
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state is BibleDownloads.State.Failed) scheme.error else scheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            when {
                state is BibleDownloads.State.Downloading && state.progress >= 0f ->
                    CircularWavyProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.size(28.dp)
                    )
                state is BibleDownloads.State.Downloading || state is BibleDownloads.State.Importing ->
                    LoadingIndicator(modifier = Modifier.size(28.dp))
                state is BibleDownloads.State.Failed ->
                    FilledTonalButton(onClick = onStart, contentPadding = PaddingValues(horizontal = 14.dp)) {
                        Text("Retry")
                    }
                done -> Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(24.dp))
                else -> FilledTonalButton(onClick = onStart, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Get")
                }
            }
        }
    }
}

// ── Monthly goal ────────────────────────────────────────────────────────────

/**
 * Fits any screen WITHOUT scrolling: the ring is the flexible element. Its size is whatever is left
 * after the fixed rows (title, hint, stepper, presets), clamped to 96..230dp. On very short screens
 * the hint text is dropped and the preset cards get slightly shorter.
 */
@Composable
private fun GoalStep(
    goal: Int,
    onGoalChange: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    StepScaffold(ctaLabel = "Continue", onCta = onContinue, scrollable = false) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val h = maxHeight
            val showHint = h >= 520.dp
            val presetH = if (h >= 480.dp) 68.dp else 56.dp
            // title 80 + gaps/rows 132 + presets (2 cards + gap) + optional hint 48
            val fixed = 212.dp + presetH * 2 + (if (showHint) 48.dp else 0.dp)
            val ringSize = (h - fixed).coerceIn(96.dp, 230.dp)

            Column(Modifier.fillMaxSize()) {
                Text(
                    buildAnnotatedString {
                        append("Your monthly\n")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("preaching goal") }
                    },
                    style = MaterialTheme.typography.headlineLargeEmphasized.copy(lineHeight = 40.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.revealOnEnter(0)
                )
                if (showHint) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hours you'd like to aim for each month. You can change it later.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.revealOnEnter(1)
                    )
                }

                Spacer(Modifier.height(12.dp))
                GoalRing(
                    goal = goal,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(ringSize)
                        .revealOnEnter(2)
                )

                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = perDayLabel(goal),
                    transitionSpec = { fadeIn(Motion.effectsDefault) togetherWith fadeOut(Motion.effectsFast) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .revealOnEnter(3),
                    label = "perDay"
                ) { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .revealOnEnter(4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepperButton(Icons.Default.Remove, "Decrease goal") { onGoalChange(goal - 1) }
                    Slider(
                        value = goal.toFloat().coerceIn(5f, 150f),
                        onValueChange = { onGoalChange(it.roundToInt()) },
                        valueRange = 5f..150f,
                        modifier = Modifier.weight(1f)
                    )
                    StepperButton(Icons.Default.Add, "Increase goal") { onGoalChange(goal + 1) }
                }

                Spacer(Modifier.height(12.dp))
                GoalPresetGrid(
                    goal = goal,
                    onGoalChange = onGoalChange,
                    cardHeight = presetH,
                    modifier = Modifier.revealOnEnter(5)
                )
            }
        }
    }
}

private fun perDayLabel(goal: Int): String {
    val minutes = goal * 2 // goal hours / 30 days, in minutes
    val h = minutes / 60
    val m = minutes % 60
    val text = when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
    return "About $text a day"
}

@Composable
private fun GoalRing(goal: Int, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = (goal / 100f).coerceIn(0.03f, 1f),
        animationSpec = Motion.spatialSlow,
        label = "goalFraction"
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val arc = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val ringSize = maxWidth
        val numberStyle = when {
            ringSize >= 180.dp -> MaterialTheme.typography.displayLarge
            ringSize >= 140.dp -> MaterialTheme.typography.displayMedium
            else -> MaterialTheme.typography.displaySmall
        }
        val strokeDp = (ringSize.value * 0.09f).coerceIn(10f, 20f).dp
        Canvas(Modifier.fillMaxSize()) {
            val strokeW = strokeDp.toPx()
            val inset = strokeW / 2
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(strokeW))
            drawArc(
                arc, -90f, 360f * fraction.coerceIn(0f, 1f), false,
                Offset(inset, inset), arcSize,
                style = Stroke(strokeW, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = goal,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(Motion.spatialDefault<IntOffset>()) { if (up) it else -it } +
                        fadeIn(Motion.effectsDefault)) togetherWith
                        (slideOutVertically(Motion.spatialDefault<IntOffset>()) { if (up) -it else it } +
                            fadeOut(Motion.effectsFast))
                },
                label = "goalNumber"
            ) { value ->
                Text(
                    "$value",
                    style = numberStyle.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "hours / month",
                style = if (ringSize >= 140.dp) MaterialTheme.typography.labelLarge
                else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick = onClick,
        interactionSource = src,
        modifier = Modifier
            .size(48.dp)
            .pressScale(src, 0.9f)
    ) {
        Icon(icon, contentDescription = description)
    }
}

/** Quick picks: a 3 x 2 grid of cards. The selected card grows rounder and fills with the container color. */
@Composable
private fun GoalPresetGrid(
    goal: Int,
    onGoalChange: (Int) -> Unit,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalPresets.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (hours, label) ->
                    GoalPresetCard(
                        hours = hours,
                        label = label,
                        selected = goal == hours,
                        height = cardHeight,
                        modifier = Modifier.weight(1f)
                    ) { onGoalChange(hours) }
                }
            }
        }
    }
}

@Composable
private fun GoalPresetCard(
    hours: Int,
    label: String,
    selected: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val radius by animateDpAsState(if (selected) 30.dp else 16.dp, Motion.spatialDefaultDp, label = "presetRadius")
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        Motion.effectsDefault(), label = "presetContainer"
    )
    val content by animateColorAsState(
        if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        Motion.effectsDefault(), label = "presetContent"
    )
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = content,
        modifier = modifier
            .height(height)
            .pressScale(src, 0.94f)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$hours", style = MaterialTheme.typography.titleLargeEmphasized)
                Text(
                    "h",
                    Modifier.padding(start = 2.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Shared UI pieces ────────────────────────────────────────────────────────

/**
 * Standard step layout: content on top, big CTA pinned at the bottom.
 * [scrollable] = false for steps that must always fit on screen (they size themselves from their
 * available height instead).
 */
@Composable
private fun StepScaffold(
    ctaLabel: String,
    onCta: () -> Unit,
    scrollable: Boolean = true,
    ctaEnabled: Boolean = true,
    ctaIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier),
            content = content
        )
        Box(Modifier.revealOnEnter(7)) {
            BigCtaButton(ctaLabel, onCta, enabled = ctaEnabled, icon = ctaIcon)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Slowly rotating cookie badge with a logo painter on top. */
@Composable
private fun HeroBadge(
    painter: Painter,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "hero")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "heroRotation"
    )
    val container = MaterialTheme.colorScheme.primaryContainer
    Box(modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(container)
        )
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.medium)
        )
    }
}

/** Slowly rotating cookie badge with an icon on top. */
@Composable
private fun HeroBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconRotation: () -> Float = { 0f },
) {
    val infinite = rememberInfiniteTransition(label = "hero")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "heroRotation"
    )
    val container = MaterialTheme.colorScheme.primaryContainer
    Box(modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(container)
        )
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer { rotationZ = iconRotation() }
        )
    }
}

@Composable
private fun BigCtaButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val cookieRotation by animateFloatAsState(
        if (pressed) 45f else 0f, Motion.spatialFast, label = "ctaCookie"
    )
    val arrowShift by animateDpAsState(
        if (pressed) 4.dp else 0.dp, Motion.spatialFastDp, label = "ctaArrow"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = src,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .pressScale(src, 0.97f, enabled),
        shapes = ButtonDefaults.shapes(
            shape = RoundedCornerShape(44.dp),
            pressedShape = RoundedCornerShape(24.dp)
        ),
        contentPadding = PaddingValues(start = 32.dp, end = 14.dp)
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLargeEmphasized
        )
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = cookieRotation }
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
            )
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { translationX = arrowShift.toPx() }
            )
        }
    }
}

@Composable
private fun SetupSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        Motion.effectsDefault(), label = "rowContainer"
    )
    val content by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface,
        Motion.effectsDefault(), label = "rowContent"
    )
    val radius by animateDpAsState(if (checked) 32.dp else 20.dp, Motion.spatialDefaultDp, label = "rowRadius")
    val src = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = content,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(src, 0.97f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.75f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { onClick() },
                thumbContent = {
                    Icon(
                        if (checked) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}

// ── Color palette (final step) ──────────────────────────────────────────────

/**
 * Last step. Picking a palette calls [AppearanceRepository.setPalette], and because the whole app
 * (this wizard included) is themed from that state, everything recolors live behind the picker.
 * Nothing extra to save: the choice is already persisted, so "Start using the app" just hands over.
 *
 * Layout: live preview, two big "featured" tiles (Default + Dynamic), then a row of color swatches.
 */
@Composable
private fun PaletteStep(
    finishing: Boolean,
    onFinish: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val prefs = AppearanceRepository.prefs
    val dark = prefs.themeMode.isDark(isSystemInDarkTheme())
    val selected = prefs.palette

    // Featured: the app's own look + the system (wallpaper) look. Dynamic needs Android 12+.
    val featured = remember {
        PaletteColors.filter { it.isBrand || (it.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) }
            .sortedBy { if (it.isBrand) 0 else 1 }
    }
    val colors = remember { PaletteColors.filter { !it.isBrand && !it.isDynamic } }
    // Start at the END of the color row, then sweep back to the start (left → right). It shows the
    // user at a glance that there are more colors to swipe through. A touch cancels the sweep.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = colors.lastIndex.coerceAtLeast(0)
    )
    LaunchedEffect(Unit) {
        delay(650)
        listState.animateScrollToItem(0)
    }

    fun choose(palette: ThemePalette) {
        if (palette == selected) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        AppearanceRepository.setPalette(palette)
        if (palette.isDynamic) AppearanceRepository.setDynamicColor(true)
    }

    StepScaffold(
        ctaLabel = "Start using the app",
        onCta = onFinish,
        scrollable = false,
        ctaEnabled = !finishing,
        ctaIcon = Icons.Default.Check,
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val h = maxHeight
            val showHint = h >= 540.dp
            val swatch = if (h >= 500.dp) 64.dp else 56.dp

            Column(Modifier.fillMaxSize()) {
                Text(
                    buildAnnotatedString {
                        append("Choose your\n")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("color palette") }
                    },
                    style = MaterialTheme.typography.headlineLargeEmphasized.copy(lineHeight = 40.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.revealOnEnter(0)
                )
                if (showHint) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The whole app recolors instantly. Change it any time in Appearance.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.revealOnEnter(1)
                    )
                }

                Spacer(Modifier.height(16.dp))
                // The preview takes whatever height is left, so the step never needs to scroll.
                PalettePreview(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .heightIn(max = 190.dp)
                        .revealOnEnter(2)
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .revealOnEnter(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    featured.forEach { palette ->
                        FeaturedPaletteTile(
                            palette = palette,
                            dark = dark,
                            selected = palette == selected,
                            onClick = { choose(palette) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                        .revealOnEnter(4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "More colors",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selected in colors) {
                        Text(
                            "  ·  ${selected.name}",
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fullBleed(24.dp)
                        .revealOnEnter(5),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(colors, key = { it.name }) { palette ->
                        PaletteSwatch(
                            palette = palette,
                            dark = dark,
                            selected = palette == selected,
                            size = swatch,
                            onClick = { choose(palette) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Lets a child draw past the parent's horizontal padding (edge-to-edge scrolling rows). */
private fun Modifier.fullBleed(padding: Dp): Modifier = layout { measurable, constraints ->
    val extra = padding.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

@Composable
private fun rememberSwatchScheme(palette: ThemePalette, dark: Boolean): androidx.compose.material3.ColorScheme {
    val context = LocalContext.current
    return remember(palette, dark) {
        if (palette.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            paletteColorScheme(palette, dark)
        }
    }
}

/**
 * Big tile for the two "system level" choices.
 *  • Default (Study)  – tri-tone cookie, the hand-tuned green look.
 *  • Dynamic          – sweep-gradient cookie built from the wallpaper colors.
 */
@Composable
private fun FeaturedPaletteTile(
    palette: ThemePalette,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatchScheme = rememberSwatchScheme(palette, dark)
    val scheme = MaterialTheme.colorScheme
    val radius by animateDpAsState(if (selected) 32.dp else 20.dp, Motion.spatialDefaultDp, label = "tileRadius")
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        Motion.effectsDefault(), label = "tileContainer"
    )
    val content by animateColorAsState(
        if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        Motion.effectsDefault(), label = "tileContent"
    )
    val src = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(radius),
        color = container,
        contentColor = content,
        border = if (selected) BorderStroke(2.dp, scheme.primary) else null,
        modifier = modifier
            .height(68.dp)
            .pressScale(src, 0.96f)
            .semantics { contentDescription = "${palette.name} palette" }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(
                Modifier
                    .size(44.dp)
                    .clip(MaterialShapes.Cookie6Sided.toShape())
            ) {
                if (palette.isDynamic) {
                    drawRect(
                        Brush.sweepGradient(
                            listOf(
                                swatchScheme.primary, swatchScheme.tertiary,
                                swatchScheme.secondary, swatchScheme.primary
                            )
                        )
                    )
                } else {
                    drawRect(swatchScheme.primary, Offset.Zero, Size(this.size.width, this.size.height / 2))
                    drawRect(
                        swatchScheme.secondary,
                        Offset(0f, this.size.height / 2),
                        Size(this.size.width / 2, this.size.height / 2)
                    )
                    drawRect(
                        swatchScheme.tertiary,
                        Offset(this.size.width / 2, this.size.height / 2),
                        Size(this.size.width / 2, this.size.height / 2)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    palette.name,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    maxLines = 1
                )
                Text(
                    if (palette.isDynamic) "Your wallpaper" else "Default",
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(Motion.spatialFast) + fadeIn(Motion.effectsFast),
                exit = scaleOut(Motion.spatialFast) + fadeOut(Motion.effectsFast),
            ) {
                Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** A plain color: a tri-tone dot (primary / secondary / tertiary) in a card that rounds when selected. */
@Composable
private fun PaletteSwatch(
    palette: ThemePalette,
    dark: Boolean,
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val swatchScheme = rememberSwatchScheme(palette, dark)
    val scheme = MaterialTheme.colorScheme
    val radius by animateDpAsState(if (selected) size / 2 else 22.dp, Motion.spatialDefaultDp, label = "swatchRadius")
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, Motion.spatialFast, label = "swatchScale")
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        Motion.effectsDefault(), label = "swatchContainer"
    )
    val src = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(radius),
        color = container,
        border = if (selected) BorderStroke(2.5.dp, scheme.primary) else null,
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pressScale(src, 0.92f)
            .semantics { contentDescription = "${palette.name} palette" }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(size * 0.58f)
                    .clip(CircleShape)
            ) {
                drawRect(swatchScheme.primary, Offset.Zero, Size(this.size.width, this.size.height / 2))
                drawRect(
                    swatchScheme.secondary,
                    Offset(0f, this.size.height / 2),
                    Size(this.size.width / 2, this.size.height / 2)
                )
                drawRect(
                    swatchScheme.tertiary,
                    Offset(this.size.width / 2, this.size.height / 2),
                    Size(this.size.width / 2, this.size.height / 2)
                )
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(Motion.spatialFast) + fadeIn(Motion.effectsFast),
                exit = scaleOut(Motion.spatialFast) + fadeOut(Motion.effectsFast),
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(scheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun animatedRole(color: Color, label: String): Color =
    animateColorAsState(color, Motion.effectsDefault(), label = label).value

/**
 * A miniature app screen (top bar, hero card, accent tiles, navigation bar) painted with the live
 * color scheme. Every color eases to its new value when the palette changes.
 */
@Composable
private fun PalettePreview(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val primary = animatedRole(cs.primary, "pvPrimary")
    val onPrimary = animatedRole(cs.onPrimary, "pvOnPrimary")
    val primaryContainer = animatedRole(cs.primaryContainer, "pvPrimaryContainer")
    val onPrimaryContainer = animatedRole(cs.onPrimaryContainer, "pvOnPrimaryContainer")
    val secondary = animatedRole(cs.secondary, "pvSecondary")
    val secondaryContainer = animatedRole(cs.secondaryContainer, "pvSecondaryContainer")
    val tertiaryContainer = animatedRole(cs.tertiaryContainer, "pvTertiaryContainer")
    val onSurface = animatedRole(cs.onSurface, "pvOnSurface")
    val navBar = animatedRole(cs.surfaceContainerHigh, "pvNav")
    val card = animatedRole(cs.surfaceContainerLow, "pvCard")

    BoxWithConstraints(modifier) {
        val showNav = maxHeight >= 150.dp
        Surface(
            Modifier.fillMaxSize(),
            shape = RoundedCornerShape(32.dp),
            color = card,
            border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // top bar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(onSurface.copy(alpha = 0.14f))
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(primary)
                    )
                }

                // hero card + accent tiles
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        Modifier
                            .weight(1.7f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(primaryContainer)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .clip(CircleShape)
                                    .background(onPrimaryContainer.copy(alpha = 0.55f))
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(onPrimaryContainer.copy(alpha = 0.25f))
                            )
                        }
                        Box(
                            Modifier
                                .width(64.dp)
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(secondaryContainer)
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(tertiaryContainer)
                        )
                    }
                }

                // navigation bar
                if (showNav) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(navBar),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .width(48.dp)
                                .height(24.dp)
                                .clip(CircleShape)
                                .background(secondary)
                        )
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(onSurface.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
        }
    }
}
