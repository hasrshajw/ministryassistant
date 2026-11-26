/**
 * Ministry Assistant — Setup Wizard (Material 3 Expressive)
 *
 * 3 focused steps:  Welcome  →  Notifications  →  Monthly goal
 *
 * Motion comes from ui/theme/Motion.kt (spatial / effects springs, MotionTransitions,
 * revealOnEnter, pressScale), shapes from MaterialShapes + AppShapes, type from Lexend Typography.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.setup

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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import com.harsha.myapplication.ui.theme.StudyDeepGreen
import com.harsha.myapplication.ui.theme.StudyDeepGreenDarkMode
import com.harsha.myapplication.ui.theme.StudyEmerald
import com.harsha.myapplication.ui.theme.StudyGreenTrack
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.notifications.AppNotifications
import com.harsha.myapplication.data.SetupRepository
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
private const val STEP_GOAL = 2
private const val STEP_COUNT = 3

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
    // True from the tap on "Start using the app" until the wizard hands over to Home.
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
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }

    BackHandler(enabled = currentStep > STEP_WELCOME && !finishing) { currentStep-- }
    BackHandler(enabled = finishing) { /* ignore back while the card morph plays */ }

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
                        finishing = finishing,
                        onFinishClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            finishing = true
                        },
                        // Runs after the button has morphed into the Monthly progress card.
                        onMorphDone = {
                            SetupRepository.setMonthlyGoal(context, monthlyGoal)
                            onGoalChosen(monthlyGoal)
                            onFinished()
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

                HeroBadge(Icons.Outlined.Book, Modifier.revealOnEnter(0))
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

        // ── What you'll get: one connected group, outer corners round, inner corners tight ──
        Spacer(Modifier.height(24.dp))
        val outer = 28.dp
        val inner = 6.dp
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            NotificationFeatureRow(
                icon = Icons.Default.Person,
                title = "Return visits",
                description = "Reminded on the day someone is due",
                enabled = notificationsOn,
                shape = RoundedCornerShape(outer, outer, inner, inner),
                modifier = Modifier.revealOnEnter(4)
            )
            NotificationFeatureRow(
                icon = Icons.Outlined.Book,
                title = "Daily text",
                description = "A morning prompt to read today's text",
                enabled = notificationsOn,
                shape = RoundedCornerShape(inner),
                modifier = Modifier.revealOnEnter(5)
            )
            NotificationFeatureRow(
                icon = Icons.Outlined.Timer,
                title = "Monthly goal",
                description = "Progress updates as you log hours",
                enabled = notificationsOn,
                shape = RoundedCornerShape(inner, inner, outer, outer),
                modifier = Modifier.revealOnEnter(6)
            )
        }

        // ── Master switch ──
        Spacer(Modifier.height(16.dp))
        SetupSwitchRow(
            icon = Icons.Default.Notifications,
            title = if (notificationsOn) "Notifications on" else "Turn on notifications",
            description = if (notificationsOn) "You can change this any time in Settings"
            else "Allow Ministry Assistant to send reminders",
            checked = notificationsOn,
            onClick = onToggle,
            modifier = Modifier.revealOnEnter(7)
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

// ── Monthly goal ────────────────────────────────────────────────────────────

@Composable
private fun GoalStep(
    goal: Int,
    onGoalChange: (Int) -> Unit,
    finishing: Boolean,
    onFinishClick: () -> Unit,
    onMorphDone: () -> Unit,
) {
    // morph: 0 = "Start using the app" pill, 1 = Home's Monthly progress card.
    val morph = remember { Animatable(0f) }
    val contentFade = remember { Animatable(1f) }

    LaunchedEffect(finishing) {
        if (finishing) {
            launch { contentFade.animateTo(0f, Motion.effectsDefault) }
            morph.animateTo(1f, Motion.spatialDefault)
            delay(500) // let the finished card breathe for a moment
            onMorphDone()
        }
    }

    StepScaffold(
        contentAlpha = { contentFade.value },
        cta = {
            FinishMorphCta(
                label = "Start using the app",
                goal = goal,
                morph = { morph.value },
                enabled = !finishing,
                onClick = onFinishClick,
            )
        }
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                append("Your monthly\n")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("preaching goal") }
            },
            style = MaterialTheme.typography.headlineLargeEmphasized.copy(lineHeight = 40.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.revealOnEnter(0)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "How many hours would you like to aim for each month? You can change this later.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.revealOnEnter(1)
        )

        Spacer(Modifier.height(20.dp))
        GoalRing(
            goal = goal,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(200.dp)
                .revealOnEnter(2)
        )

        Spacer(Modifier.height(12.dp))
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

        Spacer(Modifier.height(12.dp))
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

        Spacer(Modifier.height(16.dp))
        GoalPresetGrid(
            goal = goal,
            onGoalChange = onGoalChange,
            modifier = Modifier.revealOnEnter(5)
        )
        Spacer(Modifier.height(16.dp))
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

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeW = 18.dp.toPx()
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
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "hours / month",
                style = MaterialTheme.typography.labelLarge,
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
private fun GoalPresetGrid(goal: Int, onGoalChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalPresets.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (hours, label) ->
                    GoalPresetCard(
                        hours = hours,
                        label = label,
                        selected = goal == hours,
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
            .height(76.dp)
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

/** Standard step layout: scrollable content on top, big CTA pinned at the bottom. */
@Composable
private fun StepScaffold(
    ctaLabel: String = "",
    onCta: () -> Unit = {},
    contentAlpha: () -> Float = { 1f },
    cta: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer { alpha = contentAlpha() }
                .verticalScroll(rememberScrollState()),
            content = content
        )
        Box(Modifier.revealOnEnter(7)) {
            if (cta != null) cta() else BigCtaButton(ctaLabel, onCta)
        }
        Spacer(Modifier.height(24.dp))
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
                Icons.AutoMirrored.Filled.ArrowForward,
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

// ── Final CTA → Monthly progress card morph ─────────────────────────────────

private val MonthlyCardHeight = 128.dp

/** Same colors Home uses for the Monthly progress card, so the morph lands on the real thing. */
private data class FinishCardColors(val container: Color, val accent: Color, val track: Color)

@Composable
private fun rememberFinishCardColors(): FinishCardColors {
    val scheme = MaterialTheme.colorScheme
    val prefs = AppearanceRepository.prefs
    val dark = prefs.themeMode.isDark(isSystemInDarkTheme())
    return remember(scheme, prefs.palette, dark) {
        if (prefs.palette.isBrand) {
            FinishCardColors(
                container = if (dark) StudyDeepGreenDarkMode else StudyDeepGreen,
                accent = StudyEmerald,
                track = StudyGreenTrack,
            )
        } else {
            val container = if (dark) scheme.surfaceContainerHigh else scheme.onPrimaryContainer
            val accent = if (dark) scheme.primary else lerpColor(scheme.primary, Color.White, 0.35f)
            FinishCardColors(container, accent, lerpColor(container, accent, 0.25f))
        }
    }
}

/**
 * The wizard's last button. Tapping it morphs the pill (spring) into the Monthly progress card:
 * height 88 → 128dp, corners 44 → 28dp, primary → deep card color, label cross-fades into the
 * ring + "0 / N h" content. [morph] is 0f..1f (may overshoot slightly because it is a spring).
 */
@Composable
private fun FinishMorphCta(
    label: String,
    goal: Int,
    morph: () -> Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val card = rememberFinishCardColors()
    val scheme = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val cookieRotation by animateFloatAsState(
        if (pressed) 45f else 0f, Motion.spatialFast, label = "ctaCookie"
    )

    val m = morph()
    val t = m.coerceIn(0f, 1f)
    val height = lerp(88f, MonthlyCardHeight.value, m).dp
    val corner = lerp(44f, 28f, m).coerceAtLeast(0f).dp
    val container = lerpColor(scheme.primary, card.container, t)
    val content = lerpColor(scheme.onPrimary, Color.White, t)
    val buttonAlpha = (1f - t * 2.4f).coerceIn(0f, 1f)
    val cardAlpha = ((t - 0.4f) * 2.4f).coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = src,
        shape = RoundedCornerShape(if (pressed && enabled) 24.dp else corner),
        color = container,
        contentColor = content,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(src, 0.97f, enabled),
    ) {
        Box(Modifier.fillMaxSize()) {
            // 1) the button face
            Row(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = buttonAlpha }
                    .padding(start = 32.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
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
                            .background(scheme.primaryContainer)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward, null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 2) the Monthly progress card face (fixed 128dp so it never squishes while growing)
            MonthlyCardFace(
                goal = goal,
                colors = card,
                modifier = Modifier
                    .align(Alignment.Center)
                    .requiredHeight(MonthlyCardHeight)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardAlpha
                        translationY = (1f - cardAlpha) * 14.dp.toPx()
                    }
            )
        }
    }
}

/** Mirrors Home's Monthly progress card at 0% so the hand-over is seamless. */
@Composable
private fun MonthlyCardFace(goal: Int, colors: FinishCardColors, modifier: Modifier = Modifier) {
    val month = remember { SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date()).uppercase() }
    val perDay = remember(goal) {
        val cal = Calendar.getInstance()
        val daysLeft = (cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH) + 1)
            .coerceAtLeast(1)
        String.format(Locale.US, "%.1fh / day left", goal.toFloat() / daysLeft)
    }

    Box(modifier) {
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
                Icons.Default.ArrowOutward, null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidthPx = 8.5.dp.toPx()
                    val diameter = size.minDimension - strokeWidthPx
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    drawArc(
                        color = colors.track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                    drawCircle(colors.accent, strokeWidthPx / 2f, Offset(size.width / 2f, topLeft.y))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "0%",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        color = colors.accent
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    month,
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    letterSpacing = 1.8.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "0 / ${goal}h",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    letterSpacing = (-0.8).sp,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(50), color = Color(0x20FFFFFF)) {
                    Text(
                        perDay,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}