package com.harsha.myapplication.scripture

/**
 * Scripture detector.
 *
 * 1. Exact pass  - English abbreviations, Telugu names + short forms and every name of an imported
 *                  Bible (JWPUB: Hindi, Tamil, ...). Fast regex, behaves exactly as before.
 * 2. Smart pass  - for what the exact pass did not understand, [BookMatcher] reads the words in
 *                  front of a chapter:verse and works out which book was meant, even when it is
 *                  misspelled or romanized ("Adikandam 1:1", "prasanki 3:4", "modati samuyelu 1:1").
 *                  Each such reference carries a confidence and the closest alternatives.
 *
 * While typing, [suggestAt] / [suggestBooks] give the top books with scores ("kirt" -> Psalms).
 */
object ScriptureDetector {

    // Canonical name → accepted English forms
    private val baseBookAliases: Map<String, List<String>> = mapOf(
        "Genesis" to listOf("Genesis", "Gen", "Ge"),
        "Exodus" to listOf("Exodus", "Exod", "Ex"),
        "Leviticus" to listOf("Leviticus", "Lev", "Le"),
        "Numbers" to listOf("Numbers", "Num", "Nu"),
        "Deuteronomy" to listOf("Deuteronomy", "Deut", "De", "Dt"),
        "Joshua" to listOf("Joshua", "Josh", "Jos"),
        "Judges" to listOf("Judges", "Judg", "Jdg"),
        "Ruth" to listOf("Ruth", "Ru"),
        "1 Samuel" to listOf("1 Samuel", "1 Sam", "1 Sa", "1Samuel", "I Samuel"),
        "2 Samuel" to listOf("2 Samuel", "2 Sam", "2 Sa", "2Samuel", "II Samuel"),
        "1 Kings" to listOf("1 Kings", "1 Kgs", "1 Ki", "1Kings", "I Kings"),
        "2 Kings" to listOf("2 Kings", "2 Kgs", "2 Ki", "2Kings", "II Kings"),
        "1 Chronicles" to listOf("1 Chronicles", "1 Chron", "1 Ch", "1Chronicles"),
        "2 Chronicles" to listOf("2 Chronicles", "2 Chron", "2 Ch", "2Chronicles"),
        "Ezra" to listOf("Ezra", "Ezr"),
        "Nehemiah" to listOf("Nehemiah", "Neh", "Ne"),
        "Esther" to listOf("Esther", "Est", "Es"),
        "Job" to listOf("Job"),
        "Psalms" to listOf("Psalms", "Psalm", "Ps", "Psa"),
        "Proverbs" to listOf("Proverbs", "Prov", "Pr"),
        "Ecclesiastes" to listOf("Ecclesiastes", "Eccl", "Ec", "Qoh"),
        "Song of Solomon" to listOf("Song of Solomon", "Song of Songs", "Song", "SOS", "Canticles"),
        "Isaiah" to listOf("Isaiah", "Isa", "Is"),
        "Jeremiah" to listOf("Jeremiah", "Jer", "Je"),
        "Lamentations" to listOf("Lamentations", "Lam", "La"),
        "Ezekiel" to listOf("Ezekiel", "Ezek", "Eze"),
        "Daniel" to listOf("Daniel", "Dan", "Da"),
        "Hosea" to listOf("Hosea", "Hos", "Ho"),
        "Joel" to listOf("Joel", "Joe"),
        "Amos" to listOf("Amos", "Am"),
        "Obadiah" to listOf("Obadiah", "Obad", "Ob"),
        "Jonah" to listOf("Jonah", "Jon"),
        "Micah" to listOf("Micah", "Mic", "Mi"),
        "Nahum" to listOf("Nahum", "Nah", "Na"),
        "Habakkuk" to listOf("Habakkuk", "Hab"),
        "Zephaniah" to listOf("Zephaniah", "Zeph", "Zep"),
        "Haggai" to listOf("Haggai", "Hag"),
        "Zechariah" to listOf("Zechariah", "Zech", "Zec"),
        "Malachi" to listOf("Malachi", "Mal"),
        "Matthew" to listOf("Matthew", "Matt", "Mt", "Mat"),
        "Mark" to listOf("Mark", "Mk", "Mr"),
        "Luke" to listOf("Luke", "Lk", "Lu"),
        "John" to listOf("John", "Jn", "Joh"),
        "Acts" to listOf("Acts", "Ac"),
        "Romans" to listOf("Romans", "Rom", "Ro"),
        "1 Corinthians" to listOf("1 Corinthians", "1 Cor", "1 Co", "1Corinthians", "I Corinthians"),
        "2 Corinthians" to listOf("2 Corinthians", "2 Cor", "2 Co", "2Corinthians", "II Corinthians"),
        "Galatians" to listOf("Galatians", "Gal", "Ga"),
        "Ephesians" to listOf("Ephesians", "Eph"),
        "Philippians" to listOf("Philippians", "Phil", "Php"),
        "Colossians" to listOf("Colossians", "Col"),
        "1 Thessalonians" to listOf("1 Thessalonians", "1 Thess", "1 Th", "1Thessalonians"),
        "2 Thessalonians" to listOf("2 Thessalonians", "2 Thess", "2 Th", "2Thessalonians"),
        "1 Timothy" to listOf("1 Timothy", "1 Tim", "1 Ti", "1Timothy"),
        "2 Timothy" to listOf("2 Timothy", "2 Tim", "2 Ti", "2Timothy"),
        "Titus" to listOf("Titus", "Tit"),
        "Philemon" to listOf("Philemon", "Phlm", "Phm"),
        "Hebrews" to listOf("Hebrews", "Heb"),
        "James" to listOf("James", "Jas", "Jm"),
        "1 Peter" to listOf("1 Peter", "1 Pet", "1 Pe", "1Peter"),
        "2 Peter" to listOf("2 Peter", "2 Pet", "2 Pe", "2Peter"),
        "1 John" to listOf("1 John", "1 Jn", "1John", "I John"),
        "2 John" to listOf("2 John", "2 Jn", "2John", "II John"),
        "3 John" to listOf("3 John", "3 Jn", "3John", "III John"),
        "Jude" to listOf("Jude", "Jud"),
        "Revelation" to listOf("Revelation", "Rev", "Re", "Apocalypse")
    )

