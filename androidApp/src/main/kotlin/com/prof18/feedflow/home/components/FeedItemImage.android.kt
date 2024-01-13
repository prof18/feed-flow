package com.prof18.feedflow.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prof18.feedflow.ui.style.Spacing

@Composable
internal fun FeedItemImage(
    modifier: Modifier = Modifier,
    url: String,
    size: Dp,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Green),
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(Spacing.small)),
        )
    }
}

@Preview
@Composable
private fun FeedItemImagePreview() {
    FeedItemImage(
        url = "https://www.img.com",
        size = Spacing.large,
    )
}
