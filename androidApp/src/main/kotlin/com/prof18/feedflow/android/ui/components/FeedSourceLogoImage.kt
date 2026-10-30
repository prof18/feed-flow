package com.prof18.feedflow.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.prof18.feedflow.android.ui.style.Spacing

@Composable
fun FeedSourceLogoImage(
    imageUrl: String,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Spacing.small,
    fallbackIcon: ImageVector = Icons.Default.Category,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Green),
        )
    } else {
        val density = LocalDensity.current
        // The fallback is drawn as a tinted Icon instead of an AsyncImage placeholder painter,
        // which would render the vector untinted (black) and disappear on dark backgrounds.
        var showFallback by remember(imageUrl) { mutableStateOf(true) }
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .size(with(density) { size.roundToPx() })
                    .build(),
                onState = { showFallback = it !is AsyncImagePainter.State.Success },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (showFallback) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                )
            }
        }
    }
}
