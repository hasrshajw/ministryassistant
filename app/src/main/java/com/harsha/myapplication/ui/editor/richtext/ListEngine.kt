@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.richtext

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.offset
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.notes.INDENT_STEP
import com.harsha.myapplication.ui.notes.MAX_INDENT

internal enum class ListKind { Bullet, Number, Letter, Check }

internal const val BOX_OFF = '\u2610' // ☐
internal const val BOX_ON = '\u2611'  // ☑

// ---------------------------------------------------------------------------------------------
// List engine: markers, nesting, auto-numbering
// ---------------------------------------------------------------------------------------------

/** Optional leading indent (spaces), then "• ", "1. ", "a. " or a checklist box "☐ " / "☑ ". */
internal val listRegex = Regex("""^( {0,8})(• |(\d{1,3})\. |([a-z])\. |([\u2610\u2611]) )""")
internal val numberParenShortcut = Regex("""^(\d{1,3})\) $""")
internal val letterParenShortcut = Regex("""^([a-z])\) $""")

/** A parsed list marker at the start of a line. [length] covers indent + marker. */
internal class ListMarker(
    val indent: Int,
    val kind: ListKind,
    val number: Int,
    val letter: Char,
    val length: Int,
    val checked: Boolean = false
)

internal fun markerOf(line: String): ListMarker? {
    val m = listRegex.find(line) ?: return null
    val indent = m.groupValues[1].length
    val kind = when {
        m.groups[3] != null -> ListKind.Number
        m.groups[4] != null -> ListKind.Letter
        m.groups[5] != null -> ListKind.Check
        else -> ListKind.Bullet
    }
    return ListMarker(
        indent = indent,
        kind = kind,
        number = m.groups[3]?.value?.toIntOrNull() ?: 0,
        letter = m.groups[4]?.value?.firstOrNull() ?: 'a',
        length = indent + m.groupValues[2].length,
        checked = m.groups[5]?.value?.firstOrNull() == BOX_ON
    )
}

internal fun prefixFor(kind: ListKind, idx: Int): String = when (kind) {
    ListKind.Bullet -> "• "
    ListKind.Number -> "$idx. "
    ListKind.Letter -> "${'a' + (idx - 1).coerceIn(0, 25)}. "
    ListKind.Check -> "$BOX_OFF "
}

/** Marker text for the item that follows [m] (same indent). */
internal fun nextMarkerText(m: ListMarker): String {
    val pad = " ".repeat(m.indent)
    return pad + when (m.kind) {
        ListKind.Bullet -> "• "
        ListKind.Number -> "${m.number + 1}. "
        ListKind.Letter -> "${minOf(m.letter + 1, 'z')}. "
        ListKind.Check -> "$BOX_OFF "
    }
}

/** Markdown-style typing shortcuts: type the trigger at line start, then a space. */
internal fun shortcutPrefix(typed: String): String? = when (typed) {
    "- ", "* " -> "• "
    "[] ", "[ ] " -> "$BOX_OFF "
    "[x] ", "[X] " -> "$BOX_ON "
    else -> numberParenShortcut.find(typed)?.let { "${it.groupValues[1]}. " }
        ?: letterParenShortcut.find(typed)?.let { "${it.groupValues[1]}. " }
}

internal fun lineStartOf(text: String, offset: Int): Int = text.lastIndexOf('\n', offset - 1) + 1

internal fun lineEndOf(text: String, from: Int): Int =
    text.indexOf('\n', from).let { if (it == -1) text.length else it }

internal data class TextEdit(val start: Int, val length: Int, val replacement: String)

/** Applies [edits] (positions refer to [text]) and keeps the style spans in sync. */
internal fun applyEdits(
    text: String,
    spans: List<FormatSpan>,
    edits: List<TextEdit>
): Pair<String, List<FormatSpan>> {
    var t = text
    var r = spans
    for (e in edits.sortedByDescending { it.start }) {
        val next = t.replaceRange(e.start, e.start + e.length, e.replacement)
        r = shiftSpans(r, t, next).first
        t = next
    }
    return t to r
}

/** Maps a caret offset from before [edits] to after them. */
internal fun mapOffset(offset: Int, edits: List<TextEdit>): Int {
    var o = offset
    for (e in edits) {
        when {
            offset >= e.start + e.length -> o += e.replacement.length - e.length
            offset > e.start -> {
                val inside = offset - e.start
                o += minOf(e.replacement.length, inside) - inside
            }
        }
    }
    return o.coerceAtLeast(0)
}

/**
 * Edits that make the contiguous list block around [anchor] correctly numbered / lettered,
 * level by level (nested lists restart at 1 / a).
 */
internal fun renumberEdits(text: String, anchor: Int): List<TextEdit> {
    fun isListLine(ls: Int) = markerOf(text.substring(ls, lineEndOf(text, ls))) != null

    var start = lineStartOf(text, anchor.coerceIn(0, text.length))
    if (!isListLine(start)) {
        // Cursor may sit just under a list (e.g. after deleting a line) - try the line above.
        if (start == 0) return emptyList()
        val prev = lineStartOf(text, start - 1)
        if (!isListLine(prev)) return emptyList()
        start = prev
    }
    while (start > 0) {
        val prev = lineStartOf(text, start - 1)
        if (isListLine(prev)) start = prev else break
    }

    val levels = MAX_INDENT / INDENT_STEP + 1
    val counters = IntArray(levels)
    val kinds = arrayOfNulls<ListKind>(levels)
    val edits = mutableListOf<TextEdit>()

    var ls = start
    while (true) {
        val le = lineEndOf(text, ls)
        val m = markerOf(text.substring(ls, le)) ?: break
        val level = (m.indent / INDENT_STEP).coerceIn(0, levels - 1)
        for (i in level + 1 until levels) {
            counters[i] = 0
            kinds[i] = null
        }
        if (kinds[level] != m.kind) {
            counters[level] = 0
            kinds[level] = m.kind
        }
        counters[level]++
        // A checklist item keeps its own ticked / unticked state when the block is renumbered.
        val expected = " ".repeat(m.indent) + if (m.kind == ListKind.Check) {
            "${if (m.checked) BOX_ON else BOX_OFF} "
        } else prefixFor(m.kind, counters[level])
        if (text.substring(ls, ls + m.length) != expected) {
            edits.add(TextEdit(ls, m.length, expected))
        }
        if (le >= text.length) break
        ls = le + 1
    }
    return edits
}

internal fun renumber(
    text: String,
    spans: List<FormatSpan>,
    anchor: Int
): Triple<String, List<FormatSpan>, List<TextEdit>> {
    val edits = renumberEdits(text, anchor)
    if (edits.isEmpty()) return Triple(text, spans, edits)
    val (t, r) = applyEdits(text, spans, edits)
    return Triple(t, r, edits)
}
