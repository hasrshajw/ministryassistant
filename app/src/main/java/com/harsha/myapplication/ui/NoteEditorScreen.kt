package com.harsha.myapplication.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.harsha.myapplication.ui.theme.Motion
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.harsha.myapplication.*
import com.harsha.myapplication.data.DEFAULT_CATEGORIES
import com.harsha.myapplication.data.DEFAULT_CATEGORY
import com.harsha.myapplication.data.formatNoteDate
import com.harsha.myapplication.data.NoteDraft
import com.harsha.myapplication.data.NoteImages
import com.harsha.myapplication.data.NoteItem
import com.harsha.myapplication.data.NotesRepository
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.ScriptureDetector
import com.harsha.myapplication.scripture.ScriptureReference
import com.harsha.myapplication.scripture.localizedKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private val EditorMeta = Color(0xFF9A9A9A)
private val EditorCursor = Color(0xFFE6B800)

/** Tint used for "selected / on" states (brand yellow, M3 container-style alpha). */
private val SelectedTint = EditorCursor.copy(alpha = 0.24f)

/** Both bottom panels (Aa + Align) share this exact height so the layout never jumps. */
private val PanelHeight = 280.dp

/**
 * Optional fine-tuning: shaves this much off the space under the controls while the keyboard
 * is open (only needed if your keyboard draws shorter than the height it reports). Default 0.
 */
private val KeyboardGapTrim = 0.dp

private val ScriptureBlue = Color(0xFF1A73E8)
private val ScriptureBlueDark = Color(0xFF8AB4F8)
private val HighlightYellow = Color(0xFFFFF176)

private const val UNDO_COALESCE_MS = 700L
private const val MAX_UNDO = 100
private const val INDENT_STEP = 2
private const val MAX_INDENT = 8
private const val MAX_PICK = 10

private enum class Panel { None, Format, Align, Image }

private enum class ListKind { Bullet, Number, Letter }

// ---------------------------------------------------------------------------------------------
// List engine: markers, nesting, auto-numbering
// ---------------------------------------------------------------------------------------------

/** Optional leading indent (spaces), then "• ", "1. " or "a. ". */
private val listRegex = Regex("""^( {0,8})(• |(\d{1,3})\. |([a-z])\. )""")
private val numberParenShortcut = Regex("""^(\d{1,3})\) $""")
private val letterParenShortcut = Regex("""^([a-z])\) $""")

/** A parsed list marker at the start of a line. [length] covers indent + marker. */
private class ListMarker(
    val indent: Int,
    val kind: ListKind,
    val number: Int,
    val letter: Char,
    val length: Int
)

private fun markerOf(line: String): ListMarker? {
    val m = listRegex.find(line) ?: return null
    val indent = m.groupValues[1].length
    val kind = when {
        m.groups[3] != null -> ListKind.Number
        m.groups[4] != null -> ListKind.Letter
        else -> ListKind.Bullet
    }
    return ListMarker(
        indent = indent,
        kind = kind,
        number = m.groups[3]?.value?.toIntOrNull() ?: 0,
        letter = m.groups[4]?.value?.firstOrNull() ?: 'a',
        length = indent + m.groupValues[2].length
    )
}

private fun prefixFor(kind: ListKind, idx: Int): String = when (kind) {
    ListKind.Bullet -> "• "
    ListKind.Number -> "$idx. "
    ListKind.Letter -> "${'a' + (idx - 1).coerceIn(0, 25)}. "
}

/** Marker text for the item that follows [m] (same indent). */
private fun nextMarkerText(m: ListMarker): String {
    val pad = " ".repeat(m.indent)
    return pad + when (m.kind) {
        ListKind.Bullet -> "• "
        ListKind.Number -> "${m.number + 1}. "
        ListKind.Letter -> "${minOf(m.letter + 1, 'z')}. "
    }
}

/** Markdown-style typing shortcuts: type the trigger at line start, then a space. */
private fun shortcutPrefix(typed: String): String? = when (typed) {
    "- ", "* " -> "• "
    else -> numberParenShortcut.find(typed)?.let { "${it.groupValues[1]}. " }
        ?: letterParenShortcut.find(typed)?.let { "${it.groupValues[1]}. " }
}

private fun lineStartOf(text: String, offset: Int): Int = text.lastIndexOf('\n', offset - 1) + 1

private fun lineEndOf(text: String, from: Int): Int =
    text.indexOf('\n', from).let { if (it == -1) text.length else it }

private data class TextEdit(val start: Int, val length: Int, val replacement: String)

/** Applies [edits] (positions refer to [text]) and keeps the style spans in sync. */
private fun applyEdits(
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
private fun mapOffset(offset: Int, edits: List<TextEdit>): Int {
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
private fun renumberEdits(text: String, anchor: Int): List<TextEdit> {
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
        val expected = " ".repeat(m.indent) + prefixFor(m.kind, counters[level])
        if (text.substring(ls, ls + m.length) != expected) {
            edits.add(TextEdit(ls, m.length, expected))
        }
        if (le >= text.length) break
        ls = le + 1
    }
    return edits
}

private fun renumber(
    text: String,
    spans: List<FormatSpan>,
    anchor: Int
): Triple<String, List<FormatSpan>, List<TextEdit>> {
    val edits = renumberEdits(text, anchor)
    if (edits.isEmpty()) return Triple(text, spans, edits)
    val (t, r) = applyEdits(text, spans, edits)
    return Triple(t, r, edits)
}

// ---------------------------------------------------------------------------------------------
// Rich-text model (spans are kept next to the plain string and shifted as the text is edited)
// ---------------------------------------------------------------------------------------------

private enum class FmtType { Bold, Italic, Underline, Strike, Highlight, Size, Color, Image }

private enum class TextSize(val label: String, val size: TextUnit) {
    Normal("Normal", 16.sp),
    SubHeading("Sub-Heading", 20.sp),
    Heading("Heading", 26.sp)
}

private data class FormatSpan(val start: Int, val end: Int, val type: FmtType, val arg: Int = 0)

private val InkBlack = Color(0xFF1A1A1A)
/** Palette with names (screen readers announce them). The first entry is the theme's own text colour. */
private val FontColors: List<Pair<Color, String>> = listOf(
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

private fun List<FormatSpan>.subtract(type: FmtType, s: Int, e: Int): List<FormatSpan> =
    flatMap { sp ->
        if (sp.type != type || sp.end <= s || sp.start >= e) {
            listOf(sp)
        } else buildList {
            if (sp.start < s) add(sp.copy(end = s))
            if (sp.end > e) add(sp.copy(start = e))
        }
    }

private fun List<FormatSpan>.covers(type: FmtType, s: Int, e: Int): Boolean {
    if (e <= s) return false
    var pos = s
    for (sp in filter { it.type == type && it.end > s && it.start < e }.sortedBy { it.start }) {
        if (sp.start > pos) return false
        pos = maxOf(pos, sp.end)
        if (pos >= e) return true
    }
    return pos >= e
}

private fun List<FormatSpan>.argAt(type: FmtType, pos: Int): Int? =
    firstOrNull { it.type == type && it.start <= pos && it.end > pos }?.arg

/** Shifts spans for a text edit; also returns the range of newly inserted text (if any). */
private fun shiftSpans(
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

// ---------------------------------------------------------------------------------------------
// Persistence of styling (stored as JSON next to the note text)
// ---------------------------------------------------------------------------------------------

/** Serialises spans. Same-style neighbours are merged; no spans -> empty string. */
private fun encodeSpans(spans: List<FormatSpan>): String {
    val merged = mutableListOf<FormatSpan>()
    spans.sortedWith(compareBy({ it.type.ordinal }, { it.arg }, { it.start })).forEach { sp ->
        val last = merged.lastOrNull()
        if (last != null && last.type == sp.type && last.arg == sp.arg && sp.start <= last.end) {
            merged[merged.lastIndex] = last.copy(end = maxOf(last.end, sp.end))
        } else {
            merged.add(sp)
        }
    }
    if (merged.isEmpty()) return ""
    val arr = JSONArray()
    merged.forEach {
        arr.put(JSONObject().put("s", it.start).put("e", it.end).put("t", it.type.name).put("a", it.arg))
    }
    return arr.toString()
}

/** Parses spans defensively: unknown types and out-of-range values are dropped, never crash. */
private fun decodeSpans(json: String, textLength: Int): List<FormatSpan> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val type = FmtType.values().firstOrNull { it.name == o.optString("t") } ?: continue
                val s = o.optInt("s", 0).coerceIn(0, textLength)
                val e = o.optInt("e", 0).coerceIn(0, textLength)
                if (s < e) add(FormatSpan(s, e, type, o.optInt("a", 0)))
            }
        }
    }.getOrDefault(emptyList())
}

private fun alignToInt(a: TextAlign): Int = when (a) {
    TextAlign.Center -> 1
    TextAlign.End, TextAlign.Right -> 2
    else -> 0
}

private fun alignFromInt(i: Int): TextAlign = when (i) {
    1 -> TextAlign.Center
    2 -> TextAlign.End
    else -> TextAlign.Start
}

// ---------------------------------------------------------------------------------------------
// Styled text
// ---------------------------------------------------------------------------------------------

/**
 * Keeps a saved font colour readable on the current theme: very dark colours are lightened in dark
 * mode and very light ones darkened in light mode. Display only - the stored colour never changes.
 */
private fun readableColor(c: Color, darkTheme: Boolean): Color {
    val l = c.luminance()
    return when {
        darkTheme && l < 0.18f -> lerp(c, Color.White, 0.75f)
        !darkTheme && l > 0.85f -> lerp(c, Color.Black, 0.75f)
        else -> c
    }
}

