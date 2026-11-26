@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.MotionTransitions
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateContentSize
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.theme.RollingCount
import com.harsha.myapplication.ui.theme.ScriptureDetectionMotion
import com.harsha.myapplication.ui.theme.detectionGlow
import com.harsha.myapplication.ui.theme.detectionHalo
import com.harsha.myapplication.ui.theme.detectionIconTilt
import com.harsha.myapplication.ui.theme.detectionPop
import com.harsha.myapplication.ui.theme.detectionShake
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.ui.layout.LayoutCoordinates
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.harsha.myapplication.*
import com.harsha.myapplication.data.DEFAULT_CATEGORIES
import com.harsha.myapplication.data.DEFAULT_CATEGORY
import com.harsha.myapplication.data.formatNoteDate
import com.harsha.myapplication.data.NoteDraft
import com.harsha.myapplication.data.NoteImages
import com.harsha.myapplication.data.NoteItem
import com.harsha.myapplication.data.FolderRepository
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

// Colours come from MaterialTheme.colorScheme: cursor = primary, selected / "on" states =
// secondaryContainer, meta dot = outline. Only content colours (highlight, scripture links) stay fixed.

/** Both bottom panels (Aa + Align) share this exact height so the layout never jumps. */
private val PanelHeight = 360.dp

/**
 * Optional fine-tuning: shaves this much off the space under the controls while the keyboard
 * is open (only needed if your keyboard draws shorter than the height it reports). Default 0.
 */
private val KeyboardGapTrim = 0.dp

private val ScriptureBlue = Color(0xFF1A73E8)
private val ScriptureBlueDark = Color(0xFF8AB4F8)
/** Used when the reference is detected but does not exist in the active Bible. */
private val ScriptureRed = Color(0xFFD93025)
private val ScriptureRedDark = Color(0xFFF28B82)
private val HighlightYellow = Color(0xFFFFF176)

/**
 * Highlight palette (JW Library style). All light pastels so dark text stays readable on them
 * in both themes. A stored arg of 0 means the original yellow, so old notes look unchanged.
 */
private val HighlightColors: List<Pair<Color, String>> = listOf(
    HighlightYellow to "Yellow",
    Color(0xFFA5D6A7) to "Green",
    Color(0xFF90CAF9) to "Blue",
    Color(0xFFF8BBD0) to "Pink",
    Color(0xFFFFCC80) to "Orange",
    Color(0xFFCE93D8) to "Purple"
)

private fun highlightColorOf(arg: Int): Color = if (arg == 0) HighlightYellow else Color(arg)

/** Soft tint behind inserted scripture quotes (works on light and dark surfaces). */
private val QuoteTint = Color(0x22808080)

private const val UNDO_COALESCE_MS = 700L
private const val MAX_UNDO = 100
private const val INDENT_STEP = 2
private const val MAX_INDENT = 8
private const val MAX_PICK = 10

/** Line height = font size x this. 1.4 = a slight gap between lines; 1.2 is tight, 1.6 is airy. */
private const val BodyLineHeightFactor = 1.4f

private enum class Panel { None, Format, Align, Image }

private enum class ListKind { Bullet, Number, Letter, Check }

private const val BOX_OFF = '\u2610' // ☐
private const val BOX_ON = '\u2611'  // ☑

// ---------------------------------------------------------------------------------------------
// List engine: markers, nesting, auto-numbering
// ---------------------------------------------------------------------------------------------

/** Optional leading indent (spaces), then "• ", "1. ", "a. " or a checklist box "☐ " / "☑ ". */
private val listRegex = Regex("""^( {0,8})(• |(\d{1,3})\. |([a-z])\. |([\u2610\u2611]) )""")
private val numberParenShortcut = Regex("""^(\d{1,3})\) $""")
private val letterParenShortcut = Regex("""^([a-z])\) $""")

/** A parsed list marker at the start of a line. [length] covers indent + marker. */
private class ListMarker(
    val indent: Int,
    val kind: ListKind,
    val number: Int,
    val letter: Char,
    val length: Int,
    val checked: Boolean = false
)

