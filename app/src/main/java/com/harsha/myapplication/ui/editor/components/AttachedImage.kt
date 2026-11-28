@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import com.harsha.myapplication.telemetry.CrashReporter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.harsha.myapplication.*
import com.harsha.myapplication.data.NoteImages
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Attached image
// ---------------------------------------------------------------------------------------------

internal sealed interface ImageLoad {
    object Loading : ImageLoad
    object Failed : ImageLoad
    class Loaded(val bitmap: ImageBitmap) : ImageLoad
}

@Composable
internal fun rememberImageLoad(name: String): ImageLoad {
    // Cached bitmaps show instantly, so re-composition never flashes a spinner.
    val initial = remember(name) {
        NoteImages.peek(name)?.let { ImageLoad.Loaded(it.asImageBitmap()) }
    }
    val state by produceState<ImageLoad>(initial ?: ImageLoad.Loading, name) {
        if (initial == null) {
            value = NoteImages.load(name)?.let { ImageLoad.Loaded(it.asImageBitmap()) }
                ?: ImageLoad.Failed
        }
    }
    return state
}

/** An inline image drawn over its reserved space. Tap = select it and open the image panel. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlacedImage(
    name: String,
    description: String,
    x: Float,
    y: Float,
    widthPx: Float,
    heightPx: Float,
    rounded: Boolean,
    selected: Boolean,
    clickLabel: String,
    onClick: () -> Unit
) {
    val load = rememberImageLoad(name)
    val density = LocalDensity.current
    val shape = RoundedCornerShape(if (rounded) 16.dp else 0.dp)
    // The docked panel shrinks the editor; scroll so the selected picture stays visible above it.
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) {
        if (selected) {
            delay(320)
            CrashReporter.attempt("AttachedImage") { requester.bringIntoView() }
        }
    }
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            .bringIntoViewRequester(requester)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClickLabel = clickLabel, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (load) {
            is ImageLoad.Loaded -> Image(
                bitmap = load.bitmap,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            ImageLoad.Loading -> LoadingIndicator(modifier = Modifier.size(40.dp))
            ImageLoad.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Image unavailable",
                    fontFamily = Lexend,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Shown while a picked image is being copied into the app. */
@Composable
internal fun ImagePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(modifier = Modifier.size(48.dp))
    }
}

/**
 * Full-screen viewer: pinch to zoom, drag to pan (kept inside the picture), double tap to zoom
 * in / back out, close button top-left.
 */
@Composable
internal fun ImageViewer(name: String, onDismiss: () -> Unit) {
    val load = rememberImageLoad(name)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    /** Panning never moves the picture further than its zoomed edge. */
    fun clamp(o: Offset, s: Float): Offset {
        val maxX = boxSize.width * (s - 1f) / 2f
        val maxY = boxSize.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    // The full-screen viewer fades in from the note instead of popping up.
    val viewerAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val viewerFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(Unit) { viewerAlpha.animateTo(1f, viewerFade) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = viewerAlpha.value }
                .background(Color.Black)
                .onSizeChanged { boxSize = it }
        ) {
            when (load) {
                is ImageLoad.Loaded -> Image(
                    bitmap = load.bitmap,
                    contentDescription = "Image, full screen. Double tap to zoom",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val next = (scale * zoom).coerceIn(1f, 5f)
                                scale = next
                                offset = if (next <= 1f) Offset.Zero else clamp(offset + pan, next)
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
                ImageLoad.Loading -> LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
                ImageLoad.Failed -> Text(
                    "Image unavailable",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

/**
 * Docked image panel - same slot, height and button style as the Aa and Align panels, so the picture
 * stays visible (and updates live) while it is adjusted. Opens from any other panel.
 */
@Composable
internal fun ImagePanel(
    pct: Int,
    align: Int,
    rounded: Boolean,
    wrap: Boolean,
    wrapEnabled: Boolean,
    onPct: (Int) -> Unit,
    onAlign: (Int) -> Unit,
    onRounded: (Boolean) -> Unit,
    onWrap: (Boolean) -> Unit,
    onRotate: () -> Unit,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    PanelScaffold {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Image",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = onRotate, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate image")
            }
            FilledTonalIconButton(onClick = onView, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Default.Fullscreen, contentDescription = "View full screen")
            }
            FilledTonalIconButton(
                onClick = onRemove,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.errorContainer,
                    contentColor = colors.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove image")
            }
        }

        PanelDivider()

        PanelSection("Size  ·  $pct%") {
            Slider(
                value = pct.toFloat(),
                onValueChange = { onPct(it.roundToInt()) },
                valueRange = 20f..100f
            )
            ConnectedButtonGroup(
                items = listOf("Small" to 30, "Medium" to 50, "Large" to 75, "Full" to 100)
                    .map { (label, value) ->
                        GroupItem(
                            description = "$label size",
                            selected = pct == value,
                            onClick = { onPct(value) },
                            label = label
                        )
                    }
            )
        }

        PanelDivider()

        PanelSection("Position") {
            ConnectedButtonGroup(
                items = listOf(0 to "Left", 1 to "Center", 2 to "Right").map { (value, label) ->
                    GroupItem(
                        description = label,
                        selected = align == value,
                        onClick = { onAlign(value) },
                        label = label
                    )
                }
            )
            ConnectedButtonGroup(
                items = listOf(
                    GroupItem(
                        description = "Text beside",
                        selected = wrap,
                        onClick = { onWrap(!wrap) },
                        label = "Text beside",
                        icon = Icons.AutoMirrored.Filled.WrapText,
                        enabled = wrapEnabled,
                        role = Role.Checkbox
                    ),
                    GroupItem(
                        description = "Rounded corners",
                        selected = rounded,
                        onClick = { onRounded(!rounded) },
                        label = "Rounded",
                        icon = Icons.Default.RoundedCorner,
                        role = Role.Checkbox
                    )
                )
            )
            Text(
                text = when {
                    !wrapEnabled -> "Set text alignment to left to put text beside an image."
                    wrap && align == 2 -> "Text flows in the empty space on the left of the image."
                    wrap -> "Text flows in the empty space on the right of the image."
                    else -> "Turn on “Text beside” to write in the empty space next to a smaller image."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
