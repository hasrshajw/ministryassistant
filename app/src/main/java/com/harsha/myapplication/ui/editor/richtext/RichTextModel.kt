@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.richtext

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.*

// ---------------------------------------------------------------------------------------------
// Rich-text model (spans are kept next to the plain string and shifted as the text is edited)
// ---------------------------------------------------------------------------------------------

/**
 * [Quote] marks scripture text inserted from the verse sheet. Its arg is the reference's key hash,
 * so the editor can tell which scripture a quote belongs to (needed for "Remove scripture").
 * Kept last so the stored type names / ordering of the older types never change.
 */
internal enum class FmtType { Bold, Italic, Underline, Strike, Highlight, Size, Color, Image, Align, Quote }

internal enum class TextSize(val label: String, val size: TextUnit) {
    Normal("Normal", 16.sp),
    SubHeading("Sub-Heading", 20.sp),
    Heading("Heading", 26.sp)
}

internal data class FormatSpan(val start: Int, val end: Int, val type: FmtType, val arg: Int = 0)

internal val InkBlack = Color(0xFF1A1A1A)
/** Palette with names (screen readers announce them). The first entry is the theme's own text colour. */
internal val FontColors: List<Pair<Color, String>> = listOf(
    InkBlack to "Default",
    Color(0xFF4A4A4A) to "Dark grey",
    Color(0xFF9A9A9A) to "Grey",
    Color(0xFFD84315) to "Red",
    Color(0xFFE08A3C) to "Orange",
    Color(0xFFE8C45A) to "Yellow",
    Color(0xFF6AAE6A) to "Green",
    Color(0xFF4A90E2) to "Blue",
    Color(0xFF8E5BD0) to "Purple"
)

internal fun List<FormatSpan>.subtract(type: FmtType, s: Int, e: Int): List<FormatSpan> =
    flatMap { sp ->
        if (sp.type != type || sp.end <= s || sp.start >= e) {
            listOf(sp)
        } else buildList {
            if (sp.start < s) add(sp.copy(end = s))
            if (sp.end > e) add(sp.copy(start = e))
        }
    }

internal fun List<FormatSpan>.covers(type: FmtType, s: Int, e: Int): Boolean {
    if (e <= s) return false
    var pos = s
    for (sp in filter { it.type == type && it.end > s && it.start < e }.sortedBy { it.start }) {
        if (sp.start > pos) return false
        pos = maxOf(pos, sp.end)
        if (pos >= e) return true
    }
    return false
}

internal fun List<FormatSpan>.argAt(type: FmtType, pos: Int): Int? =
    firstOrNull { it.type == type && it.start <= pos && it.end > pos }?.arg

/** Shifts spans for a text edit; also returns the range of newly inserted text (if any). */
internal fun shiftSpans(
    spans: List<FormatSpan>,
    old: String,
    new: String
): Pair<List<FormatSpan>, IntRange?> {
    val m = minOf(old.length, new.length)
    var p = 0
    while (p < m && old[p] == new[p]) p++
    var s = 0
    while (s < m - p && old[old.length - 1 - s] == new[new.length - 1 - s]) s++
    val oldEnd = old.length - s
    val newEnd = new.length - s
    val delta = new.length - old.length
    val out = spans.mapNotNull { sp ->
        when {
            sp.end <= p -> sp
            sp.start >= oldEnd -> sp.copy(start = sp.start + delta, end = sp.end + delta)
            else -> {
                val ns = if (sp.start < p) sp.start else p
                val ne = if (sp.end > oldEnd) sp.end + delta else p
                if (ns < ne) sp.copy(start = ns, end = ne) else null
            }
        }
    }
    return out to (if (newEnd > p) p until newEnd else null)
}
