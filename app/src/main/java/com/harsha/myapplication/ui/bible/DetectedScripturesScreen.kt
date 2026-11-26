@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.harsha.myapplication.*
import com.harsha.myapplication.scripture.ScriptureReference
import com.harsha.myapplication.scripture.localizedKey
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter

/** Full-screen list of the scriptures found in the note. Tap one to read it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetectedScripturesScreen(
    references: List<ScriptureReference>,
    onClose: () -> Unit,
    onOpen: (ScriptureReference) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
            val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
                topBar = {
                    LargeFlexibleTopAppBar(
                        title = { Text("Detected scriptures", fontFamily = Lexend) },
                        subtitle = {
                            Text(
                                if (references.size == 1) "1 reference found" else "${references.size} references found",
                                fontFamily = Lexend
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                        scrollBehavior = scroll
                    )
                },
                containerColor = colors.surface
            ) { inner ->
                // Connected button list (Add-hours style): 2dp gaps, small inner corners, larger
                // outer corners on the first and last row.
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(references) { index, ref ->
                        val outer = 28.dp
                        val small = 8.dp
                        val shape = RoundedCornerShape(
                            topStart = if (index == 0) outer else small,
                            topEnd = if (index == 0) outer else small,
                            bottomStart = if (index == references.lastIndex) outer else small,
                            bottomEnd = if (index == references.lastIndex) outer else small
                        )
                        val source = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .revealOnEnter(index = index, playOnceKey = "scripture-$index-${ref.localizedKey}")
                                .fillMaxWidth()
                                .pressScale(source, 0.97f)
                                .clip(shape)
                                .background(colors.surfaceContainerHighest)
                                .clickable(
                                    interactionSource = source,
                                    indication = ripple(),
                                    role = Role.Button
                                ) { onOpen(ref) }
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                ref.localizedKey,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.rotate(-90f)
                            )
                        }
                    }
                }
            }
        }
    }
}
