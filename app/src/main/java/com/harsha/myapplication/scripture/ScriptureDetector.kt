package com.harsha.myapplication.scripture

/**
 * Improved scripture detector.
 * Supports common English abbreviations, Telugu full names + short forms,
 * and any book names that come from an imported JWPUB (Hindi, Tamil, etc.).
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

    // Runtime map: English + Telugu, extended when another Bible is imported.
    // Only touched inside synchronized(this); readers use the immutable [snapshot] below.
    private val bookAliases: MutableMap<String, MutableList<String>> =
        baseBookAliases.mapValues { (book, aliases) ->
            (aliases + (teluguAliases[book] ?: emptyList())).distinct().toMutableList()
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
    private const val TAIL_TOKEN =
        """\s*[,;]\s*(?![1-3](?!\d)\s*\p{L})\d{1,3}(?!\d)(?:\s*[:：]\s*\d{1,3}(?!\d))?(?:\s*[-–—]\s*\d{1,3}(?!\d))?"""

    private val tailRegex = Regex("""[,;]\s*(\d{1,3})(?:\s*[:：]\s*(\d{1,3}))?(?:\s*[-–—]\s*(\d{1,3}))?""")

    private fun buildSnapshot(): Snapshot {
        val allAliases = bookAliases.values.flatten()
            .sortedByDescending { it.length }
            .joinToString("|") { Regex.escape(it) }

        // Lookarounds include \p{M} (combining marks) so Telugu vowel signs are part of a word.
        // Optional dot allows "ఆది. 1:1" / "Gen. 1:1".
        // Group 1 = book, 2 = chapter, 3 = verse, 4 = end verse, 5 = extra passages ("1:1-5,7,3:7").
        val pattern = Regex(
            """(?<![\p{L}\p{M}\p{N}])($allAliases)\.?\s+(\d{1,3})\s*[:：]\s*(\d{1,3})(?!\d)(?:\s*[-–—]\s*(\d{1,3})(?!\d))?((?:$TAIL_TOKEN)*)(?![\p{L}\p{M}\p{N}])""",
            RegexOption.IGNORE_CASE
        )

        // alias (lower-case) -> canonical book. First book in map order wins, like the old linear scan.
        val lookup = HashMap<String, String>(bookAliases.size * 4)
        for ((book, aliases) in bookAliases) {
            for (alias in aliases) lookup.putIfAbsent(alias.lowercase(), book)
        }
        return Snapshot(pattern, lookup)
    }

    fun extractReferences(text: String): List<ScriptureReference> {
        // Every reference contains a chapter:verse colon. Plain text without one (the common case
        // while typing a note) skips the large alias regex completely.
        if (text.indexOf(':') < 0 && text.indexOf('：') < 0) return emptyList()

        val snap = snapshot
        return snap.pattern.findAll(text).mapNotNull { match ->
            val rawBook = match.groupValues[1].trim()
            val chapter = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val verse = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
            val endVerse = match.groupValues.getOrNull(4)?.toIntOrNull()
            val tail = match.groupValues.getOrNull(5).orEmpty()

            val book = snap.aliasToBook[rawBook.lowercase()] ?: return@mapNotNull null

            // Extra passages: "7" stays in the current chapter, "3:7" switches chapter.
            val more = mutableListOf<ScriptureReference>()
            var currentChapter = chapter
            if (tail.isNotEmpty()) {
                for (t in tailRegex.findAll(tail)) {
                    val first = t.groupValues[1].toIntOrNull() ?: continue
                    val afterColon = t.groupValues[2].toIntOrNull()
                    val end = t.groupValues[3].toIntOrNull()
                    if (afterColon != null) {
                        currentChapter = first
                        more += ScriptureReference(book, currentChapter, afterColon, end, rawText = t.value.trim(' ', ',', ';'))
                    } else {
                        more += ScriptureReference(book, currentChapter, first, end, rawText = t.value.trim(' ', ',', ';'))
                    }
                }
            }

            ScriptureReference(
                book = book,
                chapter = chapter,
                verse = verse,
                endVerse = endVerse,
                rawText = match.value,
                startIndex = match.range.first,
                endIndex = match.range.last + 1,
                more = more
            )
        }.toList()
    }
}