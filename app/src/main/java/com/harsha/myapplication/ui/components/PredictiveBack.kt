package com.harsha.myapplication.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * ============================================================================
 *  Predictive Back — reusable state + modifiers
 * ============================================================================
 *
 * Android 14+ lets the system show a preview of where the back gesture will
 * take the user. To make that preview feel *in-app* (instead of just a system
 * animation), you register a [PredictiveBackHandler] and drive your own
 * composables with the gesture progress (0f..1f).
 *
 * This file gives you three things:
 *
 *  1. [PredictiveBackState]  — a stable holder for the live gesture data:
 *                               progress, swipe edge, and whether a gesture
 *                               is currently in progress.
 *  2. [rememberPredictiveBackState] — registers the handler and returns state.
 *  3. [Modifier.predictiveBackTransform] — applies scale + alpha + translation
 *                               based on the state, so the whole overlay
 *                               shrinks/fades as the user swipes back.
 *
 * Usage inside a full-screen overlay (e.g. BookBrowser):
 *
 *     val backState = rememberPredictiveBackState(
 *         enabled = level != Level.Books,
 *         onBack = { goBack() }
 *     )
 *     Box(
 *         Modifier
 *             .fillMaxSize()
 *             .predictiveBackTransform(backState)
 *     ) { ... }
 *
 * That's it. When the user swipes back, the overlay scales down and fades out
 * in real time. If they cancel, it springs back. If they complete, onBack runs.
 *
 * NOTE ON DIALOGS:
 *   Compose `Dialog` (Material 3 1.3+) supports predictive back automatically
 *   via `DialogProperties(enablePredictiveBackBehavior = true)`, which scales
 *   the dialog card around its center. Use that for simple AlertDialogs.
 *   Use this file's utilities when you need a *custom* transform (full-screen
 *   overlays, side-sheets, or elements beyond what the built-in dialog does).
 */

// ============================================================================
//  State
// ============================================================================

/**
 * Live state of an in-progress predictive back gesture.
 *
 * All fields are Compose state — read them inside `Modifier.graphicsLayer` or
 * `animate*AsState` lambdas so only the draw phase invalidates, not the
 * composition.
 *
 * @property progress 0f = gesture just started, 1f = fully committed.
 * @property swipeEdge which edge the user is swiping from:
 *   [BackEventCompat.EDGE_LEFT] (0) or [BackEventCompat.EDGE_RIGHT] (1).
 *   Use this to choose the direction of any translation/rotation.
 * @property isInProgress true while the finger is down and the gesture is
 *   being tracked; false after commit or cancel.
 * @property touchX / touchY last known touch position in window coordinates.
 *   Useful if you want a light "lens" effect following the finger.
 */
@Stable
class PredictiveBackState internal constructor() {

    /** 0f..1f — how far the user has dragged. */
    var progress by mutableFloatStateOf(0f)
        internal set

    /** [BackEventCompat.EDGE_LEFT] or [BackEventCompat.EDGE_RIGHT]. */
    var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
        internal set

    /** True while a gesture is being tracked. */
    var isInProgress by mutableStateOf(false)
        internal set

    /** Last touch X in window coordinates. */
    var touchX by mutableFloatStateOf(0f)
        internal set

    /** Last touch Y in window coordinates. */
    var touchY by mutableFloatStateOf(0f)
        internal set

    /**
     * Horizontal sign for translations: -1 when swiping from the left edge
     * (content should move right), +1 when swiping from the right edge
     * (content should move left). Multiply by your max offset.
     */
    val edgeSign: Float
        get() = if (swipeEdge == BackEventCompat.EDGE_LEFT) -1f else 1f

    internal fun reset() {
        progress = 0f
        isInProgress = false
    }
}

// ============================================================================
//  Registration
// ============================================================================

/**
 * Registers a [PredictiveBackHandler] and returns a [PredictiveBackState]
 * that stays in sync with the gesture.
 *
 * @param enabled pass false to disable the handler (e.g. on the root screen
 *   of a hierarchy, where the system should handle back itself).
 * @param onBack invoked once the gesture completes (finger lifted past the
 *   threshold). Do your actual navigation/dismissal here.
 */
@Composable
fun rememberPredictiveBackState(
    enabled: Boolean = true,
    onBack: () -> Unit,
): PredictiveBackState {
    val state = remember { PredictiveBackState() }

    PredictiveBackHandler(enabled = enabled) { progressFlow ->
        state.isInProgress = true
        try {
            progressFlow.collect { event ->
                state.progress = event.progress
                state.swipeEdge = event.swipeEdge
                state.touchX = event.touchX
                state.touchY = event.touchY
            }
            // Flow completed without cancellation → gesture committed.
            state.reset()
            onBack()
        } catch (e: CancellationException) {
            // User lifted before the threshold → snap back.
            state.reset()
            throw e
        }
    }

    return state
}

// ============================================================================
//  Modifiers
// ============================================================================

