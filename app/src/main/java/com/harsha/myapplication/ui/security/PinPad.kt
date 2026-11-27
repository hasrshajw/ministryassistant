@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.data.SecurityRepository
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import kotlinx.coroutines.delay

private val KeySize = 76.dp
private val KeyGap = 16.dp

/**
 * Custom PIN pad. Nothing here opens the phone keyboard: every key is a Surface drawn by the app.
 *
 * Expressive touches: keys spring in one after another, morph from a circle to a rounded square while
 * pressed (and shrink slightly), the check key only appears once the PIN is long enough, and every
 * press gives a light haptic tick.
 */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KeyGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var index = 0
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                row.forEach { ch ->
                    DigitKey(ch.toString(), index++, enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigit(ch)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            ActionKey(
                index = index++, enabled = enabled, filled = false, description = "Delete",
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onBackspace() },
            ) { tint -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = tint) }
            DigitKey("0", index++, enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDigit('0')
            }
            ActionKey(
                index = index, enabled = enabled && submitEnabled, filled = true, description = "Confirm PIN",
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSubmit() },
            ) { tint -> Icon(Icons.Filled.Check, contentDescription = null, tint = tint) }
        }
    }
}

/** Springs a key in: scale 0.6 -> 1 and fade, staggered by [index]. */
@Composable
private fun rememberEntrance(index: Int): Float {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 28L); shown = true }
    val v by animateFloatAsState(if (shown) 1f else 0f, Motion.spatialDefault, label = "keyEntrance")
    return v
}

@Composable
private fun DigitKey(label: String, index: Int, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 24.dp else KeySize / 2, Motion.spatialFastDp, label = "keyCorner")
    val container by animateColorAsState(
        if (pressed) colors.secondaryContainer else colors.surfaceContainerHigh,
        Motion.effectsFast(), label = "keyColor",
    )
    val entrance = rememberEntrance(index)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = if (pressed) colors.onSecondaryContainer else colors.onSurface,
        interactionSource = source,
        modifier = Modifier
            .size(KeySize)
            .graphicsLayer { alpha = entrance; scaleX = 0.6f + 0.4f * entrance; scaleY = 0.6f + 0.4f * entrance }
            .pressScale(source, pressedScale = 0.92f)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun ActionKey(
    index: Int,
    enabled: Boolean,
    filled: Boolean,
    description: String,
    onClick: () -> Unit,
    icon: @Composable (tint: androidx.compose.ui.graphics.Color) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 24.dp else KeySize / 2, Motion.spatialFastDp, label = "actCorner")
    val container by animateColorAsState(
        when {
            filled && enabled -> colors.primary
            filled -> colors.surfaceContainerLow
            else -> colors.surfaceContainerLow
        },
        Motion.effectsDefault(), label = "actColor",
    )
    val tint by animateColorAsState(
        when {
            filled && enabled -> colors.onPrimary
            filled -> colors.onSurface.copy(alpha = 0.38f)
            else -> colors.onSurfaceVariant
        },
        Motion.effectsDefault(), label = "actTint",
    )
    val entrance = rememberEntrance(index)
    // The check key grows slightly when it becomes usable, so it is clear the PIN is long enough.
    val ready by animateFloatAsState(if (filled && enabled) 1.08f else 1f, Motion.spatialFast, label = "ready")
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        color = container,
        interactionSource = source,
        modifier = Modifier
            .size(KeySize)
            .graphicsLayer {
                alpha = entrance
                scaleX = (0.6f + 0.4f * entrance) * ready
                scaleY = (0.6f + 0.4f * entrance) * ready
            }
            .pressScale(source, pressedScale = 0.92f)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) { icon(tint) }
    }
}

/**
 * The row of dots above the pad. Each dot springs up when its digit is typed; the row shows at least
 * [SecurityRepository.MIN_LENGTH] dots and grows to the PIN length. [shakeKey] changes -> the row shakes.
 */
@Composable
fun PinDots(length: Int, shakeKey: Int, isError: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val slots = maxOf(SecurityRepository.MIN_LENGTH, length)
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        // Damped left-right wobble, then settle.
        for (target in listOf(-18f, 16f, -12f, 9f, -5f, 0f)) {
            shake.animateTo(target, spring(dampingRatio = 0.5f, stiffness = 1800f))
        }
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .semantics { contentDescription = "$length digits entered" },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(slots) { i ->
            val filled = i < length
            val size by animateDpAsState(if (filled) 20.dp else 14.dp, Motion.spatialFastDp, label = "dotSize")
            val color by animateColorAsState(
                when {
                    isError -> colors.error
                    filled -> colors.primary
                    else -> colors.outlineVariant
                },
                Motion.effectsDefault(), label = "dotColor",
            )
            Spacer(Modifier.size(size).background(color, CircleShape))
        }
    }
}
