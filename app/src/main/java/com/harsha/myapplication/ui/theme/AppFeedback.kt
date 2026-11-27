package com.harsha.myapplication.ui.theme

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.harsha.myapplication.data.FeedbackRepository

/**
 * Central place for Material 3 Expressive-style interaction feedback.
 *
 * - Sounds use the system click stream (respects the user's system sound settings
 *   and is silent when the device is muted).
 * - Haptics scale with the user preference from Customize App:
 *     Off (0)     → nothing
 *     Subtle (1)  → light selection / text-handle ticks
 *     Expressive (2) → stronger confirm / long-press style pulses
 *
 * Call the helpers from onClick / onValueChange / gesture end points.
 * Prefer [rememberAppFeedback] inside composables so the same instance is reused.
 */
class AppFeedback(
    private val view: View,
    private val haptics: HapticFeedback,
) {
    private val prefs get() = FeedbackRepository.prefs

    /** Standard button / chip / list-row tap. */
    fun click() {
        playSound()
        when (prefs.hapticLevel) {
            1 -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            2 -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            else -> Unit
        }
    }

    /** Tab / pill / segmented-control selection change. */
    fun select() {
        playSound()
        when (prefs.hapticLevel) {
            1, 2 -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            else -> Unit
        }
    }

    /** Confirming an important action (save, delete, unlock, goal reached). */
    fun confirm() {
        playSound()
        when (prefs.hapticLevel) {
            1 -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            2 -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            else -> Unit
        }
    }

    /** Soft tick for drag-reorder slots, sheet snap, etc. */
    fun tick() {
        if (prefs.hapticLevel >= 1) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /** Stronger pulse for long-press start or error / invalid state. */
    fun heavy() {
        playSound()
        if (prefs.hapticLevel >= 1) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    private fun playSound() {
        if (prefs.soundEffectsEnabled) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}

@Composable
fun rememberAppFeedback(): AppFeedback {
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    return remember(view, haptics) { AppFeedback(view, haptics) }
}
