package com.harsha.myapplication.data

import java.text.DateFormat
import java.util.Date

const val DEFAULT_CATEGORY = "Uncategorized"

/** Categories offered before the user creates their own. */
val DEFAULT_CATEGORIES = listOf(DEFAULT_CATEGORY, "Study", "Meeting", "Personal")

/**
 * A saved note. Immutable; every edit produces a new copy.
 *
 * [content] is plain text (list markers such as "• " or "1. " are part of it).
 * Rich-text styling lives next to it in [formatting] so old notes without any styling keep working.
 */
data class NoteItem(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val category: String = DEFAULT_CATEGORY,
    /** JSON array of style spans (bold, colour, size, ...). Empty string = no styling. */
    val formatting: String = "",
    /** 0 = start, 1 = center, 2 = end. */
    val alignment: Int = 0,
    /** File names of attached images (resolved by [NoteImages]). */
    val images: List<String> = emptyList()
) {
    /**
     * [content] without the invisible placeholders that mark inline images.
     * Use this (not [content]) for list previews and search.
     */
    val previewText: String
        get() = content.replace("\uFFFC", "").trim()

    /** Human readable "last edited" label, localised to the device. */
    val date: String
        get() = formatNoteDate(updatedAt)
}

/** One date format for the whole app (list, editor header, new notes). */
fun formatNoteDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** What the editor hands back when the user saves. */
data class NoteDraft(
    val title: String,
    val content: String,
    val category: String,
    val formatting: String,
    val alignment: Int,
    val images: List<String>
)