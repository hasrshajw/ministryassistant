@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.scripture

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.harsha.myapplication.*

// ---------------------------------------------------------------------------------------------
// Scripture detection inside the body text
// ---------------------------------------------------------------------------------------------

// "1:2", "1:1-5,7,3:7", "3:16; 4:2" - bare 1-3 before a word is the next book's number, not a verse
internal val verseRegex = Regex(
    """\d{1,3}:\d{1,3}(?!\d)(?:\s?[-–]\s?\d{1,3}(?!\d))?""" +
            """(?:\s?[,;]\s?(?![1-3](?!\d)\s*\p{L})\d{1,3}(?!\d)(?::\d{1,3}(?!\d))?(?:\s?[-–]\s?\d{1,3}(?!\d))?)*"""
)
// Words are split at spaces AND at separators that people type between references with no space
// ("John 3:16,Rom 5:8", "Matt 24:14;Mark 13:10", "(John 3:16)(Rom 5:8)"). Splitting only at spaces glued
// the end of one reference to the book name of the next, so the second one was dropped as overlapping.
internal val tokenRegex = Regex("""[^\s,;()\[\]{}/|\u2022&]+""")
internal val bookNumbers = setOf("1", "2", "3")
internal const val LOOKBACK_CHARS = 160

/**
 * Finds the character ranges of scripture references in [text].
 * For each "chapter:verse" it looks at up to 4 words before it and keeps the shortest
 * candidate that ScriptureDetector accepts (so "see John 3:16" highlights only "John 3:16"),
 * extending to a leading book number when valid ("1 John 3:16", "2 Timothy 3:16").
 */
internal fun findScriptureSpans(text: String): List<IntRange> {
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
