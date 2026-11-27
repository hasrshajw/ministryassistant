package com.harsha.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The three cards that share the top section of the Home screen (one page each in its pager). */
enum class HomeCard(val id: String) {
    DailyText("daily_text"),
    Ministry("ministry"),
    Notes("notes"),
}

/** Which page of the Home card pager the app opens on (among the cards that are visible). */
enum class HomeStart(val id: String, val label: String) {
    First("first", "First"),
    Center("center", "Center"),
    Last("last", "Last"),
}

/**
 * [order] is the order the cards appear in on Home (first = first page).
 * [hidden] are cards the user switched off. At least one card always stays visible.
 */
data class HomeLayout(
    val order: List<HomeCard> = listOf(HomeCard.DailyText, HomeCard.Ministry, HomeCard.Notes),
    val hidden: Set<HomeCard> = emptySet(),
    val start: HomeStart = HomeStart.Center,
) {
    val visibleCards: List<HomeCard> get() = order.filter { it !in hidden }

    /** Page index Home opens on. Center of 3 cards = the middle one (of 2 = the second). */
    fun startIndex(): Int {
        val n = visibleCards.size
        if (n == 0) return 0
        return when (start) {
            HomeStart.First -> 0
            HomeStart.Center -> n / 2
            HomeStart.Last -> n - 1
        }
    }
    fun isVisible(card: HomeCard): Boolean = card !in hidden
}

/**
 * Single source of truth for the Home card layout (same pattern as AppearanceRepository).
 * Call [init] once from MainActivity.onCreate. [layout] is Compose state, so Home and the
 * Customize App screen both recompose the moment it changes.
 */
object HomeLayoutRepository {
    private const val FILE = "home_layout_prefs"
    private const val K_ORDER = "order"
    private const val K_HIDDEN = "hidden"
    private const val K_START = "start"

    private var store: SharedPreferences? = null

    var layout by mutableStateOf(HomeLayout())
        private set

    fun init(context: Context) {
        if (store != null) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = sp

        val byId = HomeCard.values().associateBy { it.id }
        val saved = sp.getString(K_ORDER, null)
            ?.split(',')
            ?.mapNotNull { byId[it.trim()] }
            ?.distinct()
            .orEmpty()
        // Any card missing from the saved order (e.g. added in an update) goes to the end.
        val order = saved + HomeCard.values().filter { it !in saved }

        val hidden = sp.getString(K_HIDDEN, null)
            ?.split(',')
            ?.mapNotNull { byId[it.trim()] }
            ?.toSet()
            .orEmpty()

        val start = HomeStart.values().firstOrNull { it.id == sp.getString(K_START, null) }
            ?: HomeStart.Center

        layout = HomeLayout(
            order = order,
            hidden = if (hidden.size >= order.size) emptySet() else hidden,
            start = start,
        )
    }

    /** Moves the card at [from] to position [to]. */
    fun move(from: Int, to: Int) {
        val current = layout
        if (from == to || from !in current.order.indices || to !in current.order.indices) return
        val list = current.order.toMutableList()
        list.add(to, list.removeAt(from))
        save(current.copy(order = list))
    }

    /** Shows / hides a card. Hiding the last visible card is ignored. */
    fun setVisible(card: HomeCard, visible: Boolean) {
        val current = layout
        if (visible == current.isVisible(card)) return
        if (!visible && current.visibleCards.size <= 1) return
        save(
            current.copy(
                hidden = if (visible) current.hidden - card else current.hidden + card
            )
        )
    }

    /** Chooses which card Home opens on. */
    fun setStart(start: HomeStart) {
        if (start == layout.start) return
        save(layout.copy(start = start))
    }

    private fun save(next: HomeLayout) {
        layout = next
        store?.edit()
            ?.putString(K_ORDER, next.order.joinToString(",") { it.id })
            ?.putString(K_HIDDEN, next.hidden.joinToString(",") { it.id })
            ?.putString(K_START, next.start.id)
            ?.apply()
    }
}
