@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.harsha.myapplication.*

internal val ScriptureBlue = Color(0xFF1A73E8)
internal val ScriptureBlueDark = Color(0xFF8AB4F8)
/** Used when the reference is detected but does not exist in the active Bible. */
internal val ScriptureRed = Color(0xFFD93025)
internal val ScriptureRedDark = Color(0xFFF28B82)
internal val HighlightYellow = Color(0xFFFFF176)

/**
 * Highlight palette (JW Library style). All light pastels so dark text stays readable on them
 * in both themes. A stored arg of 0 means the original yellow, so old notes look unchanged.
 */
internal val HighlightColors: List<Pair<Color, String>> = listOf(
    HighlightYellow to "Yellow",
    Color(0xFFA5D6A7) to "Green",
    Color(0xFF90CAF9) to "Blue",
    Color(0xFFF8BBD0) to "Pink",
    Color(0xFFFFCC80) to "Orange",
    Color(0xFFCE93D8) to "Purple"
)

internal fun highlightColorOf(arg: Int): Color = if (arg == 0) HighlightYellow else Color(arg)

/** Soft tint behind inserted scripture quotes (works on light and dark surfaces). */
internal val QuoteTint = Color(0x22808080)