    // Telugu full names + common short forms
    private val teluguAliases: Map<String, List<String>> = mapOf(
        "Genesis" to listOf("ఆదికాండము", "ఆదికాండం", "ఆది"),
        "Exodus" to listOf("నిర్గమకాండము", "నిర్గమకాండం", "నిర్గమ"),
        "Leviticus" to listOf("లేవీయకాండము", "లేవీయకాండం", "లేవీ"),
        "Numbers" to listOf("సంఖ్యాకాండము", "సంఖ్యాకాండం", "సంఖ్యా"),
        "Deuteronomy" to listOf("ద్వితీయోపదేశకాండము", "ద్వితీయోపదేశకాండం", "ద్వితీ"),
        "Joshua" to listOf("యెహోషువ", "యెహో"),
        "Judges" to listOf("న్యాయాధిపతులు", "న్యాయా"),
        "Ruth" to listOf("రూతు"),
        "1 Samuel" to listOf("1 సమూయేలు", "1సమూయేలు", "1 సమూ"),
        "2 Samuel" to listOf("2 సమూయేలు", "2సమూయేలు", "2 సమూ"),
        "1 Kings" to listOf("1 రాజులు", "1రాజులు", "1 రాజు"),
        "2 Kings" to listOf("2 రాజులు", "2రాజులు", "2 రాజు"),
        "1 Chronicles" to listOf("1 దినవృత్తాంతములు", "1 దినవృత్తాంతాలు", "1 దిన"),
        "2 Chronicles" to listOf("2 దినవృత్తాంతములు", "2 దినవృత్తాంతాలు", "2 దిన"),
        "Ezra" to listOf("ఎజ్రా"),
        "Nehemiah" to listOf("నెహెమ్యా", "నెహె"),
        "Esther" to listOf("ఎస్తేరు", "ఎస్తే"),
        "Job" to listOf("యోబు"),
        "Psalms" to listOf("కీర్తనలు", "కీర్తన", "కీర్త"),
        "Proverbs" to listOf("సామెతలు", "సామె"),
        "Ecclesiastes" to listOf("ప్రసంగి", "ప్రసం"),
        "Song of Solomon" to listOf("పరమగీతము", "పరమగీతం", "పరమ"),
        "Isaiah" to listOf("యెషయా", "యెష"),
        "Jeremiah" to listOf("యిర్మీయా", "యిర్మీ"),
        "Lamentations" to listOf("విలాపవాక్యములు", "విలాపవాక్యాలు", "విలా"),
        "Ezekiel" to listOf("యెహెజ్కేలు", "యెహె"),
        "Daniel" to listOf("దానియేలు", "దాని"),
        "Hosea" to listOf("హోషేయ", "హోషే"),
        "Joel" to listOf("యోవేలు", "యోవే"),
        "Amos" to listOf("ఆమోసు", "ఆమో"),
        "Obadiah" to listOf("ఓబద్యా", "ఓబ"),
        "Jonah" to listOf("యోనా"),
        "Micah" to listOf("మీకా"),
        "Nahum" to listOf("నహూము", "నహూ"),
        "Habakkuk" to listOf("హబక్కూకు", "హబ"),
        "Zephaniah" to listOf("జెఫన్యా", "జెఫ"),
        "Haggai" to listOf("హగ్గయి", "హగ్గ"),
        "Zechariah" to listOf("జెకర్యా", "జెక"),
        "Malachi" to listOf("మలాకీ", "మలా"),
        "Matthew" to listOf("మత్తయి", "మత్త"),
        "Mark" to listOf("మార్కు", "మార్"),
        "Luke" to listOf("లూకా", "లూక"),
        "John" to listOf("యోహాను", "యోహా"),
        "Acts" to listOf("అపొస్తలుల కార్యములు", "అపొస్తలుల కార్యాలు", "అపొస్తలుల", "అపో"),
        "Romans" to listOf("రోమీయులకు", "రోమా"),
        "1 Corinthians" to listOf("1 కొరింథీయులకు", "1కొరింథీయులకు", "1 కొరిం"),
        "2 Corinthians" to listOf("2 కొరింథీయులకు", "2కొరింథీయులకు", "2 కొరిం"),
        "Galatians" to listOf("గలతీయులకు", "గలతీ"),
        "Ephesians" to listOf("ఎఫెసీయులకు", "ఎఫెసీ", "ఎఫె"),
        "Philippians" to listOf("ఫిలిప్పీయులకు", "ఫిలిప్పీ", "ఫిలి"),
        "Colossians" to listOf("కొలొస్సయులకు", "కొలొస్సి", "కొలొ"),
        "1 Thessalonians" to listOf("1 థెస్సలొనీకయులకు", "1 థెస్స"),
        "2 Thessalonians" to listOf("2 థెస్సలొనీకయులకు", "2 థెస్స"),
        "1 Timothy" to listOf("1 తిమోతికి", "1తిమోతికి", "1 తిమో"),
        "2 Timothy" to listOf("2 తిమోతికి", "2తిమోతికి", "2 తిమో"),
        "Titus" to listOf("తీతుకు", "తీతు"),
        "Philemon" to listOf("ఫిలేమోనుకు", "ఫిలేమోను", "ఫిలే"),
        "Hebrews" to listOf("హెబ్రీయులకు", "హెబ్రీ"),
        "James" to listOf("యాకోబు", "యాకో"),
        "1 Peter" to listOf("1 పేతురు", "1పేతురు", "1 పేతు"),
        "2 Peter" to listOf("2 పేతురు", "2పేతురు", "2 పేతు"),
        "1 John" to listOf("1 యోహాను", "1యోహాను", "1 యోహా"),
        "2 John" to listOf("2 యోహాను", "2యోహాను", "2 యోహా"),
        "3 John" to listOf("3 యోహాను", "3యోహాను", "3 యోహా"),
        "Jude" to listOf("యూదా"),
        "Revelation" to listOf("ప్రకటన గ్రంథము", "ప్రకటన గ్రంథం", "ప్రకటన", "ప్రక")
    )

