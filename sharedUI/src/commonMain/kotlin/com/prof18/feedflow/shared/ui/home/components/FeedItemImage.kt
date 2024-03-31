package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.prof18.feedflow.shared.ui.style.Spacing

@Composable
fun FeedItemImage(
    url: String,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .size(width)
                .background(Color.Green),
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(url)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(width)
                .clip(RoundedCornerShape(Spacing.small)),
        )
    }
}
