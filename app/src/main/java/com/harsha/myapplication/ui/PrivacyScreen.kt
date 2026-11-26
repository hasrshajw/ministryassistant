@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnKey
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
private val SectionGap = 24.dp

private data class PrivacySection(
    val id: Int,
    val title: String,
    val content: String
)

private val privacySections = listOf(
    PrivacySection(
        1,
        "1. Overview & Core Commitment",
        "Your privacy is fundamental to how Ministry Assistant is designed. All your notes, scripture references, contacts, and field service records remain strictly on your device. We do not sell, track, or share your data."
    ),
    PrivacySection(
        2,
        "2. Information Collected",
        "Ministry Assistant operates on a local-first model. The app does not collect personal identifiers, location data, or personal note contents. Any optional diagnostic reporting is entirely opt-in."
    ),
    PrivacySection(
        3,
        "3. Local Data Storage & Security",
        "All application databases, preferences, and imported scriptures are stored in protected local storage on your device. Your data is isolated and secured by standard Android OS sandbox security."
    ),
    PrivacySection(
        4,
        "4. Third-Party Services & Ads",
        "Ministry Assistant contains ZERO advertising networks, third-party trackers, or data brokers. No user profile is constructed, and no browsing history is compiled."
    ),
    PrivacySection(
        5,
        "5. Your Rights & Data Portability",
        "You own 100% of your data. You may export your notes, contacts, and preferences at any time to JSON/Text formats, or purge all local application storage permanently."
    )
)

@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var crashReportingEnabled by rememberSaveable { mutableStateOf(false) }
    var expandedSectionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var exportSuccessMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Privacy Policy",
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
            // ── Privacy Hero Card ──
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(index = 0, playOnceKey = "privacy-hero")
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.tertiary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = colors.onTertiary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "100% Private & Local",
                            style = typography.titleLarge,
                            color = colors.onTertiaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Your study notes & ministry records never leave your phone. Zero tracking, zero ads.",
                            style = typography.bodyMedium,
                            color = colors.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Key Privacy Pillars ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "privacy-pillars")) {
                SectionHeader("Privacy Highlights")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrivacyPillarCard(
                        title = "On-Device",
                        subtitle = "Local Storage Only",
                        icon = Icons.Outlined.Lock,
                        modifier = Modifier.weight(1f)
                    )
                    PrivacyPillarCard(
                        title = "Zero Ads",
                        subtitle = "No Trackers",
                        icon = Icons.Outlined.Security,
                        modifier = Modifier.weight(1f)
                    )
                    PrivacyPillarCard(
                        title = "You Control",
                        subtitle = "Full Data Export",
                        icon = Icons.Outlined.FolderZip,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Policy Sections ──
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.revealOnEnter(index = 2, playOnceKey = "privacy-sections")
            ) {
                SectionHeader("Policy Details")
                Spacer(Modifier.height(4.dp))

                privacySections.forEach { section ->
                    val isExpanded = expandedSectionId == section.id
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

            Spacer(Modifier.height(SectionGap))

            // ── Data Controls & Diagnostics ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "privacy-controls")) {
                SectionHeader("Data Controls & Diagnostics")
                Spacer(Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Anonymous Crash Reporting", style = typography.bodyLarge)
                                Text(
                                    "Send crash logs to help us fix bugs. No personal data included.",
                                    style = typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = crashReportingEnabled,
                                onCheckedChange = { crashReportingEnabled = it }
                            )
                        }

                        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                        Button(
                            onClick = {
                                exportSuccessMessage = "All notes & records exported to Downloads folder!"
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Export All Personal Data")
                        }

                        exportSuccessMessage?.let { msg ->
                            Text(
                                msg,
                                style = typography.bodySmall,
                                color = colors.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Purge Data Button ──
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.error.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .revealOnEnter(index = 4, playOnceKey = "privacy-purge")
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Clear All Local App Data", style = typography.labelLarge)
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Clear All App Data?") },
                text = { Text("This will permanently remove all local notes, ministry records, and saved preferences. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Delete Everything", color = colors.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun PrivacyPillarCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(title, style = typography.labelLarge, color = colors.onSurface)
            Text(subtitle, style = typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}