    // "1 John" -> also "1st John", "First John"; "2 Sam" -> "2nd Sam", "Second Sam" (English forms only).
    private val numberedAlias = Regex("""^([123]) (\p{L}+)$""")
    private val ordinalWords = mapOf(
        '1' to listOf("1st", "First"), '2' to listOf("2nd", "Second"), '3' to listOf("3rd", "Third")
    )

    private fun ordinalAliases(aliases: List<String>): List<String> = aliases.flatMap { alias ->
        val m = numberedAlias.find(alias) ?: return@flatMap emptyList()
        ordinalWords.getValue(m.groupValues[1][0]).map { "$it ${m.groupValues[2]}" }
    }

    // Runtime map: English + Telugu, extended when another Bible is imported.
    // Only touched inside synchronized(this); readers use the immutable [snapshot] below.
    private val bookAliases: MutableMap<String, MutableList<String>> =
        baseBookAliases.mapValues { (book, aliases) ->
            (aliases + ordinalAliases(aliases) + (teluguAliases[book] ?: emptyList())).distinct().toMutableList()
        }.toMutableMap()

    /**
     * Everything a lookup needs, published atomically. The detector runs on a background thread in
     * the editor while a Bible import may register new names, so readers must never see a
     * half-updated state.
     */
    private class Snapshot(val pattern: Regex, val aliasToBook: Map<String, String>)

