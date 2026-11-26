@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.richtext

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.editor.ScriptureRed

// ---------------------------------------------------------------------------------------------
// Inline images
//
// An image is ONE invisible placeholder character ("object replacement char") on its own line of
// the note text, plus a FormatSpan of type Image covering that character. The span's `arg` packs
//   bits 10+ : index into the note's image list     bit 9 : rounded corners
//   bits 2-8 : width in percent of the text width   bits 0-1 : 0 left / 1 centre / 2 right
//   bit 30   : text flows beside the image (needs left / right alignment)
// The editor reserves vertical space for the image with a tall line, draws the picture on top of
// that line and keeps the caret off it - so you can always type above and below an image, and
// because the placeholder is real text, images move/delete/undo exactly like text.
// ---------------------------------------------------------------------------------------------

internal const val OBJ = '\uFFFC'
internal const val DEFAULT_PCT = 70
internal const val DEFAULT_ASPECT = 4f / 3f

internal val textFmtTypes = FmtType.entries.filter {
    it != FmtType.Image && it != FmtType.Align && it != FmtType.Quote
}

internal fun packImage(idx: Int, pct: Int, align: Int, rounded: Boolean, wrap: Boolean = false): Int =
    (if (wrap) 1 shl 30 else 0) or (idx shl 10) or ((if (rounded) 1 else 0) shl 9) or
            (pct.coerceIn(10, 100) shl 2) or align.coerceIn(0, 2)

// bit 30 = "text beside image" (old notes never set it, so they keep working)
internal fun imgWrap(arg: Int) = ((arg shr 30) and 1) == 1
internal fun imgIdx(arg: Int) = (arg shr 10) and 0xFFFFF
internal fun imgRounded(arg: Int) = ((arg shr 9) and 1) == 1
internal fun imgPct(arg: Int) = (arg shr 2) and 127
internal fun imgAlign(arg: Int) = arg and 3

internal data class ImagePlacement(
    val start: Int,
    val idx: Int,
    val name: String,
    val widthPx: Float,
    val heightPx: Float,
    val align: Int,
    val rounded: Boolean,
    val pct: Int,
    val wrap: Boolean
)

/** Text that exists only on screen (soft line breaks, indent, spacer) - never saved. */
internal class Insertion(
    val pos: Int,
    val text: String,
    /** Caret at [pos] is shown after (true) or before (false) the inserted text. */
    val after: Boolean,
    val spacerPx: Float = 0f,
    val glyphAt: Int = -1
)

/** A text layout together with the plan + image starts it was produced from (always consistent). */
internal class SyncedLayout(
    val layout: TextLayoutResult,
    val plan: DisplayPlan,
    val starts: Map<Int, Int> // image idx -> start offset in the original text
)

/**
 * What the editor actually draws: the note text plus on-screen-only insertions that let text flow
 * beside a small image (narrow soft line breaks + a left indent) and keep the picture's height.
 */
internal class DisplayPlan(
    val original: String,
    val shown: String,
    val inserts: List<Insertion>,
    val overrides: List<SizeOverride>,
    /** image start -> original offset of the first text line beside it */
    val anchors: Map<Int, Int>
) {
    private fun lenBefore(o: Int, inclusive: Boolean): Int {
        var n = 0
        for (i in inserts) if (i.pos < o || (inclusive && i.pos == o)) n += i.text.length
        return n
    }

    /** Start of a style span / placeholder (inserted text before it is not styled with it). */
    fun spanStart(o: Int) = o + lenBefore(o, true)
    fun spanEnd(o: Int) = o + lenBefore(o, false)

    fun toShown(o: Int): Int {
        var n = 0
        for (i in inserts) if (i.pos < o || (i.pos == o && i.after)) n += i.text.length
        return o + n
    }

    fun toOriginal(t: Int): Int {
        var shift = 0
        for (i in inserts) {
            val start = i.pos + shift
            if (t < start) break
            if (t < start + i.text.length) return i.pos.coerceIn(0, original.length)
            shift += i.text.length
        }
        return (t - shift).coerceIn(0, original.length)
    }

    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int) =
            toShown(offset.coerceIn(0, original.length)).coerceIn(0, shown.length)

        override fun transformedToOriginal(offset: Int) =
            toOriginal(offset.coerceIn(0, shown.length))
    }
}

internal const val NBSP = "\u00A0"
internal const val SPACER_GLYPH = "\uFFFC"
internal const val ZWSP = "\u200B"

