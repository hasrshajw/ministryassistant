package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.*
import com.harsha.myapplication.scripture.BibleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Subtle spring "press" animation shared by clickable cards/buttons. */
@Composable
internal fun Modifier.pressScale(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenBibles: () -> Unit
) {
    val scope = rememberCoroutineScope()
    // BibleData.version is Compose state: the list refreshes when a Bible finishes loading, is
    // imported or removed.
    val dataVersion = BibleData.version
    val bibles = remember(dataVersion) { BibleData.importedBibles }

    // UI-owned selection so the check mark moves on the very tap.
    var activeName by remember { mutableStateOf(BibleData.currentTranslationName) }
    LaunchedEffect(dataVersion) { activeName = BibleData.currentTranslationName }

    var appLanguage by remember { mutableStateOf("English") }
    var langMenuOpen by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ---------------- Bible language ----------------
            SectionHeader("Bible Language", "Choose the language for scripture search")
            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, DotGray.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column {
                    if (bibles.isEmpty()) {
                        Text(
                            "No Bibles yet. Open Downloadable Bibles to import one.",
                            fontFamily = Lexend,
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                    bibles.forEachIndexed { index, bible ->
                        BibleLanguageRow(
                            name = bible.name,
                            selected = bible.name == activeName,
                            onClick = {
                                if (activeName != bible.name) {
                                    activeName = bible.name                   // 1) update UI immediately
                                    // 2) cheap now: swaps a reference; the config file is written in the background
                                    BibleData.setActiveBible(bible.name)
                                }
                            }
                        )
                        if (index != bibles.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = DotGray.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "settings-1")) {
                // ---------------- Bibles management card ----------------
                SectionHeader("Manage Bibles", "Import or remove Bible translations")
                Spacer(Modifier.height(12.dp))

                SettingsNavCard(
                    icon = Icons.Default.FileDownload,
                    title = "Downloadable Bibles",
                    subtitle = "${bibles.size} translation${if (bibles.size == 1) "" else "s"} installed",
                    onClick = onOpenBibles
                )
            }

            Spacer(Modifier.height(24.dp))

            // ---------------- App language dropdown ----------------
            SectionHeader("App Language", "Choose the display language for the app")
            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ScreenBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { langMenuOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SearchBg),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Language, null, tint = Purple) }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Language", fontFamily = Lexend, fontSize = 13.sp, color = TextMuted)
                            Text(appLanguage, fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark)
                        }
                        Icon(Icons.Default.ExpandMore, null, tint = TextMuted)
                    }
                }

                DropdownMenu(
                    expanded = langMenuOpen,
                    onDismissRequest = { langMenuOpen = false }
                ) {
                    listOf("English", "Español", "Português", "Français").forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang, fontFamily = Lexend) },
                            onClick = {
                                appLanguage = lang
                                langMenuOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ---------------- Legal ----------------
            SectionHeader("Legal documents", "Reread the documents you accepted.")
            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, DotGray.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    LegalRow(Icons.Default.Description, "Terms and Conditions", "Read the app's Terms and Conditions.") {
                        // TODO: open Terms screen / URL
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DotGray.copy(alpha = 0.4f))
                    LegalRow(Icons.Default.Security, "Privacy Policy", "Read how we handle your data.") {
                        // TODO: open Privacy screen / URL
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Text(title, fontFamily = Lexend, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
    Spacer(Modifier.height(2.dp))
    Text(subtitle, fontFamily = Lexend, fontSize = 13.sp, color = TextMuted)
}

@Composable
private fun BibleLanguageRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val iconBg by animateColorAsState(
        if (selected) Purple.copy(alpha = 0.18f) else ScreenBg, animationSpec = tween(120), label = "iconBg"
    )
    val iconTint by animateColorAsState(if (selected) Purple else TextMuted, animationSpec = tween(120), label = "iconTint")
    val radioBg by animateColorAsState(if (selected) Purple else Color.Transparent, animationSpec = tween(120), label = "radioBg")
    val radioBorder by animateColorAsState(if (selected) Purple else DotGray, animationSpec = tween(120), label = "radioBorder")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = iconTint)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            name,
            fontFamily = Lexend,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 17.sp,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
        RadioMark(selected = selected, fill = radioBg, border = radioBorder)
    }
}

/** Top-level so no Row/Column receiver leaks in and makes AnimatedVisibility ambiguous. */
@Composable
private fun RadioMark(selected: Boolean, fill: Color, border: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, border),
            modifier = Modifier.fillMaxSize()
        ) {}
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(tween(120)) + fadeIn(tween(100)),
            exit = scaleOut(tween(100)) + fadeOut(tween(100))
        ) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Suppress("SameParameterValue")
@Composable
private fun SettingsNavCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ScreenBg,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SearchBg),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Purple) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = TextDark)
                Text(subtitle, fontFamily = Lexend, fontSize = 13.sp, color = TextMuted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
        }
    }
}

@Composable
private fun LegalRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ScreenBg),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Purple) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark)
            Text(subtitle, fontFamily = Lexend, fontSize = 13.sp, color = TextMuted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
    }
}