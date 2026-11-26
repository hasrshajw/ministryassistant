package com.harsha.myapplication.scripture

import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** One candidate book with a 0..1 confidence score. */
data class BookMatch(val book: String, val score: Float)

/** True when the two best choices are so close that the UI should offer both. */
val List<BookMatch>.isAmbiguous: Boolean
    get() = size > 1 && this[0].score - this[1].score < BookMatcher.AMBIGUITY_MARGIN

/** Suggestions for the word being typed, plus the range of text they would replace. */
class BookSuggestions(val start: Int, val end: Int, val matches: List<BookMatch>)

/**
 * Understands what the user MEANT by a book name:
 *  - 300 romanized spellings per book (telugu_bible_name_variants_300.json) plus English aliases
 *  - sound folding   (ee/i, oo/u, th/t, dh/d, ph/f, k/q/c, j/z, m/n before a consonant)
 *  - consonant skeleton ("krtika" -> Keerthanalu)
 *  - typing in progress ("kirt" -> Keerthanalu)
 *  - typo tolerance (transpositions, missing / extra letters)
 *  - book numbers handled apart from the name: "2 sam", "modati samuyelu", "first timothy",
 *    "1raj", "yoh 2". "2 sam" never returns 1 Samuel.
 *  - a score per book, so callers can show the top 3 when two books are close
 *  - tie-breaking from context (books already used in the note) and from past user choices
 *
 * Pure Kotlin (no Android types). Thread-safe: lookups read an immutable index published
 * atomically; rebuilds happen under a lock.
 */
object BookMatcher {

    const val AMBIGUITY_MARGIN = 0.05f
    private const val DETECT_MIN = 0.80f
    private const val SUGGEST_MIN = 0.50f

