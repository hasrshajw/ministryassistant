@file:OptIn(ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.Lexend
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Pill search field with a rotating "Search by <hint>" placeholder.
 * Shared by PeopleScreen and NotesScreen so both look identical.
 */
@Composable
internal fun AppSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    hints: List<String>,
    modifier: Modifier = Modifier,
) {
    var hintIndex by remember { mutableIntStateOf(0) }
    val isEmpty = query.isEmpty()
    LaunchedEffect(isEmpty, hints) {
        if (isEmpty && hints.size > 1) while (true) {
            delay(2.seconds)
            hintIndex = (hintIndex + 1) % hints.size
        }
    }

    // Material 3 1.5 state-based search bar: SearchBarState + TextFieldState with the slot-based
    // SearchBar (the old expanded/onExpandedChange DockedSearchBar API is deprecated).
    // Results are shown live in the screen's own list, so the bar never expands into a results
    // surface (expanded = false). The public API still takes a String so callers keep their state;
    // it is mirrored into a TextFieldState here, in both directions.
    val textFieldState = rememberTextFieldState(query)
    val searchBarState = rememberSearchBarState()
    val currentQuery by rememberUpdatedState(query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)

    // Caller -> field (e.g. the clear button, restored state).
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }
    // Field -> caller (typing).
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect { text ->
            if (text != currentQuery) currentOnQueryChange(text)
        }
    }

    SearchBar(
        state = searchBarState,
        modifier = modifier,
        inputField = {
            SearchBarDefaults.InputField(
                state = textFieldState,
                onSearch = { onSearch() },
                expanded = false,
                onExpandedChange = {},
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (!isEmpty) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = {
                    Row {
                        Text("Search by ", fontFamily = Lexend)
                        AnimatedContent(
                            targetState = hints[hintIndex % hints.size],
                            transitionSpec = {
                                (slideInVertically(tween(350)) { (it * 0.6).toInt() } + fadeIn(tween(350))) togetherWith
                                        (slideOutVertically(tween(350)) { (-it * 0.6).toInt() } + fadeOut(tween(350)))
                            },
                            label = "searchHint",
                        ) { Text(it, fontFamily = Lexend) }
                    }
                },
            )
        },
    )
}

/**
 * Tonal sort button + Material 3 menu: a "Sort by" label, 48dp rows, and the selected
 * option shown on a secondary-container pill with a trailing check mark.
 */
@Composable
internal fun <T> SortMenuButton(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Box(modifier) {
        FilledTonalIconButton(onClick = { open = true }, modifier = Modifier.size(52.dp)) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.widthIn(min = 216.dp),
            offset = DpOffset(0.dp, 8.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = Lexend,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
            )
            options.forEach { option ->
                val isSelected = option == selected
                val fg = if (isSelected) colors.onSecondaryContainer else colors.onSurface
                DropdownMenuItem(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) colors.secondaryContainer else Color.Transparent),
                    text = {
                        Text(
                            label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = Lexend,
                            color = fg,
                        )
                    },
                    leadingIcon = { Icon(icon(option), contentDescription = null, tint = fg) },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = fg)
                        }
                    },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}