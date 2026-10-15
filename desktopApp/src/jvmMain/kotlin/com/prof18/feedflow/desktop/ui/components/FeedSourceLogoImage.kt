package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon

@Composable
internal fun FeedSourceLogoImage(
    imageUrl: String,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DesktopDesignTokens.spaceMedium,
) {
    val density = LocalDensity.current
    val context = LocalPlatformContext.current
    val inspectionMode = LocalInspectionMode.current
    val request = remember(imageUrl, size, density, context, inspectionMode) {
        ImageRequest.Builder(context)
            .data(if (inspectionMode) null else imageUrl)
            .size(with(density) { size.roundToPx() })
            .build()
    }
    var showFallback by remember(imageUrl) { mutableStateOf(true) }
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            onState = { showFallback = it !is AsyncImagePainter.State.Success },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (showFallback) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = JewelTheme.globalColors.text.normal,
            )
        }
    }
}
