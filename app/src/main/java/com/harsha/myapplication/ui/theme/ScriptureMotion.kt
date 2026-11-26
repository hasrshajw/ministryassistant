package com.harsha.myapplication.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * ============================================================================
 *  "A scripture was just detected" — one place that drives every effect
 * ============================================================================
 *
 * Three values rest at 0 and are only ever read in the draw / graphics-layer phase,
 * so playing the effect never recomposes the editor:
 *
 *   • [pop]  0 → 1 → 0   SPATIAL (bouncy spring)   button squash-and-stretch, book icon tilt
 *   • [halo] 0 → 1       EFFECTS (damped spring)   ring that expands out of the count badge
 *   • [glow] 1 → 0       EFFECTS (damped spring)   soft highlight behind the new verse in the text
 *
 * Movement uses the spatial springs, the fades use the critically damped effects springs,
 * exactly as the Motion.kt rule of thumb says.
 */
@Stable
class ScriptureDetectionMotion {
    val pop = Animatable(0f)
    val halo = Animatable(0f)
    val glow = Animatable(0f)

    /** Ranges (in the note's original text) that the glow is painted behind. */
    var glowRanges by mutableStateOf<List<IntRange>>(emptyList())
        private set

    private var job: Job? = null

    /** 0 → 1 over the shake; [detectionShake] turns it into a damped left-right wobble. */
    val shake = Animatable(0f)

    /** Ranges (in the note's original text) of wrong references that are shaking. */
    var invalidRanges by mutableStateOf<List<IntRange>>(emptyList())
        private set

    private var shakeJob: Job? = null

    /** "That reference does not exist": red flash + shake behind the wrong scripture(s). */
    fun triggerInvalid(scope: CoroutineScope, ranges: List<IntRange>) {
        if (ranges.isEmpty()) return
        shakeJob?.cancel()
        shakeJob = scope.launch {
            invalidRanges = ranges
            shake.snapTo(0f)
            shake.animateTo(1f, tween(durationMillis = 450, easing = LinearEasing))
            invalidRanges = emptyList()
        }
    }

    /**
     * Plays the effect. Safe to call while typing: a running effect is cancelled and every
     * value is snapped back to rest first, so nothing can freeze half-way.
     */
    fun trigger(scope: CoroutineScope, newRanges: List<IntRange>) {
        job?.cancel()
        job = scope.launch {
            glowRanges = newRanges
            pop.snapTo(0f)
            halo.snapTo(0f)
            glow.snapTo(if (newRanges.isEmpty()) 0f else 1f)

            launch { halo.animateTo(1f, Motion.effectsSlow) }
            launch { glow.animateTo(0f, Motion.effectsSlow) }
            // up with a bounce (spatialFast is 0.6 damping), then settle back to rest
            pop.animateTo(1f, Motion.spatialFast)
            pop.animateTo(0f, Motion.spatialDefault)

            glowRanges = emptyList()
        }
    }
}

/** Squash-and-stretch on the whole "Detected scriptures" button. */
fun Modifier.detectionPop(fx: ScriptureDetectionMotion, amount: Float = 0.06f): Modifier =
    graphicsLayer {
        val s = 1f + amount * fx.pop.value
        scaleX = s
        scaleY = s
    }

/** The book icon tilts open and grows a little while [ScriptureDetectionMotion.pop] runs. */
fun Modifier.detectionIconTilt(fx: ScriptureDetectionMotion): Modifier =
    graphicsLayer {
        val p = fx.pop.value
        rotationZ = -14f * p
        scaleX = 1f + 0.18f * p
        scaleY = 1f + 0.18f * p
    }

/** Ring that expands from the badge and fades out. Draw outside the badge's clip. */
fun Modifier.detectionHalo(fx: ScriptureDetectionMotion, color: Color): Modifier =
    drawBehind {
        val t = fx.halo.value
        if (t <= 0.001f || t >= 0.999f) return@drawBehind
        val base = minOf(size.width, size.height) / 2f
        val radius = base + 14.dp.toPx() * t
        drawCircle(
            color = color.copy(alpha = 0.55f * (1f - t)),
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 2.5.dp.toPx() * (1f - t) + 0.5f),
        )
    }

/**
 * Soft glow behind the newly detected verse(s) in the text.
 * [pathFor] maps [ScriptureDetectionMotion.glowRanges] to a layout [Path] (or null when the
 * text layout is not ready). Draw phase only, so the (expensive) display plan is never rebuilt.
 */
fun Modifier.detectionGlow(
    fx: ScriptureDetectionMotion,
    color: Color,
    maxAlpha: Float = 0.30f,
    pathFor: (List<IntRange>) -> Path?,
): Modifier = drawBehind {
    val g = fx.glow.value
    if (g <= 0.001f) return@drawBehind
    val path = pathFor(fx.glowRanges) ?: return@drawBehind
    drawPath(path, color.copy(alpha = maxAlpha * g))
}

/**
 * Red flash that shakes left-right behind a wrong scripture and fades out.
 * Draw phase only. [pathFor] maps [ScriptureDetectionMotion.invalidRanges] to a layout [Path].
 */
fun Modifier.detectionShake(
    fx: ScriptureDetectionMotion,
    color: Color,
    maxAlpha: Float = 0.28f,
    amplitude: androidx.compose.ui.unit.Dp = 5.dp,
    pathFor: (List<IntRange>) -> Path?,
): Modifier = drawBehind {
    val t = fx.shake.value
    if (t <= 0.001f || t >= 0.999f) return@drawBehind
    val path = pathFor(fx.invalidRanges) ?: return@drawBehind
    val fade = 1f - t
    // 4 full wobbles, getting smaller as it fades
    val dx = sin(t * 4f * 2f * PI.toFloat()) * fade * amplitude.toPx()
    translate(left = dx) {
        drawPath(path, color.copy(alpha = maxAlpha * fade.coerceAtLeast(0.15f)))
    }
}

/**
 * Count that rolls like an odometer: a higher count slides up in, a lower one slides down.
 * Movement = spatial spring, fade = effects spring.
 */
@Composable
fun <T : Comparable<T>> RollingCount(
    count: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = count,
        modifier = modifier,
        transitionSpec = {
            val up = targetState > initialState
            ((slideInVertically(Motion.spatialFast<IntOffset>()) { if (up) it else -it } +
                    fadeIn(Motion.effectsFast)) togetherWith
                    (slideOutVertically(Motion.spatialFast<IntOffset>()) { if (up) -it else it } +
                            fadeOut(Motion.effectsFast)))
                .using(SizeTransform(clip = false))
        },
        label = "rollingCount",
    ) { content(it) }
}