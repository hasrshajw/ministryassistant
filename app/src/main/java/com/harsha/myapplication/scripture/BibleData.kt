package com.harsha.myapplication.scripture

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.TreeMap
import java.util.TreeSet

data class VerseItem(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val canonicalBook: String = book
)

/**
 * One Bible translation. Identity-based (not a data class) so comparing two Bibles never walks
 * ~31,000 verses by accident.
 *
 * Every lookup structure is built lazily, once, and then reused. This is what makes verse lookup,
 * book lists and the chapter/verse picker O(1) instead of a scan over every verse.
 */
class ImportedBible(
    val name: String,
    val verses: List<VerseItem>
) {
    /** "book|chapter" (lower-case) -> verses of that chapter, indexed by display AND canonical name. */
    private val byChapter: Map<String, List<VerseItem>> by lazy {
        val map = HashMap<String, ArrayList<VerseItem>>(2048)
        for (v in verses) {
            map.getOrPut(chapterKey(v.canonicalBook, v.chapter)) { ArrayList() }.add(v)
            if (!v.book.equals(v.canonicalBook, ignoreCase = true)) {
                map.getOrPut(chapterKey(v.book, v.chapter)) { ArrayList() }.add(v)
            }
        }
        map
    }

    /** First verse of every book, in Bible order (used to map display names to canonical ones). */
    val firstVersePerBook: List<VerseItem> by lazy {
        val seen = HashSet<String>()
        val out = ArrayList<VerseItem>(66)
        for (v in verses) if (seen.add(v.book)) out.add(v)
        out
    }

    /** Display names of the books, in Bible order. */
    val books: List<String> by lazy { firstVersePerBook.map { it.book } }

    /** Every name that identifies a book (display + canonical), used to teach the detector. */
    val allBookNames: List<String> by lazy {
        firstVersePerBook.flatMap { listOf(it.book, it.canonicalBook) }.distinct()
    }

    /** canonical English name -> display name in this Bible. */
    val displayNames: Map<String, String> by lazy {
        val map = HashMap<String, String>(128)
        for (v in firstVersePerBook) map.putIfAbsent(v.canonicalBook, v.book)
        map
    }

    /** display book -> (chapter -> sorted verse numbers). Chapters iterate in ascending order. */
    val outline: Map<String, Map<Int, List<Int>>> by lazy {
        val tmp = LinkedHashMap<String, TreeMap<Int, TreeSet<Int>>>()
        for (v in verses) {
            tmp.getOrPut(v.book) { TreeMap() }.getOrPut(v.chapter) { TreeSet() }.add(v.verse)
        }
        val out = LinkedHashMap<String, Map<Int, List<Int>>>(tmp.size * 2)
        for ((book, chapters) in tmp) {
            val sorted = TreeMap<Int, List<Int>>()
            for ((ch, set) in chapters) sorted[ch] = set.toList()
            out[book] = sorted
        }
        out
    }

    fun chapterVerses(book: String, chapter: Int): List<VerseItem> =
        byChapter[chapterKey(book, chapter)].orEmpty()

    /** Builds every index now (call from a background thread so the first screen never stalls). */
    fun warmUp() {
        byChapter.size
        outline.size
        displayNames.size
        allBookNames.size
    }

    private fun chapterKey(book: String, chapter: Int): String = book.lowercase() + '|' + chapter
}

/**
 * Holds the imported Bibles and the one currently active.
 *
 * Threading model:
 *  - State is replaced, never mutated in place, and published through @Volatile fields, so any
 *    thread can read it safely.
 *  - Disk work (load / save / delete) always runs on Dispatchers.IO; the public mutators are cheap
 *    and safe to call from the main thread.
 *  - [version] is Compose state that is bumped on every change. Screens that read it recompose
 *    automatically when a Bible finishes loading, is imported, switched or removed.
 *  - Only the Bible that changed is written to disk (the old code rewrote every Bible each time).
 */
object BibleData {

    private const val TAG = "BibleData"
    private const val DIR_NAME = "bibles_cache"
    private const val CONFIG_NAME = "config.json"
    private const val SAMPLE_NAME = "Sample (limited)"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()        // guards the in-memory state below
    private val fileLock = Any()    // serialises file writes / deletes