/**
 * Applies a Material-3-style "peek" transform driven by [state]:
 *
 *  • Scale shrinks from 1.0 → [minScale] as progress goes 0 → 1.
 *  • Alpha fades from 1.0 → [minAlpha].
 *  • Content optionally translates toward the swipe edge by [maxTranslation].
 *
 * The transform is applied on the graphics layer only — zero recompositions
 * per frame, so it stays at 60/120 fps even over a `LazyColumn`.
 *
 * @param minScale how small the overlay gets at full commit. 0.92 is a good
 *   default for full-screen pages; use 0.96 for cards.
 * @param minAlpha how transparent the overlay gets. 0.6 keeps it readable
 *   while clearly showing "this is about to go away".
 * @param maxTranslation horizontal offset (in dp) at full commit, applied in
 *   the direction of the swipe edge. Set to 0.dp to disable translation.
 */
fun Modifier.predictiveBackTransform(
    state: PredictiveBackState,
    minScale: Float = 0.92f,
    minAlpha: Float = 0.6f,
    maxTranslation: androidx.compose.ui.unit.Dp = 0.dp,
): Modifier = this.graphicsLayer {
    val p = state.progress
    scaleX = 1f - (1f - minScale) * p
    scaleY = 1f - (1f - minScale) * p
    alpha = 1f - (1f - minAlpha) * p
    if (maxTranslation != 0.dp) {
        translationX = state.edgeSign * maxTranslation.toPx() * p
    }
}

/**
 * Convenience: scale-only transform. Matches the look of Material 3's built-in
 * dialog predictive back (scale from the center, no fade) for cases where you
 * want the page to feel "held back" rather than "fading".
 */
fun Modifier.predictiveBackScale(
    state: PredictiveBackState,
    minScale: Float = 0.88f,
): Modifier = this.graphicsLayer {
    val s = 1f - (1f - minScale) * state.progress
    scaleX = s
    scaleY = s
}

/**
 * Convenience: fade-only transform for overlays that should dissolve rather
 * than shrink (e.g. scrims).
 */
fun Modifier.predictiveBackFade(
    state: PredictiveBackState,
    minAlpha: Float = 0f,
): Modifier = this.alpha(1f - (1f - minAlpha) * state.progress)

// ============================================================================
//  Optional: animated commit / cancel
// ============================================================================

/**
 * If you want the overlay to *animate* its commit/cancel rather than snap,
 * pair [rememberPredictiveBackState] with this helper. It gives you a smoothed
 * progress value that follows the finger while dragging, then either finishes
 * to 1f (commit) or springs back to 0f (cancel).
 *
 * Usage:
 *
 *     val back = rememberPredictiveBackState(enabled, onBack = { close() })
 *     val animated = rememberAnimatedBackProgress(back)
 *     Box(Modifier.graphicsLayer {
 *         val s = 1f - 0.1f * animated.value
 *         scaleX = s; scaleY = s
 *         alpha = 1f - animated.value
 *     })
 */
@Composable
fun rememberAnimatedBackProgress(
    state: PredictiveBackState,
): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val anim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isInProgress) {
        if (!state.isInProgress) {
            // Gesture ended: either committed (progress==1) or cancelled (progress==0).
            val target = state.progress
            val spec = if (target > 0.5f) {
                // Commit → finish quickly toward 1.
                tween<Float>(durationMillis = 150)
            } else {
                // Cancel → spring back with a gentle overshoot.
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            }
            scope.launch { anim.animateTo(target, spec) }
        }
    }

    // While dragging, snap the animatable to the live finger progress so there
    // is no lag between finger and visual.
    LaunchedEffect(state.progress, state.isInProgress) {
        if (state.isInProgress) {
            anim.snapTo(state.progress)
        }
    }

    return anim
}

// ============================================================================
//  Android-14-style "peek" for whole screens
// ============================================================================

/**
 * The system-style predictive-back preview for an in-app screen: while the finger drags, the
 * screen shrinks to [minScale], slides a little toward the finger, gains rounded corners and a
 * soft shadow. Takes lambdas (not values) so only the draw phase re-reads them — no recomposition.
 *
 * @param progress 0f..1f gesture progress
 * @param edgeSign +1f when the swipe starts at the left edge (content drifts right), -1f at the right
 */
fun Modifier.predictiveBackPeek(
    progress: () -> Float,
    edgeSign: () -> Float,
    minScale: Float = 0.9f,
    maxTranslation: androidx.compose.ui.unit.Dp = 20.dp,
    maxCorner: androidx.compose.ui.unit.Dp = 28.dp,
): Modifier = this.graphicsLayer {
    val p = progress().coerceIn(0f, 1f)
    val s = 1f - (1f - minScale) * p
    scaleX = s
    scaleY = s
    translationX = edgeSign() * maxTranslation.toPx() * p
    if (p > 0f) {
        shape = RoundedCornerShape(maxCorner * p)
        clip = true
        shadowElevation = 16.dp.toPx() * p
    }
}