private fun markerOf(line: String): ListMarker? {
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

private fun prefixFor(kind: ListKind, idx: Int): String = when (kind) {
    ListKind.Bullet -> "• "
    ListKind.Number -> "$idx. "
    ListKind.Letter -> "${'a' + (idx - 1).coerceIn(0, 25)}. "
    ListKind.Check -> "$BOX_OFF "
}

/** Marker text for the item that follows [m] (same indent). */
private fun nextMarkerText(m: ListMarker): String {
    val pad = " ".repeat(m.indent)
    return pad + when (m.kind) {
        ListKind.Bullet -> "• "
        ListKind.Number -> "${m.number + 1}. "
        ListKind.Letter -> "${minOf(m.letter + 1, 'z')}. "
        ListKind.Check -> "$BOX_OFF "
    }
}

/** Markdown-style typing shortcuts: type the trigger at line start, then a space. */
private fun shortcutPrefix(typed: String): String? = when (typed) {
    "- ", "* " -> "• "
    "[] ", "[ ] " -> "$BOX_OFF "
    "[x] ", "[X] " -> "$BOX_ON "
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

/**
 * [Quote] marks scripture text inserted from the verse sheet. Its arg is the reference's key hash,
 * so the editor can tell which scripture a quote belongs to (needed for "Remove scripture").
 * Kept last so the stored type names / ordering of the older types never change.
 */
private enum class FmtType { Bold, Italic, Underline, Strike, Highlight, Size, Color, Image, Align, Quote }

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
    return false
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
                val type = FmtType.entries.firstOrNull { it.name == o.optString("t") } ?: continue
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
    TextAlign.Justify -> 3
    else -> 0
}

private fun alignFromInt(i: Int): TextAlign = when (i) {
    1 -> TextAlign.Center
    2 -> TextAlign.End
    3 -> TextAlign.Justify
    else -> TextAlign.Start
}

// ---------------------------------------------------------------------------------------------
// Per-paragraph alignment
//
// Every line (paragraph) of the note has its own alignment (0 start, 1 centre, 2 end, 3 justify),
// kept in a list with one entry per line. When text is edited the list follows: a line that is
// split or created by Enter inherits the alignment of the line it came from, joined lines keep the
// first line's alignment. Saved as FmtType.Align spans (one per line) so no schema change is needed.
// ---------------------------------------------------------------------------------------------

private fun lineCount(text: String): Int {
    var n = 1
    for (c in text) if (c == '\n') n++
    return n
}

private fun countNewlines(s: String, from: Int, to: Int): Int {
    var n = 0
    for (i in from until to) if (s[i] == '\n') n++
    return n
}

/** Index of the line that contains [offset]. */
private fun lineIndexAt(text: String, offset: Int): Int =
    countNewlines(text, 0, offset.coerceIn(0, text.length))

/** Makes the list exactly one entry per line of [text]. */
private fun fitAligns(a: List<Int>, text: String): List<Int> {
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
private fun shiftLineAligns(aligns: List<Int>, old: String, new: String, caret: Int = -1): List<Int> {
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
private fun decodeAligns(json: String, text: String, legacy: Int): List<Int> {
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
private fun alignSpansOf(text: String, aligns: List<Int>): List<FormatSpan> {
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
private class ParaSpec(
    val start: Int,
    val end: Int,
    val align: TextAlign,
    val rest: TextUnit,
    /** Biggest font size (sp) on the line(s); the line height is derived from it. */
    val size: Float = 16f
)

private fun buildParas(
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
private fun List<ParaSpec>.toShown(plan: DisplayPlan): List<ParaSpec> {
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

private fun countWords(text: String): Int {
    var n = 0
    for (line in text.split('\n')) {
        val m = markerOf(line)
        val body = (if (m != null) line.substring(m.length) else line).replace('\uFFFC', ' ')
        n += body.split(' ', '\t').count { it.isNotBlank() }
    }
    return n
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
private class SizeOverride(val start: Int, val end: Int, val size: TextUnit)

/** One undo step: everything the user can change in the editor. */
private data class EditorSnapshot(
    val title: String,
    val text: String,
    val spans: List<FormatSpan>,
    val selection: TextRange,
    val aligns: List<Int>,
    val category: String,
    val images: List<String>
)

// Savers: editor state survives rotation / process death.
private val spansSaver = Saver<List<FormatSpan>, String>(
    save = { encodeSpans(it) },
    restore = { decodeSpans(it, Int.MAX_VALUE) }
)
private val alignsSaver = Saver<List<Int>, String>(
    save = { it.joinToString("") },
    restore = { str -> str.map { (it - '0').coerceIn(0, 3) } }
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

private val textFmtTypes = FmtType.entries.filter {
    it != FmtType.Image && it != FmtType.Align && it != FmtType.Quote
}

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
private const val ZWSP = "\u200B"

private fun buildDisplayPlan(
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
 * Editor features: bold / italic / underline / strikethrough / highlight / size / colour, clear
 * formatting, PER-PARAGRAPH alignment (left / centre / right / justify), bullet / numbered / lettered
 * lists with a hanging indent, tappable checklists ("[] " shortcut), indent / outdent, inline images,
 * undo / redo, find in note, word + character count, share / copy / delete, scripture detection.
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
    ExperimentalLayoutApi::class
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
    onCreateFolder: (name: String, colorArgb: Int) -> Unit = { _, _ -> },
    /** Folder preselected for a NEW note (e.g. the folder open in the notes list). Ignored for existing notes. */
    initialFolder: String? = null,
    /**
     * Called (after the user confirms) to delete the open note. When null, or for a note that was never
     * saved, the "Delete note" menu item is hidden. The host removes the note and closes the screen,
     * e.g. `onDelete = { id -> NotesRepository.delete(id); navController.popBackStack() }`.
     */
    onDelete: ((noteId: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Make sure the notes store and the saved folders are loaded (no-ops after the first call).
    remember(context) { NotesRepository.init(context); FolderRepository.init(context); true }
    val savedFolders = FolderRepository.folders

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
            decodeSpans(initialNote?.formatting.orEmpty(), rawContent.length)
                .filter { it.type != FmtType.Align }, // alignment is kept per line, not as a text span
            initialNote?.images.orEmpty()
        )
    }
    val initialContent = upgraded.first
    val initialSpans = upgraded.second
    val initialCategory = remember {
        initialNote?.category ?: initialFolder?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY
    }
    val initialImages = remember { initialNote?.images.orEmpty() }
    val initialAligns = remember {
        decodeAligns(initialNote?.formatting.orEmpty(), initialContent, initialNote?.alignment ?: 0)
    }
    val initialFormatting = remember { encodeSpans(initialSpans) }
    val initialUsedNames = remember { usedImageNames(initialSpans, initialImages) }

    // ---- editable state ----
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var bodyValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialContent))
    }
    var spans by rememberSaveable(stateSaver = spansSaver) { mutableStateOf(initialSpans) }
    // One alignment per line (paragraph): 0 left, 1 centre, 2 right, 3 justify.
    var lineAligns by rememberSaveable(stateSaver = alignsSaver) { mutableStateOf(initialAligns) }
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
    var baseAlignKey by rememberSaveable { mutableStateOf(initialAligns.joinToString("")) }
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
    val alignKey = remember(lineAligns) { lineAligns.joinToString("") }
    val isDirty = title != baseTitle ||
            content != baseContent ||
            category != baseCategory ||
            alignKey != baseAlignKey ||
            formattingJson != baseFormatting ||
            usedNames != baseUsedNames
    val canSave = title.isNotBlank() || content.isNotBlank() || usedNames.isNotEmpty()

    var selectedReference by remember { mutableStateOf<ScriptureReference?>(null) }
    var showAddScripture by remember { mutableStateOf(false) }
    var showScriptureList by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var findOpen by rememberSaveable { mutableStateOf(false) }
    var findQuery by rememberSaveable { mutableStateOf("") }
    var findIndex by remember { mutableIntStateOf(0) }
    var replaceText by rememberSaveable { mutableStateOf("") }
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
    val allFolderColors = remember(folderColors, savedFolders) {
        folderColors + savedFolders.associate { it.name to it.colorArgb }
    }
    val categoryOptions = remember(categories, allFolderColors, customCategories, category) {
        (categories + allFolderColors.keys + customCategories + category)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    }
    val folderColorMap = remember(allFolderColors, customFolderColors) {
        val m = allFolderColors.mapValues { Color(it.value) }.toMutableMap()
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
        title, bodyValue.text, spans, bodyValue.selection, lineAligns, category, images
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
        lineAligns = s.aligns
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
            formatting = encodeSpans(cleanSpans + alignSpansOf(bodyValue.text, lineAligns)),
            alignment = lineAligns.lastOrNull() ?: 0,
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
        baseAlignKey = lineAligns.joinToString("")
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
    BackHandler(enabled = findOpen) { findOpen = false }

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
    // Bumped whenever the caret must be scrolled into view AFTER the keyboard has resized the screen
    var revealTick by remember { mutableIntStateOf(0) }

    fun focusBodyAtEnd() {
        panel = Panel.None
        bodyValue = bodyValue.copy(selection = TextRange(bodyValue.text.length))
        runCatching { bodyFocus.requestFocus() }
        keyboard?.show()
        revealTick++
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
        // Alignment is per paragraph: it follows every line that is added, split or joined.
        var la = shiftLineAligns(lineAligns, old, input.text, input.selection.start)
        fun setV(nv: TextFieldValue) {
            if (nv.text != v.text) la = shiftLineAligns(la, v.text, nv.text, nv.selection.start)
            v = nv
        }

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
                    if (line.length == m.length) {
                        forceRenumber = true
                        if (m.indent > 0) {
                            val step = minOf(INDENT_STEP, m.indent)
                            setV(
                                TextFieldValue(
                                    old.removeRange(lineStart, lineStart + step),
                                    TextRange(pos - 1 - step)
                                )
                            )
                        } else {
                            setV(
                                TextFieldValue(
                                    old.removeRange(lineStart, lineStart + m.length),
                                    TextRange(lineStart)
                                )
                            )
                        }
                    } else {
                        val ins = nextMarkerText(m)
                        setV(
                            TextFieldValue(
                                v.text.substring(0, pos) + ins + v.text.substring(pos),
                                TextRange(pos + ins.length)
                            )
                        )
                    }
                }
            } else if (oneInserted && v.text[pos - 1] == ' ') {
                // "- " / "* " -> bullet, "1) " -> "1. ", "a) " -> "a. ", "[] " -> checkbox (works when indented too)
                val ls = lineStartOf(v.text, pos - 1)
                val typed = v.text.substring(ls, pos)
                val trimmed = typed.trimStart(' ')
                val replacement = shortcutPrefix(trimmed)
                if (replacement != null) {
                    val markerStart = ls + (typed.length - trimmed.length)
                    setV(
                        TextFieldValue(
                            v.text.replaceRange(markerStart, pos, replacement),
                            TextRange(markerStart + replacement.length)
                        )
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
                    if (m.indent > 0) {
                        val step = minOf(INDENT_STEP, m.indent)
                        setV(TextFieldValue(old.removeRange(ls, ls + step), TextRange(ls + m.length - step)))
                    } else {
                        setV(TextFieldValue(old.removeRange(ls, le), TextRange(ls)))
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
                    setV(
                        TextFieldValue(
                            t,
                            TextRange(
                                mapOffset(v.selection.start, edits),
                                mapOffset(v.selection.end, edits)
                            )
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
                setV(
                    TextFieldValue(
                        norm.text,
                        TextRange(
                            mapOffset(v.selection.start, norm.edits),
                            mapOffset(v.selection.end, norm.edits)
                        )
                    )
                )
            }
            setV(keepCaretOffImages(v, spans, bodyValue.selection.start))
        }
        lineAligns = fitAligns(la, v.text)
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
        lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, pos + sb.length), t)
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
        lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, sp.start), t)
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

    /** Aligns the lines touched by the selection only (just the caret's line when nothing is selected). */
    fun setAlign(a: TextAlign) {
        val value = alignToInt(a)
        val text = bodyValue.text
        val cur = fitAligns(lineAligns, text)
        val idxs = selectedLineStarts().map { lineIndexAt(text, it) }
        if (idxs.all { cur.getOrElse(it) { 0 } == value }) return
        pushUndo()
        val next = cur.toMutableList()
        idxs.forEach { if (it in next.indices) next[it] = value }
        lineAligns = next
    }

    /** Ticks / unticks the checklist item that starts at [lineStart]. */
    fun toggleCheck(lineStart: Int) {
        val text = bodyValue.text
        if (lineStart < 0 || lineStart >= text.length) return
        val m = markerOf(text.substring(lineStart, lineEndOf(text, lineStart))) ?: return
        if (m.kind != ListKind.Check) return
        val at = lineStart + m.indent
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pushUndo()
        val flipped = if (text[at] == BOX_ON) BOX_OFF else BOX_ON
        bodyValue = TextFieldValue(
            text.substring(0, at) + flipped + text.substring(at + 1),
            bodyValue.selection
        )
    }

    /** Removes bold / italic / underline / strike / highlight / size / colour from the selection. */
    fun clearFormatting() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val s0 = bodyValue.selection.min
        val e0 = bodyValue.selection.max
        if (s0 == e0) {
            typing.clear()
            return
        }
        pushUndo()
        var r = spans
        textFmtTypes.forEach { r = r.subtract(it, s0, e0) }
        spans = r
    }

    /** Title + text of the note without the invisible image placeholders. */
    fun noteAsText(): String = buildString {
        if (title.isNotBlank()) append(title.trim()).append("\n\n")
        append(bodyValue.text.filter { it != OBJ }.trim())
    }

    fun shareNote() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title.trim())
            putExtra(Intent.EXTRA_TEXT, noteAsText())
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, "Share note").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun copyNote() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Note", noteAsText()))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
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

    // Alignment of the line the caret is on (shown as selected in the Align panel)
    val caretLineAlign: TextAlign =
        alignFromInt(lineAligns.getOrElse(lineIndexAt(content, bodyValue.selection.min)) { 0 })

    // ---- find in note ----
    val findMatches: List<IntRange> = remember(content, findQuery, findOpen) {
        if (!findOpen || findQuery.isEmpty()) emptyList() else buildList {
            var i = content.indexOf(findQuery, 0, ignoreCase = true)
            while (i >= 0 && size < 1000) {
                add(i until i + findQuery.length)
                i = content.indexOf(findQuery, i + findQuery.length, ignoreCase = true)
            }
        }
    }
    val findCurrent = if (findMatches.isEmpty()) 0 else findIndex.coerceIn(0, findMatches.size - 1)

    /** Find & replace: replaces the current match, or every match, keeping the text styling in sync. */
    fun replaceMatches(all: Boolean) {
        if (!editing) return
        val targets = if (all) findMatches else listOfNotNull(findMatches.getOrNull(findCurrent))
        if (targets.isEmpty()) return
        val text = bodyValue.text
        val edits = targets.map { TextEdit(it.first, it.last + 1 - it.first, replaceText) }
        pushUndo()
        val (t, r) = applyEdits(text, spans, edits)
        val caret = mapOffset(targets.last().last + 1, edits).coerceIn(0, t.length)
        spans = r
        bodyValue = TextFieldValue(t, TextRange(caret))
    }
    val wordCount = remember(content) { countWords(content) }
    val charCount = remember(content) { content.count { it != OBJ && it != '\n' } }
    val bodyBring = remember { BringIntoViewRequester() }

    // Detection is debounced and runs on a background thread so long notes never freeze typing.
    var detectedReferences by remember { mutableStateOf<List<ScriptureReference>>(emptyList()) }
    var scriptureSpans by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    var invalidScriptureSpans by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    // Drives the "scripture detected" animation (glow in the text, button pop, badge halo).
    val detectionFx = remember { ScriptureDetectionMotion() }
    // Texts of the wrong references from the previous pass, so the shake only plays for NEW ones.
    val lastInvalidTexts = remember { mutableListOf<String>() }
    LaunchedEffect(content, BibleData.version) {
        delay(150)
        // The caret is still inside / at the end of a chapter:verse number ("2:2" on the way to
        // "2:20"). Wait a little longer so a half-typed number is never judged (blue / red).
        // Typing another character restarts this effect, so the wait only ends when the user pauses.
        if (editing) {
            val sel = bodyValue.selection
            val caret = if (sel.collapsed) sel.end else -1
            if (caret in 1..content.length && content[caret - 1].isDigit()) {
                val typingNumber = withContext(Dispatchers.Default) {
                    ScriptureDetector.extractReferences(content)
                        .any { it.startIndex < caret && caret <= it.endIndex }
                }
                if (typingNumber) delay(700)
            }
        }
        val (refs, ranges, invalid) = withContext(Dispatchers.Default) {
            val found = ScriptureDetector.extractReferences(content).toList()
            // The detector's own references are the source of truth: each one is exactly one span, so
            // neighbouring scriptures can never merge and fuzzy / misspelled names are covered too.
            val fromDetector = found
                .map { it.startIndex.coerceAtLeast(0) until it.endIndex.coerceAtMost(content.length) }
                .filter { !it.isEmpty() }
            // The older word-by-word finder stays as a safety net for anything the detector did not claim.
            val fallback = findScriptureSpans(content).filter { r ->
                fromDetector.none { it.first <= r.last && r.first <= it.last }
            }
            val allRanges = (fromDetector + fallback).sortedBy { it.first }
            // Split ranges into valid / invalid against the active Bible.
            val valid = mutableListOf<IntRange>()
            val bad = mutableListOf<IntRange>()
            for (range in allRanges) {
                val overlapping = found.filter {
                    it.startIndex < range.last + 1 && it.endIndex > range.first
                }
                val isBad = overlapping.isNotEmpty() && overlapping.all { !BibleData.isValidPassage(it) }
                if (isBad) bad += range else valid += range
            }
            Triple(found, valid, bad)
        }
        // Play the effect only when a reference was ADDED (count went up), never while one is
        // merely being edited, so typing "John 3:1" -> "John 3:16" does not flash twice.
        val previousTexts = detectedReferences.map { it.rawText }.toMutableList()
        val grew = refs.size > detectedReferences.size
        val fresh = if (grew) {
            ranges.filter { r ->
                val t = content.substring(r.first, (r.last + 1).coerceAtMost(content.length))
                !previousTexts.remove(t)
            }
        } else emptyList()
        detectedReferences = refs
        scriptureSpans = ranges
        invalidScriptureSpans = invalid
        // Wrong reference (does not exist in this Bible): red + shake + reject vibration.
        val oldInvalid = lastInvalidTexts.toMutableList()
        val newInvalid = invalid.filter { r ->
            val t = content.substring(r.first, (r.last + 1).coerceAtMost(content.length))
            !oldInvalid.remove(t)
        }
        lastInvalidTexts.clear()
        invalid.mapTo(lastInvalidTexts) { content.substring(it.first, (it.last + 1).coerceAtMost(content.length)) }
        if (editing && newInvalid.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            detectionFx.triggerInvalid(scope, newInvalid)
        }
        if (grew) {
            // Haptics only for what the user just typed, not when an existing note is opened.
            if (editing) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            detectionFx.trigger(scope, fresh.ifEmpty { ranges.takeLast(1) })
        }
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
                // Open the docked image panel straight away (image icon lights up like Aa / Align).
                // Clearing focus stops the returning keyboard from closing the panel again.
                focusManager.clearFocus()
                selectImage(firstIdx + added.size - 1)
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
        textAlign = TextAlign.Start // real alignment is applied per paragraph (see buildParas)
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
    val invalidScriptureColor = if (darkSurface) ScriptureRedDark else ScriptureRed
    val allScriptureSpans = remember(scriptureSpans, invalidScriptureSpans) {
        scriptureSpans + invalidScriptureSpans
    }
    // Width of a list marker ("1. ", "• ", ...) in the note font: wrapped lines are indented by exactly that much
    val hangCache = remember(measurer, density) { HashMap<String, Float>() }
    val hangOf: (String, Int) -> TextUnit = remember(measurer, density) {
        { prefix: String, sizeIdx: Int ->
            val px = hangCache.getOrPut("$sizeIdx|$prefix") {
                val st = TextStyle(
                    fontFamily = Lexend,
                    fontSize = TextSize.entries.getOrNull(sizeIdx)?.size ?: 16.sp
                )
                val withMarker = measurer.measure(prefix.replace(' ', '\u00A0') + "|", st).size.width
                val barOnly = measurer.measure("|", st).size.width
                (withMarker - barOnly).toFloat().coerceAtLeast(0f)
            }
            with(density) { px.toSp() }
        }
    }
    val paras = remember(content, lineAligns, spans, hangOf) {
        buildParas(content, lineAligns, spans, hangOf)
    }
    // What is drawn = the saved styling + checked items struck through + find matches highlighted
    val displaySpans = remember(spans, content, findMatches, findCurrent) {
        val extra = ArrayList<FormatSpan>()
        if (content.indexOf(BOX_ON) >= 0) {
            var ls = 0
            while (true) {
                val le = lineEndOf(content, ls)
                val m = markerOf(content.substring(ls, le))
                if (m != null && m.kind == ListKind.Check && m.checked && le > ls + m.length) {
                    extra.add(FormatSpan(ls + m.length, le, FmtType.Strike))
                }
                if (le >= content.length) break
                ls = le + 1
            }
        }
        findMatches.forEachIndexed { i, r ->
            extra.add(FormatSpan(r.first, r.last + 1, FmtType.Highlight))
            if (i == findCurrent) {
                extra.add(FormatSpan(r.first, r.last + 1, FmtType.Bold))
                extra.add(FormatSpan(r.first, r.last + 1, FmtType.Underline))
            }
        }
        if (extra.isEmpty()) spans else spans + extra
    }
    val plan = remember(
        content, spans, scriptureSpans, invalidScriptureSpans, placements, bodyWidthPx, paras,
        scriptureColor, invalidScriptureColor, linePerFont
    ) {
        buildDisplayPlan(
            text = content,
            spans = spans,
            scripture = scriptureSpans,
            scriptureColor = scriptureColor,
            placements = placements,
            bodyWidthPx = bodyWidthPx,
            paras = paras,
            measurer = measurer,
            baseStyle = bodyTextStyle,
            linePerFont = linePerFont,
            pxToSp = { px -> with(density) { px.toSp() } },
            marginPx = imageMarginPx,
            gapPx = sideGapPx,
            minTextPx = minTextPx,
            invalidScripture = invalidScriptureSpans,
            invalidScriptureColor = invalidScriptureColor
        )
    }
    val planState = rememberUpdatedState(plan)
    val scriptureSpansState = rememberUpdatedState(allScriptureSpans)
    val detectedReferencesState = rememberUpdatedState(detectedReferences)
    val contentState = rememberUpdatedState(content)
    val placementsState = rememberUpdatedState(placements)
    val editingState = rememberUpdatedState(editing)
    val toggleCheckState = rememberUpdatedState<(Int) -> Unit>({ ls -> toggleCheck(ls) })
    val styleTransformation = remember(
        plan, displaySpans, paras, lineAligns, hangOf, scriptureSpans, invalidScriptureSpans,
        scriptureColor, invalidScriptureColor, placements, linePerFont, darkSurface
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
                    buildStyled(
                        text.text, displaySpans, scriptureSpans, scriptureColor, keep, darkSurface,
                        invalidScriptureSpans, invalidScriptureColor,
                        paras = buildParas(text.text, lineAligns, spans, hangOf)
                    ),
                    OffsetMapping.Identity
                )
            } else {
                TransformedText(
                    buildStyled(
                        plan.shown,
                        displaySpans.map { it.copy(start = plan.spanStart(it.start), end = plan.spanEnd(it.end)) },
                        scriptureSpans.map { plan.spanStart(it.first) until plan.spanEnd(it.last + 1) },
                        scriptureColor,
                        plan.overrides,
                        darkSurface,
                        invalidScriptureSpans.map { plan.spanStart(it.first) until plan.spanEnd(it.last + 1) },
                        invalidScriptureColor,
                        paras = paras.toShown(plan)
                    ),
                    plan.mapping
                )
            }
        }
    }
    LaunchedEffect(findOpen, findCurrent, findMatches) {
        if (!findOpen) return@LaunchedEffect
        val m = findMatches.getOrNull(findCurrent) ?: return@LaunchedEffect
        val sl = synced ?: return@LaunchedEffect
        val len = sl.layout.layoutInput.text.length
        val line = sl.layout.getLineForOffset(sl.plan.toShown(m.first).coerceIn(0, len))
        runCatching {
            bodyBring.bringIntoView(
                Rect(0f, sl.layout.getLineTop(line), sl.layout.size.width.toFloat(), sl.layout.getLineBottom(line))
            )
        }
    }

    // Tap in the empty space below the text: the keyboard opens and shrinks the editor, so wait for it
    // to settle and then scroll the caret into view (otherwise the cursor stays hidden behind the keyboard).
    LaunchedEffect(revealTick) {
        if (revealTick == 0) return@LaunchedEffect
        // follow the keyboard animation: re-check a few times until the layout has settled
        repeat(4) {
            delay(150)
            val layout = bodyLayout ?: return@repeat
            val shown = planState.value.toShown(bodyValue.selection.end.coerceAtLeast(0))
                .coerceIn(0, layout.layoutInput.text.length)
            val r = layout.getCursorRect(shown)
            val pad = with(density) { 32.dp.toPx() }
            runCatching {
                bodyBring.bringIntoView(Rect(r.left, r.top - pad, r.right, r.bottom + pad))
            }
        }
    }

    // "More" menu in the top bar: find, share, copy, delete
    val moreMenu: @Composable () -> Unit = {
        Box {
            TipIconButton(label = "More", onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.widthIn(min = 216.dp),
                offset = DpOffset(0.dp, 8.dp),
                shape = MaterialTheme.shapes.largeIncreased,
            ) {
                MoreMenuItem(
                    label = "Find in note",
                    icon = Icons.Default.Search,
                    onClick = {
                        menuOpen = false
                        panel = Panel.None
                        findOpen = true
                    }
                )
                MoreMenuItem(
                    label = "Share",
                    icon = Icons.Default.Share,
                    enabled = canSave,
                    onClick = {
                        menuOpen = false
                        shareNote()
                    }
                )
                MoreMenuItem(
                    label = "Copy text",
                    icon = Icons.Default.ContentCopy,
                    enabled = canSave,
                    onClick = {
                        menuOpen = false
                        copyNote()
                    }
                )
                if (onDelete != null && noteId != null) {
                    MoreMenuItem(
                        label = "Delete note",
                        icon = Icons.Default.Delete,
                        color = colors.error,
                        onClick = {
                            menuOpen = false
                            showDeleteDialog = true
                        }
                    )
                }
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
                StudyTopBar(
                    title = when {
                        !editing -> "Note"
                        noteId == null -> "New Note"
                        else -> "Edit Note"
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
                            moreMenu()
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
                            moreMenu()
                        }
                        Spacer(Modifier.width(8.dp))
                    },
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
                AnimatedVisibility(
                    visible = findOpen,
                    enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()) +
                            fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()) +
                            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>())
                ) {
                    FindBar(
                        query = findQuery,
                        onQuery = { findQuery = it; findIndex = 0 },
                        count = findMatches.size,
                        index = findCurrent,
                        onPrev = {
                            if (findMatches.isNotEmpty()) {
                                findIndex = (findCurrent - 1 + findMatches.size) % findMatches.size
                            }
                        },
                        onNext = {
                            if (findMatches.isNotEmpty()) findIndex = (findCurrent + 1) % findMatches.size
                        },
                        onClose = { findOpen = false },
                        replaceText = if (editing) replaceText else null,
                        onReplaceText = { replaceText = it.replace("\n", " ") },
                        onReplace = { replaceMatches(false) },
                        onReplaceAll = { replaceMatches(true) }
                    )
                }

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
                ) {
                    val fieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primary
                    )
                    val titleStyle = MaterialTheme.typography.headlineMediumEmphasized.copy(fontFamily = Lexend)

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
                                style = titleStyle,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = titleStyle.copy(color = colors.onSurface),
                        singleLine = true,
                        readOnly = !editing,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusBodyAtEnd() }),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().revealOnEnter(index = 0)
                    )

                    // Date | category
                    Row(
                        modifier = Modifier.revealOnEnter(index = 1).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelLarge,
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

                    AnimatedVisibility(
                        visible = wordCount > 0,
                        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()) +
                                fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
                        exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()) +
                                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>())
                    ) {
                        Text(
                            text = (if (wordCount == 1) "1 word" else "$wordCount words") +
                                    " \u00B7 " + (if (charCount == 1) "1 character" else "$charCount characters"),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Body - BasicTextField so tap positions map exactly onto the text layout.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        val selectionToolbar = remember { SelectionToolbarState() }
                        // Scripture inside the selection (e.g. "John 3:16") -> offer a verse lookup button
                        val selectedRef: ScriptureReference? = run {
                            val sel = bodyValue.selection
                            if (!selectionToolbar.isShown || sel.collapsed || sel.length > 120) null
                            else {
                                val t = bodyValue.text
                                val a = sel.min.coerceIn(0, t.length)
                                val b = sel.max.coerceIn(a, t.length)
                                ScriptureDetector.extractReferences(t.substring(a, b)).firstOrNull()
                                    ?.let { it.copy(startIndex = a + it.startIndex, endIndex = a + it.endIndex) }
                            }
                        }
                        CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides selectionToolbar) {
                            BasicTextField(
                                value = bodyValue,
                                onValueChange = { updateBody(it, byUser = true) },
                                readOnly = !editing,
                                textStyle = bodyTextStyle,
                                cursorBrush = SolidColor(colors.primary),
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
                                    .detectionGlow(detectionFx, scriptureColor) { ranges ->
                                        val sl = synced ?: return@detectionGlow null
                                        val len = sl.layout.layoutInput.text.length
                                        val path = Path()
                                        for (r in ranges) {
                                            val a = sl.plan.spanStart(r.first).coerceIn(0, len)
                                            val b = sl.plan.spanEnd(r.last + 1).coerceIn(0, len)
                                            if (b > a) path.addPath(sl.layout.getPathForRange(a, b))
                                        }
                                        path
                                    }
                                    .detectionShake(detectionFx, invalidScriptureColor) { ranges ->
                                        val sl = synced ?: return@detectionShake null
                                        val len = sl.layout.layoutInput.text.length
                                        val path = Path()
                                        for (r in ranges) {
                                            val a = sl.plan.spanStart(r.first).coerceIn(0, len)
                                            val b = sl.plan.spanEnd(r.last + 1).coerceIn(0, len)
                                            if (b > a) path.addPath(sl.layout.getPathForRange(a, b))
                                        }
                                        path
                                    }
                                    .onSizeChanged { bodyWidthPx = it.width }
                                    .onGloballyPositioned { selectionToolbar.anchor = it }
                                    .focusRequester(bodyFocus)
                                    .bringIntoViewRequester(bodyBring)
                                    // Hardware keyboard: Ctrl+B / I / U / Z / Y / F / S, Ctrl+Shift+L / E / R / J, Tab / Shift+Tab on lists
                                    .onPreviewKeyEvent { e ->
                                        if (!editing || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when {
                                            e.isCtrlPressed -> when (e.key) {
                                                Key.B -> { toggleFmt(FmtType.Bold); true }
                                                Key.I -> { toggleFmt(FmtType.Italic); true }
                                                Key.U -> { toggleFmt(FmtType.Underline); true }
                                                Key.Z -> { if (e.isShiftPressed) redo() else undo(); true }
                                                Key.Y -> { redo(); true }
                                                Key.F -> { panel = Panel.None; findOpen = true; true }
                                                Key.S -> { if (canSave) finishEditing(); true }
                                                // Ctrl+Shift+L / E / R / J = align left / centre / right / justify
                                                Key.L -> if (e.isShiftPressed) { setAlign(TextAlign.Start); true } else false
                                                Key.E -> if (e.isShiftPressed) { setAlign(TextAlign.Center); true } else false
                                                Key.R -> if (e.isShiftPressed) { setAlign(TextAlign.End); true } else false
                                                Key.J -> if (e.isShiftPressed) { setAlign(TextAlign.Justify); true } else false
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

                                            // Tapping the box of a checklist item ticks / unticks it
                                            if (editingState.value) {
                                                val liveText = contentState.value
                                                val ols = lineStartOf(liveText, offset.coerceIn(0, liveText.length))
                                                val mk = markerOf(liveText.substring(ols, lineEndOf(liveText, ols)))
                                                if (mk != null && mk.kind == ListKind.Check) {
                                                    val pl = planState.value
                                                    val shownLen = layout.layoutInput.text.length
                                                    val firstLine = layout.getLineForOffset(
                                                        pl.toShown(ols).coerceAtMost(shownLen)
                                                    )
                                                    val textX = layout.getHorizontalPosition(
                                                        pl.toShown(ols + mk.length).coerceAtMost(shownLen), true
                                                    )
                                                    if (line == firstLine && pos.x <= textX) {
                                                        toggleCheckState.value(ols)
                                                        return@awaitEachGesture
                                                    }
                                                }
                                            }

                                            val span = scriptureSpansState.value.firstOrNull {
                                                offset >= it.first && offset <= it.last + 1
                                            } ?: return@awaitEachGesture
                                            // ranges are computed a moment after typing; ignore stale ones
                                            val liveContent = contentState.value
                                            if (span.last + 1 > liveContent.length) return@awaitEachGesture
                                            // Prefer the already-detected ref (correct absolute indices + confidence).
                                            // closest to the tapped character when several overlap the span
                                            val fromDetected = detectedReferencesState.value
                                                .filter { it.startIndex < span.last + 1 && it.endIndex > span.first }
                                                .minByOrNull {
                                                    when {
                                                        offset < it.startIndex -> it.startIndex - offset
                                                        offset > it.endIndex -> offset - it.endIndex
                                                        else -> 0
                                                    }
                                                }
                                            val ref = fromDetected ?: run {
                                                val raw = liveContent.substring(span.first, span.last + 1)
                                                ScriptureDetector.extractReferences(raw).firstOrNull()?.let {
                                                    // Re-extraction is relative to the span; map indices back.
                                                    it.copy(
                                                        startIndex = span.first + it.startIndex,
                                                        endIndex = span.first + it.endIndex
                                                    )
                                                }
                                            }
                                            if (ref != null) {
                                                focusManager.clearFocus()
                                                selectedReference = ref
                                            }
                                        }
                                    }
                            )
                        }
                        SelectionToolbar(
                            state = selectionToolbar,
                            editing = editing,
                            boldOn = isOn(FmtType.Bold),
                            italicOn = isOn(FmtType.Italic),
                            underlineOn = isOn(FmtType.Underline),
                            highlightArg = argOf(FmtType.Highlight)?.let { highlightColorOf(it).toArgb() },
                            onBold = { toggleFmt(FmtType.Bold) },
                            onItalic = { toggleFmt(FmtType.Italic) },
                            onUnderline = { toggleFmt(FmtType.Underline) },
                            onHighlight = { setFmt(FmtType.Highlight, it.toArgb(), false) },
                            onHighlightClear = { setFmt(FmtType.Highlight, 0, true) },
                            onClearFormatting = { clearFormatting() },
                            onScripture = selectedRef?.let { r ->
                                {
                                    focusManager.clearFocus()
                                    selectedReference = r
                                }
                            },
                            selection = bodyValue.selection
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

                }

                // Detected scriptures: one button pinned at the bottom (not at the end of the text).
                // Hidden while the keyboard is open so it never sits on top of the typing area.
                AnimatedVisibility(
                    visible = detectedReferences.isNotEmpty() && !imeVisible,
                    enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()) +
                            scaleIn(MaterialTheme.motionScheme.fastSpatialSpec<Float>(), initialScale = 0.8f) +
                            fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()) +
                            scaleOut(MaterialTheme.motionScheme.fastSpatialSpec<Float>(), targetScale = 0.9f) +
                            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Same button language as the Add-hours sheet: tonal fill, labelLarge text,
                        // plus a small primary badge that carries the count.
                        FilledTonalButton(
                            onClick = { focusManager.clearFocus(); showScriptureList = true },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .height(56.dp)
                                .detectionPop(detectionFx)
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(ButtonDefaults.IconSize)
                                    .detectionIconTilt(detectionFx)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Detected scriptures", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            // Outer box is NOT clipped so the halo ring can expand past the badge.
                            Box(
                                modifier = Modifier.detectionHalo(detectionFx, colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 22.dp)
                                        .widthIn(min = 22.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                        .padding(horizontal = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RollingCount(count = detectedReferences.size) { n ->
                                        Text(
                                            "$n",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ------------------------- Controls bar (+ format panel) -------------------------
                if (editing) Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            MaterialTheme.shapes.largeIncreased.copy(
                                bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
                                bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
                            )
                        )
                        .background(colors.surfaceContainer)
                        .bottomInset(barBottomPx)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filled with the theme's primary (the app's dark green), like Save / Close.
                        Button(
                            onClick = { showAddScripture = true },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            contentPadding = PaddingValues(start = 12.dp, end = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Add Scripture")
                        }
                        Tip("Text format") {
                            BarToggle(
                                checked = panel == Panel.Format,
                                onClick = { togglePanel(Panel.Format) }
                            ) {
                                Text(
                                    "Aa",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = if (panel == Panel.Format) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        Tip("Align and lists") {
                            BarToggle(
                                checked = panel == Panel.Align,
                                onClick = { togglePanel(Panel.Align) }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Align and lists")
                            }
                        }
                        Tip("Add image") {
                            BarToggle(
                                checked = panel == Panel.Image,
                                onClick = {
                                    if (panel == Panel.Image) {
                                        togglePanel(Panel.Image) // close the image panel
                                    } else {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Add image")
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = panel != Panel.None,
                        enter = expandVertically(Motion.spatialDefault<androidx.compose.ui.unit.IntSize>()) +
                                fadeIn(Motion.effectsFast),
                        exit = shrinkVertically(Motion.spatialFast<androidx.compose.ui.unit.IntSize>()) +
                                fadeOut(Motion.effectsFast)
                    ) {
                        AnimatedContent(
                            targetState = lastPanel,
                            transitionSpec = { MotionTransitions.fadeThrough() },
                            label = "panelSwap"
                        ) { shownPanel ->
                            when (shownPanel) {
                                Panel.Align -> AlignPanel(
                                    align = caretLineAlign,
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
                                            wrapEnabled = lineAligns.getOrElse(
                                                lineIndexAt(content, sp.end + 1)
                                            ) { 0 } == 0,
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
                                    sizeSelected = TextSize.entries
                                        .getOrNull(argOf(FmtType.Size) ?: 0) ?: TextSize.Normal,
                                    onSize = { setFmt(FmtType.Size, it.ordinal, it == TextSize.Normal) },
                                    colorSelected = argOf(FmtType.Color) ?: InkBlack.toArgb(),
                                    onColor = { setFmt(FmtType.Color, it.toArgb(), it == InkBlack) },
                                    highlightSelected = argOf(FmtType.Highlight)?.let { highlightColorOf(it).toArgb() },
                                    onHighlight = { setFmt(FmtType.Highlight, it.toArgb(), false) },
                                    onHighlightClear = { setFmt(FmtType.Highlight, 0, true) },
                                    onClear = { clearFormatting() }
                                )
                            }
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
            shape = MaterialTheme.shapes.extraLarge,
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
                FolderRepository.add(name, color.toArgb())      // saved to disk
                onCreateFolder(name, color.toArgb())
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete this note?", fontFamily = Lexend) },
            text = {
                Text(
                    "The note and its images will be deleted permanently.",
                    fontFamily = Lexend
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontFamily = Lexend)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val id = noteId
                        if (id != null) {
                            // images imported in this session but never saved
                            sessionAdded.forEach { NoteImages.delete(it) }
                            onDelete?.invoke(id)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("Delete", fontFamily = Lexend) }
            }
        )
    }

    pendingRemoveIdx?.let { idx ->
        AlertDialog(
            onDismissRequest = { pendingRemoveIdx = null },
            shape = MaterialTheme.shapes.extraLarge,
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

    if (showScriptureList && detectedReferences.isNotEmpty()) {
        DetectedScripturesScreen(
            references = detectedReferences,
            onClose = { showScriptureList = false },
            onOpen = { selectedReference = it }
        )
    }

    /**
     * The quote block that belongs to [ref], or null when its verse text is not in the note.
     * Quotes sit on their own line(s) right under the line that holds the reference. Tapping the
     * reference *inside* a quote also finds that quote.
     */
    fun quoteSpanFor(ref: ScriptureReference): FormatSpan? {
        val text = bodyValue.text
        val key = ref.localizedKey.hashCode()
        val at = ref.startIndex.coerceIn(0, text.length)
        spans.firstOrNull { it.type == FmtType.Quote && it.arg == key && at >= it.start && at < it.end }
            ?.let { return it }
        var pos = lineEndOf(text, ref.endIndex.coerceIn(0, text.length))
        while (pos < text.length && text[pos] == '\n') {
            val sp = spans.firstOrNull { it.type == FmtType.Quote && it.start == pos + 1 } ?: return null
            if (sp.arg == key) return sp
            pos = sp.end
        }
        return null
    }

    selectedReference?.let { ref ->
        ScriptureBottomSheet(
            reference = ref,
            onDismiss = { selectedReference = null },
            isInNote = quoteSpanFor(ref) != null,
            onInsertQuote = { r, quote ->
                val text = bodyValue.text
                // after the reference's line, below any quotes already added for it
                var pos = lineEndOf(text, r.endIndex.coerceIn(0, text.length))
                while (pos < text.length && text[pos] == '\n') {
                    val sp = spans.firstOrNull { it.type == FmtType.Quote && it.start == pos + 1 } ?: break
                    pos = sp.end
                }
                val insert = "\n" + quote
                pushUndo()
                val (t, shifted) = applyEdits(text, spans, listOf(TextEdit(pos, 0, insert)))
                var next = shifted
                // the quote starts plain: no bold / colour / highlight leaking in from the line above
                textFmtTypes.forEach { next = next.subtract(it, pos, pos + insert.length) }
                spans = next + FormatSpan(pos + 1, pos + insert.length, FmtType.Quote, r.localizedKey.hashCode())
                lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, pos), t)
                bodyValue = TextFieldValue(t, TextRange(pos + insert.length))
                selectedReference = null
            },
            onRemoveQuote = { r ->
                quoteSpanFor(r)?.let { sp ->
                    val text = bodyValue.text
                    val from = (sp.start - 1).coerceAtLeast(0) // also drop the line break before it
                    val to = sp.end.coerceAtMost(text.length)
                    pushUndo()
                    val (t, shifted) = applyEdits(text, spans, listOf(TextEdit(from, to - from, "")))
                    spans = shifted
                    lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, from), t)
                    bodyValue = TextFieldValue(t, TextRange(from.coerceAtMost(t.length)))
                }
                selectedReference = null
            },
            onCorrectName = { start, end, corrected ->
                val text = bodyValue.text
                val s = start.coerceIn(0, text.length)
                val e = end.coerceIn(s, text.length)
                if (s < e && text.substring(s, e) != corrected) {
                    pushUndo()
                    val next = text.replaceRange(s, e, corrected)
                    // updateBody shifts format spans for the text change.
                    updateBody(
                        TextFieldValue(next, TextRange(s + corrected.length)),
                        byUser = false
                    )
                }
                selectedReference = null
            }
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
            ImageLoad.Loading -> LoadingIndicator(modifier = Modifier.size(40.dp))
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
        LoadingIndicator(modifier = Modifier.size(48.dp))
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
    val viewerFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(Unit) { viewerAlpha.animateTo(1f, viewerFade) }

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
                ImageLoad.Loading -> LoadingIndicator(
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
 * Docked image panel - same slot, height and button style as the Aa and Align panels, so the picture
 * stays visible (and updates live) while it is adjusted. Opens from any other panel.
 */
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
    PanelScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Image",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = onRotate, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate image")
            }
            FilledTonalIconButton(onClick = onView, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Default.Fullscreen, contentDescription = "View full screen")
            }
            FilledTonalIconButton(
                onClick = onRemove,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.errorContainer,
                    contentColor = colors.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove image")
            }
        }

        PanelDivider()

        PanelSection("Size  ·  $pct%") {
            Slider(
                value = pct.toFloat(),
                onValueChange = { onPct(it.roundToInt()) },
                valueRange = 20f..100f
            )
            ConnectedButtonGroup(
                items = listOf("Small" to 30, "Medium" to 50, "Large" to 75, "Full" to 100)
                    .map { (label, value) ->
                        GroupItem(
                            description = "$label size",
                            selected = pct == value,
                            onClick = { onPct(value) },
                            label = label
                        )
                    }
            )
        }

        PanelDivider()

        PanelSection("Position") {
            ConnectedButtonGroup(
                items = listOf(0 to "Left", 1 to "Center", 2 to "Right").map { (value, label) ->
                    GroupItem(
                        description = label,
                        selected = align == value,
                        onClick = { onAlign(value) },
                        label = label
                    )
                }
            )
            ConnectedButtonGroup(
                items = listOf(
                    GroupItem(
                        description = "Text beside",
                        selected = wrap,
                        onClick = { onWrap(!wrap) },
                        label = "Text beside",
                        icon = Icons.AutoMirrored.Filled.WrapText,
                        enabled = wrapEnabled,
                        role = Role.Checkbox
                    ),
                    GroupItem(
                        description = "Rounded corners",
                        selected = rounded,
                        onClick = { onRounded(!rounded) },
                        label = "Rounded",
                        icon = Icons.Default.RoundedCorner,
                        role = Role.Checkbox
                    )
                )
            )
            Text(
                text = when {
                    !wrapEnabled -> "Set text alignment to left to put text beside an image."
                    wrap && align == 2 -> "Text flows in the empty space on the left of the image."
                    wrap -> "Text flows in the empty space on the right of the image."
                    else -> "Turn on “Text beside” to write in the empty space next to a smaller image."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
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
            checkedContainerColor = colors.secondaryContainer,
            checkedContentColor = colors.onSecondaryContainer
        ),
        shapes = IconButtonDefaults.toggleableShapes(), // round -> squircle when checked
        content = content
    )
}

// Hours-sheet style building blocks shared by the Aa, Align and Image panels: caption sections
// split by hairline dividers, connected button groups (primary-filled selection, spring-animated
// corners) and ringed colour swatches. Everything comes from MaterialTheme.

/** One button of a [ConnectedButtonGroup]. */
private class GroupItem(
    val description: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val label: String? = null,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val role: Role = Role.RadioButton
)

/**
 * Material 3 Expressive connected button group built from the real [ToggleButton]:
 * equal-width buttons, 2dp apart. Pressing squares a button off, selecting rounds it fully and
 * fills it with primary - the corner morph is driven by the theme's MotionScheme.
 * [stacked] puts the icon above its label (taller buttons, used for the list types).
 */
@Composable
private fun ConnectedButtonGroup(
    items: List<GroupItem>,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    stacked: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val last = items.lastIndex
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val shapes = when {
                last == 0 -> ToggleButtonDefaults.shapesFor(height)
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == last -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = item.selected,
                onCheckedChange = { item.onClick() },
                enabled = item.enabled,
                shapes = shapes,
                colors = ToggleButtonDefaults.colors(
                    containerColor = colors.surfaceContainerHighest,
                    contentColor = colors.onSurface,
                    checkedContainerColor = colors.primary,
                    checkedContentColor = colors.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = height)
                    .semantics { contentDescription = item.description }
            ) {
                val icon = item.icon
                val label = item.label
                if (stacked) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (icon != null) Icon(icon, null, modifier = Modifier.size(20.dp))
                        if (label != null) {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                } else {
                    if (icon != null) Icon(icon, null, modifier = Modifier.size(20.dp))
                    if (icon != null && label != null) Spacer(Modifier.width(6.dp))
                    if (label != null) {
                        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Fixed-height scrolling body of a docked panel (all three panels share [PanelHeight]). */
@Composable
private fun PanelScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** A labelled block: small caption on top, content 8dp below it. */
@Composable
private fun PanelSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun PanelDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SwatchRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

/** Round colour swatch in a 44dp touch target. [color] null = "none" (shows the reset icon). */
@Composable
private fun Swatch(
    color: Color?,
    label: String,
    selected: Boolean,
    checkTint: Color,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val corner by animateDpAsState(
        if (selected) 10.dp else 16.dp,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "swatchCorner"
    )
    val swatchShape = RoundedCornerShape(corner)
    val swatchSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(swatchSource, 0.88f)
            .size(44.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = swatchSource,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(swatchShape)
                .then(if (color != null) Modifier.background(color) else Modifier)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) colors.primary else colors.outline.copy(alpha = 0.5f),
                    swatchShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                color == null -> Icon(
                    Icons.Default.FormatColorReset,
                    contentDescription = null,
                    tint = if (selected) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                selected -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = checkTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Custom text-selection toolbar (replaces the system "Cut / Copy / Paste" bubble)
// ---------------------------------------------------------------------------------------------

private enum class ToolbarMode { Main, Colors, More }

/**
 * Plugged into the text field through [LocalTextContextMenuToolbarProvider] (Compose Foundation 1.9+;
 * the old LocalTextToolbar is no longer used by text fields). While the field wants a menu,
 * [showTextContextMenu] is suspended here and [dataProvider] is set; [SelectionToolbar] reads it
 * (it is snapshot-aware, so the toolbar follows the selection) and draws the actions.
 */
private class SelectionToolbarState : TextContextMenuProvider {
    var dataProvider by mutableStateOf<TextContextMenuDataProvider?>(null)
        private set
    /** Coordinates of the text field; the menu bounds are expressed relative to them. */
    var anchor: LayoutCoordinates? = null
    var mode by mutableStateOf(ToolbarMode.Main)
    /**
     * Where the toolbar sits after a style toggle (Bold / Italic / Underline). Restyling the text
     * changes the reported selection bounds, which made the toolbar jump. While pinned it stays put;
     * it is released as soon as the selection changes.
     */
    var pinnedRect by mutableStateOf<Rect?>(null)
    private var session: TextContextMenuSession? = null

    val isShown: Boolean get() = dataProvider != null

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        val done = CompletableDeferred<Unit>()
        session = object : TextContextMenuSession {
            override fun close() {
                done.complete(Unit)
            }
        }
        this.dataProvider = dataProvider
        try {
            done.await() // stays open until closed here or cancelled by the field (selection cleared)
        } finally {
            this.dataProvider = null
            session = null
            pinnedRect = null
            mode = ToolbarMode.Main
        }
    }

    /** Runs one of the field's own actions (copy / cut / paste / select all). */
    fun click(item: TextContextMenuItem) {
        val s = session ?: return
        item.onClick(s)
    }

    fun hide() {
        session?.close()
    }
}

/**
 * Material 3 selection toolbar. Main row: overlapping highlight swatches (tap = show every colour,
 * all other buttons disappear), bold, underline, copy, verse lookup (only when the selection holds a
 * scripture) and a "more" button. With only a caret (no selection) it offers paste / select all.
 */
@Composable
private fun SelectionToolbar(
    state: SelectionToolbarState,
    editing: Boolean,
    boldOn: Boolean,
    italicOn: Boolean,
    underlineOn: Boolean,
    highlightArg: Int?,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onHighlight: (Color) -> Unit,
    onHighlightClear: () -> Unit,
    onClearFormatting: () -> Unit,
    onScripture: (() -> Unit)?,
    selection: TextRange
) {
    // A new selection (drag a handle, tap elsewhere) releases the pinned position.
    LaunchedEffect(selection) { state.pinnedRect = null }
    val menuData = state.dataProvider ?: return
    val coords = state.anchor?.takeIf { it.isAttached } ?: return
    // Both calls are snapshot-aware: the toolbar moves and updates while the selection changes.
    val bounds = menuData.contentBounds(coords)
    val liveRect = Rect(coords.localToWindow(bounds.topLeft), bounds.size)
    val rect = state.pinnedRect ?: liveRect
    /**
     * UX rule: style TOGGLES (bold / italic / underline) keep the toolbar open and in place, so
     * formats can be stacked and the selected state is visible. One-shot actions (copy, cut, paste,
     * pick a highlight colour, clear formatting) are finished, so they dismiss the toolbar.
     */
    fun keepOpen(action: () -> Unit) {
        state.pinnedRect = rect
        action()
    }
    val items = menuData.data().components.filterIsInstance<TextContextMenuItem>()
    fun actionFor(key: Any): (() -> Unit)? =
        items.firstOrNull { it.key == key }?.let { item -> { state.click(item) } }
    val copyAction = actionFor(TextContextMenuKeys.CopyKey)
    val cutAction = actionFor(TextContextMenuKeys.CutKey)
    val pasteAction = actionFor(TextContextMenuKeys.PasteKey)
    val selectAllAction = actionFor(TextContextMenuKeys.SelectAllKey)
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val marginPx = with(density) { 8.dp.roundToPx() }
    val gapPx = with(density) { 12.dp.roundToPx() }
    val handlePx = with(density) { 28.dp.roundToPx() } // selection handles hang below the text

    val position = remember(rect, topInsetPx, marginPx, gapPx, handlePx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val w = popupContentSize.width
                val h = popupContentSize.height
                val x = (rect.center.x - w / 2f).roundToInt()
                    .coerceIn(marginPx, maxOf(marginPx, windowSize.width - w - marginPx))
                val above = rect.top.roundToInt() - h - gapPx
                val y = if (above >= topInsetPx + marginPx) above
                else rect.bottom.roundToInt() + gapPx + handlePx
                return IntOffset(x, y.coerceIn(topInsetPx, maxOf(topInsetPx, windowSize.height - h - marginPx)))
            }
        }
    }

    // focusable = false keeps the keyboard and the selection exactly as they are while a button is tapped
    Popup(
        popupPositionProvider = position,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            AnimatedContent(
                targetState = state.mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "selectionToolbar"
            ) { mode ->
                val copy = copyAction
                val cut = cutAction
                val paste = pasteAction
                val selectAll = selectAllAction
                val hasSelection = copy != null || cut != null
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        // Just a caret: the only useful actions are paste / select all.
                        !hasSelection -> {
                            if (paste != null) ToolbarButton(Icons.Default.ContentPaste, "Paste") { paste(); state.hide() }
                            if (selectAll != null) ToolbarButton(Icons.Default.SelectAll, "Select all") { selectAll() }
                        }

                        // All highlight colours; everything else is hidden so the swatches get the room.
                        mode == ToolbarMode.Colors && editing -> {
                            ToolbarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { state.mode = ToolbarMode.Main }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .selectable(
                                        selected = highlightArg == null,
                                        role = Role.RadioButton,
                                        onClick = { onHighlightClear(); state.hide() }
                                    )
                                    .semantics { contentDescription = "No highlight" },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, colors.outline.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.FormatColorReset,
                                        contentDescription = null,
                                        tint = if (highlightArg == null) colors.primary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HighlightColors.forEach { (c, label) ->
                                val selected = highlightArg != null && highlightColorOf(highlightArg) == c
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = { onHighlight(c); state.hide() }
                                        )
                                        .semantics { contentDescription = "$label highlight" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(1.dp, colors.outline.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = InkBlack,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Less common actions.
                        mode == ToolbarMode.More -> {
                            ToolbarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { state.mode = ToolbarMode.Main }
                            if (editing) ToolbarButton(Icons.Default.FormatItalic, "Italic", selected = italicOn) { keepOpen(onItalic) }
                            if (cut != null) ToolbarButton(Icons.Default.ContentCut, "Cut") { cut(); state.hide() }
                            if (paste != null) ToolbarButton(Icons.Default.ContentPaste, "Paste") { paste(); state.hide() }
                            if (selectAll != null) ToolbarButton(Icons.Default.SelectAll, "Select all") { selectAll() }
                            if (editing) ToolbarButton(Icons.Default.FormatClear, "Clear formatting") { onClearFormatting(); state.hide() }
                        }

                        else -> {
                            if (editing) {
                                // Fanned swatches like JW Library: yellow in front, the others peeking out
                                // behind it. One tap opens the full colour row.
                                Row(
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable(role = Role.Button) { state.mode = ToolbarMode.Colors }
                                        .semantics { contentDescription = "Highlight colours" }
                                        .padding(start = 10.dp, end = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy((-14).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val fan = HighlightColors.take(4)
                                    fan.forEachIndexed { i, (c, _) ->
                                        Box(
                                            modifier = Modifier
                                                .zIndex((fan.size - i).toFloat()) // first colour on top
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                        )
                                    }
                                }
                                ToolbarButton(Icons.Default.FormatBold, "Bold", selected = boldOn) { keepOpen(onBold) }
                                ToolbarButton(Icons.Default.FormatUnderlined, "Underline", selected = underlineOn) { keepOpen(onUnderline) }
                            }
                            if (copy != null) ToolbarButton(Icons.Default.ContentCopy, "Copy") { copy(); state.hide() }
                            if (onScripture != null) {
                                ToolbarButton(Icons.AutoMirrored.Filled.MenuBook, "Read scripture") { onScripture(); state.hide() }
                            }
                            ToolbarButton(Icons.Default.MoreVert, "More") { state.mode = ToolbarMode.More }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    /** null = plain action (copy, paste ...); true / false = a toggle (bold, italic ...). */
    selected: Boolean? = null,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    if (selected == null) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.onSurface),
            shapes = IconButtonDefaults.shapes()
        ) { Icon(icon, contentDescription = label) }
    } else {
        IconToggleButton(
            checked = selected,
            onCheckedChange = { onClick() },
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = colors.onSurface,
                checkedContainerColor = colors.secondaryContainer,
                checkedContentColor = colors.onSecondaryContainer
            ),
            shapes = IconButtonDefaults.toggleableShapes()
        ) { Icon(icon, contentDescription = label) }
    }
}

// ---------------------------------------------------------------------------------------------
// Format panel (B / I / U / S / highlight, font size, font colour)
// ---------------------------------------------------------------------------------------------

private val toggleItems: List<Triple<FmtType, ImageVector, String>> = listOf(
    Triple(FmtType.Bold, Icons.Default.FormatBold, "Bold"),
    Triple(FmtType.Italic, Icons.Default.FormatItalic, "Italic"),
    Triple(FmtType.Underline, Icons.Default.FormatUnderlined, "Underline"),
    Triple(FmtType.Strike, Icons.Default.StrikethroughS, "Strikethrough")
)

@Composable
private fun FormatPanel(
    isOn: (FmtType) -> Boolean,
    onToggle: (FmtType) -> Unit,
    sizeSelected: TextSize,
    onSize: (TextSize) -> Unit,
    colorSelected: Int,
    onColor: (Color) -> Unit,
    /** ARGB of the highlight under the selection, or null when none. */
    highlightSelected: Int?,
    onHighlight: (Color) -> Unit,
    onHighlightClear: () -> Unit,
    onClear: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    PanelScaffold {
        PanelSection("Style") {
            ConnectedButtonGroup(
                items = toggleItems.map { (type, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = isOn(type),
                        onClick = { onToggle(type) },
                        icon = icon,
                        role = Role.Checkbox
                    )
                } + GroupItem(
                    description = "Clear formatting",
                    selected = false,
                    onClick = onClear,
                    icon = Icons.Default.FormatClear,
                    role = Role.Button
                )
            )
        }

        PanelDivider()

        PanelSection("Font size") {
            ConnectedButtonGroup(
                items = TextSize.entries.map { size ->
                    GroupItem(
                        description = size.label,
                        selected = size == sizeSelected,
                        onClick = { onSize(size) },
                        label = size.label
                    )
                }
            )
        }

        PanelDivider()

        PanelSection("Font color") {
            SwatchRow {
                FontColors.forEach { (c, label) ->
                    // "Default" follows the theme, so it never disappears on a dark panel
                    val shown = if (c == InkBlack) colors.onSurface else c
                    Swatch(
                        color = shown,
                        label = label,
                        selected = c.toArgb() == colorSelected,
                        checkTint = if (shown.luminance() > 0.5f) InkBlack else Color.White,
                        onClick = { onColor(c) }
                    )
                }
            }
        }

        PanelDivider()

        PanelSection("Highlight") {
            SwatchRow {
                Swatch(
                    color = null,
                    label = "No highlight",
                    selected = highlightSelected == null,
                    checkTint = colors.primary,
                    onClick = onHighlightClear
                )
                HighlightColors.forEach { (c, label) ->
                    Swatch(
                        color = c,
                        label = "$label highlight",
                        selected = highlightSelected != null && highlightColorOf(highlightSelected) == c,
                        checkTint = InkBlack,
                        onClick = { onHighlight(c) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Align panel (alignment, list types, indent). Same height as the format panel.
// ---------------------------------------------------------------------------------------------

private val alignItems = listOf(
    Triple(TextAlign.Start, Icons.AutoMirrored.Filled.FormatAlignLeft, "Align left"),
    Triple(TextAlign.Center, Icons.Default.FormatAlignCenter, "Align center"),
    Triple(TextAlign.End, Icons.AutoMirrored.Filled.FormatAlignRight, "Align right"),
    Triple(TextAlign.Justify, Icons.Default.FormatAlignJustify, "Justify")
)

private val listItems = listOf(
    Triple(ListKind.Bullet, Icons.AutoMirrored.Filled.FormatListBulleted, "Bullets"),
    Triple(ListKind.Number, Icons.Default.FormatListNumbered, "Numbers"),
    Triple(ListKind.Letter, Icons.Default.SortByAlpha, "Letters"),
    Triple(ListKind.Check, Icons.Default.CheckBox, "Tasks")
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
    PanelScaffold {
        PanelSection("Alignment") {
            ConnectedButtonGroup(
                items = alignItems.map { (value, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = value == align,
                        onClick = { onAlign(value) },
                        icon = icon
                    )
                }
            )
        }

        PanelDivider()

        PanelSection("Lists") {
            ConnectedButtonGroup(
                items = listItems.map { (kind, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = kind == activeList,
                        onClick = { onList(kind) },
                        label = label,
                        icon = icon
                    )
                },
                height = 56.dp,
                stacked = true
            )
        }

        PanelDivider()

        PanelSection("Indent") {
            ConnectedButtonGroup(
                items = indentItems.map { (delta, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = false,
                        onClick = { onIndent(delta) },
                        label = label,
                        icon = icon,
                        enabled = activeList != null,
                        role = Role.Button
                    )
                }
            )
        }
    }
}

/** Find-in-note bar: query field, "2/5" counter, previous / next and close. */
@Composable
private fun FindBar(
    query: String,
    onQuery: (String) -> Unit,
    count: Int,
    index: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    /** null = read-only (view mode): no replace row. */
    replaceText: String? = null,
    onReplaceText: (String) -> Unit = {},
    onReplace: () -> Unit = {},
    onReplaceAll: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("Find in note", fontFamily = Lexend) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colors.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
            )
            if (query.isNotEmpty()) {
                Text(
                    text = if (count == 0) "0/0" else "${index + 1}/$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
            TipIconButton(label = "Previous match", onClick = onPrev, enabled = count > 0) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
            }
            TipIconButton(label = "Next match", onClick = onNext, enabled = count > 0) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
            }
            TipIconButton(label = "Close find", onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close find")
            }
        }
        if (replaceText != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = replaceText,
                    onValueChange = onReplaceText,
                    placeholder = { Text("Replace with", fontFamily = Lexend) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onReplace() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onReplace,
                    enabled = count > 0,
                    shapes = ButtonDefaults.shapes()
                ) { Text("Replace") }
                TextButton(
                    onClick = onReplaceAll,
                    enabled = count > 0,
                    shapes = ButtonDefaults.shapes()
                ) { Text("All") }
            }
        }
    } // end Column
}

/**
 * Folder button under the note title. Same look as the Add-hours buttons: 40dp tall, fully round,
 * surfaceContainerHighest container; tapping it opens the "Move note to" bottom sheet.
 */
@Composable
private fun CategorySelector(
    selected: String,
    folderColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = colors.surfaceContainerHighest,
        contentColor = colors.onSurface,
        interactionSource = source,
        modifier = Modifier
            .pressScale(source, 0.95f, enabled)
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(start = 14.dp, end = if (enabled) 10.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = if (selected == DEFAULT_CATEGORY) colors.onSurfaceVariant else folderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                selected,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
            if (enabled) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change folder",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// "Move note to" modal bottom sheet. The sheet wraps its content, so one or two folders give a
// small sheet; a long list is capped at FolderListMaxHeight and scrolls inside the sheet instead of
// growing to full screen.
private val FolderRowHeight = 64.dp
private val FolderListMaxHeight = 352.dp   // ~5 rows; the half-visible 6th row hints that it scrolls

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header: title + hint on the left, tonal close button on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Move note to",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )
                    Text(
                        "Choose a folder for this note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = { close() }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Folder cards: wraps content up to FolderListMaxHeight, then scrolls.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = FolderListMaxHeight)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(ordered, key = { _, name -> name }) { _, name ->
                    FolderRow(
                        name = name,
                        isDefault = name == DEFAULT_CATEGORY,
                        color = colorOf(name),
                        selected = name == selected,
                        onClick = { close { onSelect(name) } }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // New folder: same button as "Detected scriptures" (48dp tonal button, icon + labelLarge text),
            // coloured from the theme so it follows the sheet's green.
            FilledTonalButton(
                onClick = { close { onNewFolder() } },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("New folder", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** One folder card: tinted folder tile, name, and a radio-style indicator that fills when selected. */
@Composable
private fun FolderRow(
    name: String,
    isDefault: Boolean,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    // Selected rows round off more (spatial spring), colours fade with the effects spring.
    val corner by animateDpAsState(
        if (selected) 28.dp else 16.dp, motion.defaultSpatialSpec(), label = "folderCorner"
    )
    val shape = RoundedCornerShape(corner)
    val source = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) colors.primaryContainer else colors.surfaceContainerLowest,
        motion.defaultEffectsSpec(),
        label = "folderContainer"
    )
    val borderColor by animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        motion.defaultEffectsSpec(),
        label = "folderBorder"
    )
    val tile = if (isDefault) colors.onSurfaceVariant else color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FolderRowHeight)
            .pressScale(source, 0.97f)
            .clip(shape)
            .background(container)
            .border(1.5.dp, borderColor, shape)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tile.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = tile,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        // Radio-style indicator: empty ring -> filled primary circle with a check
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(colors.primary)
                    else Modifier.border(2.dp, colors.outline, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** One row of the top-bar "More" menu: same look as the Sort menu (Lexend, 14dp rounded row, 8dp inset). */
@Composable
private fun MoreMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
) {
    val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    DropdownMenuItem(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.medium),
        text = {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = Lexend,
                color = if (enabled) tint else tint.copy(alpha = 0.38f),
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else tint.copy(alpha = 0.38f))
        },
        enabled = enabled,
        onClick = onClick,
    )
}

/** Full-screen list of the scriptures found in the note. Tap one to read it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetectedScripturesScreen(
    references: List<ScriptureReference>,
    onClose: () -> Unit,
    onOpen: (ScriptureReference) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
            val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
                topBar = {
                    LargeFlexibleTopAppBar(
                        title = { Text("Detected scriptures", fontFamily = Lexend) },
                        subtitle = {
                            Text(
                                if (references.size == 1) "1 reference found" else "${references.size} references found",
                                fontFamily = Lexend
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                        scrollBehavior = scroll
                    )
                },
                containerColor = colors.surface
            ) { inner ->
                // Connected button list (Add-hours style): 2dp gaps, small inner corners, larger
                // outer corners on the first and last row.
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(references) { index, ref ->
                        val outer = 28.dp
                        val small = 8.dp
                        val shape = RoundedCornerShape(
                            topStart = if (index == 0) outer else small,
                            topEnd = if (index == 0) outer else small,
                            bottomStart = if (index == references.lastIndex) outer else small,
                            bottomEnd = if (index == references.lastIndex) outer else small
                        )
                        val source = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .revealOnEnter(index = index, playOnceKey = "scripture-$index-${ref.localizedKey}")
                                .fillMaxWidth()
                                .pressScale(source, 0.97f)
                                .clip(shape)
                                .background(colors.surfaceContainerHighest)
                                .clickable(
                                    interactionSource = source,
                                    indication = ripple(),
                                    role = Role.Button
                                ) { onOpen(ref) }
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                ref.localizedKey,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.rotate(-90f)
                            )
                        }
                    }
                }
            }
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
        IconButton(
            onClick = onClick,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
            content = content
        )
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
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
            content = content
        )
    }
}

@Composable
private fun MetaDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outline)
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
// Words are split at spaces AND at separators that people type between references with no space
// ("John 3:16,Rom 5:8", "Matt 24:14;Mark 13:10", "(John 3:16)(Rom 5:8)"). Splitting only at spaces glued
// the end of one reference to the book name of the next, so the second one was dropped as overlapping.
private val tokenRegex = Regex("""[^\s,;()\[\]{}/|\u2022&]+""")
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

        // A reference may never reach back into the previous one.
        val prevEnd = result.maxOfOrNull { it.last + 1 } ?: 0

        fun candidateStart(n: Int): Int? {
            if (n > tokens.size) return null
            val start = tokens[tokens.size - n].second
            if (start < prevEnd) return null
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

        // Multi-word book names ("రెండో యోహాను", "మొదటి యోహాను", "మూడో యోహాను", "Song of Solomon"):
        // the last word alone is often a book too ("యోహాను" = John), so the shortest candidate wins
        // and the leading word would be left unhighlighted. Try one or two more words to the left and
        // keep the longer span only when the detector reads it as ONE reference that starts at its
        // first character and is a different book than the shorter one.
        val shortStart = candidateStart(bestN)
        val shortRef = shortStart?.let {
            ScriptureDetector.extractReferences(text.substring(it, verseEnd)).firstOrNull()
        }
        if (shortRef != null) {
            for (extra in 1..2) {
                val n = bestN + extra
                val s = candidateStart(n) ?: break
                val longRef = ScriptureDetector
                    .extractReferences(text.substring(s, verseEnd)).firstOrNull() ?: continue
                if (longRef.startIndex == 0 && longRef.book != shortRef.book) {
                    bestN = n
                    break
                }
            }
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