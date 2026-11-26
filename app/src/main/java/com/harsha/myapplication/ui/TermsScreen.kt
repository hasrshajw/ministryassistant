@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.ui.theme.revealOnEnter

private val ScreenMargin = 16.dp
private val SectionGap = 20.dp

private data class TermsSection(
    val id: Int,
    val title: String,
    val content: String
)

private val termsSections = listOf(
    TermsSection(
        1,
        "1. Acceptance of Terms",
        "By accessing, downloading, or using Ministry Assistant, you agree to be bound by these Terms and Conditions. If you do not agree to all terms, please discontinue using the application immediately."
    ),
    TermsSection(
        2,
        "2. User License & Account Use",
        "Ministry Assistant grants you a personal, non-exclusive, non-transferable, revocable license to use the app for personal study, note-taking, and ministry management in accordance with these terms."
    ),
    TermsSection(
        3,
        "3. Content & Notes Data Ownership",
        "All study notes, ministry records, contacts, and custom content created by you within the app remain exclusively your property. Ministry Assistant does not claim ownership or rights to your personal data."
    ),
    TermsSection(
        4,
        "4. Scripture Content & Copyright",
        "Scripture texts and translations imported into or accessed via Ministry Assistant are protected by copyright laws of their respective copyright holders. Users are responsible for complying with license terms of imported Bible files."
    ),
    TermsSection(
        5,
        "5. Disclaimer of Warranties",
        "Ministry Assistant is provided on an 'AS IS' and 'AS AVAILABLE' basis without warranties of any kind, whether express or implied, including fitness for a particular purpose or non-infringement."
    ),
    TermsSection(
        6,
        "6. Limitation of Liability & Amendments",
        "To the maximum extent permitted by applicable law, the developers of Ministry Assistant shall not be liable for any indirect, incidental, or consequential damages. We reserve the right to update these terms at any time."
    )
)

@Composable
fun TermsScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var expandedSectionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var hasAccepted by rememberSaveable { mutableStateOf(true) }

    val filteredSections = remember(searchQuery) {
        if (searchQuery.isBlank()) termsSections
        else termsSections.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Terms & Conditions",
                navigationIcon = {
                    StudyBarIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                },
            )
        },
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin)
                .padding(bottom = 32.dp)
        ) {
            // ── Document Header ──
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(index = 0, playOnceKey = "terms-header")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Article,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Legal Agreement", style = typography.titleMedium)
                        Text(
                            "Last updated: October 2024 • v1.2",
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    if (hasAccepted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Accepted",
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Search Bar ──
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search terms & conditions...") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(index = 1, playOnceKey = "terms-search")
            )

            Spacer(Modifier.height(SectionGap))

            // ── Terms Sections List ──
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.revealOnEnter(index = 2, playOnceKey = "terms-list")
            ) {
                if (filteredSections.isEmpty()) {
                    Text(
                        "No matching sections found.",
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    filteredSections.forEach { section ->
                        val isExpanded = expandedSectionId == section.id || searchQuery.isNotBlank()
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .clickable {
                                        expandedSectionId = if (isExpanded) null else section.id
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = section.title,
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = colors.onSurfaceVariant
                                    )
                                }

                                if (isExpanded) {
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = section.content,
                                        style = typography.bodyMedium,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Bottom Action Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(index = 3, playOnceKey = "terms-actions"),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }

                Button(
                    onClick = { hasAccepted = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasAccepted) colors.secondaryContainer else colors.primary,
                        contentColor = if (hasAccepted) colors.onSecondaryContainer else colors.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasAccepted) "Accepted" else "Accept Terms")
                }
            }
        }
    }
}
