package com.harsha.myapplication.data

import android.content.Context
import android.util.AtomicFile
import android.util.JsonWriter
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single source of truth for notes.
 *
 *  - [notes] is Compose snapshot state, so any screen reading it recomposes on change.
 *  - Every change is written to disk on a background thread. Writes go through [AtomicFile]
 *    (write-to-temp + rename), so a crash or kill mid-write can never leave a half-written file.
 *  - A corrupt file is copied aside instead of being silently overwritten.
 *
 * MUST be initialised once from MainActivity.onCreate via [init].
 */
object NotesRepository {

    private const val TAG = "NotesRepository"
    private const val FILE_NAME = "notes.json"
    private const val SCHEMA_VERSION = 1
    const val UNTITLED = "Untitled Note"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private var atomicFile: AtomicFile? = null
    private var nextId = 1
    private var initialized = false

    /** Newest-edited first. */
    var notes: List<NoteItem> by mutableStateOf(emptyList())
        private set

    /**
     * Built-in categories plus every category already used by a note.
     * Cached: recomputed only when [notes] changes, not on every read (it is read during composition).
     */
    private val categoriesState = derivedStateOf {
        (DEFAULT_CATEGORIES + notes.map { it.category }).distinct()
    }
    val categories: List<String>
        get() = categoriesState.value

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val startedAt = System.currentTimeMillis()
        val app = context.applicationContext
        NoteImages.init(app)
        val base = File(app.filesDir, FILE_NAME)
        atomicFile = AtomicFile(base)

        notes = readFromDisk(base).sortedByDescending { it.updatedAt }
        nextId = (notes.maxOfOrNull { it.id } ?: 0) + 1
        initialized = true

        val referenced = notes.flatMap { it.images }.toSet()
        ioScope.launch { NoteImages.deleteUnreferenced(referenced, olderThan = startedAt) }
    }

    /** Creates a note (when [existingId] is null / unknown) or updates it. */
    fun save(draft: NoteDraft, existingId: Int?): NoteItem {
        val now = System.currentTimeMillis()
        val existing = existingId?.let { id -> notes.firstOrNull { it.id == id } }
        val title = draft.title.ifBlank { UNTITLED }

        val saved = existing?.copy(
            title = title,
            content = draft.content,
            updatedAt = now,
            category = draft.category,
            formatting = draft.formatting,
            alignment = draft.alignment,
            images = draft.images
        ) ?: NoteItem(
            id = nextId++,
            title = title,
            content = draft.content,
            createdAt = now,
            updatedAt = now,
            category = draft.category,
            formatting = draft.formatting,
            alignment = draft.alignment,
            images = draft.images
        )

        // Images the user removed from this note are no longer needed.
        existing?.images?.filterNot { it in saved.images }?.forEach { NoteImages.delete(it) }

        notes = listOf(saved) + notes.filterNot { it.id == saved.id }
        persist()
        return saved
    }

    /**
     * Moves every note of category [old] to [new] (used when a folder is renamed). The notes keep
     * their edit time, so the list order does not change.
     */
    fun renameCategory(old: String, new: String) {
        val n = new.trim()
        if (n.isEmpty() || n == old) return
        if (notes.none { it.category.equals(old, ignoreCase = true) }) return
        notes = notes.map { if (it.category.equals(old, ignoreCase = true)) it.copy(category = n) else it }
        persist()
    }

    /** Deletes a note and its images. */
    fun delete(id: Int) {
        val note = notes.firstOrNull { it.id == id } ?: return
        notes = notes.filterNot { it.id == id }
        note.images.forEach { NoteImages.delete(it) }
        persist()
    }

    // ---------------------------------------------------------------- persistence

    /** True while a change has not been written yet. Several quick saves collapse into one write. */
    private val dirty = AtomicBoolean(false)

    private fun persist() {
        dirty.set(true)
        ioScope.launch {
            writeLock.withLock {
                // An earlier job may already have written the latest state while this one waited.
                if (!dirty.getAndSet(false)) return@withLock
                val file = atomicFile ?: run {
                    Log.e(TAG, "save() called before init(): notes were NOT written to disk")
                    return@withLock
                }
                // Always write the *latest* state, so out-of-order launches can't save stale data.
                val snapshot = notes
                val out = file.startWrite()
                try {
                    val writer = JsonWriter(BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8), 32 * 1024))
                    writeNotes(writer, snapshot)
                    writer.flush()          // flush only: AtomicFile closes the stream in finishWrite
                    file.finishWrite(out)
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to save notes", e)
                    file.failWrite(out)
                    dirty.set(true)         // try again with the next save
                }
            }
        }
    }

    /** Streams the notes straight to disk instead of building one big JSON string first. */
    private fun writeNotes(w: JsonWriter, list: List<NoteItem>) {
        w.beginObject()
        w.name("version").value(SCHEMA_VERSION.toLong())
        w.name("notes").beginArray()
        for (n in list) {
            w.beginObject()
            w.name("id").value(n.id.toLong())
            w.name("title").value(n.title)
            w.name("content").value(n.content)
            w.name("createdAt").value(n.createdAt)
            w.name("updatedAt").value(n.updatedAt)
            w.name("category").value(n.category)
            w.name("formatting").value(n.formatting)
            w.name("alignment").value(n.alignment.toLong())
            w.name("images").beginArray()
            for (img in n.images) w.value(img)
            w.endArray()
            w.endObject()
        }
        w.endArray()
        w.endObject()
    }

    private fun readFromDisk(base: File): List<NoteItem> {
        val file = atomicFile ?: return emptyList()
        val text = try {
            file.openRead().use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: IOException) {
            return emptyList() // first launch
        }
        return try {
            decode(text)
        } catch (e: Exception) {
            Log.e(TAG, "Notes file is corrupt; keeping a copy", e)
            runCatching {
                File(base.parentFile, "notes.corrupt-${System.currentTimeMillis()}.json")
                    .writeText(text)
            }
            emptyList()
        }
    }

    private fun decode(text: String): List<NoteItem> {
        val arr = JSONObject(text).getJSONArray("notes")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val created = o.optLong("createdAt", System.currentTimeMillis())
            val imgs = o.optJSONArray("images")
            NoteItem(
                id = o.getInt("id"),
                title = o.optString("title", ""),
                content = o.optString("content", ""),
                createdAt = created,
                updatedAt = o.optLong("updatedAt", created),
                category = o.optString("category", DEFAULT_CATEGORY),
                formatting = o.optString("formatting", ""),
                alignment = o.optInt("alignment", 0),
                images = if (imgs == null) emptyList()
                else (0 until imgs.length()).map { imgs.getString(it) }.filter { it.isNotBlank() }
            )
        }
    }
}