package com.harsha.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Single source of truth for UI sound + haptic preferences.
 * Same pattern as AppearanceRepository / HomeLayoutRepository:
 * call [init] once from MainActivity.onCreate so the first frame respects saved values.
 *
 * Levels:
 *   0 = Off
 *   1 = Subtle  (light ticks, selection)
 *   2 = Expressive (stronger confirms, matching M3 Expressive feel)
 */
data class FeedbackPrefs(
    val soundEffectsEnabled: Boolean = true,
    val hapticLevel: Int = 2, // 0 Off, 1 Subtle, 2 Expressive
)

object FeedbackRepository {
    private const val FILE = "feedback_prefs"
    private const val K_SOUND = "sound_effects"
    private const val K_HAPTIC = "haptic_level"

    private var store: SharedPreferences? = null

    var prefs by mutableStateOf(FeedbackPrefs())
        private set

    fun init(context: Context) {
        if (store != null) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = sp
        prefs = FeedbackPrefs(
            soundEffectsEnabled = sp.getBoolean(K_SOUND, true),
            hapticLevel = sp.getInt(K_HAPTIC, 2).coerceIn(0, 2),
        )
    }

    fun setSoundEffectsEnabled(enabled: Boolean) = update { it.copy(soundEffectsEnabled = enabled) }

    fun setHapticLevel(level: Int) = update { it.copy(hapticLevel = level.coerceIn(0, 2)) }

    private fun update(transform: (FeedbackPrefs) -> FeedbackPrefs) {
        val next = transform(prefs)
        prefs = next
        store?.edit()
            ?.putBoolean(K_SOUND, next.soundEffectsEnabled)
            ?.putInt(K_HAPTIC, next.hapticLevel)
            ?.apply()
    }
}