    @Volatile
    private var snapshot: Snapshot = buildSnapshot()

    /**
     * Called by BibleData after a Bible is imported or loaded: every display name of the Bible is
     * mapped to its canonical English book, so a reference written in Telugu / Hindi / Tamil ...
     * resolves to the SAME canonical book in every translation. Names whose canonical book is
     * unknown become their own book (same as before).
     */
    fun registerBookNames(displayToCanonical: Map<String, String>) {
        synchronized(this) {
            var changed = false
            val known = snapshot.aliasToBook
            for ((display, canonical) in displayToCanonical) {
                val name = display.trim()
                if (name.isEmpty()) continue
                if (bookAliases.containsKey(name) || known.containsKey(name.lowercase())) continue
                val target = canonical.trim()
                val list = bookAliases[target]
                if (list != null) {
                    list += name                       // alias of an existing canonical book
                } else {
                    bookAliases[name] = mutableListOf(name)   // unknown book: its own canonical form
                }
                changed = true
            }
            if (changed) snapshot = buildSnapshot()
        }
    }

    /**
     * Older entry point: adds every name as its own book. Kept for compatibility;
     * [registerBookNames] is preferred because it keeps canonical names consistent.
     */
    fun registerImportedBooks(bookNames: Collection<String>) {
        registerBookNames(bookNames.associateWith { it })
    }

    // One more passage after the first: ",7"  ",3:7"  ";3:7-9". A bare 1-3 followed by a word is
    // treated as the next book's number ("Genesis 1:1, 2 Kings 3:4"), so it is not swallowed.
    //
    // Chapter/verse separator: a colon (spaces allowed: "3 : 16") OR a full stop with NO spaces
    // ("3.16"). A full stop with spaces is a sentence end ("... chapter 3. 16 people came").
    // Every such reference is stored as plain numbers, so it is shown / keyed with a colon.
    private const val SEP = """(?:\s*[:：]\s*|\.)"""

    // Exact pass only (the book name is certain there): "John 3 verse 16", "John 3 v. 16", "John 3v16",
    // "John chapter 3, verse 16" as well as the colon / dot forms.
    private const val VERSE_WORD = """\s*,?\s*(?:(?:verses?|vss?|ver)\.?\s*|vv?\.\s*|vv?(?=\d))"""
    private const val SEP_LONG = """(?:\s*[:：]\s*|\.|$VERSE_WORD)"""
    private const val CHAPTER_WORD = """(?:(?:chapter|chap|ch)\.?\s*)?"""

    // List connector: "," ";" or "&" / "and" (the latter two only when the number after them is not
    // followed by a word, so "John 3:16 and 17 people came" is NOT read as verse 17).
    private const val LIST_JOIN =
        """(?:[,;]|(?:&|(?i:and))(?=\s*\d{1,3}(?:[:：.\-–—,;\s]*\d{1,3})*(?![\p{L}\d]|\s+\p{L})))"""

    // Not a reference when the verse is directly followed by ".digit" ("Mark 2.5.1", a version number).
    private const val END_GUARD = """(?![\p{L}\p{M}\p{N}]|\.\d)"""

    private const val TAIL_TOKEN =
        """\s*$LIST_JOIN\s*(?![1-3](?!\d)\s*\p{L})\d{1,3}(?!\d)(?:$SEP\d{1,3}(?!\d))?(?:\s*[-–—]\s*\d{1,3}(?!\d))?"""

