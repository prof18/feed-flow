package com.prof18.feedflow.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import com.prof18.feedflow.ui.style.Spacing
import com.seiko.imageloader.rememberAsyncImagePainter

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
            painter = rememberAsyncImagePainter(url),
            modifier = modifier
                .width(width)
                .clip(RoundedCornerShape(Spacing.small)),
            contentDescription = null,
        )
    }
}