    /** The 66 canonical books in Bible order (same order as the variants file). */
    val CANONICAL: List<String> = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
        "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
        "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
        "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
        "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
        "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
        "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
        "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
        "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
    )

    // ------------------------------------------------------------------ number words

    private val NUMBER_WORDS: Map<String, Int> = buildMap {
        fun words(n: Int, list: String) = list.split(' ').forEach { w -> put(w, n) }
        // Includes common Telugu typos (మోదటి / మొదతి / మోదతి) and extra romanizations so
        // "మోదటి సమూయేలు 1:1" still resolves to 1 Samuel.
        words(1, "1 i first 1st modati modatidi modaati modhati modatiokati okati " +
                "మొదటి మోదటి మొదతి మోదతి మొదటిది మోదటిది 1వ 1వది")
        words(2, "2 ii second 2nd rendava rendo rendu rendavadi rendavaadi " +
                "రెండవ రెండో రెండు రెండవది రెండోది 2వ 2వది")
        words(3, "3 iii third 3rd mudava moodava mudo moodo mudavadi moodavadi " +
                "మూడవ మూడో మూడు మూడవది మూడోది 3వ 3వది")
    }
    private val DIGIT_PREFIXED = Regex("""^([123])([\p{L}\p{M}]+)$""")
    private val SPLITTER = Regex("""[\s.\-_]+""")
    private val LEADING_NUMBER = Regex("""^\s*(?:[1-3]|i{1,3}(?=\s))\s*""", RegexOption.IGNORE_CASE)

    /**
     * Filler words that often appear after a Telugu book name ("ప్రకటన గరంథం 1:1",
     * "ఆదికాండము కాండం 1:1"). They are dropped so the real book name is matched.
     * Includes common misspellings of గ్రంథం / కాండము.
     */
    private val FILLER_WORDS: Set<String> = setOf(
        "కాండము", "కాండం", "కాండమ్", "కాండ", "కాండముు",
        "గ్రంథము", "గ్రంథం", "గ్రంథమ్", "గ్రంథ", "గరంథం", "గరంథము", "గరంథమ్", "గరంథ",
        "grandhamu", "grandham", "granthamu", "grantham", "granth", "grandam", "grantamu",
        "kandamu", "kandam", "kaandamu", "kaandam", "kandamuu",
        "book", "books", "scripture", "scriptures", "chapter", "chapters"
    )

    /** Ordinary words that must never be read as a book when they sit in front of "3:16". */
    private val STOP_WORDS: Set<String> = (
            "at in on to is of the a an and or by for as be it we he she you i my me us do no so if up " +
                    "see vs ref chapter chap verse page pg pp time version part section item number nr num step " +
                    "day week month year am pm hour min sec ratio score look class room price track"
            ).split(' ').toSet() + FILLER_WORDS

    private class Parsed(val number: Int, val softNumber: Int, val name: String)

    /**
     * Splits "2 sam", "2sam", "II Samuel", "modati samuyelu", "first timothy", "1st john" into
     * a book number and the name. A trailing 1-3 ("yoh 2") is only a SOFT number: it boosts
     * that numbered book but the unnumbered book stays a candidate.
     * Filler words (గరంథం, కాండము, grandham, …) are dropped so they never pollute the name.
     */
    private fun parse(raw: String): Parsed {
        var toks = raw.trim().lowercase().split(SPLITTER).filter { it.isNotEmpty() }
        var number = 0
        var soft = 0
        if (toks.isNotEmpty()) {
            val first = toks[0]
            val word = NUMBER_WORDS[first]
            if (word != null && toks.size > 1) {
                number = word
                toks = toks.drop(1)
            } else {
                val m = DIGIT_PREFIXED.matchEntire(first)
                if (m != null && NUMBER_WORDS[first] == null) {
                    number = m.groupValues[1].toInt()
                    toks = listOf(m.groupValues[2]) + toks.drop(1)
                }
            }
        }
        if (number == 0 && toks.size > 1) {
            val last = toks.last()
            if (last == "1" || last == "2" || last == "3") {
                soft = last.toInt()
                toks = toks.dropLast(1)
            }
        }
        // Drop fillers so "ప్రకటన గరంథం" → name "ప్రకటన", "ఆదికాండము కాండం" → "ఆదికాండము"
        toks = toks.filter { it !in FILLER_WORDS }
        return Parsed(number, soft, toks.joinToString(" "))
    }

    // ------------------------------------------------------------------ index

    private class Index(
        val names: Array<String>,
        val numbers: IntArray,
        val folded: Array<String>,
        val skeletons: Array<String>,
        val books: Array<IntArray>,
        /** loose key -> how many known spellings of each book share it (see PhoneticFold.looseOfFolded) */
        val looseSupport: Map<String, IntArray>
    )

    @Volatile private var variants: List<List<String>> = emptyList()
    @Volatile private var aliases: Map<String, List<String>> = emptyMap()
    @Volatile private var index: Index = buildIndex(emptyList(), emptyMap())
    private val lock = Any()

    /** "book|foldedQuery" -> times the user picked that book for that spelling. */
    private val learned = ConcurrentHashMap<String, Int>()

    /**
     * Loads telugu_bible_name_variants_300.json (66 keys in Bible order, 300 spellings each).
     * Call once from a background thread, e.g. BibleData.init:
     *   context.assets.open("telugu_bible_name_variants_300.json").use { BookMatcher.loadVariants(it) }
     */
    fun loadVariants(stream: InputStream) {
        val parsed = parseVariantLists(stream.readBytes().toString(Charsets.UTF_8))
        if (parsed.size != CANONICAL.size) return        // wrong file: keep what we have
        synchronized(lock) {
            variants = parsed
            index = buildIndex(variants, aliases)
        }
    }

    /** Every alias the detector knows (English, Telugu script, imported Bible names), canonical -> names. */
    fun setAliases(all: Map<String, List<String>>) {
        synchronized(lock) {
            aliases = all
            index = buildIndex(variants, aliases)
        }
    }

    /** Minimal, order-preserving reader for {"Name": ["a","b",...], ...}. */
    private fun parseVariantLists(text: String): List<List<String>> {
        val out = ArrayList<List<String>>()
        var cur: ArrayList<String>? = null
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when (c) {
                '"' -> {
                    val sb = StringBuilder()
                    i++
                    while (i < n && text[i] != '"') {
                        if (text[i] == '\\' && i + 1 < n) {
                            i++
                            when (val e = text[i]) {
                                'n' -> sb.append('\n')
                                't' -> sb.append('\t')
                                'u' -> {
                                    if (i + 4 < n) {
                                        sb.append(text.substring(i + 1, i + 5).toInt(16).toChar())
                                        i += 4
                                    }
                                }
                                else -> sb.append(e)
                            }
                        } else sb.append(text[i])
                        i++
                    }
                    cur?.add(sb.toString())
                }
                '[' -> cur = ArrayList()
                ']' -> { cur?.let { out.add(it) }; cur = null }
            }
            i++
        }
        return out
    }

    private fun stripBookNumber(raw: String) = LEADING_NUMBER.replaceFirst(raw, "")

    private fun leadingBookNumber(name: String): Int {
        val c = name.trim().firstOrNull() ?: return 0
        return if (c in '1'..'3') c - '0' else 0
    }

    private fun buildIndex(vars: List<List<String>>, extra: Map<String, List<String>>): Index {
        val names = ArrayList<String>(CANONICAL)
        val idOf = HashMap<String, Int>()
        CANONICAL.forEachIndexed { i, n -> idOf[n] = i }
        for (k in extra.keys) if (k !in idOf) { idOf[k] = names.size; names += k }
        val numbers = IntArray(names.size) { leadingBookNumber(names[it]) }

        // Books that share a name ("1 John", "2 John", "3 John" and "John"): every spelling learned
        // for one is learned for all, so a spelling known only for John also works as "2 <spelling>".
        // A numbered book shares with its numbered siblings; the plain book shares with all of them.
        val byName = HashMap<String, MutableList<Int>>()
        for (i in names.indices) byName.getOrPut(stripBookNumber(names[i]).trim().lowercase()) { ArrayList() }.add(i)
        val siblings = Array(names.size) { i ->
            val group = byName[stripBookNumber(names[i]).trim().lowercase()] ?: listOf(i)
            if (numbers[i] != 0) group.filter { numbers[it] != 0 }.toIntArray() else group.toIntArray()
        }

        val map = LinkedHashMap<String, MutableList<Int>>(24000)
        fun add(book: Int, raw: String) {
            val f = PhoneticFold.fold(stripBookNumber(raw))
            if (f.isEmpty()) return
            val l = map.getOrPut(f) { ArrayList(2) }
            for (sb in siblings[book]) if (sb !in l) l.add(sb)
        }

        if (vars.size == CANONICAL.size) vars.forEachIndexed { b, list -> list.forEach { add(b, it) } }
        CANONICAL.forEachIndexed { b, name ->
            add(b, name)
            // "Song of Solomon" -> also "Song", "Solomon"
            name.split(' ').filter { it.length >= 4 && !it[0].isDigit() }.forEach { add(b, it) }
        }
        for ((k, list) in extra) {
            val b = idOf[k] ?: continue
            add(b, k)
            list.forEach { add(b, it) }
        }

        val folded = map.keys.toTypedArray()
        val loose = HashMap<String, IntArray>(4096)
        for (f in folded) {
            val key = PhoneticFold.looseOfFolded(f)
            if (key.length < 3) continue
            val arr = loose.getOrPut(key) { IntArray(names.size) }
            for (b in map[f]!!) arr[b]++
        }
        return Index(
            names = names.toTypedArray(),
            numbers = numbers,
            folded = folded,
            skeletons = Array(folded.size) { PhoneticFold.skeletonOfFolded(folded[it]) },
            books = Array(folded.size) { map[folded[it]]!!.toIntArray() },
            looseSupport = loose
        )
    }

    // ------------------------------------------------------------------ scoring

    private val scratch: ThreadLocal<Array<IntArray>> = ThreadLocal.withInitial { Array(3) { IntArray(64) } }

    /** Optimal-string-alignment distance (insert, delete, replace, swap) with early exit above [cap]. */
    private fun editDistance(a: String, b: String, cap: Int): Int {
        val la = a.length
        val lb = b.length
        if (abs(la - lb) > cap) return cap + 1
        if (lb >= 63) return cap + 1
        val rows: Array<IntArray> = scratch.get()!!
        var prev2 = rows[0]
        var prev = rows[1]
        var cur = rows[2]
        for (j in 0..lb) prev[j] = j
        for (i in 1..la) {
            cur[0] = i
            var rowMin = i
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = min(min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = min(v, prev2[j - 2] + 1)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > cap) return cap + 1
            val t = prev2; prev2 = prev; prev = cur; cur = t
        }
        return prev[lb]
    }

    private fun commonPrefix(a: String, b: String): Int {
        val n = min(a.length, b.length)
        var i = 0
        while (i < n && a[i] == b[i]) i++
        return i
    }

    /**
     * [typing] = true : the user may still be typing, so a prefix of a known spelling counts
     *                    ("kirt") and so does a shared consonant start ("krtika").
     * [typing] = false: detection inside note text. Only whole-word matches (exact sound, small
     *                    typos, same consonant skeleton), so "at 10:30" or "ratio 3:1" never link.
     */
    private fun scoreOne(fq: String, sq: String, f: String, s: String, typing: Boolean): Float {
        if (fq == f) return 1f
        var best = 0f
        if (typing) {
            if (fq.length >= 2 && f.length > fq.length && f.startsWith(fq)) {
                best = max(best, 0.78f + 0.2f * fq.length / f.length)
            }
            if (f.length >= 3 && fq.length > f.length && fq.startsWith(f)) {
                best = max(best, 0.70f + 0.2f * f.length / fq.length)
            }
        }
        val l = max(fq.length, f.length)
        val tol = if (l <= 5) 1 else if (l <= 9) 2 else 3
        if (l >= 3 && abs(fq.length - f.length) <= tol) {
            val e = editDistance(fq, f, tol)
            if (e <= tol) best = max(best, (1f - e.toFloat() / l) * 0.95f)
        }
        if (sq.length >= 3 && s.length >= 3) {
            if (sq == s) {
                // While typing any same-skeleton spelling counts; in note text it must also be
                // within one edit, so ordinary words with the same consonants are not linked.
                if (typing || editDistance(fq, f, 1) <= 1) best = max(best, 0.82f)
            } else if (typing) {
                val c = commonPrefix(sq, s)
                if (c >= 3 && c >= sq.length - 1) {
                    best = max(best, 0.55f + 0.25f * c / max(sq.length, s.length))
                }
            }
        }
        return best
    }

    private fun rank(p: Parsed, typing: Boolean, context: Collection<String>): List<BookMatch> {
        val idx = index
        val fq = PhoneticFold.fold(p.name)
        if (fq.isEmpty()) return emptyList()
        if (!typing && (fq.length < 3 || p.name in STOP_WORDS)) return emptyList()
        val sq = PhoneticFold.skeletonOfFolded(fq)

        val scores = FloatArray(idx.names.size)

        fun consider(b: Int, v: Float) {
            val num = idx.numbers[b]
            var x = v
            if (p.number != 0 && num != p.number) return          // "2 sam" never returns 1 Samuel
            if (num != 0 && p.number == 0) {
                x *= when {
                    p.softNumber != 0 && num == p.softNumber -> 1.12f
                    p.softNumber != 0 -> 0.85f
                    else -> 0.9f                                   // number missing: numbered books rank lower
                }
            } else if (num == 0 && p.softNumber != 0) {
                x *= 0.92f
            }
            if (x > 1f) x = 1f
            if (x > scores[b]) scores[b] = x
        }

        for (i in idx.folded.indices) {
            val v = scoreOne(fq, sq, idx.folded[i], idx.skeletons[i], typing)
            if (v <= 0f) continue
            for (b in idx.books[i]) consider(b, v)
        }

        // Heavily garbled spelling while typing ("yuvhn" for Yohaanu): same loose consonant key as
        // many known spellings of a book. The book with the most support wins, the others stay far
        // behind. Suggestions only: automatic detection in note text stays strict so ordinary
        // words ("prayer 3:4") are never linked by accident.
        val support = if (typing) idx.looseSupport[PhoneticFold.looseOfFolded(fq)] else null
        if (support != null) {
            var top = 0
            for (c in support) if (c > top) top = c
            if (top > 0) {
                for (b in support.indices) {
                    if (support[b] > 0) consider(b, 0.78f + 0.14f * support[b] / top)
                }
            }
        }

        val floor = if (typing) SUGGEST_MIN else DETECT_MIN
        val out = ArrayList<BookMatch>()
        for (b in scores.indices) {
            var x = scores[b]
            if (x < floor) continue
            val name = idx.names[b]
            if (x >= 0.6f) {
                if (name in context) x += 0.04f                                // used earlier in this note
                val l = learned["$name|$fq"]
                if (l != null) x += min(0.06f, 0.02f * l)                       // user picked it before
            }
            out.add(BookMatch(name, min(x, 1f)))
        }
        out.sortByDescending { it.score }
        return out
    }

    // ------------------------------------------------------------------ public API

    /**
     * Best books for text the user is typing, best first (default top 3), each with a confidence.
     * Check [isAmbiguous] to decide whether to show more than one choice.
     */
    fun suggest(query: String, limit: Int = 3, context: Collection<String> = emptyList()): List<BookMatch> =
        rank(parse(query), typing = true, context = context).take(limit)

    /** A single confident book for a whole word/phrase, or null when unsure or ambiguous. */
    fun resolve(phrase: String, context: Collection<String> = emptyList()): BookMatch? {
        val r = rank(parse(phrase), typing = false, context = context)
        return confident(r)?.first
    }

    private fun confident(r: List<BookMatch>): Pair<BookMatch, List<BookMatch>>? {
        if (r.isEmpty()) return null
        if (r.size > 1 && r[0].score - r[1].score < AMBIGUITY_MARGIN) return null
        return r[0] to r.take(3)
    }

    /** Remember that [query] meant [book] (call when the user accepts a suggestion). */
    fun learn(query: String, book: String) {
        val key = "$book|${PhoneticFold.fold(parse(query).name)}"
        learned.merge(key, 1) { a, b -> min(a + b, 3) }
    }

    fun learnedSnapshot(): Map<String, Int> = HashMap(learned)
    fun restoreLearned(saved: Map<String, Int>) { learned.putAll(saved) }

    // ------------------------------------------------------------------ text helpers

    private val TOKEN = Regex("""\S+""")

    private class Tok(val start: Int, val end: Int, val text: String)

    private fun tokensIn(window: String, offset: Int): List<Tok> {
        val out = ArrayList<Tok>()
        for (m in TOKEN.findAll(window)) {
            var s = m.range.first
            var e = m.range.last + 1
            while (s < e && !window[s].isLetterOrDigit() && !isMark(window[s])) s++
            while (e > s && !window[e - 1].isLetterOrDigit() && !isMark(window[e - 1])) e--
            if (e > s) out.add(Tok(offset + s, offset + e, window.substring(s, e)))
        }
        return out
    }

    private fun isMark(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK -> true
        else -> false
    }

    /**
     * Suggestions while typing: looks at the (up to three) words before [caret] on the current
     * line, e.g. "kirt|" -> Psalms, "2 sam|" -> 2 Samuel. Null when nothing useful is there.
     */
    fun suggestAt(text: String, caret: Int, limit: Int = 3, context: Collection<String> = emptyList()): BookSuggestions? {
        val c = caret.coerceIn(0, text.length)
        if (c == 0 || text[c - 1].isWhitespace()) return null
        val lineStart = text.lastIndexOf('\n', c - 1) + 1
        val from = max(lineStart, c - 60)
        val toks = tokensIn(text.substring(from, c), from)
        val last = toks.lastOrNull() ?: return null
        if (last.end != c && !(last.end == c - 1 && text[c - 1] == '.')) return null
        if (last.text.any { it == ':' || it == '：' } || last.text.none { it.isLetter() }) return null

        var best: List<BookMatch> = emptyList()
        var bestStart = last.start
        var bestHasNumber = false
        for (n in 1..min(3, toks.size)) {
            val use = toks.takeLast(n)
            if (use.drop(1).any { t -> t.text.all { it.isDigit() } }) continue
            val phrase = use.joinToString(" ") { it.text }
            val p = parse(phrase)
            val r = rank(p, typing = true, context = context)
            if (r.isEmpty()) continue
            val hasNumber = p.number != 0
            val better = when {
                best.isEmpty() -> true
                hasNumber != bestHasNumber -> hasNumber && r[0].score >= 0.75f
                else -> r[0].score > best[0].score + 0.001f
            }
            if (better) { best = r; bestStart = use.first().start; bestHasNumber = hasNumber }
        }
        if (best.isEmpty()) return null
        return BookSuggestions(bestStart, last.end, best.take(limit))
    }

    /** What [resolveBefore] found: where the name starts, the book and the closest alternatives. */
    class PhraseHit(val start: Int, val match: BookMatch, val alternatives: List<BookMatch>)

    /**
     * Detection: looks at the (up to four) words that end right before [end] (the start of a
     * chapter:verse) and picks the phrase that names ONE book with confidence.
     * "modati samuyelu 1:1" -> 1 Samuel;  "see Jhon 3:16" -> John.
     */
    internal fun resolveBefore(text: String, end: Int, lineStart: Int, context: Collection<String>): PhraseHit? {
        if (end <= lineStart) return null
        val before = text[end - 1]
        if (!before.isWhitespace() && before != '.') return null
        val from = max(lineStart, end - 80)
        val toks = tokensIn(text.substring(from, end), from)
        val last = toks.lastOrNull() ?: return null
        if (last.text.none { it.isLetter() }) return null

        var best: PhraseHit? = null
        var bestHasNumber = false
        for (n in 1..min(4, toks.size)) {
            val use = toks.takeLast(n)
            if (use.drop(1).any { t -> t.text.all { it.isDigit() } }) continue
            val p = parse(use.joinToString(" ") { it.text })
            val r = rank(p, typing = false, context = context)
            val (top, alts) = confident(r) ?: continue
            val hasNumber = p.number != 0
            val b = best
            // "2 yuvhan" is 2 John even though "yuvhan" alone would give John: a book number written
            // right before the name is stronger evidence than the name's own score.
            val better = when {
                b == null -> true
                hasNumber != bestHasNumber -> hasNumber           // a written number beats "no number"
                else -> top.score > (b?.match?.score ?: 0f) + 0.001f
            }
            if (better) {
                best = PhraseHit(use.first().start, top, alts)
                bestHasNumber = hasNumber
            }
        }
        return best
    }
}