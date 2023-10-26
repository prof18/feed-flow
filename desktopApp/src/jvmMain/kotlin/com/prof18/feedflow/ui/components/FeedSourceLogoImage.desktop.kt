package com.prof18.feedflow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import com.prof18.feedflow.ui.style.Spacing
import com.seiko.imageloader.rememberImagePainter

@Composable
fun FeedSourceLogoImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    size: Dp,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Green),
        )
    } else {
        Image(
            painter = rememberImagePainter(
                url = imageUrl,
                errorPainter = {
                    rememberVectorPainter(Icons.Default.Category)
                },
                placeholderPainter = {
                    rememberVectorPainter(Icons.Default.Category)
                },
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(Spacing.small)),
        )
    }
}