    private val tailRegex = Regex("""(?:[,;]|&|(?i:and))\s*(\d{1,3})(?:$SEP(\d{1,3}))?(?:\s*[-–—]\s*(\d{1,3}))?""")

    private val dotDigitRegex = Regex("""\d\.\d|(?i:\d\s*,?\s*v)""")

    /** Smart-pass (fuzzy book) matches written with a dot must be at least this sure of the book. */
    private const val SMART_DOT_MIN_CONFIDENCE = 0.85f

    /** True when chapter and verse were written "3.16" / "3 verse 16" instead of "3:16". */
    private fun usesDot(text: String, chapterEnd: Int): Boolean {
        var i = chapterEnd
        while (i < text.length && text[i].isWhitespace()) i++
        val c = text.getOrNull(i)
        return c != ':' && c != '：'
    }

    /**
     * "Is 3.14", "am 5.5", "Re 2.0" are decimals in ordinary sentences, so a dot reference with a
     * very short English book word is only accepted when it is written like an abbreviation
     * ("Ps 23.1", "Jn 3.16"): 3+ letters, or a capital first letter, or a non-Latin script name.
     */
    private fun dotBookAllowed(bookText: String): Boolean {
        val word = bookText.trim()
        if (word.any { it.isLetter() && it.code > 0x24F }) return true   // Telugu, Hindi, Tamil ...
        return word.count { it.isLetter() } >= 3 || word.firstOrNull()?.isUpperCase() == true
    }

    // chapter:verse on its own. Used by the smart pass, where the book name is not an exact alias.
    // Group 1 = chapter, 2 = verse, 3 = end verse, 4 = extra passages.
    private val verseRegex = Regex(
        """(?<![\p{L}\p{M}\p{N}:：]|\d\.)(\d{1,3})$SEP(\d{1,3})(?!\d)(?:\s*[-–—]\s*(\d{1,3})(?!\d))?((?:$TAIL_TOKEN)*)$END_GUARD"""
    )

    private fun buildSnapshot(): Snapshot {
        val allAliases = bookAliases.values.flatten()
            .sortedByDescending { it.length }
            .joinToString("|") { Regex.escape(it) }

        // Lookarounds include \p{M} (combining marks) so Telugu vowel signs are part of a word.
        // Optional dot allows "ఆది. 1:1" / "Gen. 1:1"; the space is optional too ("John3:16", "Gen.1:1").
        // Chapter and verse may also be split by a dot: "John 3.16", "Gen. 1.1" (see [SEP]).
        // Group 1 = book, 2 = chapter, 3 = verse, 4 = end verse, 5 = extra passages ("1:1-5,7,3:7").
        val pattern = Regex(
            """(?<![\p{L}\p{M}\p{N}])($allAliases)\.?\s*$CHAPTER_WORD(\d{1,3})$SEP_LONG(\d{1,3})(?!\d)(?:\s*[-–—]\s*(\d{1,3})(?!\d))?((?:$TAIL_TOKEN)*)$END_GUARD""",
            RegexOption.IGNORE_CASE
        )

        // alias (lower-case) -> canonical book. First book in map order wins, like the old linear scan.
        val lookup = HashMap<String, String>(bookAliases.size * 4)
        for ((book, aliases) in bookAliases) {
            for (alias in aliases) lookup.putIfAbsent(alias.lowercase(), book)
        }

        // The fuzzy matcher learns every name too (English, Telugu script, imported Bibles).
        BookMatcher.setAliases(bookAliases.mapValues { it.value.toList() })
        return Snapshot(pattern, lookup)
    }

    /** Builds a reference, expanding ",7" / ",3:7" tails into [ScriptureReference.more]. */
    private fun buildReference(
        book: String, chapter: Int, verse: Int, endVerse: Int?, tail: String,
        raw: String, start: Int, end: Int,
        confidence: Float = 1f, alternatives: List<BookMatch> = emptyList()
    ): ScriptureReference {
        // Extra passages: "7" stays in the current chapter, "3:7" switches chapter.
        val more = mutableListOf<ScriptureReference>()
        var currentChapter = chapter
        if (tail.isNotEmpty()) {
            for (t in tailRegex.findAll(tail)) {
                val first = t.groupValues[1].toIntOrNull() ?: continue
                val afterColon = t.groupValues[2].toIntOrNull()
                val last = t.groupValues[3].toIntOrNull()
                val text = t.value.trim(' ', ',', ';')
                if (afterColon != null) {
                    currentChapter = first
                    more += ScriptureReference(book, currentChapter, afterColon, last, rawText = text)
                } else {
                    more += ScriptureReference(book, currentChapter, first, last, rawText = text)
                }
            }
        }
        return ScriptureReference(
            book = book, chapter = chapter, verse = verse, endVerse = endVerse,
            rawText = raw, startIndex = start, endIndex = end, more = more,
            confidence = confidence, alternatives = alternatives
        )
    }