    @Volatile private var bibles: List<ImportedBible> = emptyList()
    @Volatile private var active: ImportedBible? = null
    @Volatile private var savedOrder: List<String> = emptyList()

    /** Bumped after every change. Read it inside composition (or use it as a `remember` key). */
    var version by mutableIntStateOf(0)
        private set

    /** True while the saved Bibles are still being read from disk after launch. */
    var isLoading by mutableStateOf(false)
        private set

    val importedBibles: List<ImportedBible> get() = bibles
    val activeBible: ImportedBible? get() = active

    private val sampleBible = ImportedBible(
        SAMPLE_NAME,
        listOf(
            VerseItem("Genesis", 1, 1, "In the beginning God created the heavens and the earth."),
            VerseItem("Matthew", 24, 14, "And this good news of the Kingdom will be preached in all the inhabited earth for a witness to all the nations, and then the end will come."),
            VerseItem("John", 3, 16, "For God loved the world so much that he gave his only-bogotten Son, so that everyone exercising faith in him might not be destroyed but have everlasting life."),
            VerseItem("Psalms", 83, 18, "May people know that you, whose name is Jehovah, you alone are the Most High over all the earth."),
            VerseItem("Proverbs", 3, 5, "Trust in Jehovah with all your heart, and do not rely on your own understanding."),
            VerseItem("Isaiah", 41, 10, "Do not be afraid, for I am with you. Do not be anxious, for I am your God. I will fortify you, yes, I will help you, I will really hold on to you with my right hand of righteousness."),
            VerseItem("Philippians", 4, 13, "For all things I have the strength through the one who gives me power."),
            VerseItem("Revelation", 21, 4, "And he will wipe out every tear from their eyes, and death will be no more, neither will there be mourning nor outcry nor pain anymore. The former things have passed away.")
        )
    )
    private val emptyBible = ImportedBible("", emptyList())

    /** The Bible every lookup uses: the active one, else the built-in sample (empty while loading). */
    private fun current(): ImportedBible =
        active ?: if (isLoading) emptyBible else sampleBible

    val verses: List<VerseItem> get() = current().verses

    val currentTranslationName: String
        get() = active?.name ?: if (isLoading) "Loading…" else SAMPLE_NAME

    private var appContext: Context? = null
    private var started = false

    // ---------------------------------------------------------------- lifecycle