private fun buildStyled(
    text: String,
    fmt: List<FormatSpan>,
    scripture: List<IntRange>,
    scriptureColor: Color,
    overrides: List<SizeOverride>,
    darkTheme: Boolean = false
): AnnotatedString {
    val n = text.length
    val cuts = sortedSetOf(0, n)
    fmt.forEach { cuts.add(it.start.coerceIn(0, n)); cuts.add(it.end.coerceIn(0, n)) }
    scripture.forEach { cuts.add(it.first.coerceIn(0, n)); cuts.add((it.last + 1).coerceIn(0, n)) }
    val pts = cuts.toList()

    // Sweep state. Segments are visited left to right, so a span joins once (when its start is reached)
    // and leaves for good once it ends. Each segment therefore only looks at the spans that really
    // cover it. (The old code re-filtered ALL spans for every segment: O(segments x spans).)
    val startOrder = fmt.indices.sortedBy { fmt[it].start }
    var nextStart = 0
    val covering = ArrayList<Int>()                       // indices into fmt, ascending = original order
    val sortedScripture = scripture.sortedBy { it.first }
    var firstLive = 0

    return buildAnnotatedString {
        append(text)
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
            if (active.isEmpty() && !inScripture) continue

            val isImage = active.any { it.type == FmtType.Image }
            fun has(t: FmtType) = active.any { it.type == t }
            val deco = buildList {
                if (has(FmtType.Underline)) add(TextDecoration.Underline)
                if (has(FmtType.Strike)) add(TextDecoration.LineThrough)
            }
            val sizeArg = active.firstOrNull { it.type == FmtType.Size }?.arg
            val colorArg = active.firstOrNull { it.type == FmtType.Color }?.arg
            addStyle(
                SpanStyle(
                    color = when {
                        // blue on the yellow highlight needs the darker blue to stay readable
                        isImage -> Color.Transparent // placeholder glyph must never show
                        inScripture -> if (has(FmtType.Highlight)) ScriptureBlue else scriptureColor
                        // on the yellow highlight the colour is used as chosen (it is a light background)
                        colorArg != null ->
                            if (has(FmtType.Highlight)) Color(colorArg) else readableColor(Color(colorArg), darkTheme)
                        // theme text can be white in dark mode; keep text readable on yellow
                        has(FmtType.Highlight) -> InkBlack
                        else -> Color.Unspecified
                    },
                    fontSize = sizeArg?.let { TextSize.values().getOrNull(it)?.size }
                        ?: TextUnit.Unspecified,
                    fontWeight = if (has(FmtType.Bold) || inScripture) FontWeight.Bold else null,
                    fontStyle = if (has(FmtType.Italic)) FontStyle.Italic else null,
                    textDecoration = if (deco.isEmpty()) null else TextDecoration.combine(deco),
                    background = if (has(FmtType.Highlight)) HighlightYellow else Color.Unspecified
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
private class SizeOverride(val start: Int, val end: Int, val size: TextUnit)

/** One undo step: everything the user can change in the editor. */
private data class EditorSnapshot(
    val title: String,
    val text: String,
    val spans: List<FormatSpan>,
    val selection: TextRange,
    val align: TextAlign,
    val category: String,
    val images: List<String>
)

// Savers: editor state survives rotation / process death.
private val spansSaver = Saver<List<FormatSpan>, String>(
    save = { encodeSpans(it) },
    restore = { decodeSpans(it, Int.MAX_VALUE) }
)
private val alignSaver = Saver<TextAlign, Int>(
    save = { alignToInt(it) },
    restore = { alignFromInt(it) }
)
private val stringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it }
)

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

private const val OBJ = '\uFFFC'
private const val DEFAULT_PCT = 70
private const val DEFAULT_ASPECT = 4f / 3f

private val textFmtTypes = FmtType.values().filter { it != FmtType.Image }

private fun packImage(idx: Int, pct: Int, align: Int, rounded: Boolean, wrap: Boolean = false): Int =
    (if (wrap) 1 shl 30 else 0) or (idx shl 10) or ((if (rounded) 1 else 0) shl 9) or
            (pct.coerceIn(10, 100) shl 2) or align.coerceIn(0, 2)

// bit 30 = "text beside image" (old notes never set it, so they keep working)
private fun imgWrap(arg: Int) = ((arg shr 30) and 1) == 1
private fun imgIdx(arg: Int) = (arg shr 10) and 0xFFFFF
private fun imgRounded(arg: Int) = ((arg shr 9) and 1) == 1
private fun imgPct(arg: Int) = (arg shr 2) and 127
private fun imgAlign(arg: Int) = arg and 3

private data class ImagePlacement(
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
private class Insertion(
    val pos: Int,
    val text: String,
    /** Caret at [pos] is shown after (true) or before (false) the inserted text. */
    val after: Boolean,
    val spacerPx: Float = 0f,
    val glyphAt: Int = -1
)

/** A text layout together with the plan + image starts it was produced from (always consistent). */
private class SyncedLayout(
    val layout: TextLayoutResult,
    val plan: DisplayPlan,
    val starts: Map<Int, Int> // image idx -> start offset in the original text
)

/**
 * What the editor actually draws: the note text plus on-screen-only insertions that let text flow
 * beside a small image (narrow soft line breaks + a left indent) and keep the picture's height.
 */
private class DisplayPlan(
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

private const val NBSP = "\u00A0"
private const val SPACER_GLYPH = "\uFFFC"

private fun buildDisplayPlan(
    text: String,
    spans: List<FormatSpan>,
    scripture: List<IntRange>,
    scriptureColor: Color,
    placements: List<ImagePlacement>,
    bodyWidthPx: Int,
    bodyAlign: TextAlign,
    measurer: TextMeasurer,
    baseStyle: TextStyle,
    linePerFont: Float,
    pxToSp: (Float) -> TextUnit,
    marginPx: Float,
    gapPx: Float,
    minTextPx: Float
): DisplayPlan {
    val inserts = mutableListOf<Insertion>()
    val anchors = HashMap<Int, Int>()
    val eligible = placements.filter {
        it.wrap && it.align != 1 && bodyAlign == TextAlign.Start && bodyWidthPx > 0 &&
                bodyWidthPx - it.widthPx - gapPx >= minTextPx
    }.map { it.start }.toSet()

    if (eligible.isNotEmpty()) {
        val annotated = buildStyled(text, spans, scripture, scriptureColor, emptyList())
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
private fun usedImageNames(spans: List<FormatSpan>, images: List<String>): List<String> =
    spans.filter { it.type == FmtType.Image }.sortedBy { it.start }
        .mapNotNull { images.getOrNull(imgIdx(it.arg)) }

/** Notes saved with images but no placeholders get their images appended at the end. */
private fun upgradeLegacy(
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
private fun compactImages(
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

private class NormResult(val text: String, val spans: List<FormatSpan>, val edits: List<TextEdit>)

/**
 * Keeps every placeholder alone on its line:
 *  - stray placeholders (no image behind them) are removed,
 *  - a deleted line break after an image (Backspace under it) deletes the image,
 *  - typing right next to an image pushes the typed text onto its own line.
 */
private fun normalizeImages(text: String, spans: List<FormatSpan>, oldLen: Int): NormResult {
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
private fun keepCaretOffImages(v: TextFieldValue, spans: List<FormatSpan>, oldCaret: Int): TextFieldValue {
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

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

/**
 * Note editor.
 *
 * Existing notes open in VIEW mode (read only); a brand-new note opens straight in EDIT mode.
 * The tick saves and returns to view mode, the pencil switches back to edit mode.
 *
 * @param initialNote note being shown, or null for a new one.
 * @param onSave persist the draft and return the stored note. [existingId] is null until the note
 *               has been saved once; the editor remembers the id afterwards, so saving again
 *               updates the same note. The caller must NOT close the screen here.
 * @param onBack leave the screen. The editor itself asks "Save changes?" before calling this
 *               when there are unsaved edits, so the caller just closes the screen.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun NoteEditorView(
    initialNote: NoteItem?,
    onSave: (draft: NoteDraft, existingId: Int?) -> NoteItem,
    onBack: () -> Unit,
    categories: List<String> = DEFAULT_CATEGORIES,
    /** Folder colours (ARGB) already stored by the host, keyed by folder name. */
    folderColors: Map<String, Int> = emptyMap(),
    /** Called when the user creates a folder in the "New folder" popup, so the host can store its colour. */
    onCreateFolder: (name: String, colorArgb: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---- initial values (used for dirty detection) ----
    // A note saved without a title is stored as "Untitled Note". Show that as a hint, never as
    // real text in the field, so the user can simply start typing a title.
    val initialTitle = remember {
        initialNote?.title?.takeUnless { it == NotesRepository.UNTITLED } ?: ""
    }
    val rawContent = remember { initialNote?.content ?: "" }
    // Old notes (images but no placeholders) are upgraded in memory so they are not "dirty".
    val upgraded = remember {
        upgradeLegacy(
            rawContent,
            decodeSpans(initialNote?.formatting.orEmpty(), rawContent.length),
            initialNote?.images.orEmpty()
        )
    }
    val initialContent = upgraded.first
    val initialSpans = upgraded.second
    val initialCategory = remember { initialNote?.category ?: DEFAULT_CATEGORY }
    val initialImages = remember { initialNote?.images.orEmpty() }
    val initialAlign = remember { alignFromInt(initialNote?.alignment ?: 0) }
    val initialFormatting = remember { encodeSpans(initialSpans) }
    val initialUsedNames = remember { usedImageNames(initialSpans, initialImages) }

    // ---- editable state ----
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var bodyValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialContent))
    }
    var spans by rememberSaveable(stateSaver = spansSaver) { mutableStateOf(initialSpans) }
    var bodyAlign by rememberSaveable(stateSaver = alignSaver) { mutableStateOf(initialAlign) }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var images by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(initialImages) }
    // Images imported during this editing session (so unsaved ones can be cleaned up).
    var sessionAdded by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(emptyList<String>())
    }
    var customCategories by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(emptyList<String>())
    }
    var importing by remember { mutableIntStateOf(0) } // images currently being copied
    val typing = remember { mutableStateMapOf<FmtType, Int>() } // style for newly typed text

    // Saved baseline. It moves forward on every save, so "dirty" means "changed since last save".
    var baseTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var baseContent by rememberSaveable { mutableStateOf(initialContent) }
    var baseCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var baseAlign by rememberSaveable(stateSaver = alignSaver) { mutableStateOf(initialAlign) }
    var baseFormatting by rememberSaveable { mutableStateOf(initialFormatting) }
    var baseUsedNames by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(initialUsedNames)
    }
    // Id of the stored note (null until a new note is saved the first time).
    var noteId by rememberSaveable { mutableStateOf(initialNote?.id) }
    // New notes start in edit mode, existing notes in view mode.
    var editing by rememberSaveable { mutableStateOf(initialNote == null) }
    // Same format as the notes list: the note's last-edited time, or "now" for a new note.
    var dateText by rememberSaveable {
        mutableStateOf(
            initialNote?.date?.takeIf { it.isNotBlank() } ?: formatNoteDate(System.currentTimeMillis())
        )
    }

    val content = bodyValue.text
    val usedNames = usedImageNames(spans, images)
    val formattingJson = remember(spans) { encodeSpans(spans) }
    val isDirty = title != baseTitle ||
            content != baseContent ||
            category != baseCategory ||
            bodyAlign != baseAlign ||
            formattingJson != baseFormatting ||
            usedNames != baseUsedNames
    val canSave = title.isNotBlank() || content.isNotBlank() || usedNames.isNotEmpty()

    var selectedReference by remember { mutableStateOf<ScriptureReference?>(null) }
    var showAddScripture by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    // Colours of folders created in this session, stored as "name<US>argb" so they survive rotation.
    var customFolderColors by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(emptyList<String>())
    }
    var viewerImage by remember { mutableStateOf<String?>(null) }
    var imagePanelIdx by remember { mutableStateOf<Int?>(null) } // image shown in the docked image panel
    var pendingRemoveIdx by rememberSaveable { mutableStateOf<Int?>(null) } // image awaiting delete confirmation
    val snackbar = remember { SnackbarHostState() }

    // Same sources as the notes list menu: default/hosted categories, every folder the host has a
    // colour for (i.e. created in either screen), folders created here, and the note's own folder.
    val categoryOptions = remember(categories, folderColors, customCategories, category) {
        (categories + folderColors.keys + customCategories + category)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    }
    val folderColorMap = remember(folderColors, customFolderColors) {
        val m = folderColors.mapValues { Color(it.value) }.toMutableMap()
        customFolderColors.forEach { e ->
            val i = e.lastIndexOf('\u001F')
            if (i > 0) e.substring(i + 1).toIntOrNull()?.let { m[e.substring(0, i)] = Color(it) }
        }
        m
    }
    val folderColorOf: (String) -> Color = { name ->
        folderColorMap[name]
            ?: folderColorMap.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
            ?: DefaultFolderColor
    }

    // ---- panels / focus ----
    var panel by remember { mutableStateOf(Panel.None) }
    var focusOnEdit by remember { mutableStateOf(false) } // open the keyboard after tapping Edit
    var lastPanel by remember { mutableStateOf(Panel.Format) }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val bodyFocus = remember { FocusRequester() }
    val imeVisible = WindowInsets.isImeVisible
    val emptyTapSource = remember { MutableInteractionSource() }

    // ---- undo / redo (full snapshots, typing is grouped into bursts) ----
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }
    var lastUndoPushAt by remember { mutableLongStateOf(0L) }

    fun snapshot() = EditorSnapshot(
        title, bodyValue.text, spans, bodyValue.selection, bodyAlign, category, images
    )

    /** Records the current state so it can be undone. [coalesce] groups rapid typing in one step. */
    fun pushUndo(coalesce: Boolean = false) {
        val now = SystemClock.uptimeMillis()
        val inBurst = coalesce && undoStack.isNotEmpty() && now - lastUndoPushAt < UNDO_COALESCE_MS
        lastUndoPushAt = if (coalesce) now else 0L
        redoStack.clear()
        if (inBurst) return
        undoStack.add(snapshot())
        if (undoStack.size > MAX_UNDO) undoStack.removeAt(0)
    }

    fun restore(s: EditorSnapshot) {
        title = s.title
        spans = s.spans
        bodyAlign = s.align
        category = s.category
        images = s.images
        bodyValue = TextFieldValue(
            s.text,
            TextRange(s.selection.start.coerceIn(0, s.text.length), s.selection.end.coerceIn(0, s.text.length))
        )
        lastUndoPushAt = 0L
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(snapshot())
        restore(undoStack.removeAt(undoStack.lastIndex))
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(snapshot())
        restore(redoStack.removeAt(redoStack.lastIndex))
    }

    // ---- save / exit / view <-> edit ----

    /** Stores the note. The editor stays open and its state now mirrors exactly what was stored. */
    fun persistNote() {
        keyboard?.hide()
        val (cleanSpans, cleanImages) = compactImages(spans, images)
        val draft = NoteDraft(
            title = title.trimEnd(),
            content = bodyValue.text,
            category = category,
            formatting = encodeSpans(cleanSpans),
            alignment = alignToInt(bodyAlign),
            images = cleanImages
        )
        // Images imported (or rotated) this session but not in the final note are never needed.
        sessionAdded.filter { it !in draft.images }.forEach { NoteImages.delete(it) }
        val saved = onSave(draft, noteId)
        noteId = saved.id

        spans = cleanSpans
        images = cleanImages
        sessionAdded = emptyList()
        // Undo history would point at image files that may be gone now - start fresh.
        undoStack.clear()
        redoStack.clear()
        lastUndoPushAt = 0L

        baseTitle = title
        baseContent = bodyValue.text
        baseCategory = category
        baseAlign = bodyAlign
        baseFormatting = encodeSpans(cleanSpans)
        baseUsedNames = usedImageNames(cleanSpans, cleanImages)
        dateText = saved.date
    }

    fun enterViewMode() {
        keyboard?.hide()
        focusManager.clearFocus()
        panel = Panel.None
        editing = false
    }

    fun enterEditMode() {
        bodyValue = bodyValue.copy(selection = TextRange(bodyValue.text.length))
        editing = true
        focusOnEdit = true
    }

    /** Tick: save and show the note in view mode. */
    fun saveAndView() {
        persistNote()
        enterViewMode()
    }

    /** "Save" in the unsaved-changes dialog: save and leave the screen. */
    fun saveAndExit() {
        persistNote()
        onBack()
    }

    /** Tick button: saves when something changed, otherwise just leaves edit mode. */
    fun finishEditing() {
        if (isDirty || noteId == null) saveAndView() else enterViewMode()
    }

    fun discardAndExit() {
        // Everything imported in this session is dropped together with the changes.
        sessionAdded.forEach { NoteImages.delete(it) }
        onBack()
    }

    fun requestExit() {
        if (isDirty) showDiscardDialog = true else discardAndExit() // also cleans files added this session
    }

    // Registered first so the handlers below (registered later) win while they are enabled.
    BackHandler(enabled = !isDirty && sessionAdded.isNotEmpty()) { discardAndExit() }
    BackHandler(enabled = isDirty) { showDiscardDialog = true }
    BackHandler(enabled = panel != Panel.None) { panel = Panel.None }

    // Keyboard coming back closes any open panel
    LaunchedEffect(imeVisible) { if (imeVisible) panel = Panel.None }

    // The image panel closes when its image is gone (removed, or undone away)
    LaunchedEffect(panel, spans, imagePanelIdx) {
        if (panel == Panel.Image &&
            spans.none { it.type == FmtType.Image && imgIdx(it.arg) == imagePanelIdx }
        ) panel = Panel.None
    }

    // New notes open ready to type
    LaunchedEffect(Unit) {
        if (noteId == null && editing && title.isEmpty() && bodyValue.text.isEmpty()) {
            delay(250) // let the screen transition finish before opening the keyboard
            runCatching { bodyFocus.requestFocus() }
            keyboard?.show()
        }
    }

    // After tapping Edit: wait one frame so the field is writable, then open the keyboard
    LaunchedEffect(editing, focusOnEdit) {
        if (editing && focusOnEdit) {
            focusOnEdit = false
            delay(60)
            runCatching { bodyFocus.requestFocus() }
            keyboard?.show()
        }
    }

    fun togglePanel(target: Panel) {
        if (panel == target) {
            panel = Panel.None
            runCatching { bodyFocus.requestFocus() }
            keyboard?.show()
        } else {
            panel = target
            lastPanel = target
            keyboard?.hide()
        }
    }

    /** Tapping a picture opens the docked image panel - whatever panel was open before. */
    fun selectImage(idx: Int) {
        imagePanelIdx = idx
        panel = Panel.Image
        lastPanel = Panel.Image
        keyboard?.hide()
    }

    /** Tap on empty space: focus the body, put the caret at the end and open the keyboard. */
    fun focusBodyAtEnd() {
        panel = Panel.None
        bodyValue = bodyValue.copy(selection = TextRange(bodyValue.text.length))
        runCatching { bodyFocus.requestFocus() }
        keyboard?.show()
    }

    // When the cursor moves, "typing style" follows the character before it
    LaunchedEffect(bodyValue.selection) {
        val sel = bodyValue.selection
        typing.clear()
        if (sel.collapsed && sel.start > 0) {
            textFmtTypes.forEach { t -> spans.argAt(t, sel.start - 1)?.let { typing[t] = it } }
        }
    }

    // ---- body editing ----
    fun updateBody(input: TextFieldValue, byUser: Boolean) {
        val old = bodyValue.text
        var v = input
        var forceRenumber = false

        if (byUser && v.selection.collapsed) {
            val pos = v.selection.start
            val oneInserted = v.text.length == old.length + 1 && pos > 0 &&
                    v.text.removeRange(pos - 1, pos) == old

            if (oneInserted && v.text[pos - 1] == '\n') {
                // Enter on a list line continues the list; on an empty item it outdents / ends it
                val lineStart = lineStartOf(v.text, pos - 1)
                val line = v.text.substring(lineStart, pos - 1)
                val m = markerOf(line)
                if (m != null) {
                    v = if (line.length == m.length) {
                        forceRenumber = true
                        if (m.indent > 0) {
                            val step = minOf(INDENT_STEP, m.indent)
                            TextFieldValue(
                                old.removeRange(lineStart, lineStart + step),
                                TextRange(pos - 1 - step)
                            )
                        } else {
                            TextFieldValue(
                                old.removeRange(lineStart, lineStart + m.length),
                                TextRange(lineStart)
                            )
                        }
                    } else {
                        val ins = nextMarkerText(m)
                        TextFieldValue(
                            v.text.substring(0, pos) + ins + v.text.substring(pos),
                            TextRange(pos + ins.length)
                        )
                    }
                }
            } else if (oneInserted && v.text[pos - 1] == ' ') {
                // "- " / "* " -> bullet, "1) " -> "1. ", "a) " -> "a. " (works when indented too)
                val ls = lineStartOf(v.text, pos - 1)
                val typed = v.text.substring(ls, pos)
                val trimmed = typed.trimStart(' ')
                val replacement = shortcutPrefix(trimmed)
                if (replacement != null) {
                    val markerStart = ls + (typed.length - trimmed.length)
                    v = TextFieldValue(
                        v.text.replaceRange(markerStart, pos, replacement),
                        TextRange(markerStart + replacement.length)
                    )
                }
            } else if (v.text.length == old.length - 1 && old.removeRange(pos, pos + 1) == v.text) {
                // Backspace on an empty list item: outdent first, then remove the marker
                val ls = lineStartOf(old, pos)
                val le = lineEndOf(old, ls)
                val line = old.substring(ls, le)
                val m = markerOf(line)
                if (m != null && line.length == m.length && pos == ls + m.length - 1) {
                    forceRenumber = true
                    v = if (m.indent > 0) {
                        val step = minOf(INDENT_STEP, m.indent)
                        TextFieldValue(old.removeRange(ls, ls + step), TextRange(ls + m.length - step))
                    } else {
                        TextFieldValue(old.removeRange(ls, le), TextRange(ls))
                    }
                }
            }
        }

        if (v.text != old) {
            if (byUser) pushUndo(coalesce = true)
            val (shifted, inserted) = shiftSpans(spans, old, v.text)
            var r = shifted
            if (byUser && inserted != null) {
                textFmtTypes.forEach { t ->
                    r = r.subtract(t, inserted.first, inserted.last + 1)
                    typing[t]?.let { r = r + FormatSpan(inserted.first, inserted.last + 1, t, it) }
                }
            }
            // Keep numbered / lettered lists in sequence after lines are added or removed
            val newlinesChanged = v.text.count { it == '\n' } != old.count { it == '\n' }
            if (newlinesChanged || forceRenumber) {
                val (t, r2, edits) = renumber(v.text, r, v.selection.start)
                if (edits.isNotEmpty()) {
                    r = r2
                    v = TextFieldValue(
                        t,
                        TextRange(
                            mapOffset(v.selection.start, edits),
                            mapOffset(v.selection.end, edits)
                        )
                    )
                }
            }
            spans = r
        }
        if (byUser) {
            val norm = normalizeImages(v.text, spans, old.length)
            if (norm.edits.isNotEmpty()) {
                spans = norm.spans
                v = TextFieldValue(
                    norm.text,
                    TextRange(
                        mapOffset(v.selection.start, norm.edits),
                        mapOffset(v.selection.end, norm.edits)
                    )
                )
            }
            v = keepCaretOffImages(v, spans, bodyValue.selection.start)
        }
        bodyValue = v
    }

    // ---- inline image operations ----

    /** Puts [names] (image indexes [firstIdx]..) on their own lines at the caret. */
    fun insertImages(firstIdx: Int, names: List<String>) {
        val text = bodyValue.text
        var pos = bodyValue.selection.max.coerceIn(0, text.length)
        val ls = lineStartOf(text, pos)
        if (ls < text.length && text[ls] == OBJ) pos = (lineEndOf(text, ls) + 1).coerceAtMost(text.length)
        val before = text.substring(0, pos)
        val sb = StringBuilder()
        if (!before.endsWith("\n")) sb.append('\n') // before is empty or mid-line
        val offsets = mutableListOf<Int>()
        names.forEach {
            offsets += pos + sb.length
            sb.append(OBJ).append('\n')
        }
        val (t, shifted) = applyEdits(text, spans, listOf(TextEdit(pos, 0, sb.toString())))
        spans = shifted + offsets.mapIndexed { i, off ->
            FormatSpan(off, off + 1, FmtType.Image, packImage(firstIdx + i, DEFAULT_PCT, 1, true))
        }
        bodyValue = TextFieldValue(t, TextRange(pos + sb.length))
    }

    fun updateImage(
        idx: Int,
        pct: Int? = null,
        align: Int? = null,
        rounded: Boolean? = null,
        wrap: Boolean? = null
    ) {
        val sp = spans.firstOrNull { it.type == FmtType.Image && imgIdx(it.arg) == idx } ?: return
        val arg = packImage(
            idx,
            pct ?: imgPct(sp.arg),
            align ?: imgAlign(sp.arg),
            rounded ?: imgRounded(sp.arg),
            wrap ?: imgWrap(sp.arg)
        )
        if (arg == sp.arg) return
        pushUndo(coalesce = true)
        spans = spans.map { if (it == sp) it.copy(arg = arg) else it }
    }

    fun removeImage(idx: Int) {
        val sp = spans.firstOrNull { it.type == FmtType.Image && imgIdx(it.arg) == idx } ?: return
        val text = bodyValue.text
        val end = if (sp.end < text.length && text[sp.end] == '\n') sp.end + 1 else sp.end
        pushUndo()
        val (t, r) = applyEdits(text, spans, listOf(TextEdit(sp.start, end - sp.start, "")))
        spans = r
        bodyValue = TextFieldValue(t, TextRange(sp.start.coerceAtMost(t.length)))
    }

    /** Removes the image and offers an Undo snackbar (only undoes if nothing else was edited since). */
    fun confirmRemoveImage(idx: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        removeImage(idx)
        val mark = undoStack.size
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Image removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && undoStack.size == mark) undo()
        }
    }

    fun rotateImage(idx: Int) {
        val old = images.getOrNull(idx) ?: return
        scope.launch {
            val fresh = NoteImages.rotate(old)
            if (fresh == null) {
                Toast.makeText(context, "Couldn't rotate that image", Toast.LENGTH_SHORT).show()
            } else {
                pushUndo()
                images = images.toMutableList().also { it[idx] = fresh }
                sessionAdded = sessionAdded + fresh
            }
        }
    }

    fun toggleFmt(type: FmtType) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        if (s == e) {
            if (typing.containsKey(type)) typing.remove(type) else typing[type] = 0
        } else {
            pushUndo()
            val cleared = spans.subtract(type, s, e)
            spans = if (spans.covers(type, s, e)) cleared else cleared + FormatSpan(s, e, type)
        }
    }

    fun setFmt(type: FmtType, arg: Int, isDefault: Boolean) {
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        if (s == e) {
            if (isDefault) typing.remove(type) else typing[type] = arg
        } else {
            pushUndo()
            val cleared = spans.subtract(type, s, e)
            spans = if (isDefault) cleared else cleared + FormatSpan(s, e, type, arg)
        }
    }

    fun isOn(type: FmtType): Boolean {
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        return if (s == e) typing.containsKey(type) else spans.covers(type, s, e)
    }

    fun argOf(type: FmtType): Int? {
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        return if (s == e) typing[type]
        else if (spans.covers(type, s, e)) spans.argAt(type, s) else null
    }

    fun setAlign(a: TextAlign) {
        if (a == bodyAlign) return
        pushUndo()
        bodyAlign = a
    }

    /** Start offsets of every line touched by the selection. */
    fun selectedLineStarts(): List<Int> {
        val text = bodyValue.text
        val len = text.length
        val sMin = bodyValue.selection.min.coerceIn(0, len)
        val sMax = bodyValue.selection.max.coerceIn(0, len)
        val firstStart = lineStartOf(text, sMin)
        val end = if (sMax > sMin) sMax - 1 else sMax
        val starts = mutableListOf<Int>()
        var i = firstStart
        while (i <= end) {
            starts.add(i)
            val nl = text.indexOf('\n', i)
            if (nl == -1) break
            i = nl + 1
        }
        if (starts.isEmpty()) starts.add(firstStart)
        return starts
    }

    fun applyList(kind: ListKind) {
        val text = bodyValue.text
        val starts = selectedLineStarts()
        val markers = starts.map { markerOf(text.substring(it, lineEndOf(text, it))) }
        val allHave = markers.all { it?.kind == kind }
        val edits = starts.mapIndexedNotNull { i, st ->
            val m = markers[i]
            val length = m?.length ?: 0
            val replacement =
                if (allHave) "" else " ".repeat(m?.indent ?: 0) + prefixFor(kind, i + 1)
            if (length == 0 && replacement.isEmpty()) null else TextEdit(st, length, replacement)
        }
        if (edits.isEmpty()) return
        pushUndo()
        val (t1, r1) = applyEdits(text, spans, edits)
        val lastLine = mapOffset(starts.last(), edits)
        val (t2, r2, e2) = renumber(t1, r1, lastLine)
        val cursor = lineEndOf(t2, mapOffset(lastLine, e2).coerceIn(0, t2.length))
        spans = r2
        bodyValue = TextFieldValue(t2, TextRange(cursor))
    }

    /** Indent (+1) or outdent (-1) the selected list items; numbering is fixed up afterwards. */
    fun indentSelection(delta: Int) {
        val text = bodyValue.text
        val edits = selectedLineStarts().mapNotNull { st ->
            val m = markerOf(text.substring(st, lineEndOf(text, st))) ?: return@mapNotNull null
            val newIndent = (m.indent + delta * INDENT_STEP).coerceIn(0, MAX_INDENT)
            if (newIndent == m.indent) null else TextEdit(st, m.indent, " ".repeat(newIndent))
        }
        if (edits.isEmpty()) return
        pushUndo()
        val (t1, r1) = applyEdits(text, spans, edits)
        val s1 = mapOffset(bodyValue.selection.start, edits)
        val e1 = mapOffset(bodyValue.selection.end, edits)
        val (t2, r2, e2) = renumber(t1, r1, s1)
        spans = r2
        bodyValue = TextFieldValue(t2, TextRange(mapOffset(s1, e2), mapOffset(e1, e2)))
    }

    /** Inserts a scripture reference at the caret, adding spaces only where needed. */
    fun insertScripture(reference: String) {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val before = text.substring(0, sel.min)
        val after = text.substring(sel.max)
        val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        val trail = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
        updateBody(
            TextFieldValue(
                before + lead + reference + trail + after,
                TextRange(before.length + lead.length + reference.length)
            ),
            byUser = true
        )
    }

    val activeList: ListKind? = run {
        val st = bodyValue.selection.min.coerceIn(0, content.length)
        val ls = lineStartOf(content, st)
        markerOf(content.substring(ls, lineEndOf(content, ls)))?.kind
    }

    // Detection is debounced and runs on a background thread so long notes never freeze typing.
    var detectedReferences by remember { mutableStateOf<List<ScriptureReference>>(emptyList()) }
    var scriptureSpans by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    LaunchedEffect(content, BibleData.version) {
        delay(150)
        val (refs, ranges) = withContext(Dispatchers.Default) {
            ScriptureDetector.extractReferences(content).toList() to findScriptureSpans(content)
        }
        detectedReferences = refs
        scriptureSpans = ranges
    }

    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var bodyLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var synced by remember { mutableStateOf<SyncedLayout?>(null) }

    // System photo picker: no storage permission needed, falls back to the file picker on old devices.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            importing += uris.size
            val added = mutableListOf<String>()
            var failed = 0
            for (uri in uris) {
                val name = try {
                    NoteImages.import(context, uri)
                } finally {
                    importing -= 1
                }
                if (name != null) added += name else failed++
            }
            if (added.isNotEmpty()) {
                pushUndo()
                val firstIdx = images.size
                images = images + added
                sessionAdded = sessionAdded + added
                insertImages(firstIdx, added)
            }
            if (failed > 0) {
                Toast.makeText(
                    context,
                    if (failed == 1) "Couldn't add that image" else "Couldn't add $failed images",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val colors = MaterialTheme.colorScheme

    // ---- bottom spacing: measured, so it is right no matter what the parent already pads ----
    val view = LocalView.current
    val density = LocalDensity.current

    // ---- inline image layout (sizes depend on the text width) ----
    val aspects = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(images) {
        images.forEach { n ->
            if (!aspects.containsKey(n)) NoteImages.aspectRatio(n)?.let { aspects[n] = it }
        }
    }
    var bodyWidthPx by remember { mutableIntStateOf(0) }
    val imageMarginPx = with(density) { 6.dp.toPx() }
    val placements = spans.filter { it.type == FmtType.Image }.sortedBy { it.start }.mapNotNull { sp ->
        val idx = imgIdx(sp.arg)
        val name = images.getOrNull(idx) ?: return@mapNotNull null
        val w = bodyWidthPx * imgPct(sp.arg) / 100f
        ImagePlacement(
            start = sp.start, idx = idx, name = name,
            widthPx = w, heightPx = w / (aspects[name] ?: DEFAULT_ASPECT),
            align = imgAlign(sp.arg), rounded = imgRounded(sp.arg), pct = imgPct(sp.arg),
            wrap = imgWrap(sp.arg)
        )
    }
    val measurer = rememberTextMeasurer()
    val bodyTextStyle = TextStyle(
        fontFamily = Lexend,
        fontSize = 16.sp,
        color = colors.onSurface,
        textAlign = bodyAlign
    )
    // How tall a line gets per px of font size for the placeholder glyph (used to reserve image space)
    val linePerFont = remember(measurer, density) {
        val fontPx = with(density) { 100.sp.toPx() }
        (measurer.measure("\uFFFC", TextStyle(fontFamily = Lexend, fontSize = 100.sp)).size.height / fontPx)
            .coerceAtLeast(0.5f)
    }
    val sideGapPx = with(density) { 12.dp.toPx() }
    val minTextPx = with(density) { 96.dp.toPx() }
    val scriptureColor = if (darkSurface) ScriptureBlueDark else ScriptureBlue
    val plan = remember(
        content, spans, scriptureSpans, placements, bodyWidthPx, bodyAlign, scriptureColor, linePerFont
    ) {
        buildDisplayPlan(
            text = content,
            spans = spans,
            scripture = scriptureSpans,
            scriptureColor = scriptureColor,
            placements = placements,
            bodyWidthPx = bodyWidthPx,
            bodyAlign = bodyAlign,
            measurer = measurer,
            baseStyle = bodyTextStyle,
            linePerFont = linePerFont,
            pxToSp = { px -> with(density) { px.toSp() } },
            marginPx = imageMarginPx,
            gapPx = sideGapPx,
            minTextPx = minTextPx
        )
    }
    val planState = rememberUpdatedState(plan)
    val scriptureSpansState = rememberUpdatedState(scriptureSpans)
    val contentState = rememberUpdatedState(content)
    val placementsState = rememberUpdatedState(placements)
    val styleTransformation = remember(
        plan, spans, scriptureSpans, scriptureColor, placements, linePerFont, darkSurface
    ) {
        VisualTransformation { text ->
            if (text.text != plan.original) {
                // Field is one frame ahead of the plan: draw plain text BUT keep the image lines tall,
                // otherwise the whole text below the image jumps up and back down.
                val keep = placements.mapNotNull { p ->
                    if (p.start < text.text.length && text.text[p.start] == OBJ) {
                        val h = p.heightPx + 2 * imageMarginPx
                        SizeOverride(
                            p.start, p.start + 1,
                            with(density) { (h / linePerFont).toSp() }
                        )
                    } else null
                }
                TransformedText(
                    buildStyled(text.text, spans, scriptureSpans, scriptureColor, keep, darkSurface),
                    OffsetMapping.Identity
                )
            } else {
                TransformedText(
                    buildStyled(
                        plan.shown,
                        spans.map { it.copy(start = plan.spanStart(it.start), end = plan.spanEnd(it.end)) },
                        scriptureSpans.map { plan.spanStart(it.first) until plan.spanEnd(it.last + 1) },
                        scriptureColor,
                        plan.overrides,
                        darkSurface
                    ),
                    plan.mapping
                )
            }
        }
    }
    var distanceToWindowBottomPx by remember { mutableStateOf(0) }
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars
    val keyboardGapTrimPx = with(density) { KeyboardGapTrim.roundToPx() }
    // Same value as before, but only READ while laying out (Modifier.bottomInset). Reading the keyboard
    // inset in composition recomposed this whole screen on every frame of the keyboard animation.
    val barBottomPx: () -> Int = {
        val ime = imeInsets.getBottom(density)
        val nav = navInsets.getBottom(density)
        val trim = if (ime > nav) keyboardGapTrimPx else 0
        (maxOf(ime, nav) - distanceToWindowBottomPx - trim).coerceAtLeast(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { c ->
                val bottom = c.positionInWindow().y + c.size.height
                distanceToWindowBottomPx = (view.rootView.height - bottom).toInt().coerceAtLeast(0)
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        // Note / Edit Note: fade instead of snapping when the mode flips.
                        androidx.compose.animation.Crossfade(
                            targetState = when {
                                !editing -> "Note"
                                noteId == null -> "New Note"
                                else -> "Edit Note"
                            },
                            animationSpec = Motion.effectsFast,
                            label = "editorTitle"
                        ) { title ->
                            Text(
                                title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        TipIconButton(label = "Back", onClick = { requestExit() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (editing) {
                            TipIconButton(
                                label = "Undo",
                                onClick = { undo() },
                                enabled = undoStack.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }
                            TipIconButton(
                                label = "Redo",
                                onClick = { redo() },
                                enabled = redoStack.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                            }
                            // Primary action: filled tonal icon button (M3), so it stands out from Undo / Redo
                            TipTonalIconButton(
                                label = "Done",
                                onClick = { finishEditing() },
                                enabled = canSave
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Done")
                            }
                        } else {
                            TipTonalIconButton(label = "Edit", onClick = { enterEditMode() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit note")
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.onSurface,
                        navigationIconContentColor = colors.onSurface,
                        actionIconContentColor = colors.onSurfaceVariant
                    )
                )
            },
            containerColor = colors.surface,
            // We handle keyboard / nav-bar insets ourselves in the controls bar below.
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Scrollable editor area. The clickable sits BEFORE verticalScroll so it covers the
                // whole viewport: tapping any empty space focuses the note and opens the keyboard.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 640.dp) // tablets / foldables: keep a comfortable line length
                        .fillMaxWidth()
                        .clickable(
                            enabled = editing, // view mode: tapping empty space does nothing
                            interactionSource = emptyTapSource,
                            indication = null
                        ) { focusBodyAtEnd() }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    val fieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = EditorCursor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title - Enter / Next jumps into the body
                    TextField(
                        value = title,
                        onValueChange = {
                            if (it != title) pushUndo(coalesce = true)
                            title = it
                        },
                        placeholder = {
                            Text(
                                "Untitled Note",
                                fontFamily = Lexend,
                                fontSize = 30.sp,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = Lexend,
                            fontSize = 30.sp,
                            color = colors.onSurface
                        ),
                        singleLine = true,
                        readOnly = !editing,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusBodyAtEnd() }),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date | category
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateText,
                            fontFamily = Lexend,
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                        MetaDivider()
                        CategorySelector(
                            selected = category,
                            folderColor = folderColorOf(category),
                            enabled = editing,
                            onClick = { showFolderSheet = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Body - BasicTextField so tap positions map exactly onto the text layout.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        BasicTextField(
                            value = bodyValue,
                            onValueChange = { updateBody(it, byUser = true) },
                            readOnly = !editing,
                            textStyle = bodyTextStyle,
                            cursorBrush = SolidColor(EditorCursor),
                            visualTransformation = styleTransformation,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            decorationBox = { inner ->
                                Box(propagateMinConstraints = true) {
                                    if (editing && bodyValue.text.isEmpty()) {
                                        Text(
                                            "Start writing\u2026",
                                            style = bodyTextStyle,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    inner()
                                }
                            },
                            onTextLayout = { l ->
                                bodyLayout = l
                                val p = planState.value
                                // Only trust a layout that was made from the CURRENT plan's text.
                                if (l.layoutInput.text.text == p.shown) {
                                    synced = SyncedLayout(
                                        l, p, placementsState.value.associate { it.idx to it.start }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 208.dp)
                                .onSizeChanged { bodyWidthPx = it.width }
                                .focusRequester(bodyFocus)
                                // Hardware keyboard: Ctrl+B / I / U / Z / Y, Tab / Shift+Tab on lists
                                .onPreviewKeyEvent { e ->
                                    if (!editing || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when {
                                        e.isCtrlPressed -> when (e.key) {
                                            Key.B -> { toggleFmt(FmtType.Bold); true }
                                            Key.I -> { toggleFmt(FmtType.Italic); true }
                                            Key.U -> { toggleFmt(FmtType.Underline); true }
                                            Key.Z -> { if (e.isShiftPressed) redo() else undo(); true }
                                            Key.Y -> { redo(); true }
                                            else -> false
                                        }
                                        e.key == Key.Tab && activeList != null -> {
                                            indentSelection(if (e.isShiftPressed) -1 else 1)
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                // Keyed on Unit and reading the latest values through rememberUpdatedState:
                                // the gesture coroutine is no longer cancelled + restarted on every keystroke.
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial
                                        )
                                        val up = waitForUpOrCancellation(PointerEventPass.Initial)
                                            ?: return@awaitEachGesture
                                        if ((up.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                            return@awaitEachGesture
                                        }
                                        val layout = bodyLayout ?: return@awaitEachGesture
                                        val pos = up.position
                                        val shownOffset = layout.getOffsetForPosition(pos)
                                        val line = layout.getLineForOffset(shownOffset)
                                        val offset = planState.value.toOriginal(shownOffset)
                                        val onText = pos.y >= layout.getLineTop(line) &&
                                                pos.y <= layout.getLineBottom(line) &&
                                                pos.x >= layout.getLineLeft(line) &&
                                                pos.x <= layout.getLineRight(line)
                                        if (!onText) return@awaitEachGesture

                                        val span = scriptureSpansState.value.firstOrNull {
                                            offset >= it.first && offset <= it.last + 1
                                        } ?: return@awaitEachGesture
                                        // ranges are computed a moment after typing; ignore stale ones
                                        val liveContent = contentState.value
                                        if (span.last + 1 > liveContent.length) return@awaitEachGesture
                                        val raw = liveContent.substring(span.first, span.last + 1)
                                        val ref = ScriptureDetector.extractReferences(raw).firstOrNull()
                                        if (ref != null) {
                                            focusManager.clearFocus()
                                            selectedReference = ref
                                        }
                                    }
                                }
                        )

                        // Inline images are drawn over the space reserved for them.
                        // Positions come from `synced`, so layout and plan always belong together
                        // (no one-frame jump when the keyboard suggestion / deletion changes the text).
                        val s = synced
                        if (s != null) {
                            placements.forEach { p ->
                                val start = s.starts[p.idx] ?: return@forEach
                                val shownStart = s.plan.spanStart(start)
                                val textLen = s.layout.layoutInput.text.length
                                if (shownStart >= textLen) return@forEach
                                key(p.idx) {
                                    val line = s.layout.getLineForOffset(shownStart)
                                    val top = s.layout.getLineTop(line)
                                    val lineH = s.layout.getLineBottom(line) - top
                                    val anchor = s.plan.anchors[start]
                                    val y = if (anchor != null) {
                                        // text flows beside the picture: it starts level with the first text line
                                        val shownAnchor = s.plan.toShown(anchor).coerceAtMost(textLen)
                                        s.layout.getLineTop(s.layout.getLineForOffset(shownAnchor))
                                    } else {
                                        top + (lineH - p.heightPx) / 2f
                                    }
                                    val x = when (p.align) {
                                        0 -> 0f
                                        1 -> (bodyWidthPx - p.widthPx) / 2f
                                        else -> bodyWidthPx - p.widthPx
                                    }
                                    PlacedImage(
                                        name = p.name,
                                        description = if (editing) {
                                            "Image ${p.idx + 1}. Tap for options"
                                        } else {
                                            "Image ${p.idx + 1}. Tap to view full screen"
                                        },
                                        x = x,
                                        y = y,
                                        widthPx = p.widthPx,
                                        heightPx = p.heightPx,
                                        rounded = p.rounded,
                                        selected = editing && panel == Panel.Image && imagePanelIdx == p.idx,
                                        clickLabel = if (editing) "Image options" else "View full screen",
                                        onClick = {
                                            if (editing) selectImage(p.idx)
                                            else viewerImage = images.getOrNull(p.idx)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Images still being copied in
                    if (importing > 0) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(importing) { ImagePlaceholder() }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Detected scriptures (only shown when something is found)
                    if (detectedReferences.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Detected Scriptures (${detectedReferences.size})",
                            fontFamily = Lexend,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            detectedReferences.forEach { ref ->
                                AssistChip(
                                    onClick = { selectedReference = ref },
                                    label = {
                                        Text(
                                            ref.localizedKey,
                                            fontFamily = Lexend,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Book,
                                            contentDescription = null,
                                            tint = Purple
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = SearchBg
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ------------------------- Controls bar (+ format panel) -------------------------
                if (editing) Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainer)
                        .bottomInset(barBottomPx)
                ) {
                    HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showAddScripture = true },
                            contentPadding = PaddingValues(start = 12.dp, end = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Add Scripture", fontFamily = Lexend, fontWeight = FontWeight.Medium)
                        }
                        Tip("Text format") {
                            BarToggle(
                                checked = panel == Panel.Format,
                                onClick = { togglePanel(Panel.Format) }
                            ) {
                                Text(
                                    "Aa",
                                    fontFamily = Lexend,
                                    fontSize = 20.sp,
                                    fontWeight = if (panel == Panel.Format) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        Tip("Align and lists") {
                            BarToggle(
                                checked = panel == Panel.Align,
                                onClick = { togglePanel(Panel.Align) }
                            ) {
                                Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align and lists")
                            }
                        }
                        TipIconButton(
                            label = "Add image",
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Add image",
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = panel != Panel.None,
                        enter = expandVertically(Motion.spatialDefault<androidx.compose.ui.unit.IntSize>()) +
                                fadeIn(Motion.effectsFast),
                        exit = shrinkVertically(Motion.spatialFast<androidx.compose.ui.unit.IntSize>()) +
                                fadeOut(Motion.effectsFast)
                    ) {
                        when (lastPanel) {
                            Panel.Align -> AlignPanel(
                                align = bodyAlign,
                                onAlign = { setAlign(it) },
                                activeList = activeList,
                                onList = { applyList(it) },
                                onIndent = { indentSelection(it) }
                            )
                            Panel.Image -> {
                                val idx = imagePanelIdx
                                val sp = spans.firstOrNull {
                                    it.type == FmtType.Image && imgIdx(it.arg) == idx
                                }
                                if (idx == null || sp == null) {
                                    Spacer(Modifier.fillMaxWidth().height(PanelHeight))
                                } else {
                                    ImagePanel(
                                        pct = imgPct(sp.arg),
                                        align = imgAlign(sp.arg),
                                        rounded = imgRounded(sp.arg),
                                        wrap = imgWrap(sp.arg),
                                        wrapEnabled = bodyAlign == TextAlign.Start,
                                        onPct = { updateImage(idx, pct = it) },
                                        onAlign = { a ->
                                            updateImage(idx, align = a, wrap = if (a == 1) false else null)
                                        },
                                        onRounded = { updateImage(idx, rounded = it) },
                                        onWrap = { on ->
                                            updateImage(
                                                idx,
                                                wrap = on,
                                                pct = if (on && imgPct(sp.arg) > 55) 45 else null,
                                                align = if (on && imgAlign(sp.arg) == 1) 0 else null
                                            )
                                        },
                                        onRotate = { rotateImage(idx) },
                                        onView = { viewerImage = images.getOrNull(idx) },
                                        onRemove = { pendingRemoveIdx = idx }
                                    )
                                }
                            }
                            else -> FormatPanel(
                                isOn = ::isOn,
                                onToggle = ::toggleFmt,
                                sizeSelected = TextSize.values()
                                    .getOrNull(argOf(FmtType.Size) ?: 0) ?: TextSize.Normal,
                                onSize = { setFmt(FmtType.Size, it.ordinal, it == TextSize.Normal) },
                                colorSelected = argOf(FmtType.Color) ?: InkBlack.toArgb(),
                                onColor = { setFmt(FmtType.Color, it.toArgb(), it == InkBlack) }
                            )
                        }
                    }
                } else {
                    // View mode has no controls bar: keep the text clear of the nav bar / keyboard
                    Spacer(Modifier.bottomInset(barBottomPx))
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .bottomInset(barBottomPx, extraPx = with(density) { (if (editing) 64.dp else 16.dp).roundToPx() })
        )
    } // end root Box

    if (showAddScripture) {
        // Full-screen book -> chapter -> verse picker. The chosen verse is typed into the note
        // as plain text, e.g. "Proverbs 1:2" (or "సామెతలు 1:2" with a Telugu Bible).
        BookBrowserDialog(
            onClose = { showAddScripture = false },
            onVersePicked = { reference ->
                showAddScripture = false
                insertScripture(reference)
                runCatching { bodyFocus.requestFocus() }
                keyboard?.show()
            }
        )
    }

    if (showDiscardDialog) {
        // Two actions (Material 3 dialog rule). Tapping outside / Back = keep editing.
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Save changes?", fontFamily = Lexend) },
            text = {
                Text(
                    "You have unsaved changes in this note. Save them, or discard them and leave. " +
                            "Tap outside this box to keep editing.",
                    fontFamily = Lexend
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        discardAndExit()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("Discard", fontFamily = Lexend) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        saveAndExit()
                    },
                    enabled = canSave
                ) { Text("Save", fontFamily = Lexend) }
            }
        )
    }

    if (showFolderSheet) {
        FolderSheet(
            folders = categoryOptions,
            selected = category,
            colorOf = folderColorOf,
            onSelect = { if (category != it) pushUndo(); category = it },
            onNewFolder = { showNewCategory = true },
            onDismiss = { showFolderSheet = false }
        )
    }

    if (showNewCategory) {
        NewFolderDialog(
            existing = categoryOptions,
            onDismiss = { showNewCategory = false },
            onCreate = { name, color ->
                pushUndo()
                customCategories = customCategories + name
                customFolderColors = customFolderColors + "$name\u001F${color.toArgb()}"
                category = name
                showNewCategory = false
                onCreateFolder(name, color.toArgb())
            }
        )
    }

    pendingRemoveIdx?.let { idx ->
        AlertDialog(
            onDismissRequest = { pendingRemoveIdx = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Remove image?", fontFamily = Lexend) },
            text = {
                Text(
                    "The image will be removed from this note. You can bring it back with Undo.",
                    fontFamily = Lexend
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveIdx = null }) {
                    Text("Cancel", fontFamily = Lexend)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoveIdx = null
                        confirmRemoveImage(idx)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("Remove", fontFamily = Lexend) }
            }
        )
    }

    viewerImage?.let { name ->
        ImageViewer(name = name, onDismiss = { viewerImage = null })
    }

    selectedReference?.let { ref ->
        ScriptureBottomSheet(
            reference = ref,
            onDismiss = { selectedReference = null }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Attached image
// ---------------------------------------------------------------------------------------------

private sealed interface ImageLoad {
    object Loading : ImageLoad
    object Failed : ImageLoad
    class Loaded(val bitmap: ImageBitmap) : ImageLoad
}

@Composable
private fun rememberImageLoad(name: String): ImageLoad {
    // Cached bitmaps show instantly, so re-composition never flashes a spinner.
    val initial = remember(name) {
        NoteImages.peek(name)?.let { ImageLoad.Loaded(it.asImageBitmap()) }
    }
    val state by produceState<ImageLoad>(initial ?: ImageLoad.Loading, name) {
        if (initial == null) {
            value = NoteImages.load(name)?.let { ImageLoad.Loaded(it.asImageBitmap()) }
                ?: ImageLoad.Failed
        }
    }
    return state
}

/** An inline image drawn over its reserved space. Tap = select it and open the image panel. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlacedImage(
    name: String,
    description: String,
    x: Float,
    y: Float,
    widthPx: Float,
    heightPx: Float,
    rounded: Boolean,
    selected: Boolean,
    clickLabel: String,
    onClick: () -> Unit
) {
    val load = rememberImageLoad(name)
    val density = LocalDensity.current
    val shape = RoundedCornerShape(if (rounded) 16.dp else 0.dp)
    // The docked panel shrinks the editor; scroll so the selected picture stays visible above it.
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) {
        if (selected) {
            delay(320)
            runCatching { requester.bringIntoView() }
        }
    }
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            .bringIntoViewRequester(requester)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClickLabel = clickLabel, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (load) {
            is ImageLoad.Loaded -> Image(
                bitmap = load.bitmap,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            ImageLoad.Loading -> CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
            ImageLoad.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Image unavailable",
                    fontFamily = Lexend,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Shown while a picked image is being copied into the app. */
@Composable
private fun ImagePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
    }
}

/**
 * Full-screen viewer: pinch to zoom, drag to pan (kept inside the picture), double tap to zoom
 * in / back out, close button top-left.
 */
@Composable
private fun ImageViewer(name: String, onDismiss: () -> Unit) {
    val load = rememberImageLoad(name)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    /** Panning never moves the picture further than its zoomed edge. */
    fun clamp(o: Offset, s: Float): Offset {
        val maxX = boxSize.width * (s - 1f) / 2f
        val maxY = boxSize.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    // The full-screen viewer fades in from the note instead of popping up.
    val viewerAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { viewerAlpha.animateTo(1f, androidx.compose.animation.core.tween(220)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = viewerAlpha.value }
                .background(Color.Black)
                .onSizeChanged { boxSize = it }
        ) {
            when (load) {
                is ImageLoad.Loaded -> Image(
                    bitmap = load.bitmap,
                    contentDescription = "Image, full screen. Double tap to zoom",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val next = (scale * zoom).coerceIn(1f, 5f)
                                scale = next
                                offset = if (next <= 1f) Offset.Zero else clamp(offset + pan, next)
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
                ImageLoad.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                ImageLoad.Failed -> Text(
                    "Image unavailable",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

/**
 * Docked image panel - same slot, height and card style as the Aa and Align panels, so the picture
 * stays visible (and updates live) while it is adjusted. Opens from any other panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePanel(
    pct: Int,
    align: Int,
    rounded: Boolean,
    wrap: Boolean,
    wrapEnabled: Boolean,
    onPct: (Int) -> Unit,
    onAlign: (Int) -> Unit,
    onRounded: (Boolean) -> Unit,
    onWrap: (Boolean) -> Unit,
    onRotate: () -> Unit,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Image",
                fontFamily = Lexend,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            FilledTonalIconButton(onClick = onRotate) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate image")
            }
            FilledTonalIconButton(onClick = onView) {
                Icon(Icons.Default.Fullscreen, contentDescription = "View full screen")
            }
            FilledTonalIconButton(
                onClick = onRemove,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.errorContainer,
                    contentColor = colors.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove image")
            }
        }

        ImageSection(title = "Size  \u00B7  $pct%") {
            Slider(
                value = pct.toFloat(),
                onValueChange = { onPct(it.roundToInt()) },
                valueRange = 20f..100f
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Small" to 30, "Medium" to 50, "Large" to 75, "Full" to 100)
                    .forEach { (label, value) ->
                        FilterChip(
                            selected = pct == value,
                            onClick = { onPct(value) },
                            label = { Text(label, fontFamily = Lexend, fontSize = 13.sp) }
                        )
                    }
            }
        }

        ImageSection(title = "Position") {
            val positions = listOf(0 to "Left", 1 to "Center", 2 to "Right")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                positions.forEachIndexed { i, (value, label) ->
                    SegmentedButton(
                        selected = align == value,
                        onClick = { onAlign(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = positions.size),
                        label = { Text(label, fontFamily = Lexend, fontSize = 13.sp) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = wrap,
                    enabled = wrapEnabled,
                    onClick = { onWrap(!wrap) },
                    label = { Text("Text beside", fontFamily = Lexend, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.WrapText,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
                FilterChip(
                    selected = rounded,
                    onClick = { onRounded(!rounded) },
                    label = { Text("Rounded", fontFamily = Lexend, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.RoundedCorner,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
            }
            Text(
                text = when {
                    !wrapEnabled -> "Set text alignment to left to put text beside an image."
                    wrap && align == 2 -> "Text flows in the empty space on the left of the image."
                    wrap -> "Text flows in the empty space on the right of the image."
                    else -> "Turn on \u201CText beside\u201D to write in the empty space next to a smaller image."
                },
                fontFamily = Lexend,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** Rounded section card used inside the image panel. */
@Composable
private fun ImageSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                fontFamily = Lexend,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            content()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared M3 building blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun BarToggle(
    checked: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = colors.onSurfaceVariant,
            checkedContainerColor = SelectedTint,
            checkedContentColor = colors.onSurface
        ),
        content = content
    )
}

/** Rounded card used by both bottom panels (keeps them visually identical). */
@Composable
private fun PanelCard(
    modifier: Modifier,
    title: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (title != null) {
                Text(
                    title,
                    fontFamily = Lexend,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            content()
        }
    }
}

@Composable
private fun PanelTile(
    modifier: Modifier,
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SelectedTint else Color.Transparent)
            .selectable(selected = selected, enabled = enabled, role = role, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

// ---------------------------------------------------------------------------------------------
// Format panel (B / I / U / S / highlight, font size, font colour)
// ---------------------------------------------------------------------------------------------

private val toggleItems: List<Triple<FmtType, ImageVector, String>> = listOf(
    Triple(FmtType.Bold, Icons.Default.FormatBold, "Bold"),
    Triple(FmtType.Italic, Icons.Default.FormatItalic, "Italic"),
    Triple(FmtType.Underline, Icons.Default.FormatUnderlined, "Underline"),
    Triple(FmtType.Strike, Icons.Default.StrikethroughS, "Strikethrough"),
    Triple(FmtType.Highlight, Icons.Default.Highlight, "Highlight")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatPanel(
    isOn: (FmtType) -> Boolean,
    onToggle: (FmtType) -> Unit,
    sizeSelected: TextSize,
    onSize: (TextSize) -> Unit,
    colorSelected: Int,
    onColor: (Color) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Style toggles
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainerLowest
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                toggleItems.forEach { (type, icon, label) ->
                    val on = isOn(type)
                    PanelTile(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        selected = on,
                        role = Role.Checkbox,
                        onClick = { onToggle(type) }
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (on) colors.onSurface else colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Font size
        PanelCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f),
            title = "Font size"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextSize.values().forEach { size ->
                    FilterChip(
                        selected = size == sizeSelected,
                        onClick = { onSize(size) },
                        label = { Text(size.label, fontFamily = Lexend, fontSize = 13.sp) }
                    )
                }
            }
        }

        // Font colour
        PanelCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f),
            title = "Font color"
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FontColors.forEach { (c, label) ->
                    val selected = c.toArgb() == colorSelected
                    // "Default" follows the theme, so it never disappears on a dark panel
                    val shown = if (c == InkBlack) colors.onSurface else c
                    // 44dp touch target around a 32dp swatch
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onColor(c) }
                            )
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(shown)
                                .border(1.dp, colors.outline.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null, // selection is announced by selectable()
                                    tint = if (shown.luminance() > 0.5f) InkBlack else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Align panel (alignment, list types, indent). Same height as the format panel.
// ---------------------------------------------------------------------------------------------

private val alignItems = listOf(
    Triple(TextAlign.Start, Icons.Default.FormatAlignLeft, "Align left"),
    Triple(TextAlign.Center, Icons.Default.FormatAlignCenter, "Align center"),
    Triple(TextAlign.End, Icons.Default.FormatAlignRight, "Align right")
)

private val listItems = listOf(
    Triple(ListKind.Bullet, Icons.Default.FormatListBulleted, "Bullets"),
    Triple(ListKind.Number, Icons.Default.FormatListNumbered, "Numbers"),
    Triple(ListKind.Letter, Icons.Default.SortByAlpha, "Letters")
)

private val indentItems = listOf(
    Triple(-1, Icons.AutoMirrored.Filled.FormatIndentDecrease, "Outdent"),
    Triple(1, Icons.AutoMirrored.Filled.FormatIndentIncrease, "Indent")
)

@Composable
private fun AlignPanel(
    align: TextAlign,
    onAlign: (TextAlign) -> Unit,
    activeList: ListKind?,
    onList: (ListKind) -> Unit,
    onIndent: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PanelCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            title = "Alignment"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                alignItems.forEach { (value, icon, label) ->
                    val selected = value == align
                    PanelTile(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onAlign(value) }
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selected) colors.onSurface else colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        PanelCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.6f),
            title = "Lists"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listItems.forEach { (kind, icon, label) ->
                        val selected = kind == activeList
                        val tint = if (selected) colors.onSurface else colors.onSurfaceVariant
                        PanelTile(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onList(kind) }
                        ) {
                            Icon(icon, contentDescription = null, tint = tint)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontFamily = Lexend,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = tint,
                                maxLines = 1
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    indentItems.forEach { (delta, icon, label) ->
                        val enabled = activeList != null
                        val tint = if (enabled) colors.onSurfaceVariant
                        else colors.onSurface.copy(alpha = 0.38f)
                        PanelTile(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            selected = false,
                            role = Role.Button,
                            enabled = enabled,
                            onClick = { onIndent(delta) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, tint = tint)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    label,
                                    fontFamily = Lexend,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tint,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tonal pill showing the note's folder; tapping it opens the "Move note to" bottom sheet. */
@Composable
private fun CategorySelector(
    selected: String,
    folderColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        modifier = Modifier
            .minimumInteractiveComponentSize() // 48dp touch target around the 32dp pill
            .heightIn(min = 32.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 10.dp,
                end = if (enabled) 6.dp else 12.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = if (selected == DEFAULT_CATEGORY) colors.onSurfaceVariant else folderColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                selected,
                fontFamily = Lexend,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
            if (enabled) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change folder",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// "Move note to" modal bottom sheet. The sheet wraps its content, so one or two folders give a
// small sheet; a long list is capped at FolderListMaxHeight and scrolls inside the sheet instead of
// growing to full screen.
private val FolderRowMinHeight = 56.dp
private val FolderListMaxHeight = 296.dp   // ~5 rows; the half-visible 6th row hints that it scrolls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSheet(
    folders: List<String>,
    selected: String,
    colorOf: (String) -> Color,
    onSelect: (String) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // "Uncategorized" always comes first, the rest keep their order.
    val ordered = remember(folders) { folders.sortedBy { if (it == DEFAULT_CATEGORY) 0 else 1 } }

    // Slide the sheet away first, then report back (so there is no abrupt disappearance).
    fun close(after: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Title + close
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Move note to",
                    fontFamily = Lexend,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = colors.onSurface
                )
                IconButton(
                    onClick = { close() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurface)
                }
            }

            // Folder list: wraps content up to FolderListMaxHeight, then scrolls.
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = FolderListMaxHeight)
            ) {
                LazyColumn {
                    itemsIndexed(ordered, key = { _, name -> name }) { index, name ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = colors.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        FolderRow(
                            name = name,
                            isDefault = name == DEFAULT_CATEGORY,
                            color = colorOf(name),
                            selected = name == selected,
                            onClick = { close { onSelect(name) } }
                        )
                    }
                }
            }

            // New folder
            TextButton(
                onClick = { close { onNewFolder() } },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("New folder", fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    isDefault: Boolean,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FolderRowMinHeight)
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDefault) colors.surfaceContainerHigh else color.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            if (isDefault) {
                Text("?", fontFamily = Lexend, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.onSurfaceVariant)
            } else {
                Icon(Icons.Default.Folder, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            name,
            fontFamily = Lexend,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) colors.onSecondaryContainer else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = colors.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Long-press label (tooltip) around any control. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content
    )
}

@Composable
private fun TipIconButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Tip(label) {
        IconButton(onClick = onClick, enabled = enabled, content = content)
    }
}

/** Primary top-bar action (Edit / Done): M3 filled tonal icon button with a long-press label. */
@Composable
private fun TipTonalIconButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Tip(label) {
        FilledTonalIconButton(onClick = onClick, enabled = enabled, content = content)
    }
}

@Composable
private fun MetaDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(EditorMeta)
    )
}

// ---------------------------------------------------------------------------------------------
// Scripture detection inside the body text
// ---------------------------------------------------------------------------------------------

// "1:2", "1:1-5,7,3:7", "3:16; 4:2" - bare 1-3 before a word is the next book's number, not a verse
private val verseRegex = Regex(
    """\d{1,3}:\d{1,3}(?!\d)(?:\s?[-–]\s?\d{1,3}(?!\d))?""" +
            """(?:\s?[,;]\s?(?![1-3](?!\d)\s*\p{L})\d{1,3}(?!\d)(?::\d{1,3}(?!\d))?(?:\s?[-–]\s?\d{1,3}(?!\d))?)*"""
)
private val tokenRegex = Regex("""\S+""")
private val bookNumbers = setOf("1", "2", "3")
private const val LOOKBACK_CHARS = 160

/**
 * Finds the character ranges of scripture references in [text].
 * For each "chapter:verse" it looks at up to 4 words before it and keeps the shortest
 * candidate that ScriptureDetector accepts (so "see John 3:16" highlights only "John 3:16"),
 * extending to a leading book number when valid ("1 John 3:16", "2 Timothy 3:16").
 */
private fun findScriptureSpans(text: String): List<IntRange> {
    if (text.isBlank()) return emptyList()
    val result = mutableListOf<IntRange>()

    for (m in verseRegex.findAll(text)) {
        val verseEnd = m.range.last + 1
        // Only the last few words before the verse matter, so look back a bounded window. The old code
        // re-tokenized the whole text before every match (quadratic on long notes).
        val windowStart = maxOf(0, m.range.first - LOOKBACK_CHARS)
        val window = text.substring(windowStart, m.range.first)
        val found = tokenRegex.findAll(window).toList()
        // A word cut off by the window start is incomplete: ignore it.
        val whole = if (windowStart > 0 && found.isNotEmpty() && found.first().range.first == 0 &&
            !text[windowStart - 1].isWhitespace()
        ) found.drop(1) else found
        // (word, absolute start offset)
        val tokens = whole.takeLast(5).map { it.value to (it.range.first + windowStart) }
        if (tokens.isEmpty()) continue

        fun candidateStart(n: Int): Int? {
            if (n > tokens.size) return null
            val start = tokens[tokens.size - n].second
            val candidate = text.substring(start, verseEnd)
            return if ('\n' in candidate) null else start
        }

        fun isValid(start: Int) =
            ScriptureDetector.extractReferences(text.substring(start, verseEnd)).isNotEmpty()

        var bestN = 0
        for (n in 1..4) {
            val s = candidateStart(n) ?: break
            if (isValid(s)) { bestN = n; break }
        }
        if (bestN == 0) continue

        val prev = tokens.getOrNull(tokens.size - bestN - 1)
        if (prev != null && prev.first in bookNumbers) {
            val s = candidateStart(bestN + 1)
            if (s != null && isValid(s)) bestN += 1
        }

        val start = candidateStart(bestN) ?: continue
        if (result.none { start <= it.last && verseEnd - 1 >= it.first }) {
            result.add(start until verseEnd)
        }
    }
    return result
}

// ---------------------------------------------------------------------------------------------
// Layout helpers
// ---------------------------------------------------------------------------------------------

/**
 * Adds `insetPx() + extraPx` of empty space below the content, like Modifier.padding(bottom = ...),
 * but [insetPx] is read during LAYOUT. A changing value (the animating keyboard) then only re-runs
 * layout instead of recomposing the whole screen on every frame.
 */
private fun Modifier.bottomInset(insetPx: () -> Int, extraPx: Int = 0): Modifier =
    layout { measurable, constraints ->
        val pad = insetPx() + extraPx
        val placeable = measurable.measure(constraints.offset(vertical = -pad))
        layout(placeable.width, placeable.height + pad) { placeable.placeRelative(0, 0) }
    }