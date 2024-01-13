package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.Image
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
import com.prof18.feedflow.shared.ui.style.Spacing
import com.seiko.imageloader.rememberImagePainter

@Composable
fun FeedItemImage(
    modifier: Modifier = Modifier,
    url: String,
    width: Dp,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .size(width)
                .background(Color.Green),
        )
    } else {
        Image(
            painter = rememberImagePainter(url),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(width)
                .clip(RoundedCornerShape(Spacing.small)),
            contentDescription = null,
        )
    }
}
