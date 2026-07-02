package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.prof18.feedflow.shared.ui.style.Spacing

private const val HERO_IMAGE_ASPECT_RATIO_WIDTH = 16f
private const val HERO_IMAGE_ASPECT_RATIO_HEIGHT = 9f

@Composable
internal fun FeedItemImage(
    url: String,
    width: Dp,
    modifier: Modifier = Modifier,
    imageAlpha: Float = 1f,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .size(width)
                .alpha(imageAlpha)
                .background(Color.Green),
        )
    } else {
        val density = LocalDensity.current
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(url)
                .size(with(density) { width.roundToPx() })
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(width)
                .alpha(imageAlpha)
                .clip(RoundedCornerShape(Spacing.small)),
        )
    }
}

@Composable
internal fun FeedItemHeroImage(
    url: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = HERO_IMAGE_ASPECT_RATIO_WIDTH / HERO_IMAGE_ASPECT_RATIO_HEIGHT,
    imageAlpha: Float = 1f,
) {
    val imageModifier = modifier
        .fillMaxWidth()
        .aspectRatio(aspectRatio)
        .alpha(imageAlpha)

    if (LocalInspectionMode.current) {
        Box(
            modifier = imageModifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(url)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        )
    }
}
