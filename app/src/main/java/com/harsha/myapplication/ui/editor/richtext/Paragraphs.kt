@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.richtext

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.*

// ---------------------------------------------------------------------------------------------
// Per-paragraph alignment
//
// Every line (paragraph) of the note has its own alignment (0 start, 1 centre, 2 end, 3 justify),
// kept in a list with one entry per line. When text is edited the list follows: a line that is
// split or created by Enter inherits the alignment of the line it came from, joined lines keep the
// first line's alignment. Saved as FmtType.Align spans (one per line) so no schema change is needed.
// ---------------------------------------------------------------------------------------------

internal fun lineCount(text: String): Int {
    var n = 1
    for (c in text) if (c == '\n') n++
    return n
}

internal fun countNewlines(s: String, from: Int, to: Int): Int {
    var n = 0
    for (i in from until to) if (s[i] == '\n') n++
    return n
}

/** Index of the line that contains [offset]. */
internal fun lineIndexAt(text: String, offset: Int): Int =
    countNewlines(text, 0, offset.coerceIn(0, text.length))

/** Makes the list exactly one entry per line of [text]. */
internal fun fitAligns(a: List<Int>, text: String): List<Int> {
    val n = lineCount(text)
    return when {
        a.size == n -> a
        a.size > n -> a.subList(0, n).toList()
        else -> a + List(n - a.size) { a.lastOrNull() ?: 0 }
    }
}

/**
 * Keeps the per-line alignment list in step with a text change [old] -> [new].
 * [caret] (caret after the change) pins down WHERE a single insertion / deletion happened, so that
 * pressing Enter on a line always gives the new line that line's alignment.
 */
