package com.prof18.feedflow.shared.ui.accounts.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.theme.FeedFlowThemePreview
import org.jetbrains.compose.ui.tooling.preview.Preview

val GoogleDriveLogo: ImageVector
    get() {
        val current = _googleDriveLogo
        if (current != null) return current

        return ImageVector.Builder(
            name = "your.app.package.theme.YourAppComposeTheme.OutputIconFile",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF0F0F0F)),
                pathFillType = PathFillType.EvenOdd,
            ) {
                // M 8.57 2
                moveTo(x = 8.57f, y = 2.0f)
                // a 2 2 0 0 0 -1.72 1
                arcToRelative(
                    a = 2.0f,
                    b = 2.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -1.72f,
                    dy1 = 1.0f,
                )
                // l -6.4 10.96
                lineToRelative(dx = -6.4f, dy = 10.96f)
                // a 2 2 0 0 0 0.06 2.11
                arcToRelative(
                    a = 2.0f,
                    b = 2.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.06f,
                    dy1 = 2.11f,
                )
                // l 3.36 5.04
                lineToRelative(dx = 3.36f, dy = 5.04f)
                // A 2 2 0 0 0 5.54 22
                arcTo(
                    horizontalEllipseRadius = 2.0f,
                    verticalEllipseRadius = 2.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 5.54f,
                    y1 = 22.0f,
                )
                // h 12.93
                horizontalLineToRelative(dx = 12.93f)
                // a 2 2 0 0 0 1.66 -0.89
                arcToRelative(
                    a = 2.0f,
                    b = 2.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 1.66f,
                    dy1 = -0.89f,
                )
                // l 3.36 -5.04
                lineToRelative(dx = 3.36f, dy = -5.04f)
                // a 2 2 0 0 0 0.06 -2.11
                arcToRelative(
                    a = 2.0f,
                    b = 2.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.06f,
                    dy1 = -2.11f,
                )
                // l -6.4 -10.97
                lineToRelative(dx = -6.4f, dy = -10.97f)
                // A 2 2 0 0 0 15.43 2z
                arcTo(
                    horizontalEllipseRadius = 2.0f,
                    verticalEllipseRadius = 2.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 15.43f,
                    y1 = 2.0f,
                )
                close()
                // M 9.8 4
                moveTo(x = 9.8f, y = 4.0f)
                // h 5.63
                horizontalLineToRelative(dx = 5.63f)
                // l 5.83 10
                lineToRelative(dx = 5.83f, dy = 10.0f)
                // h -5.2z
                horizontalLineToRelative(dx = -5.2f)
                close()
                // m 3.9 10
                moveToRelative(dx = 3.9f, dy = 10.0f)
                // l -1.8 -2.86
                lineToRelative(dx = -1.8f, dy = -2.86f)
                // L 10.25 14z
                lineTo(x = 10.25f, y = 14.0f)
                close()
                // m -4.63 2
                moveToRelative(dx = -4.63f, dy = 2.0f)
                // l -2.33 4
                lineToRelative(dx = -2.33f, dy = 4.0f)
                // h 11.73
                horizontalLineToRelative(dx = 11.73f)
                // l 2.66 -4z
                lineToRelative(dx = 2.66f, dy = -4.0f)
                close()
                // m 1.64 -6.77
                moveToRelative(dx = 1.64f, dy = -6.77f)
                // L 4.94 19.1
                lineTo(x = 4.94f, y = 19.1f)
                // l -2.76 -4.14
                lineToRelative(dx = -2.76f, dy = -4.14f)
                // L 8.03 4.93z
                lineTo(x = 8.03f, y = 4.93f)
                close()
            }
        }.build().also { _googleDriveLogo = it }
    }

@Preview
@Composable
private fun IconPreview() {
    FeedFlowThemePreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                imageVector = GoogleDriveLogo,
                contentDescription = null,
                modifier = Modifier
                    .width(800.0.dp)
                    .height(800.0.dp),
            )
        }
    }
}

@Suppress("ObjectPropertyName")
private var _googleDriveLogo: ImageVector? = null
