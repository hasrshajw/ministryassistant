package com.harsha.myapplication.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

/**
 * ============================================================================
 *  Motion tokens for Material 3 Expressive
 * ============================================================================
 *
 * Two families, per Google's M3 Expressive motion spec:
 *
 *   • Spatial  — position, size, rotation, shape, opacity of moving surfaces.
 *                Uses *springs* so movement feels physical and interruptible.
 *   • Effects  — color, elevation, non-moving alpha fades.
 *                Uses *critically damped springs* (no bounce), as in the M3 Expressive spec.
 *
 * Rule of thumb:
 *   - Something MOVES  → spatial*
 *   - Something FADES  → effects*
 *   - Never bounce a fade — effects springs are damped to 1.0 on purpose.
 *
 * All values below are the official Material 3 Expressive motion tokens
 * (same numbers as MotionScheme.expressive(), which MaterialExpressiveTheme gives
 * every Material component):
 *   spatial fast 1400 / 0.6, default 700 / 0.8, slow 300 / 0.8
 *   effects fast 3800, default 1600, slow 800  (all damping 1.0)
 * Prefer MaterialTheme.motionScheme inside components; use these for app-level
 * transitions that live outside Material components.
 */
object Motion {

    // ------------------------------------------------------------------ Springs
    // `SpringSpec<Float>` are the primary variants — every `animateFloatAsState`,
    // `animateColorAsState` target, or `HorizontalPager` spring accepts them.

    /** Snappy. Use for in-place moves that need to feel instant: button morph, pill slide. */
    val spatialFast: SpringSpec<Float> =
        spring(dampingRatio = 0.6f, stiffness = 1400f)

    /** The workhorse. Use for state swaps, panel slides, tab changes. */
    val spatialDefault: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 700f)

    /** Deliberate. Use for full-screen / hierarchical navigation. */
    val spatialSlow: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 300f)

    /** Container transform (card→detail, FAB→sheet). Slight extra bounce. */
    val containerTransform: SpringSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 700f)

    // Dp variants for size animations (`animateDpAsState` needs AnimationSpec<Dp>)
    val spatialFastDp: SpringSpec<Dp> =
        spring(dampingRatio = 0.6f, stiffness = 1400f)
    val spatialDefaultDp: SpringSpec<Dp> =
        spring(dampingRatio = 0.8f, stiffness = 700f)
    val spatialSlowDp: SpringSpec<Dp> =
        spring(dampingRatio = 0.8f, stiffness = 300f)

    // ---------------------------------------------------------------- Effects
    // FastOutLinearIn = "decelerate into the end". Reads as instant but smooth.

    /** Micro fades: ripple-adjacent alpha, colour swap on press. */
    val effectsFast: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 1f, stiffness = 3800f)

    /** Standard visibility fades: AnimatedVisibility, Crossfade. */
    val effectsDefault: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 1f, stiffness = 1600f)

    /** Long alpha ramps: overlay scrims, page-level crossfades. */
    val effectsSlow: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 1f, stiffness = 800f)

    // ------------------------------------------------------- Generic helpers
    // For animation of other types (Int, Color, Dp) where the property above
    // has the wrong generic. Usage:  Motion.spatialFast<Dp>()

    fun <T> spatialFast(): SpringSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 1400f)

    fun <T> spatialDefault(): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 700f)

    fun <T> spatialSlow(): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 300f)

    // Effects (colour, alpha) for non-Float types, e.g. animateColorAsState(..., Motion.effectsDefault())
    fun <T> effectsFast(): SpringSpec<T> =
        spring(dampingRatio = 1f, stiffness = 3800f)

    fun <T> effectsDefault(): SpringSpec<T> =
        spring(dampingRatio = 1f, stiffness = 1600f)

    fun <T> effectsSlow(): SpringSpec<T> =
        spring(dampingRatio = 1f, stiffness = 800f)
}

/**
 * M3 Expressive easing curves. Prefer [Motion] specs; these are for edge cases
 * where you need a raw `Easing` (e.g. passing to `KeyframesSpec` or custom
 * `TransformedText`).
 */
object MotionEasing {
    /** For elements entering the screen (slow at the end). */
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** For elements leaving the screen (slow at the start). */
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** For elements that both enter and leave (or morph in place). */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** For frequent, small transitions. Snappier than emphasized. */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val standardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
}

// ============================================================================
//  Press feedback modifiers
// ============================================================================
//
// Every clickable surface in an Expressive app shrinks slightly on press.
// This is the single biggest "feels alive" improvement you can make, and it's
// cheap: it only runs on the graphics layer (draw phase), never recomposes.
//
// Two ways to use it:
//
//   1. Already have a clickable?  Pass its interaction source.
//
//        val src = remember { MutableInteractionSource() }
//        Surface(
//            onClick = ...,
//            interactionSource = src,
//            modifier = Modifier.pressScale(src)
//        )
//
//   2. Standalone modifier that wires up click + scale in one call:
//
//        Modifier.pressScaleClickable { onClick() }

