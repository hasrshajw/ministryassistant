package com.harsha.myapplication.scripture

data class ScriptureReference(
    val book: String,          // Canonical book name e.g. "Matthew"
    val chapter: Int,
    val verse: Int,
    val endVerse: Int? = null,
    val rawText: String,       // Exact matched text e.g. "Matt 24:14"
    val startIndex: Int = 0,
    val endIndex: Int = 0,
    /**
     * Extra passages of the same book when several are written together,
     * e.g. "Song 1:1-5,7,3:7" -> this = 1:1-5, more = [1:7, 3:7].
     */
    val more: List<ScriptureReference> = emptyList()
)

/** Every passage in this reference, in the order written (just one for a plain reference). */
val ScriptureReference.allPassages: List<ScriptureReference>
    get() = listOf(copy(more = emptyList())) + more

/** "Song 1:1-5,7,3:7": chapter number is only repeated when it changes. */
fun passagesKey(bookName: String, passages: List<ScriptureReference>): String {
    val parts = mutableListOf<String>()
    var lastChapter = -1
    for (p in passages) {
        val range = if (p.endVerse != null && p.endVerse != p.verse) "${p.verse}-${p.endVerse}" else "${p.verse}"
        parts += if (p.chapter != lastChapter) "${p.chapter}:$range" else range
        lastChapter = p.chapter
    }
    return "$bookName ${parts.joinToString(",")}"
}

/** English/canonical key (kept for existing callers). */
val ScriptureReference.displayKey: String
    get() = passagesKey(book, allPassages)

/** Key with the book name in the active Bible's language, e.g. "ఆదికాండము 1:1-4,11,3:1,6,9". */
val ScriptureReference.localizedKey: String
    get() = passagesKey(BibleData.displayBookName(book), allPassages)

/** Consecutive passages in the same chapter are merged into one group. */
fun List<ScriptureReference>.groupByChapterRuns(): List<List<ScriptureReference>> {
    val out = mutableListOf<MutableList<ScriptureReference>>()
    for (p in this) {
        if (out.isNotEmpty() && out.last().first().chapter == p.chapter) out.last().add(p)
        else out.add(mutableListOf(p))
    }
    return out
}