package com.harsha.myapplication.scripture

import com.harsha.myapplication.telemetry.CrashReporter
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.database.sqlite.SQLiteDatabase
import java.util.zip.Inflater
import java.io.ByteArrayOutputStream
import java.io.InputStream

object JwpubImporter {

    private const val TAG = "JwpubImporter"
    private val MASK = hexToBytes("11cbb5587e32846d4c26790c633da289f66fe5842a3a585ce1bc3a294af5ada7")

    /**
     * Canonical English book names in Bible order.
     * Position (index + 1) == BookNumber in the JWPUB SQLite database.
     * This is the stable identity used for cross-translation lookups, so it
     * does not matter if the display title is localized (Telugu, Hindi, …).
     */
    private val CANONICAL_BOOKS = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth",
        "1 Samuel", "2 Samuel", "1 Kings", "2 Kings",
        "1 Chronicles", "2 Chronicles",
        "Ezra", "Nehemiah", "Esther",
        "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
        "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel",
        "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah",
        "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi",
        "Matthew", "Mark", "Luke", "John", "Acts",
        "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
        "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
        "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
        "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John",
        "Jude", "Revelation"
    )

    // Fallback: maps JW Library display titles → canonical names.
    // Only needed if BookNumber is somehow missing or out of range.
    private val BOOK_NAME_MAP = mapOf(
        "Genesis" to "Genesis",
        "Exodus" to "Exodus",
        "Leviticus" to "Leviticus",
        "Numbers" to "Numbers",
        "Deuteronomy" to "Deuteronomy",
        "Joshua" to "Joshua",
        "Judges" to "Judges",
        "Ruth" to "Ruth",
        "1 Samuel" to "1 Samuel",
        "2 Samuel" to "2 Samuel",
        "1 Kings" to "1 Kings",
        "2 Kings" to "2 Kings",
        "1 Chronicles" to "1 Chronicles",
        "2 Chronicles" to "2 Chronicles",
        "Ezra" to "Ezra",
        "Nehemiah" to "Nehemiah",
        "Esther" to "Esther",
        "Job" to "Job",
        "Psalms" to "Psalms",
        "Proverbs" to "Proverbs",
        "Ecclesiastes" to "Ecclesiastes",
        "Song of Solomon" to "Song of Solomon",
        "Isaiah" to "Isaiah",
        "Jeremiah" to "Jeremiah",
        "Lamentations" to "Lamentations",
        "Ezekiel" to "Ezekiel",
        "Daniel" to "Daniel",
        "Hosea" to "Hosea",
        "Joel" to "Joel",
        "Amos" to "Amos",
        "Obadiah" to "Obadiah",
        "Jonah" to "Jonah",
        "Micah" to "Micah",
        "Nahum" to "Nahum",
        "Habakkuk" to "Habakkuk",
        "Zephaniah" to "Zephaniah",
        "Haggai" to "Haggai",
        "Zechariah" to "Zechariah",
        "Malachi" to "Malachi",
        "According to Matthew" to "Matthew",
        "According to Mark" to "Mark",
        "According to Luke" to "Luke",
        "According to John" to "John",
        "Acts of Apostles" to "Acts",
        "To the Romans" to "Romans",
        "The First to the Corinthians" to "1 Corinthians",
        "The Second to the Corinthians" to "2 Corinthians",
        "To the Galatians" to "Galatians",
        "To the Ephesians" to "Ephesians",
        "To the Philippians" to "Philippians",
        "Letter to the Colossians" to "Colossians",
        "The First to the Thessalonians" to "1 Thessalonians",
        "The Second to the Thessalonians" to "2 Thessalonians",
        "The First to Timothy" to "1 Timothy",
        "The Second to Timothy" to "2 Timothy",
        "To Titus" to "Titus",
        "To Philemon" to "Philemon",
        "To the Hebrews" to "Hebrews",
        "The Letter of James" to "James",
        "The First of Peter" to "1 Peter",
        "The Second of Peter" to "2 Peter",
        "The First of John" to "1 John",
        "The Second of John" to "2 John",
        "The Third of John" to "3 John",
        "The Letter of Jude" to "Jude",
        "A Revelation to John" to "Revelation"
    )

    data class ImportResult(
        val success: Boolean,
        val verses: List<VerseItem> = emptyList(),
        val translationTitle: String = "",
        val message: String = ""
    )

    /**
     * Where a given BibleVerseId belongs:
     *  - displayBook   : localized title (used for reading in the Bible tab)
     *  - canonicalBook : stable English name (used for cross-Bible lookups)
     */
    private data class VerseLocation(
        val displayBook: String,
        val canonicalBook: String,
        val chapter: Int,
        val verse: Int
    )

    // Compiled once. (The old code compiled these inside the per-verse loop: ~60,000 times per import.)
    private val TAG_RE = Regex("<[^>]+>")
    private val WS_RE = Regex("\\s+")

    suspend fun importFromUri(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "jwpub_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            // 1+2. Stream the picked file straight into its "contents" entry (no full temp copy).
            val contentsFile = File(tempDir, "contents")
            val opened = context.contentResolver.openInputStream(uri)?.use { input ->
                extractSingleEntry(input, "contents", contentsFile)
                true
            } ?: false
            if (!opened) return@withContext ImportResult(false, message = "Could not open file")
            if (!contentsFile.exists()) return@withContext ImportResult(false, message = "Invalid JWPUB (no contents)")

            // 3. Extract inner ZIP -> get the .db
            val dbFile = File(tempDir, "bible.db")
            extractDbFromContents(contentsFile, dbFile)
            if (!dbFile.exists()) return@withContext ImportResult(false, message = "No SQLite database found")

            // 4-6. Open the DB (always closed, even on failure) and decrypt every verse
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                readBible(db)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            CrashReporter.record(TAG, e, "Import failed")
            ImportResult(false, message = e.message ?: "Unknown error")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun readBible(db: SQLiteDatabase): ImportResult {
        val (key, iv, title) = deriveKeyAndTitle(db)
            ?: return ImportResult(false, message = "Could not read Publication metadata")

        // verseId -> where it belongs
        val bookMap = loadBookMap(db)

        // One cipher + two inflaters for the whole import (creating them per verse was the slowest part).
        val decoder = VerseDecoder(key, iv)
        val verses = ArrayList<VerseItem>(32_000)
        try {
            db.rawQuery("SELECT BibleVerseId, Content FROM BibleVerse", null).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("BibleVerseId")
                val contentIdx = cursor.getColumnIndexOrThrow("Content")
                var n = 0
                while (cursor.moveToNext()) {
                    if (++n and 511 == 0) currentCoroutineContext().ensureActive()
                    val verseId = cursor.getInt(idIdx)
                    val blob = cursor.getBlob(contentIdx) ?: continue
                    val mapping = bookMap[verseId] ?: continue
                    val html = try {
                        decoder.decode(blob)
                    } catch (e: Exception) {
                        CrashReporter.record(TAG, e, "Decrypt failed for verseId=$verseId")
                        continue
                    }
                    val plain = stripHtml(html)
                    if (plain.isNotBlank()) {
                        verses.add(
                            VerseItem(
                                book = mapping.displayBook,
                                chapter = mapping.chapter,
                                verse = mapping.verse,
                                text = plain,
                                canonicalBook = mapping.canonicalBook
                            )
                        )
                    }
                }
            }
        } finally {
            decoder.close()
        }

        if (verses.isEmpty()) {
            return ImportResult(false, message = "No verses could be decrypted")
        }
        return ImportResult(
            success = true,
            verses = verses,
            translationTitle = title,
            message = "Imported ${verses.size} verses"
        )
    }

    // ---------- helpers ----------

    private fun extractSingleEntry(input: InputStream, entryName: String, outFile: File) {
        ZipInputStream(BufferedInputStream(input, 64 * 1024)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    FileOutputStream(outFile).use { zis.copyTo(it, 64 * 1024) }
                    return
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun extractDbFromContents(contentsFile: File, outDb: File) {
        ZipInputStream(BufferedInputStream(contentsFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".db", ignoreCase = true)) {
                    FileOutputStream(outDb).use { zis.copyTo(it, 64 * 1024) }
                    return
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun deriveKeyAndTitle(db: SQLiteDatabase): Triple<ByteArray, ByteArray, String>? {
        db.rawQuery(
            "SELECT MepsLanguageIndex, Symbol, Year, IssueTagNumber, Title FROM Publication LIMIT 1",
            null
        ).use { c ->
            if (!c.moveToFirst()) return null
            val lang = c.getInt(0)
            val symbol = c.getString(1) ?: return null
            val year = c.getInt(2)
            val issue = c.getString(3)?.toIntOrNull() ?: 0
            val title = c.getString(4) ?: "Imported Bible"

            val card = if (issue != 0) "${lang}_${symbol}_${year}_$issue" else "${lang}_${symbol}_${year}"
            val digest = MessageDigest.getInstance("SHA-256").digest(card.toByteArray(Charsets.UTF_8))
            val xored = ByteArray(32) { i -> (digest[i].toInt() xor MASK[i].toInt()).toByte() }
            return Triple(xored.copyOfRange(0, 16), xored.copyOfRange(16, 32), title)
        }
    }

    /**
     * Loads verseId → VerseLocation.
     *
     * Canonical book name is now derived from `BookNumber` (1..66), which is
     * language-independent, instead of from the localized display title.
     * This is what lets an English reference like "Genesis 1:1" resolve to
     * the same verse across every imported translation.
     */
    private fun loadBookMap(db: SQLiteDatabase): Map<Int, VerseLocation> {
        val result = mutableMapOf<Int, VerseLocation>()

        data class Chap(
            val displayBook: String,
            val canonicalBook: String,
            val chapter: Int,
            val first: Int,
            val last: Int
        )
        val chapters = mutableListOf<Chap>()

        db.rawQuery(
            """
            SELECT bc.BookNumber, bc.ChapterNumber, bc.FirstVerseId, bc.LastVerseId, bb.ChapterDisplayTitle
            FROM BibleChapter bc
            JOIN BibleBook bb ON bb.BibleBookId = bc.BookNumber
            ORDER BY bc.BookNumber, bc.ChapterNumber
            """.trimIndent(), null
        ).use { c ->
            while (c.moveToNext()) {
                val bookNumber = c.getInt(0)
                val display = c.getString(4) ?: "Book $bookNumber"

                // Primary: map by BookNumber (language independent)
                // Fallback: try English display map, then use display as-is
                val canonical = CANONICAL_BOOKS.getOrNull(bookNumber - 1)
                    ?: BOOK_NAME_MAP[display]
                    ?: display

                chapters.add(
                    Chap(
                        displayBook = display,
                        canonicalBook = canonical,
                        chapter = c.getInt(1),
                        first = c.getInt(2),
                        last = c.getInt(3)
                    )
                )
            }
        }

        for (ch in chapters) {
            var verseNum = 1
            for (vid in ch.first..ch.last) {
                result[vid] = VerseLocation(
                    displayBook = ch.displayBook,
                    canonicalBook = ch.canonicalBook,
                    chapter = ch.chapter,
                    verse = verseNum
                )
                verseNum++
            }
        }
        return result
    }

    /**
     * Decrypts + decompresses verse blobs. All heavy objects (Cipher, Inflater, buffers) are created
     * once and reused for every verse. Not thread-safe: one instance per import.
     */
    private class VerseDecoder(private val key: ByteArray, private val iv: ByteArray) {
        private val cipher: Cipher = Cipher.getInstance("AES/CBC/NoPadding")
        private val keySpec = SecretKeySpec(key, "AES")
        private val ivSpec = IvParameterSpec(iv)
        private val zlib = Inflater(false)       // JW uses standard zlib (with header)
        private val raw = Inflater(true)         // fallback: raw deflate
        private val buffer = ByteArray(8192)
        private val out = ByteArrayOutputStream(4096)

        init { resetCipher() }

        private fun resetCipher() = cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        fun decode(blob: ByteArray): String {
            var decrypted = try {
                cipher.doFinal(blob)
            } catch (e: Exception) {
                resetCipher()               // a failed doFinal must not poison the next verse (caller reports it)
                throw e
            }

            // strip PKCS#7 if present
            if (decrypted.isNotEmpty()) {
                val pad = decrypted.last().toInt() and 0xFF
                if (pad in 1..16 && pad <= decrypted.size) {
                    var ok = true
                    for (i in decrypted.size - pad until decrypted.size) {
                        if ((decrypted[i].toInt() and 0xFF) != pad) { ok = false; break }
                    }
                    if (ok) decrypted = decrypted.copyOf(decrypted.size - pad)
                }
            }

            return try {
                inflate(zlib, decrypted)
            } catch (e: Exception) {
                // Expected: not every verse is zlib-wrapped, so fall back to raw deflate.
                // (Breadcrumb only; a failure of the fallback itself throws and is reported by the caller.)
                CrashReporter.breadcrumb("JwpubImporter: zlib inflate failed, trying raw deflate")
                inflate(raw, decrypted)
            }
        }

        private fun inflate(inflater: Inflater, data: ByteArray): String {
            inflater.reset()
            inflater.setInput(data)
            out.reset()
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                out.write(buffer, 0, count)
            }
            return out.toString(Charsets.UTF_8.name())
        }

        fun close() {
            zlib.end()
            raw.end()
        }
    }

    private fun stripHtml(html: String): String {
        // Fast path: most verses contain markup, but the entity chain only matters when there is an '&'.
        var s = TAG_RE.replace(html, "")
        if (s.indexOf('&') >= 0) {
            s = s
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
        }
        return WS_RE.replace(s, " ").trim()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}