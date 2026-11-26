@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.about

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.R
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar

// ───────────────────────────── Config ─────────────────────────────
// TODO: replace with your real values
private const val CONTACT_EMAIL = "you@example.com"
private const val LETTER_URL = "https://example.com/letter"
private const val INSTAGRAM_URL = "https://instagram.com/yourprofile"
private const val TELEGRAM_URL = "https://t.me/yourchannel"
private const val DEVELOPER_URL = "https://example.com"

private data class Faq(val question: String, val answer: String)

private val faqs = listOf(
    Faq(
        "What does the name Ministry Assistant mean?",
        "The name reflects its purpose: an assistant that helps you organize and manage your ministry activities."
    ),
    Faq(
        "Is Ministry Assistant an official app?",
        "No. Ministry Assistant is an independent project and is not affiliated with any official organization."
    ),
    Faq(
        "Why was Ministry Assistant created?",
        "To give you a simple, practical tool for keeping your ministry notes and records organized."
    ),
    Faq(
        "Is Ministry Assistant always free?",
        "At present, all features and functions in Ministry Assistant are completely free to use."
    )
)

private data class SocialLink(
    val title: String,
    val subtitle: String,
    val tileColor: Color,
    val iconRes: Int,
    val url: String,
)

private val socialLinks = listOf(
    SocialLink("Instagram", "Previews and news", Color(0xFFFCE4EC), R.drawable.ic_instagram, INSTAGRAM_URL),
    SocialLink("Telegram", "Updates channel", Color(0xFFE3F2FD), R.drawable.ic_telegram, TELEGRAM_URL),
)

// ── Design tokens ──────────────────────────────────────────────────────────
private val ScreenMargin = 16.dp
private val SectionGap = 28.dp
private val CardRadius = 28.dp
private val GroupInner = 4.dp
private val GroupGap = 2.dp

// Critically damped spring for colour fades (same numbers as Motion.effectsDefault, typed for Color).
private val ColorEffects = spring<Color>(dampingRatio = 1f, stiffness = 1600f)

// ───────────────────────────── Screen ─────────────────────────────
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    fun openUrl(url: String) =
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    fun writeToDeveloper() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "Ministry Assistant feedback")
        }
        runCatching { context.startActivity(intent) }
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "About",
                navigationIcon = {
                    StudyBarIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                },
            )
        },
        bottomBar = { Footer(onDeveloperClick = { openUrl(DEVELOPER_URL) }) },
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            // ── App header (simple) ──
            Box(Modifier.revealOnEnter(index = 0, playOnceKey = "about-header")) { AppHeader() }

            Spacer(Modifier.height(SectionGap))

            // ── About ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "about-0")) {
                SectionHeader("About")
                Spacer(Modifier.height(10.dp))
                AboutCard()
            }

            Spacer(Modifier.height(SectionGap))

            // ── Social ──
            Column(Modifier.revealOnEnter(index = 1, playOnceKey = "about-1")) {
                SectionHeader("Social")
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GroupGap)
                ) {
                    socialLinks.forEachIndexed { index, link ->
                        LinkRow(
                            link = link,
                            index = index,
                            count = socialLinks.size,
                            onClick = { openUrl(link.url) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ── Contact ──
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "about-2")) {
                SectionHeader("Contact")
                Spacer(Modifier.height(10.dp))
                ContactCard(onWriteClick = ::writeToDeveloper)
            }

            Spacer(Modifier.height(SectionGap))

            // ── FAQ ──
            Column(Modifier.revealOnEnter(index = 3, playOnceKey = "about-3")) {
                SectionHeader("FAQ")
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GroupGap)
                ) {
                    faqs.forEachIndexed { index, faq ->
                        FaqRow(number = index + 1, faq = faq, index = index, count = faqs.size)
                    }
                }
            }

            Spacer(Modifier.height(SectionGap + 8.dp))

            // ── Letter: now sits low, as the closing element of the page ──
            Box(Modifier.revealOnEnter(index = 4, playOnceKey = "about-4")) {
                LetterBanner(onClick = { openUrl(LETTER_URL) })
            }
        }
    }
}

// ───────────────────────────── Building blocks ─────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

/**
 * Grouped-list shape whose joined corners are animated: they breathe open while a row is
 * pressed or expanded (Expressive shape morph), then spring back.
 */
@Composable
private fun rememberGroupShape(index: Int, count: Int, innerTarget: Dp): Shape {
    val inner by animateDpAsState(innerTarget, Motion.spatialFastDp, label = "groupInner")
    val safeInner = inner.coerceAtLeast(0.dp)
    val top = if (index == 0) CardRadius else safeInner
    val bottom = if (index == count - 1) CardRadius else safeInner
    return RoundedCornerShape(
        topStart = top, topEnd = top,
        bottomStart = bottom, bottomEnd = bottom
    )
}