    /** Cheap: returns immediately and loads the saved Bibles on a background thread. */
    fun init(context: Context) {
        synchronized(lock) {
            if (started) return
            started = true
            appContext = context.applicationContext
            isLoading = true
        }
        ioScope.launch { loadFromDisk() }
        // Teach the book matcher the 300 romanized spellings per book (background, ~50 ms).
        val app = appContext
        if (app != null) ioScope.launch {
            try {
                app.assets.open("telugu_bible_name_variants_300.json").use { BookMatcher.loadVariants(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Book name variants not loaded", e)
            }
        }
    }

    private fun biblesDir(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, DIR_NAME).apply { mkdirs() }
    }

    // ---------------------------------------------------------------- file names

    /** Same rule the old versions used. Kept only so old cache files can be found and migrated. */
    private fun legacyFileName(name: String) = name.replace(Regex("[^A-Za-z0-9-_]"), "_") + ".json"

    /**
     * Non-Latin names (Telugu, Hindi, Tamil, ...) all collapse to underscores under the legacy rule,
     * so two Bibles could overwrite each other. The hash keeps every name unique.
     */
    private fun fileName(name: String): String {
        val safe = name.replace(Regex("[^A-Za-z0-9-_]"), "_")
        return "${safe}_${Integer.toHexString(name.hashCode())}.json"
    }

    // ---------------------------------------------------------------- disk: load

    private fun loadFromDisk() {
        try {
            val dir = biblesDir() ?: return
            val config = readConfig(dir)
            savedOrder = config.order

            val files = dir.listFiles { _, n -> n.endsWith(".json") && n != CONFIG_NAME }
                ?.toList().orEmpty()

            // The previously active Bible is read first, so the app is usable as soon as possible.
            val first = setOf(fileName(config.activeName), legacyFileName(config.activeName))
            val ordered = files.sortedBy { if (it.name in first) 0 else 1 }

            for (file in ordered) {
                val bible = readBible(file) ?: continue
                // Migrate old / colliding file names to the unique one.
                if (file.name != fileName(bible.name)) {
                    writeBible(bible)
                    file.delete()
                }
                publishLoaded(bible, config.activeName)
                bible.warmUp()
                DetectorSync.register(bible)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bibles from disk", e)
        } finally {
            synchronized(lock) {
                if (active == null) active = bibles.lastOrNull()
                isLoading = false
                version++
            }
            active?.warmUp()
        }
    }

    private fun publishLoaded(bible: ImportedBible, activeName: String) {
        synchronized(lock) {
            if (bibles.any { it.name == bible.name }) return
            bibles = sortByOrder(bibles + bible)
            if (bible.name == activeName) active = bible
            version++
        }
    }

    private fun sortByOrder(list: List<ImportedBible>): List<ImportedBible> {
        val order = savedOrder
        if (order.isEmpty()) return list
        return list.sortedBy { b -> order.indexOf(b.name).let { if (it < 0) Int.MAX_VALUE else it } }
    }

    private class Config(val activeName: String, val order: List<String>)

    private fun readConfig(dir: File): Config = try {
        val f = File(dir, CONFIG_NAME)
        if (!f.exists()) Config("", emptyList())
        else {
            val o = JSONObject(f.readText())
            val arr = o.optJSONArray("order")
            Config(
                activeName = o.optString("activeName", ""),
                order = if (arr == null) emptyList() else (0 until arr.length()).map { arr.getString(it) }
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Bad config file", e)
        Config("", emptyList())
    }

    /** Streams the file (no giant in-memory JSON tree) and shares one String per book name. */
    private fun readBible(file: File): ImportedBible? = try {
        JsonReader(BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8), 64 * 1024)).use { r ->
            var name: String? = null
            var verses: List<VerseItem> = emptyList()
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "name" -> name = r.nextString()
                    "verses" -> verses = readVerses(r)
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (name != null && verses.isNotEmpty()) ImportedBible(name, verses) else null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse bible file: ${file.name}", e)
        null
    }

    private fun readVerses(r: JsonReader): List<VerseItem> {
        val pool = HashMap<String, String>(256)
        fun share(s: String): String = pool.getOrPut(s) { s }
        val out = ArrayList<VerseItem>(32_000)
        r.beginArray()
        while (r.hasNext()) {
            var book = ""
            var chapter = 0
            var verse = 0
            var text = ""
            var canonical: String? = null
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "book" -> book = share(r.nextString())
                    "chapter" -> chapter = r.nextInt()
                    "verse" -> verse = r.nextInt()
                    "text" -> text = r.nextString()
                    "canonicalBook" ->
                        if (r.peek() == JsonToken.NULL) r.skipValue() else canonical = share(r.nextString())
                    else -> r.skipValue()
                }
            }
            r.endObject()
            out.add(VerseItem(book, chapter, verse, text, canonical ?: book))
        }
        r.endArray()
        return out
    }

    // ---------------------------------------------------------------- disk: save (only what changed)

    private fun writeBible(bible: ImportedBible) {
        val dir = biblesDir() ?: return
        synchronized(fileLock) {
            try {
                val target = File(dir, fileName(bible.name))
                val tmp = File(dir, target.name + ".tmp")
                JsonWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(tmp), Charsets.UTF_8), 64 * 1024)).use { w ->
                    w.beginObject()
                    w.name("name").value(bible.name)
                    w.name("verses").beginArray()
                    for (v in bible.verses) {
                        w.beginObject()
                        w.name("book").value(v.book)
                        w.name("chapter").value(v.chapter.toLong())
                        w.name("verse").value(v.verse.toLong())
                        w.name("text").value(v.text)
                        w.name("canonicalBook").value(v.canonicalBook)
                        w.endObject()
                    }
                    w.endArray()
                    w.endObject()
                }
                if (!tmp.renameTo(target)) {   // atomic on the same file system; fallback just in case
                    target.delete()
                    tmp.renameTo(target)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save bible '${bible.name}'", e)
            }
        }
    }

    private fun writeConfig() {
        val dir = biblesDir() ?: return
        val activeName: String
        val order: List<String>
        synchronized(lock) {
            activeName = active?.name.orEmpty()
            order = bibles.map { it.name }
        }
        synchronized(fileLock) {
            try {
                val o = JSONObject()
                    .put("activeName", activeName)
                    .put("order", JSONArray(order))
                File(dir, CONFIG_NAME).writeText(o.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save bible config", e)
            }
        }
    }

    // ---------------------------------------------------------------- public mutators

    /** Adds (or replaces) a Bible and makes it active. Cheap; the file is written in the background. */
    fun loadVerses(newVerses: List<VerseItem>, translationName: String) {
        if (newVerses.isEmpty()) return

        val bible: ImportedBible
        synchronized(lock) {
            var finalName = translationName
            val existing = bibles.find { it.name == finalName }
            if (existing != null && !isSameContent(existing.verses, newVerses)) {
                var n = 2
                while (bibles.any { it.name == "$translationName ($n)" }) n++
                finalName = "$translationName ($n)"
            }
            bible = ImportedBible(finalName, newVerses)

            val index = bibles.indexOfFirst { it.name == finalName }
            bibles = if (index >= 0) {
                bibles.toMutableList().also { it[index] = bible }
            } else {
                bibles + bible
            }
            active = bible
            version++
        }

        ioScope.launch {
            bible.warmUp()
            DetectorSync.register(bible)
            writeBible(bible)
            writeConfig()
        }
    }

    /** Switches the active Bible. Cheap and main-thread safe: only the small config file is written. */
    fun setActiveBible(name: String): Boolean {
        val found: ImportedBible
        synchronized(lock) {
            found = bibles.find { it.name == name } ?: return false
            active = found
            version++
        }
        ioScope.launch {
            found.warmUp()
            writeConfig()
        }
        return true
    }

    /** Removes a Bible. Only its own file is deleted. */
    fun removeBible(name: String) {
        synchronized(lock) {
            bibles = bibles.filterNot { it.name == name }
            if (active?.name == name) active = bibles.lastOrNull()
            version++
        }
        ioScope.launch {
            val dir = biblesDir()
            if (dir != null) {
                synchronized(fileLock) {
                    File(dir, fileName(name)).delete()
                    File(dir, legacyFileName(name)).takeIf { it.exists() && readBible(it)?.name == name }?.delete()
                }
            }
            writeConfig()
            active?.warmUp()
        }
    }

    // ---------------------------------------------------------------- reading

    private fun VerseItem.inRefRange(ref: ScriptureReference): Boolean {
        return when {
            ref.endVerse == null -> verse == ref.verse
            else -> verse >= ref.verse && verse <= ref.endVerse
        }
    }

    /** Book name as written in the active Bible (e.g. Telugu), else the canonical English name. */
    fun displayBookName(canonical: String): String =
        active?.displayNames?.get(canonical) ?: canonical

    /**
     * Whether every verse of [ref] (including [ScriptureReference.more]) exists in the active Bible.
     * Uses the chapter index, so it is O(verses in the chapter), not a full scan.
     */
    fun isValidPassage(ref: ScriptureReference): Boolean {
        fun ok(r: ScriptureReference): Boolean {
            val matched = current().chapterVerses(r.book, r.chapter).filter { it.inRefRange(r) }
            if (matched.isEmpty()) return false
            val want = if (r.endVerse == null) 1 else (r.endVerse - r.verse + 1).coerceAtLeast(1)
            return matched.size >= want
        }
        return ok(ref) && ref.more.all { ok(it) }
    }

    /**
     * Human-readable reason why [ref] is missing, or null when the passage exists.
     * Examples:
     *  - "Luke has 24 chapters. You selected chapter 25."
     *  - "Luke 24 has 53 verses. You selected verse 99."
     *  - "Luke is not in the current Bible (Sample (limited))."
     */
    fun validatePassageMessage(ref: ScriptureReference): String? {
        if (isValidPassage(ref)) return null

        val bible = current()
        val display = displayBookName(ref.book)
        val outline = bible.outline
        // Resolve by canonical or display name
        val chapters = outline[ref.book]
            ?: outline.entries.firstOrNull { it.key.equals(ref.book, ignoreCase = true) }?.value
            ?: outline.entries.firstOrNull {
                bible.displayNames[it.key]?.equals(ref.book, ignoreCase = true) == true
            }?.value

        if (chapters == null) {
            return "$display is not in the current Bible ($currentTranslationName)."
        }

        val chapterCount = chapters.size
        val maxChapter = chapters.keys.maxOrNull() ?: 0
        if (ref.chapter !in chapters) {
            return "$display has $chapterCount chapters (1–$maxChapter). You selected chapter ${ref.chapter}."
        }

        val verseNums = chapters[ref.chapter].orEmpty()
        val verseCount = verseNums.size
        val maxVerse = verseNums.maxOrNull() ?: 0
        val selected = when {
            ref.endVerse != null && ref.endVerse != ref.verse ->
                "verses ${ref.verse}–${ref.endVerse}"
            else -> "verse ${ref.verse}"
        }
        return "$display ${ref.chapter} has $verseCount verses (1–$maxVerse). You selected $selected."
    }

    fun getPassage(ref: ScriptureReference): String {
        val matched = current().chapterVerses(ref.book, ref.chapter).filter { it.inRefRange(ref) }

        return if (matched.isEmpty()) {
            val detail = validatePassageMessage(ref)
                ?: "Verse not found for ${ref.localizedKey}."
            "$detail\n\nCurrent Bible: $currentTranslationName"
        } else {
            matched.joinToString("\n\n") { "${it.verse} ${it.text}" }
        }
    }

    fun getPassageFromAll(ref: ScriptureReference): List<Pair<String, String>> {
        return bibles.mapNotNull { bible ->
            val matched = bible.chapterVerses(ref.book, ref.chapter).filter { it.inRefRange(ref) }
            if (matched.isEmpty()) null
            else bible.name to matched.joinToString("\n\n") { "${it.verse} ${it.text}" }
        }
    }

    fun getChapterVerses(book: String, chapter: Int): List<VerseItem> =
        current().chapterVerses(book, chapter)

    fun getAvailableBooks(): List<String> = current().books.sorted()

    /** Display names of the books in the active Bible, in Bible order. Cached. */
    fun bookNames(): List<String> = current().books

    /** First verse of each book (gives display + canonical name pairs). Cached. */
    fun firstVersePerBook(): List<VerseItem> = current().firstVersePerBook

    /** book -> chapter -> verse numbers of the active Bible. Cached. */
    fun outline(): Map<String, Map<Int, List<Int>>> = current().outline

    /** Chapter numbers of a book in the active Bible, ascending. Cached. */
    fun chaptersOf(book: String?): List<Int> =
        if (book == null) emptyList() else current().outline[book]?.keys?.toList().orEmpty()

    /**
     * Searches verse text and "Book 3:16" labels in the active Bible. Runs off the main thread,
     * stops at [limit] and is cancellable (a new keystroke cancels the previous search).
     */
    suspend fun search(query: String, limit: Int = 100): List<VerseItem> = withContext(Dispatchers.Default) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        val list = current().verses
        val labelNeedsVerse = q.any { it.isDigit() || it == ':' }
        // Without digits / ':' the query can only match the "Book " part of the label, so that is
        // decided once per book instead of building a string for every verse.
        val bookHit = HashMap<String, Boolean>()
        val out = ArrayList<VerseItem>(limit)

        for (i in list.indices) {
            if (i and 1023 == 0) ensureActive()
            val v = list[i]
            val hit = v.text.contains(q, ignoreCase = true) ||
                    if (labelNeedsVerse) "${v.book} ${v.chapter}:${v.verse}".contains(q, ignoreCase = true)
                    else bookHit.getOrPut(v.book) { "${v.book} ".contains(q, ignoreCase = true) }
            if (hit) {
                out.add(v)
                if (out.size >= limit) break
            }
        }
        out
    }

    private fun isSameContent(a: List<VerseItem>, b: List<VerseItem>): Boolean =
        a.size == b.size &&
                a.firstOrNull() == b.firstOrNull() &&
                a.lastOrNull() == b.lastOrNull()

    /** Teaches the scripture detector the book names of a Bible (display name -> canonical name). */
    private object DetectorSync {
        fun register(bible: ImportedBible) {
            ScriptureDetector.registerBookNames(
                bible.firstVersePerBook.associate { it.book to it.canonicalBook }
            )
        }
    }
}