    fun extractReferences(text: String): List<ScriptureReference> {
        // Every reference contains a chapter:verse colon or a digit.digit dot. Plain text without
        // either (the common case while typing a note) skips the large alias regex completely.
        if (text.indexOf(':') < 0 && text.indexOf('：') < 0 && !dotDigitRegex.containsMatchIn(text)) return emptyList()

        val snap = snapshot
        val exact = snap.pattern.findAll(text).mapNotNull { match ->
            val chapter = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val verse = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
            val endVerse = match.groupValues.getOrNull(4)?.toIntOrNull()
            val book = snap.aliasToBook[match.groupValues[1].trim().lowercase()] ?: return@mapNotNull null
            if (usesDot(text, match.groups[2]!!.range.last + 1) &&
                (chapter < 1 || verse < 1 || !dotBookAllowed(match.groupValues[1]))
            ) return@mapNotNull null
            buildReference(
                book, chapter, verse, endVerse, match.groupValues.getOrNull(5).orEmpty(),
                match.value, match.range.first, match.range.last + 1
            )
        }.toList()

        val smart = smartPass(text, exact)
        return if (smart.isEmpty()) exact else (exact + smart).sortedBy { it.startIndex }
    }

    /**
     * Finds chapter:verse spots the exact pass did not claim and asks [BookMatcher] which book
     * the words in front of them name. Books already found in the note break ties
     * (a note about Psalms leans to Psalms for "kirt").
     */
    private fun smartPass(text: String, exact: List<ScriptureReference>): List<ScriptureReference> {
        val out = mutableListOf<ScriptureReference>()
        val context = HashSet<String>()
        exact.mapTo(context) { it.book }
        var lastEnd = -1

        for (m in verseRegex.findAll(text)) {
            val vStart = m.range.first
            val vEnd = m.range.last + 1
            if (exact.any { vStart < it.endIndex && vEnd > it.startIndex }) continue

            val lineStart = text.lastIndexOf('\n', vStart - 1) + 1
            val hit = BookMatcher.resolveBefore(text, vStart, lineStart, context) ?: continue
            if (hit.start < lastEnd || exact.any { hit.start < it.endIndex && vEnd > it.startIndex }) continue

            val chapter = m.groupValues[1].toIntOrNull() ?: continue
            val verse = m.groupValues[2].toIntOrNull() ?: continue
            if (usesDot(text, m.groups[1]!!.range.last + 1) &&
                (chapter < 1 || verse < 1 || hit.match.score < SMART_DOT_MIN_CONFIDENCE)
            ) continue
            val ref = buildReference(
                hit.match.book, chapter, verse, m.groupValues[3].toIntOrNull(), m.groupValues[4],
                text.substring(hit.start, vEnd), hit.start, vEnd,
                confidence = hit.match.score, alternatives = hit.alternatives
            )
            out += ref
            context += ref.book
            lastEnd = vEnd
        }
        return out
    }

    // ---------------------------------------------------------------- typing helpers

    /** Top books (default 3) for what the user has typed so far, each with a confidence score. */
    fun suggestBooks(query: String, limit: Int = 3, context: Collection<String> = emptyList()): List<BookMatch> =
        BookMatcher.suggest(query, limit, context)

    /** Suggestions for the word(s) right before [caret], with the text range they would replace. */
    fun suggestAt(text: String, caret: Int, limit: Int = 3, context: Collection<String> = emptyList()): BookSuggestions? =
        BookMatcher.suggestAt(text, caret, limit, context)

    /** Call when the user accepts a suggestion so the same spelling resolves faster next time. */
    fun recordChoice(typed: String, book: String) = BookMatcher.learn(typed, book)
}