internal fun buildDisplayPlan(
    text: String,
    spans: List<FormatSpan>,
    scripture: List<IntRange>,
    scriptureColor: Color,
    placements: List<ImagePlacement>,
    bodyWidthPx: Int,
    paras: List<ParaSpec>,
    measurer: TextMeasurer,
    baseStyle: TextStyle,
    linePerFont: Float,
    pxToSp: (Float) -> TextUnit,
    marginPx: Float,
    gapPx: Float,
    minTextPx: Float,
    invalidScripture: List<IntRange> = emptyList(),
    invalidScriptureColor: Color = ScriptureRed
): DisplayPlan {
    val inserts = mutableListOf<Insertion>()
    val anchors = HashMap<Int, Int>()
    val eligible = placements.filter { pl ->
        val rs0 = lineEndOf(text, pl.start) + 1
        // text only flows beside the picture when the line under it is left-aligned
        val startAligned = paras.firstOrNull { it.start <= rs0 && (rs0 < it.end || it.start == it.end) }
            ?.align == TextAlign.Start
        pl.wrap && pl.align != 1 && startAligned && bodyWidthPx > 0 &&
                bodyWidthPx - pl.widthPx - gapPx >= minTextPx
    }.map { it.start }.toSet()

    if (eligible.isNotEmpty()) {
        val annotated = buildStyled(
            text, spans, scripture, scriptureColor, emptyList(),
            invalidScripture = invalidScripture,
            invalidScriptureColor = invalidScriptureColor,
            paras = paras
        )
        val nbspW = (measurer.measure("a$NBSP" + "a", baseStyle).size.width -
                measurer.measure("aa", baseStyle).size.width).toFloat().coerceAtLeast(1f)

        for (p in placements) {
            if (p.start !in eligible) continue
            val sideW = p.widthPx + gapPx
            val rs = lineEndOf(text, p.start) + 1
            if (rs > text.length) continue
            val layout = measurer.measure(
                annotated, baseStyle,
                constraints = Constraints(maxWidth = (bodyWidthPx - sideW).toInt())
            )
            val leading = if (p.align == 0) NBSP.repeat((sideW / nbspW).toInt()) else ""
            val needH = p.heightPx + marginPx

            var li = layout.getLineForOffset(rs.coerceAtMost(text.length))
            val firstTop = layout.getLineTop(li)
            var bottom = firstTop
            var lastLs = -1
            var lastLe = -1
            var reached = false
            while (li < layout.lineCount) {
                val ls = layout.getLineStart(li)
                if (ls < text.length && text[ls] == OBJ) break // another image starts here
                val le = layout.getLineEnd(li)
                if (leading.isNotEmpty()) inserts += Insertion(ls, leading, true)
                lastLs = ls
                lastLe = le
                bottom = layout.getLineBottom(li)
                val soft = le < text.length && text[le - 1] != '\n'
                li++
                if (soft) inserts += Insertion(le, "\n", true)
                if (bottom - firstTop >= needH) {
                    reached = true
                    break
                }
            }
            if (!reached) {
                // Text beside the image is shorter than the picture: keep the picture's height.
                val shortBy = needH - (if (lastLs >= 0) bottom - firstTop else 0f)
                if (shortBy > 1f) {
                    if (lastLs >= 0) {
                        val pos = if (lastLe > lastLs && text[lastLe - 1] == '\n') lastLe - 1 else lastLe
                        inserts += Insertion(pos, "\n$SPACER_GLYPH", false, shortBy, 1)
                    } else {
                        inserts += Insertion(rs, "$SPACER_GLYPH\n", false, needH, 0)
                    }
                }
            }
            if (lastLs >= 0) anchors[p.start] = rs
        }
    }

    // A note ending in a line break has an empty last line. A zero-width character (display only)
    // gives that line its own paragraph, so the caret there follows ITS alignment.
    if (text.endsWith("\n")) inserts += Insertion(text.length, ZWSP, false)

    val sorted = inserts.sortedBy { it.pos } // stable
    val sb = StringBuilder()
    var last = 0
    for (i in sorted) {
        sb.append(text, last, i.pos)
        sb.append(i.text)
        last = i.pos
    }
    sb.append(text, last, text.length)

    val overrides = mutableListOf<SizeOverride>()
    for (p in placements) {
        var before = 0
        for (i in sorted) if (i.pos <= p.start) before += i.text.length
        val ts = p.start + before
        if (p.start in eligible) {
            overrides += SizeOverride(ts, ts + 2, 1.sp) // tiny line: the text beside it defines the height
        } else {
            val h = p.heightPx + 2 * marginPx
            overrides += SizeOverride(ts, ts + 1, pxToSp(h / linePerFont))
        }
    }
    var shift = 0
    for (i in sorted) {
        val ts = i.pos + shift
        if (i.spacerPx > 0f) {
            overrides += SizeOverride(ts + i.glyphAt, ts + i.glyphAt + 1, pxToSp(i.spacerPx / linePerFont))
        }
        shift += i.text.length
    }
    return DisplayPlan(text, sb.toString(), sorted, overrides, anchors)
}

