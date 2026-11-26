@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.richtext

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.*
import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------------------------------------
// Persistence of styling (stored as JSON next to the note text)
// ---------------------------------------------------------------------------------------------

/** Serialises spans. Same-style neighbours are merged; no spans -> empty string. */
internal fun encodeSpans(spans: List<FormatSpan>): String {
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
internal fun decodeSpans(json: String, textLength: Int): List<FormatSpan> {
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

internal fun alignToInt(a: TextAlign): Int = when (a) {
    TextAlign.Center -> 1
    TextAlign.End, TextAlign.Right -> 2
    TextAlign.Justify -> 3
    else -> 0
}

internal fun alignFromInt(i: Int): TextAlign = when (i) {
    1 -> TextAlign.Center
    2 -> TextAlign.End
    3 -> TextAlign.Justify
    else -> TextAlign.Start
}
