@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.richtext

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.editor.QuoteTint
import com.harsha.myapplication.ui.editor.ScriptureBlue
import com.harsha.myapplication.ui.editor.ScriptureRed
import com.harsha.myapplication.ui.editor.highlightColorOf
import com.harsha.myapplication.ui.notes.BodyLineHeightFactor

// ---------------------------------------------------------------------------------------------
// Styled text
// ---------------------------------------------------------------------------------------------

/**
 * Keeps a saved font colour readable on the current theme: very dark colours are lightened in dark
 * mode and very light ones darkened in light mode. Display only - the stored colour never changes.
 */
internal fun readableColor(c: Color, darkTheme: Boolean): Color {
    val l = c.luminance()
    return when {
        darkTheme && l < 0.18f -> lerp(c, Color.White, 0.75f)
        !darkTheme && l > 0.85f -> lerp(c, Color.Black, 0.75f)
        else -> c
    }
}

internal fun buildStyled(
    text: String,
    fmt: List<FormatSpan>,
    scripture: List<IntRange>,
    scriptureColor: Color,
    overrides: List<SizeOverride>,
    darkTheme: Boolean = false,
    invalidScripture: List<IntRange> = emptyList(),
    invalidScriptureColor: Color = ScriptureRed,
    paras: List<ParaSpec> = emptyList()
): AnnotatedString {
    val n = text.length
    val cuts = sortedSetOf(0, n)
    fmt.forEach { cuts.add(it.start.coerceIn(0, n)); cuts.add(it.end.coerceIn(0, n)) }
    scripture.forEach { cuts.add(it.first.coerceIn(0, n)); cuts.add((it.last + 1).coerceIn(0, n)) }
    invalidScripture.forEach { cuts.add(it.first.coerceIn(0, n)); cuts.add((it.last + 1).coerceIn(0, n)) }
    val pts = cuts.toList()

    // Sweep state. Segments are visited left to right, so a span joins once (when its start is reached)
    // and leaves for good once it ends. Each segment therefore only looks at the spans that really
    // cover it. (The old code re-filtered ALL spans for every segment: O(segments x spans).)
    val startOrder = fmt.indices.sortedBy { fmt[it].start }
    var nextStart = 0
    val covering = ArrayList<Int>()                       // indices into fmt, ascending = original order
    val sortedScripture = scripture.sortedBy { it.first }
    val sortedInvalid = invalidScripture.sortedBy { it.first }
    var firstLive = 0
    var firstInvalid = 0

    return buildAnnotatedString {
        append(text)
        // Per-paragraph alignment + hanging indent for list items
        for (pa in paras) {
            val ps = pa.start.coerceIn(0, n)
            val pe = pa.end.coerceIn(0, n)
            if (ps < pe) {
                // Normal, even line spacing for ordinary text. Paragraphs that hold an inline image
                // (or its reserved tall line) keep the font's own metrics so image spacing is unchanged.
                val hasImage = fmt.any { it.type == FmtType.Image && it.start < pe && it.end > ps } ||
                        overrides.any { it.start < pe && it.end > ps }
                val biggest = pa.size
                addStyle(
                    ParagraphStyle(
                        textAlign = pa.align,
                        textIndent = if (pa.rest.value > 0f) TextIndent(0.sp, pa.rest) else null,
                        lineHeight = if (hasImage) TextUnit.Unspecified else (biggest * BodyLineHeightFactor).sp,
                        lineHeightStyle = if (hasImage) null else LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    ),
                    ps,
                    pe
                )
            }
        }
        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            if (a >= b) continue

            while (nextStart < startOrder.size && fmt[startOrder[nextStart]].start <= a) {
                val idx = startOrder[nextStart++]
                var pos = covering.size
                while (pos > 0 && covering[pos - 1] > idx) pos--
                covering.add(pos, idx)                    // keep original order: "first Size / Color wins" is unchanged
            }
            covering.removeAll { fmt[it].end < b }
            val active = if (covering.isEmpty()) emptyList() else covering.map { fmt[it] }

            while (firstLive < sortedScripture.size && sortedScripture[firstLive].last + 1 <= a) firstLive++
            var inScripture = false
            var k = firstLive
            while (k < sortedScripture.size && sortedScripture[k].first <= a) {
                if (b <= sortedScripture[k].last + 1) { inScripture = true; break }
                k++
            }
            while (firstInvalid < sortedInvalid.size && sortedInvalid[firstInvalid].last + 1 <= a) firstInvalid++
            var inInvalid = false
            var ik = firstInvalid
            while (ik < sortedInvalid.size && sortedInvalid[ik].first <= a) {
                if (b <= sortedInvalid[ik].last + 1) { inInvalid = true; break }
                ik++
            }
            if (active.isEmpty() && !inScripture && !inInvalid) continue

            val isImage = active.any { it.type == FmtType.Image }
            fun has(t: FmtType) = active.any { it.type == t }
            val deco = buildList {
                if (has(FmtType.Underline)) add(TextDecoration.Underline)
                // A reference that does not exist in the active Bible is struck through as well as red.
                if (has(FmtType.Strike) || inInvalid) add(TextDecoration.LineThrough)
            }
            val sizeArg = active.firstOrNull { it.type == FmtType.Size }?.arg
            val colorArg = active.firstOrNull { it.type == FmtType.Color }?.arg
            // last wins: find-bar matches are appended after the user's own highlight
            val highlightArg = active.lastOrNull { it.type == FmtType.Highlight }?.arg
            val isQuote = has(FmtType.Quote)
            val isLink = inScripture || inInvalid
            addStyle(
                SpanStyle(
                    color = when {
                        // blue on the yellow highlight needs the darker blue to stay readable
                        isImage -> Color.Transparent // placeholder glyph must never show
                        inInvalid -> if (has(FmtType.Highlight)) ScriptureRed else invalidScriptureColor
                        inScripture -> if (has(FmtType.Highlight)) ScriptureBlue else scriptureColor
                        // on the yellow highlight the colour is used as chosen (it is a light background)
                        colorArg != null ->
                            if (has(FmtType.Highlight)) Color(colorArg) else readableColor(Color(colorArg), darkTheme)
                        // theme text can be white in dark mode; keep text readable on yellow
                        has(FmtType.Highlight) -> InkBlack
                        else -> Color.Unspecified
                    },
                    fontSize = sizeArg?.let { TextSize.entries.getOrNull(it)?.size }
                        ?: TextUnit.Unspecified,
                    fontWeight = if (has(FmtType.Bold) || isLink) FontWeight.Bold else null,
                    fontStyle = if (has(FmtType.Italic) || isQuote) FontStyle.Italic else null,
                    textDecoration = if (deco.isEmpty()) null else TextDecoration.combine(deco),
                    background = when {
                        highlightArg != null -> highlightColorOf(highlightArg)
                        isQuote -> QuoteTint
                        else -> Color.Unspecified
                    }
                ),
                a,
                b
            )
        }
        // Reserved image space: the placeholder glyph is scaled so its LINE is exactly as tall as
        // the picture. (A ParagraphStyle line height made Compose add a second tall empty line.)
        overrides.forEach { o ->
            val st = o.start.coerceIn(0, n)
            val en = o.end.coerceIn(0, n)
            if (st < en) addStyle(SpanStyle(fontSize = o.size, color = Color.Transparent), st, en)
        }
    }
}

/** Font-size override for [start, end) in the DISPLAYED text. */
internal class SizeOverride(val start: Int, val end: Int, val size: TextUnit)

/** One undo step: everything the user can change in the editor. */
internal data class EditorSnapshot(
    val title: String,
    val text: String,
    val spans: List<FormatSpan>,
    val selection: TextRange,
    val aligns: List<Int>,
    val category: String,
    val images: List<String>
)

// Savers: editor state survives rotation / process death.
internal val spansSaver = Saver<List<FormatSpan>, String>(
    save = { encodeSpans(it) },
    restore = { decodeSpans(it, Int.MAX_VALUE) }
)
internal val alignsSaver = Saver<List<Int>, String>(
    save = { it.joinToString("") },
    restore = { str -> str.map { (it - '0').coerceIn(0, 3) } }
)
internal val stringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it }
)