/** Expressive icon tile: a Material shape (cookie, clover, burst…) instead of a plain rounded square. */
@Composable
private fun ShapeTile(
    shape: Shape,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Simple app card: icon + name + tagline on a tonal surface. */
@Composable
private fun AppHeader() {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppIcon(Modifier.size(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Ministry Assistant",
                    style = typography.titleLargeEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Your companion for ministry",
                    style = typography.bodyMedium,
                    color = colors.onSecondaryContainer.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/** The app's own launcher icon; falls back to a book icon. */
@Composable
private fun AppIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val bitmap = remember {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val size = 192
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(android.graphics.Canvas(bmp))
            bmp.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Ministry Assistant icon",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    } else {
        Box(
            modifier.clip(shape).background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun AboutCard() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Ministry Assistant helps you stay organized in your ministry. " +
                    "Keep your notes, schedules and records in one simple place, " +
                    "so you can focus on what matters most.",
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}

/** External link row. Press = scale-down + corners morph open. */
@Composable
private fun LinkRow(link: SocialLink, index: Int, count: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = rememberGroupShape(index, count, if (pressed) 20.dp else GroupInner)
    val tileShape = MaterialShapes.Cookie9Sided.toShape()

    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.97f)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    link.title,
                    style = typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    link.subtitle,
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                ShapeTile(shape = tileShape, container = link.tileColor, size = 44.dp) {
                    Icon(
                        painter = painterResource(link.iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            trailingContent = {
                Icon(
                    Icons.Filled.ArrowOutward,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ContactCard(onWriteClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val tileShape = MaterialShapes.Clover4Leaf.toShape()

    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShapeTile(shape = tileShape, container = colors.secondaryContainer, size = 52.dp) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = colors.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Get in touch",
                        style = typography.titleMediumEmphasized,
                        color = colors.onSurface
                    )
                    Text(
                        "Have questions or suggestions?",
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "If you notice any issues (bugs) or have suggestions, don't hesitate to contact me!",
                style = typography.bodyMedium,
                lineHeight = 22.sp,
                color = colors.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // Expressive button: its shape morphs (round → squarer) while pressed.
            Button(
                onClick = onWriteClick,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Outlined.Mail,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Write to me", style = typography.labelLargeEmphasized)
            }
        }
    }
}

@Composable
private fun FaqRow(number: Int, faq: Faq, index: Int, count: Int) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var expanded by rememberSaveable(faq.question) { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    // Expanded row becomes a fully rounded card, then folds back into the group.
    val shape = rememberGroupShape(
        index, count,
        when {
            expanded -> CardRadius
            pressed -> 20.dp
            else -> GroupInner
        }
    )
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, Motion.spatialDefault, label = "chevron")
    val badgeFill by animateColorAsState(
        if (expanded) colors.primary else colors.secondaryContainer,
        animationSpec = ColorEffects,
        label = "faqBadgeFill"
    )
    val badgeText by animateColorAsState(
        if (expanded) colors.onPrimary else colors.onSecondaryContainer,
        animationSpec = ColorEffects,
        label = "faqBadgeText"
    )
    val badgeShape = MaterialShapes.Cookie6Sided.toShape()
    val badgeSpin by animateFloatAsState(if (expanded) 30f else 0f, Motion.spatialDefault, label = "badgeSpin")

    Surface(
        onClick = { expanded = !expanded },
        interactionSource = source,
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.98f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .rotate(badgeSpin)
                            .clip(badgeShape)
                            .background(badgeFill)
                    )
                    Text(
                        number.toString(),
                        style = typography.labelLargeEmphasized,
                        color = badgeText
                    )
                }
                Text(
                    faq.question,
                    style = typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>())
            ) {
                Text(
                    faq.answer,
                    style = typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 58.dp, top = 10.dp, bottom = 4.dp, end = 8.dp)
                )
            }
        }
    }
}

/**
 * The letter call-to-action. Placed at the bottom of the page (after the FAQ) so it reads as a
 * closing note. Pill-shaped; morphs to a squarer card on press; the burst behind the icon
 * slowly rotates.
 */
@Composable
private fun LetterBanner(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 16.dp else 40.dp,
        Motion.spatialFastDp,
        label = "letterCorner"
    )
    val burst = MaterialShapes.SoftBurst.toShape()
    // Love-letter pinks (fixed, so it reads as "love" in both light and dark themes).
    val dark = isSystemInDarkTheme()
    val container = if (dark) Color(0xFF4A1F33) else Color(0xFFFFD9E6)
    val onContainer = if (dark) Color(0xFFFFD9E6) else Color(0xFF5C1236)
    val accent = if (dark) Color(0xFFFF8FB8) else Color(0xFFE91E63)
    val onAccent = if (dark) Color(0xFF4A1F33) else Color.White
    val spin by rememberInfiniteTransition(label = "letterSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "letterSpinAngle"
    )

    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
        color = container,
        contentColor = onContainer,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.95f)
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .rotate(spin)
                        .clip(burst)
                        .background(accent)
                )
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = onAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "A letter of love",
                    style = typography.titleMediumEmphasized,
                    color = onContainer
                )
                Text(
                    "A heartfelt message from the developer, just for you",
                    style = typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Filled.ArrowOutward,
                contentDescription = null,
                tint = onContainer
            )
        }
    }
}

@Composable
private fun Footer(onDeveloperClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val source = remember { MutableInteractionSource() }
    Column {
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Made with ❤️ by",
                style = typography.labelMedium,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onDeveloperClick,
                interactionSource = source,
                shape = RoundedCornerShape(50),
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                modifier = Modifier.pressScale(source, pressedScale = 0.92f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("HarshaVardhan", style = typography.labelMediumEmphasized)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}