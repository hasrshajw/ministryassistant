@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*

/** Find-in-note bar: query field, "2/5" counter, previous / next and close. */
@Composable
internal fun FindBar(
    query: String,
    onQuery: (String) -> Unit,
    count: Int,
    index: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    /** null = read-only (view mode): no replace row. */
    replaceText: String? = null,
    onReplaceText: (String) -> Unit = {},
    onReplace: () -> Unit = {},
    onReplaceAll: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("Find in note", fontFamily = Lexend) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colors.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
            )
            if (query.isNotEmpty()) {
                Text(
                    text = if (count == 0) "0/0" else "${index + 1}/$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
            TipIconButton(label = "Previous match", onClick = onPrev, enabled = count > 0) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
            }
            TipIconButton(label = "Next match", onClick = onNext, enabled = count > 0) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
            }
            TipIconButton(label = "Close find", onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close find")
            }
        }
        if (replaceText != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = replaceText,
                    onValueChange = onReplaceText,
                    placeholder = { Text("Replace with", fontFamily = Lexend) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onReplace() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onReplace,
                    enabled = count > 0,
                    shapes = ButtonDefaults.shapes()
                ) { Text("Replace") }
                TextButton(
                    onClick = onReplaceAll,
                    enabled = count > 0,
                    shapes = ButtonDefaults.shapes()
                ) { Text("All") }
            }
        }
    } // end Column
}
