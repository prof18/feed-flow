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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.utils.PreviewTheme

val Miniflux: ImageVector
    get() {
        val current = _minifluxLogo
        if (current != null) return current

        return ImageVector.Builder(
            name = "Miniflux",
            defaultWidth = 32.dp,
            defaultHeight = 22.14.dp,
            viewportWidth = 431.27f,
            viewportHeight = 298.29f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                // M 140.51 14.5
                moveTo(x = 140.51f, y = 14.5f)
                // a 96 96 0 0 1 45.48 -11
                arcToRelative(
                    a = 96.0f,
                    b = 96.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 45.48f,
                    dy1 = -11.0f,
                )
                // q 50.36 0 64.16 43.8
                quadToRelative(
                    dx1 = 50.36f,
                    dy1 = 0.0f,
                    dx2 = 64.16f,
                    dy2 = 43.8f,
                )
                // a 139 139 0 0 1 37.57 -31.07
                arcToRelative(
                    a = 139.0f,
                    b = 139.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 37.57f,
                    dy1 = -31.07f,
                )
                // Q 309.85 3.5 337.06 3.5
                quadTo(
                    x1 = 309.85f,
                    y1 = 3.5f,
                    x2 = 337.06f,
                    y2 = 3.5f,
                )
                // q 37.77 0 54 23.08
                quadToRelative(
                    dx1 = 37.77f,
                    dy1 = 0.0f,
                    dx2 = 54.0f,
                    dy2 = 23.08f,
                )
                // t 16.21 69.3
                reflectiveQuadToRelative(
                    dx1 = 16.21f,
                    dy1 = 69.3f,
                )
                // v 174
                verticalLineToRelative(dy = 174.0f)
                // q 0 6.52 1.83 8.88
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 6.52f,
                    dx2 = 1.83f,
                    dy2 = 8.88f,
                )
                // c 1.83 2.36 3.86 3 7.92 4.14
                curveToRelative(
                    dx1 = 1.83f,
                    dy1 = 2.36f,
                    dx2 = 3.86f,
                    dy2 = 3.0f,
                    dx3 = 7.92f,
                    dy3 = 4.14f,
                )
                // l 14.25 4.74
                lineToRelative(dx = 14.25f, dy = 4.74f)
                // v 10.65
                verticalLineToRelative(dy = 10.65f)
                // H 346.8
                horizontalLineTo(x = 346.8f)
                // q -11 0 -15.83 -8.29
                quadToRelative(
                    dx1 = -11.0f,
                    dy1 = 0.0f,
                    dx2 = -15.83f,
                    dy2 = -8.29f,
                )
                // t -4.88 -24.86
                reflectiveQuadToRelative(
                    dx1 = -4.88f,
                    dy1 = -24.86f,
                )
                // V 85.23
                verticalLineTo(y = 85.23f)
                // q 0 -26.64 -5.89 -37.88
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -26.64f,
                    dx2 = -5.89f,
                    dy2 = -37.88f,
                )
                // T 300.51 36.1
                reflectiveQuadTo(
                    x1 = 300.51f,
                    y1 = 36.1f,
                )
                // q -21.93 0 -46.7 26
                quadToRelative(
                    dx1 = -21.93f,
                    dy1 = 0.0f,
                    dx2 = -46.7f,
                    dy2 = 26.0f,
                )
                // a 210 210 0 0 1 2.46 33.78
                arcToRelative(
                    a = 210.0f,
                    b = 210.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.46f,
                    dy1 = 33.78f,
                )
                // v 174
                verticalLineToRelative(dy = 174.0f)
                // q 0 6.52 1.83 8.88
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 6.52f,
                    dx2 = 1.83f,
                    dy2 = 8.88f,
                )
                // c 1.83 2.36 3.86 3 7.92 4.14
                curveToRelative(
                    dx1 = 1.83f,
                    dy1 = 2.36f,
                    dx2 = 3.86f,
                    dy2 = 3.0f,
                    dx3 = 7.92f,
                    dy3 = 4.14f,
                )
                // l 14.21 4.74
                lineToRelative(dx = 14.21f, dy = 4.74f)
                // v 10.65
                verticalLineToRelative(dy = 10.65f)
                // h -84.49
                horizontalLineToRelative(dx = -84.49f)
                // q -11 0 -15.84 -8.29
                quadToRelative(
                    dx1 = -11.0f,
                    dy1 = 0.0f,
                    dx2 = -15.84f,
                    dy2 = -8.29f,
                )
                // t -4.88 -24.86
                reflectiveQuadToRelative(
                    dx1 = -4.88f,
                    dy1 = -24.86f,
                )
                // V 85.23
                verticalLineTo(y = 85.23f)
                // q 0 -26.64 -5.88 -37.88
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -26.64f,
                    dx2 = -5.88f,
                    dy2 = -37.88f,
                )
                // t -19.7 -11.25
                reflectiveQuadToRelative(
                    dx1 = -19.7f,
                    dy1 = -11.25f,
                )
                // q -21.53 0 -44.26 23.68
                quadToRelative(
                    dx1 = -21.53f,
                    dy1 = 0.0f,
                    dx2 = -44.26f,
                    dy2 = 23.68f,
                )
                // v 210.1
                verticalLineToRelative(dy = 210.1f)
                // q 0 6.52 1.82 9.17
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 6.52f,
                    dx2 = 1.82f,
                    dy2 = 9.17f,
                )
                // c 1.82 2.65 3.72 3.26 7.52 4.44
                curveToRelative(
                    dx1 = 1.82f,
                    dy1 = 2.65f,
                    dx2 = 3.72f,
                    dy2 = 3.26f,
                    dx3 = 7.52f,
                    dy3 = 4.44f,
                )
                // l 13.8 4.15
                lineToRelative(dx = 13.8f, dy = 4.15f)
                // v 10.65
                verticalLineToRelative(dy = 10.65f)
                // H 0
                horizontalLineTo(x = 0.0f)
                // v -10.65
                verticalLineToRelative(dy = -10.65f)
                // l 14.21 -4.74
                lineToRelative(dx = 14.21f, dy = -4.74f)
                // q 6.09 -1.77 7.92 -4.14
                quadToRelative(
                    dx1 = 6.09f,
                    dy1 = -1.77f,
                    dx2 = 7.92f,
                    dy2 = -4.14f,
                )
                // t 1.83 -8.88
                reflectiveQuadToRelative(
                    dx1 = 1.83f,
                    dy1 = -8.88f,
                )
                // V 46.17
                verticalLineTo(y = 46.17f)
                // q 0 -6.51 -1.83 -8.88
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -6.51f,
                    dx2 = -1.83f,
                    dy2 = -8.88f,
                )
                // t -7.92 -4.15
                reflectiveQuadToRelative(
                    dx1 = -7.92f,
                    dy1 = -4.15f,
                )
                // L 0 28.41
                lineTo(x = 0.0f, y = 28.41f)
                // V 17.76
                verticalLineTo(y = 17.76f)
                // L 97.46 0
                lineTo(x = 97.46f, y = 0.0f)
                // h 6.9
                horizontalLineToRelative(dx = 6.9f)
                // v 41.43
                verticalLineToRelative(dy = 41.43f)
                // a 143 143 0 0 1 36.15 -26.93
                arcToRelative(
                    a = 143.0f,
                    b = 143.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 36.15f,
                    dy1 = -26.93f,
                )
            }
        }.build().also { _minifluxLogo = it }
    }

@Preview
@Composable
private fun IconPreview() {
    PreviewTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                imageVector = Miniflux,
                contentDescription = null,
                modifier = Modifier
                    .width(32.dp)
                    .height(22.14.dp),
            )
        }
    }
}

@Suppress("ObjectPropertyName")
private var _minifluxLogo: ImageVector? = null
