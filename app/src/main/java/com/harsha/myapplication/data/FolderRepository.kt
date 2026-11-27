package com.harsha.myapplication.data

import com.harsha.myapplication.telemetry.CrashReporter
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent store for the folders (name + colour) the user creates, from the Notes screen
 * side menu or from the note editor.
 *
 *  - [folders] is Compose snapshot state, so every screen reading it updates when a folder is added.
 *  - Folders are saved to SharedPreferences right away, so they are still there after the app is
 *    closed and reopened (even if no note uses them yet).
 *
 * Call [init] before use. It is safe to call many times (the Notes screen and the editor both do it).
 */
object FolderRepository {

    private const val PREFS = "note_folders"
    private const val KEY = "folders"

    data class Entry(val name: String, val colorArgb: Int)

    private var prefs: SharedPreferences? = null

    /** Folders in the order they were created. */
    var folders: List<Entry> by mutableStateOf(emptyList())
        private set

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        folders = decode(p.getString(KEY, null))
    }

    /** Adds a folder (ignored when one with the same name already exists) and saves it. */
    @Synchronized
    fun add(name: String, colorArgb: Int) {
        val n = name.trim()
        if (n.isEmpty() || n.equals(DEFAULT_CATEGORY, ignoreCase = true)) return
        if (folders.any { it.name.equals(n, ignoreCase = true) }) return
        folders = folders + Entry(n, colorArgb)
        prefs?.edit()?.putString(KEY, encode(folders))?.apply()
    }

    /**
     * Renames / recolours a folder and saves it. Works for folders that were never stored too
     * (the built-in categories): they are added. A rename onto another existing folder is ignored.
     */
    @Synchronized
    fun update(oldName: String, newName: String, colorArgb: Int) {
        val n = newName.trim()
        if (n.isEmpty() || n.equals(DEFAULT_CATEGORY, ignoreCase = true)) return
        val clash = !n.equals(oldName.trim(), ignoreCase = true) &&
                folders.any { it.name.equals(n, ignoreCase = true) }
        if (clash) return
        val idx = folders.indexOfFirst { it.name.equals(oldName.trim(), ignoreCase = true) }
        folders = if (idx >= 0) folders.toMutableList().also { it[idx] = Entry(n, colorArgb) }
        else folders + Entry(n, colorArgb)
        prefs?.edit()?.putString(KEY, encode(folders))?.apply()
    }

    private fun encode(list: List<Entry>): String {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("color", it.colorArgb)) }
        return arr.toString()
    }

    private fun decode(text: String?): List<Entry> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val name = o.optString("name", "").trim()
                if (name.isEmpty()) null else Entry(name, o.optInt("color", 0xFFFFD54F.toInt()))
            }
        } catch (e: Exception) {
            CrashReporter.record("FolderRepository", e)
            emptyList()
        }
    }
}