/** Names of the images that are actually placed in the text, in reading order. */
internal fun usedImageNames(spans: List<FormatSpan>, images: List<String>): List<String> =
    spans.filter { it.type == FmtType.Image }.sortedBy { it.start }
        .mapNotNull { images.getOrNull(imgIdx(it.arg)) }

/** Notes saved with images but no placeholders get their images appended at the end. */
internal fun upgradeLegacy(
    content: String,
    spans: List<FormatSpan>,
    images: List<String>
): Pair<String, List<FormatSpan>> {
    val placed = spans.filter { it.type == FmtType.Image }.map { imgIdx(it.arg) }.toSet()
    var text = content
    var out = spans
    images.indices.filter { it !in placed }.forEach { idx ->
        val lead = if (text.isEmpty() || text.endsWith("\n")) "" else "\n"
        val start = text.length + lead.length
        text = text + lead + OBJ + "\n"
        out = out + FormatSpan(start, start + 1, FmtType.Image, packImage(idx, DEFAULT_PCT, 1, true))
    }
    return text to out
}

/** Drops images no longer placed in the text and renumbers the rest (used when saving). */
internal fun compactImages(
    spans: List<FormatSpan>,
    images: List<String>
): Pair<List<FormatSpan>, List<String>> {
    val remap = LinkedHashMap<Int, Int>()
    val outImages = mutableListOf<String>()
    spans.filter { it.type == FmtType.Image }.sortedBy { it.start }.forEach { sp ->
        val old = imgIdx(sp.arg)
        val name = images.getOrNull(old) ?: return@forEach
        remap.getOrPut(old) { outImages.add(name); outImages.size - 1 }
    }
    val newSpans = spans.mapNotNull { sp ->
        if (sp.type != FmtType.Image) sp
        else remap[imgIdx(sp.arg)]?.let { ni ->
            sp.copy(arg = packImage(ni, imgPct(sp.arg), imgAlign(sp.arg), imgRounded(sp.arg), imgWrap(sp.arg)))
        }
    }
    return newSpans to outImages
}

internal class NormResult(val text: String, val spans: List<FormatSpan>, val edits: List<TextEdit>)

/**
 * Keeps every placeholder alone on its line:
 *  - stray placeholders (no image behind them) are removed,
 *  - a deleted line break after an image (Backspace under it) deletes the image,
 *  - typing right next to an image pushes the typed text onto its own line.
 */
internal fun normalizeImages(text: String, spans: List<FormatSpan>, oldLen: Int): NormResult {
    val starts = spans.filter { it.type == FmtType.Image }.map { it.start }.toSet()
    val edits = mutableListOf<TextEdit>()
    for (i in text.indices) {
        if (text[i] != OBJ) continue
        if (i !in starts) {
            edits += TextEdit(i, 1, "")
            continue
        }
        val next = text.getOrNull(i + 1)
        if (next == null) {
            edits += TextEdit(i + 1, 0, "\n")
        } else if (next != '\n') {
            if (text.length < oldLen) {
                edits += TextEdit(i, 1, "")
                continue
            }
            edits += TextEdit(i + 1, 0, "\n")
        }
        if (i > 0 && text[i - 1] != '\n') edits += TextEdit(i, 0, "\n")
    }
    if (edits.isEmpty()) return NormResult(text, spans, edits)
    val (t, s) = applyEdits(text, spans, edits)
    return NormResult(t, s, edits)
}

/** The caret never rests on an image line: it jumps below it (or above, when moving up). */
internal fun keepCaretOffImages(v: TextFieldValue, spans: List<FormatSpan>, oldCaret: Int): TextFieldValue {
    if (!v.selection.collapsed) return v
    val text = v.text
    val p = v.selection.start.coerceIn(0, text.length)
    val ls = lineStartOf(text, p)
    if (ls >= text.length || text[ls] != OBJ) return v
    if (spans.none { it.type == FmtType.Image && it.start == ls }) return v
    val le = lineEndOf(text, ls)
    val target = when {
        p < oldCaret && ls > 0 -> ls - 1
        le < text.length -> le + 1
        else -> le
    }
    return v.copy(selection = TextRange(target))
}
