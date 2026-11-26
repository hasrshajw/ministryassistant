package com.harsha.myapplication.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale

/**
 * The app's one back button: a clean 48dp circle with a centred 24dp arrow.
 * Expressive touch: on press it morphs from a circle into a rounded square and
 * squishes slightly, then springs back. Use it in every top bar so they all match.
 */
@Composable
fun ExpressiveBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Back",
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 16.dp else 24.dp, // 24dp on a 48dp box = perfect circle
        animationSpec = Motion.spatialFastDp,
        label = "backCorner",
    )
    Surface(
        onClick = onClick,
        interactionSource = src,
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .size(48.dp)
            .pressScale(src, 0.92f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}