/**
 * Scales the composable down to [pressedScale] while [interactionSource] is in
 * a [PressInteraction.Press], then springs back on release. Uses a bouncier
 * spring on release than press for that satisfying "snap back" feel.
 *
 * @param pressedScale 1.0 = no press effect. 0.96 is the M3 default. Use 0.94
 *   for large cards, 0.98 for tiny chips.
 * @param enabled when false, no press effect plays (also matches clickable).
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
): Modifier = composed {
    // State read only inside the graphicsLayer lambda → zero recompositions per frame.
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressed = true
                is PressInteraction.Release,
                is PressInteraction.Cancel -> pressed = false
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Combined click + press-scale. Use when the composable is not already an M3
 * `Surface(onClick)` / `Card(onClick)` / `Button` (those have their own onClick).
 * Ripple indication is preserved.
 *
 * Pass [interactionSource] explicitly only if the caller needs the source for
 * something else (e.g. to disable the effect externally). Otherwise omit it.
 */
fun Modifier.pressScaleClickable(
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    // Always create the fallback so `remember` call counts stay stable.
    val fallback = remember { MutableInteractionSource() }
    val source = interactionSource ?: fallback

    pressScale(source, pressedScale, enabled)
        .clickable(
            interactionSource = source,
            indication = androidx.compose.material3.ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}

// ============================================================================
//  Screen-level transitions (Material 3 motion patterns)
// ============================================================================
//
//   • fadeThrough — sibling destinations (bottom-nav tabs). Outgoing fades out
//     fast, incoming fades + scales up from 92% after a short delay.
//   • push / pop  — hierarchical navigation (list → detail). The incoming
//     screen slides over the outgoing one, which parallaxes 25% and dims out.
//     On pop the roles reverse: the screen being closed stays ON TOP while it
//     slides away (targetContentZIndex), so the back motion is actually seen.
//   • containerFade — destinations that own a shared-bounds morph. The morph
//     does the spatial work; this only fades/scales what is around it.
object MotionTransitions {

    fun fadeThrough(): ContentTransform =
        (fadeIn(tween(210, delayMillis = 90, easing = LinearOutSlowInEasing)) +
                scaleIn(spring(dampingRatio = 0.9f, stiffness = 400f), initialScale = 0.92f)) togetherWith
                fadeOut(tween(90, easing = FastOutLinearInEasing))

    fun push(): ContentTransform =
        ((slideInHorizontally(Motion.spatialDefault<IntOffset>()) { it } + fadeIn(tween(120))) togetherWith
                (slideOutHorizontally(Motion.spatialDefault<IntOffset>()) { -it / 4 } +
                        fadeOut(tween(220, delayMillis = 120))))
            .apply { targetContentZIndex = 1f }

    fun pop(): ContentTransform =
        ((slideInHorizontally(Motion.spatialDefault<IntOffset>()) { -it / 4 } + fadeIn(tween(220))) togetherWith
                slideOutHorizontally(Motion.spatialDefault<IntOffset>()) { it })
            .apply { targetContentZIndex = -1f }

    fun containerFade(forward: Boolean): ContentTransform =
        ((fadeIn(tween(220, delayMillis = 60, easing = LinearOutSlowInEasing)) +
                scaleIn(spring(dampingRatio = 0.9f, stiffness = 400f), initialScale = 0.96f)) togetherWith
                fadeOut(tween(120)))
            .apply { targetContentZIndex = if (forward) 1f else -1f }
}

// ============================================================================
//  Entrance reveal (staggered)
// ============================================================================

/** Keys that have already played their reveal in this process (so tabs don't replay it on every visit). */
private val playedReveals = mutableSetOf<Any>()

/**
 * Fades + lifts a composable into place with a spring, optionally staggered by [index].
 * Graphics-layer only: no recomposition while it runs.
 *
 * @param playOnceKey when non-null the reveal plays once per process for that key; later
 *   compositions (e.g. coming back to a tab) start fully visible.
 */
fun Modifier.revealOnEnter(
    index: Int = 0,
    stepMillis: Int = 45,
    maxSteps: Int = 8,
    offsetY: Dp = 20.dp,
    playOnceKey: Any? = null,
): Modifier = composed {
    val skip = playOnceKey != null && playOnceKey in playedReveals
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            if (playOnceKey != null) playedReveals += playOnceKey
            delay(index.coerceIn(0, maxSteps) * stepMillis.toLong())
            progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 320f))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = (p * 1.6f).coerceIn(0f, 1f)
        translationY = (1f - p) * offsetY.toPx()
    }
}