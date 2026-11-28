package com.harsha.myapplication.ui.notes

import com.harsha.myapplication.telemetry.CrashReporter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.harsha.myapplication.*
import kotlinx.coroutines.delay

// Shared by the notes list (left menu) and the note editor ("Move note to" sheet).

// ---- Folder colours (pastel, same set as the "New folder" popup) ----
internal val FolderPalette = listOf(
    Color(0xFFF7E48A), // yellow
    Color(0xFFB4DDB9), // green
    Color(0xFFB8D6F4), // blue
    Color(0xFFF2CB92), // orange
    Color(0xFFF3C0BF), // pink
)
internal val DefaultFolderColor = FolderPalette[0]

/** "New folder" popup: name field, colour swatches, OK / Cancel (Material 3 dialog surface). */
@Composable
internal fun NewFolderDialog(
    existing: List<String>,
    onDismiss: () -> Unit,
    onCreate: (name: String, color: Color) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") }
    var colorIdx by rememberSaveable { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val trimmed = name.trim()
    val duplicate = trimmed.isNotEmpty() && existing.any { it.equals(trimmed, ignoreCase = true) }
    val canAdd = trimmed.isNotEmpty() && !duplicate

    fun submit() {
        if (canAdd) onCreate(trimmed, FolderPalette[colorIdx])
    }

    LaunchedEffect(Unit) {
        delay(100) // wait for the dialog window before asking for focus
        CrashReporter.attempt("FolderComponents") { focus.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "New folder",
                    fontFamily = Lexend,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(16.dp))

                TextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    singleLine = true,
                    isError = duplicate,
                    placeholder = { Text("Enter folder name", fontFamily = Lexend) },
                    supportingText = {
                        Text(
                            if (duplicate) "That folder already exists" else "${name.length}/30",
                            fontFamily = Lexend
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "Select color",
                    fontFamily = Lexend,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FolderPalette.forEachIndexed { i, c ->
                        val isSelected = i == colorIdx
                        Box(
                            modifier = Modifier
                                .size(48.dp) // touch target around the 40dp swatch
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { colorIdx = i }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(c),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF2B2B2B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { submit() },
                    enabled = canAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shapes = ButtonDefaults.shapes(),
                ) { Text("OK", fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shapes = ButtonDefaults.shapes(),
                ) { Text("Cancel", fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 16.sp) }
            }
        }
    }
}