internal fun shiftLineAligns(aligns: List<Int>, old: String, new: String, caret: Int = -1): List<Int> {
    if (old == new) return aligns
    val delta = new.length - old.length
    var p = 0
    var oldEnd = 0
    var newEnd = 0
    val grew = caret >= 0 && delta > 0 && caret - delta >= 0 && caret <= new.length &&
            new.removeRange(caret - delta, caret) == old
    val shrank = caret >= 0 && delta < 0 && caret - delta <= old.length &&
            old.removeRange(caret, caret - delta) == new
    if (grew) {
        p = caret - delta; oldEnd = p; newEnd = caret
    } else if (shrank) {
        p = caret; oldEnd = caret - delta; newEnd = caret
    } else {
        val m = minOf(old.length, new.length)
        while (p < m && old[p] == new[p]) p++
        var suffix = 0
        while (suffix < m - p && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        oldEnd = old.length - suffix
        newEnd = new.length - suffix
    }
    val k1 = countNewlines(old, p, oldEnd)
    val k2 = countNewlines(new, p, newEnd)
    if (k1 == k2) return fitAligns(aligns, new)
    val first = countNewlines(old, 0, p)
    var keep = aligns.getOrElse(first) { 0 }
    // Removing an EMPTY line (Backspace on it) must not restyle the line that follows it.
    val firstLineEmpty = (p == 0 || old[p - 1] == '\n') && p < old.length && old[p] == '\n'
    if (k1 > k2 && firstLineEmpty) keep = aligns.getOrElse(first + k1) { keep }
    val out = ArrayList<Int>(aligns.size + (k2 - k1).coerceAtLeast(0))
    for (i in 0 until minOf(first, aligns.size)) out.add(aligns[i])
    repeat(k2 + 1) { out.add(keep) }
    for (i in first + k1 + 1 until aligns.size) out.add(aligns[i])
    return fitAligns(out, new)
}

/** Reads per-line alignment from the saved spans; notes saved before this feature had ONE alignment for all lines. */
internal fun decodeAligns(json: String, text: String, legacy: Int): List<Int> {
    val n = lineCount(text)
    val fallback = legacy.coerceIn(0, 3)
    val saved = decodeSpans(json, text.length).filter { it.type == FmtType.Align }
    if (saved.isEmpty()) return List(n) { fallback }
    val out = ArrayList<Int>(n)
    var ls = 0
    for (i in 0 until n) {
        val le = lineEndOf(text, ls)
        val pos = if (le < text.length) le else if (le > ls) le - 1 else -1
        out.add(if (pos >= 0) (saved.argAt(FmtType.Align, pos) ?: 0).coerceIn(0, 3) else fallback)
        ls = le + 1
    }
    return out
}

/** One Align span per line (only written when some line is not left-aligned). */
internal fun alignSpansOf(text: String, aligns: List<Int>): List<FormatSpan> {
    if (aligns.all { it == 0 }) return emptyList()
    val out = ArrayList<FormatSpan>()
    var ls = 0
    var i = 0
    while (true) {
        val le = lineEndOf(text, ls)
        val end = if (le < text.length) le + 1 else le
        if (end > ls) out.add(FormatSpan(ls, end, FmtType.Align, aligns.getOrElse(i) { 0 }))
        if (le >= text.length) break
        ls = le + 1
        i++
    }
    return out
}

/** Paragraph layout of one line: alignment + hanging indent (wrapped list lines sit under the text, not the marker). */
internal class ParaSpec(
    val start: Int,
    val end: Int,
    val align: TextAlign,
    val rest: TextUnit,
    /** Biggest font size (sp) on the line(s); the line height is derived from it. */
    val size: Float = 16f
)

internal fun buildParas(
    text: String,
    aligns: List<Int>,
    spans: List<FormatSpan>,
    hang: (prefix: String, sizeIdx: Int) -> TextUnit
): List<ParaSpec> {
    val out = ArrayList<ParaSpec>()
    var ls = 0
    var i = 0
    var prevHasImage = false
    while (true) {
        val le = lineEndOf(text, ls)
        val end = if (le < text.length) le + 1 else le
        val m = if (le > ls) markerOf(text.substring(ls, le)) else null
        val rest = if (m == null) 0.sp else hang(text.substring(ls, ls + m.length), spans.argAt(FmtType.Size, ls) ?: 0)
        val align = alignFromInt(aligns.getOrElse(i) { 0 })
        val size = spans.filter { it.type == FmtType.Size && it.start < end && it.end > ls }
            .maxOfOrNull { TextSize.entries.getOrNull(it.arg)?.size?.value ?: 16f } ?: 16f
        val hasImage = text.indexOf(OBJ, ls).let { it in ls until maxOf(le, ls + 1) && it < text.length }
        // Consecutive lines with the SAME look are ONE paragraph. A paragraph that ends in a line break
        // draws an extra empty line, so one paragraph per text line doubled the spacing between lines.
        val prev = out.lastOrNull()
        if (prev != null && !hasImage && !prevHasImage && prev.align == align && prev.size == size &&
            kotlin.math.abs(prev.rest.value - rest.value) < 1.5f
        ) {
            out[out.lastIndex] = ParaSpec(prev.start, end, align, if (rest.value > prev.rest.value) rest else prev.rest, size)
        } else {
            out.add(ParaSpec(ls, end, align, rest, size))
        }
        prevHasImage = hasImage
        if (le >= text.length) break
        ls = le + 1
        i++
    }
    return out
}

/** Maps paragraph ranges onto the displayed text (which has on-screen-only insertions). Ranges stay contiguous. */
internal fun List<ParaSpec>.toShown(plan: DisplayPlan): List<ParaSpec> {
    val out = ArrayList<ParaSpec>(size)
    var prevEnd = 0
    forEachIndexed { i, p ->
        val e = if (i == lastIndex) plan.shown.length else plan.spanEnd(p.end)
        if (e > prevEnd) {
            out.add(ParaSpec(prevEnd, e, p.align, p.rest, p.size))
            prevEnd = e
        }
    }
    return out
}

internal fun countWords(text: String): Int {
    var n = 0
    for (line in text.split('\n')) {
        val m = markerOf(line)
        val body = (if (m != null) line.substring(m.length) else line).replace('\uFFFC', ' ')
        n += body.split(' ', '\t').count { it.isNotBlank() }
    }
    return n
}
