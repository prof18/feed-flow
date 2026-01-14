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
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.utils.PreviewTheme

private var _Feedbin: ImageVector? = null

val Feedbin: ImageVector
    get() {
        val current = _Feedbin
        if (current != null) return current

        return ImageVector.Builder(
            name = "Feedbin",
            defaultWidth = 32.dp,
            defaultHeight = 29.dp,
            viewportWidth = 50.0f,
            viewportHeight = 47.0f,
        ).apply {
            group(
                // M 0 0 h 50 v 46.22 H 0z
                clipPathData = PathData {
                    // M 0 0
                    moveTo(x = 0.0f, y = 0.0f)
                    // h 50
                    horizontalLineToRelative(dx = 50.0f)
                    // v 46.22
                    verticalLineToRelative(dy = 46.22f)
                    // H 0z
                    horizontalLineTo(x = 0.0f)
                    close()
                },
            ) {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    // M 49.7 22.48
                    moveTo(x = 49.7f, y = 22.48f)
                    // a 10 10 0 0 0 -1.22 -2.61
                    arcToRelative(
                        a = 10.0f,
                        b = 10.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -1.22f,
                        dy1 = -2.61f,
                    )
                    // a 0.8 0.8 0 0 1 -0.14 -0.68
                    arcToRelative(
                        a = 0.8f,
                        b = 0.8f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -0.14f,
                        dy1 = -0.68f,
                    )
                    // c 0.4 -1.9 0.3 -3.79 -0.22 -5.65
                    curveToRelative(
                        dx1 = 0.4f,
                        dy1 = -1.9f,
                        dx2 = 0.3f,
                        dy2 = -3.79f,
                        dx3 = -0.22f,
                        dy3 = -5.65f,
                    )
                    // a 14 14 0 0 0 -2.94 -5.37
                    arcToRelative(
                        a = 14.0f,
                        b = 14.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -2.94f,
                        dy1 = -5.37f,
                    )
                    // a 20 20 0 0 0 -5.81 -4.63
                    arcToRelative(
                        a = 20.0f,
                        b = 20.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -5.81f,
                        dy1 = -4.63f,
                    )
                    // a 30 30 0 0 0 -13.1 -3.5
                    arcToRelative(
                        a = 30.0f,
                        b = 30.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -13.1f,
                        dy1 = -3.5f,
                    )
                    // a 33 33 0 0 0 -6.22 0.3
                    arcToRelative(
                        a = 33.0f,
                        b = 33.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -6.22f,
                        dy1 = 0.3f,
                    )
                    // a 30 30 0 0 0 -7.4 2.03
                    arcToRelative(
                        a = 30.0f,
                        b = 30.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -7.4f,
                        dy1 = 2.03f,
                    )
                    // A 23 23 0 0 0 5.75 6.8
                    arcTo(
                        horizontalEllipseRadius = 23.0f,
                        verticalEllipseRadius = 23.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        x1 = 5.75f,
                        y1 = 6.8f,
                    )
                    // c -1.48 1.4 -2.71 3 -3.55 4.86
                    curveToRelative(
                        dx1 = -1.48f,
                        dy1 = 1.4f,
                        dx2 = -2.71f,
                        dy2 = 3.0f,
                        dx3 = -3.55f,
                        dy3 = 4.86f,
                    )
                    // a 11.8 11.8 0 0 0 -0.6 8.7
                    arcToRelative(
                        a = 11.8f,
                        b = 11.8f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -0.6f,
                        dy1 = 8.7f,
                    )
                    // C 1.67 20.62 1.62 20.8 1.47 21
                    curveTo(
                        x1 = 1.67f,
                        y1 = 20.62f,
                        x2 = 1.62f,
                        y2 = 20.8f,
                        x3 = 1.47f,
                        y3 = 21.0f,
                    )
                    // a 9 9 0 0 0 -1.12 1.96
                    arcToRelative(
                        a = 9.0f,
                        b = 9.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -1.12f,
                        dy1 = 1.96f,
                    )
                    // a 4 4 0 0 0 -0.3 1.2
                    arcToRelative(
                        a = 4.0f,
                        b = 4.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -0.3f,
                        dy1 = 1.2f,
                    )
                    // a 12 12 0 0 0 0 2.15
                    arcToRelative(
                        a = 12.0f,
                        b = 12.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 0.0f,
                        dy1 = 2.15f,
                    )
                    // a 5.2 5.2 0 0 0 1.52 3.38
                    arcToRelative(
                        a = 5.2f,
                        b = 5.2f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 1.52f,
                        dy1 = 3.38f,
                    )
                    // l 0.14 0.17
                    lineToRelative(dx = 0.14f, dy = 0.17f)
                    // v 0.9
                    verticalLineToRelative(dy = 0.9f)
                    // c 0 1.27 0.05 2.53 0.24 3.79
                    curveToRelative(
                        dx1 = 0.0f,
                        dy1 = 1.27f,
                        dx2 = 0.05f,
                        dy2 = 2.53f,
                        dx3 = 0.24f,
                        dy3 = 3.79f,
                    )
                    // a 11 11 0 0 0 3.75 6.78
                    arcToRelative(
                        a = 11.0f,
                        b = 11.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 3.75f,
                        dy1 = 6.78f,
                    )
                    // a 15 15 0 0 0 3.62 2.29
                    arcToRelative(
                        a = 15.0f,
                        b = 15.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 3.62f,
                        dy1 = 2.29f,
                    )
                    // a 25 25 0 0 0 5.32 1.7
                    arcToRelative(
                        a = 25.0f,
                        b = 25.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 5.32f,
                        dy1 = 1.7f,
                    )
                    // c 2.6 0.54 5.22 0.8 7.87 0.87
                    curveToRelative(
                        dx1 = 2.6f,
                        dy1 = 0.54f,
                        dx2 = 5.22f,
                        dy2 = 0.8f,
                        dx3 = 7.87f,
                        dy3 = 0.87f,
                    )
                    // a 78 78 0 0 0 10.06 -0.3
                    arcToRelative(
                        a = 78.0f,
                        b = 78.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 10.06f,
                        dy1 = -0.3f,
                    )
                    // a 36 36 0 0 0 4.97 -0.81
                    arcToRelative(
                        a = 36.0f,
                        b = 36.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 4.97f,
                        dy1 = -0.81f,
                    )
                    // a 18 18 0 0 0 4.63 -1.79
                    arcToRelative(
                        a = 18.0f,
                        b = 18.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 4.63f,
                        dy1 = -1.79f,
                    )
                    // A 11 11 0 0 0 46.92 38
                    arcTo(
                        horizontalEllipseRadius = 11.0f,
                        verticalEllipseRadius = 11.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        x1 = 46.92f,
                        y1 = 38.0f,
                    )
                    // a 17 17 0 0 0 1.15 -4.2
                    arcToRelative(
                        a = 17.0f,
                        b = 17.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 1.15f,
                        dy1 = -4.2f,
                    )
                    // a 21 21 0 0 0 0.16 -4.45
                    arcToRelative(
                        a = 21.0f,
                        b = 21.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 0.16f,
                        dy1 = -4.45f,
                    )
                    // v -0.5
                    verticalLineToRelative(dy = -0.5f)
                    // l 0.23 -0.2
                    lineToRelative(dx = 0.23f, dy = -0.2f)
                    // a 3.3 3.3 0 0 0 1.33 -2.09
                    arcToRelative(
                        a = 3.3f,
                        b = 3.3f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 1.33f,
                        dy1 = -2.09f,
                    )
                    // a 8.3 8.3 0 0 0 -0.1 -4.08
                    arcToRelative(
                        a = 8.3f,
                        b = 8.3f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -0.1f,
                        dy1 = -4.08f,
                    )
                    // M 5.3 13.92
                    moveTo(x = 5.3f, y = 13.92f)
                    // q 0.67 -1.79 1.92 -3.25
                    quadToRelative(
                        dx1 = 0.67f,
                        dy1 = -1.79f,
                        dx2 = 1.92f,
                        dy2 = -3.25f,
                    )
                    // a 16 16 0 0 1 4 -3.38
                    arcToRelative(
                        a = 16.0f,
                        b = 16.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 4.0f,
                        dy1 = -3.38f,
                    )
                    // a 23 23 0 0 1 6.73 -2.74
                    arcToRelative(
                        a = 23.0f,
                        b = 23.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 6.73f,
                        dy1 = -2.74f,
                    )
                    // a 29 29 0 0 1 8.26 -0.77
                    arcToRelative(
                        a = 29.0f,
                        b = 29.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 8.26f,
                        dy1 = -0.77f,
                    )
                    // a 27 27 0 0 1 10.22 2.47
                    arcToRelative(
                        a = 27.0f,
                        b = 27.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 10.22f,
                        dy1 = 2.47f,
                    )
                    // a 12 12 0 0 1 2.75 1.77
                    arcToRelative(
                        a = 12.0f,
                        b = 12.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 2.75f,
                        dy1 = 1.77f,
                    )
                    // a 6 6 0 0 1 1.57 2.05
                    arcToRelative(
                        a = 6.0f,
                        b = 6.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 1.57f,
                        dy1 = 2.05f,
                    )
                    // a 5.3 5.3 0 0 1 0.34 3.21
                    arcToRelative(
                        a = 5.3f,
                        b = 5.3f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 0.34f,
                        dy1 = 3.21f,
                    )
                    // a 8.8 8.8 0 0 1 -3.6 5.69
                    arcToRelative(
                        a = 8.8f,
                        b = 8.8f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -3.6f,
                        dy1 = 5.69f,
                    )
                    // a 15 15 0 0 1 -2.97 1.65
                    arcToRelative(
                        a = 15.0f,
                        b = 15.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -2.97f,
                        dy1 = 1.65f,
                    )
                    // q -0.89 0.37 -1.78 0.72
                    quadToRelative(
                        dx1 = -0.89f,
                        dy1 = 0.37f,
                        dx2 = -1.78f,
                        dy2 = 0.72f,
                    )
                    // a 6 6 0 0 0 -0.6 0.3
                    arcToRelative(
                        a = 6.0f,
                        b = 6.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -0.6f,
                        dy1 = 0.3f,
                    )
                    // a 1 1 0 0 0 -0.3 0.25
                    arcToRelative(
                        a = 1.0f,
                        b = 1.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = -0.3f,
                        dy1 = 0.25f,
                    )
                    // c -0.13 0.14 -0.11 0.35 0.05 0.44
                    curveToRelative(
                        dx1 = -0.13f,
                        dy1 = 0.14f,
                        dx2 = -0.11f,
                        dy2 = 0.35f,
                        dx3 = 0.05f,
                        dy3 = 0.44f,
                    )
                    // a 2 2 0 0 0 0.57 0.22
                    arcToRelative(
                        a = 2.0f,
                        b = 2.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 0.57f,
                        dy1 = 0.22f,
                    )
                    // a 7 7 0 0 0 2.03 0.08
                    arcToRelative(
                        a = 7.0f,
                        b = 7.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 2.03f,
                        dy1 = 0.08f,
                    )
                    // a 26 26 0 0 0 5.5 -1
                    arcToRelative(
                        a = 26.0f,
                        b = 26.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 5.5f,
                        dy1 = -1.0f,
                    )
                    // a 13 13 0 0 0 4.01 -1.93
                    arcToRelative(
                        a = 13.0f,
                        b = 13.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 4.01f,
                        dy1 = -1.93f,
                    )
                    // q 0.1 -0.07 0.2 -0.11
                    quadToRelative(
                        dx1 = 0.1f,
                        dy1 = -0.07f,
                        dx2 = 0.2f,
                        dy2 = -0.11f,
                    )
                    // l 0.08 0.02
                    lineToRelative(dx = 0.08f, dy = 0.02f)
                    // l -0.12 0.3
                    lineToRelative(dx = -0.12f, dy = 0.3f)
                    // a 8 8 0 0 1 -1.86 2.4
                    arcToRelative(
                        a = 8.0f,
                        b = 8.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -1.86f,
                        dy1 = 2.4f,
                    )
                    // a 13 13 0 0 1 -3.03 1.96
                    arcToRelative(
                        a = 13.0f,
                        b = 13.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -3.03f,
                        dy1 = 1.96f,
                    )
                    // a 24 24 0 0 1 -5.3 1.77
                    arcToRelative(
                        a = 24.0f,
                        b = 24.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -5.3f,
                        dy1 = 1.77f,
                    )
                    // a 41 41 0 0 1 -6.34 0.83
                    arcToRelative(
                        a = 41.0f,
                        b = 41.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -6.34f,
                        dy1 = 0.83f,
                    )
                    // a 49 49 0 0 1 -6.33 -0.04
                    arcToRelative(
                        a = 49.0f,
                        b = 49.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -6.33f,
                        dy1 = -0.04f,
                    )
                    // a 35 35 0 0 1 -8.5 -1.56
                    arcToRelative(
                        a = 35.0f,
                        b = 35.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -8.5f,
                        dy1 = -1.56f,
                    )
                    // a 19 19 0 0 1 -3.6 -1.6
                    arcToRelative(
                        a = 19.0f,
                        b = 19.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -3.6f,
                        dy1 = -1.6f,
                    )
                    // A 11 11 0 0 1 6.98 22
                    arcTo(
                        horizontalEllipseRadius = 11.0f,
                        verticalEllipseRadius = 11.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        x1 = 6.98f,
                        y1 = 22.0f,
                    )
                    // a 7 7 0 0 1 -2.06 -3.63
                    arcToRelative(
                        a = 7.0f,
                        b = 7.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -2.06f,
                        dy1 = -3.63f,
                    )
                    // a 8 8 0 0 1 0.38 -4.46
                    arcToRelative(
                        a = 8.0f,
                        b = 8.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 0.38f,
                        dy1 = -4.46f,
                    )
                    // m 39.53 19.85
                    moveToRelative(dx = 39.53f, dy = 19.85f)
                    // a 7 7 0 0 1 -1.54 3.1
                    arcToRelative(
                        a = 7.0f,
                        b = 7.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -1.54f,
                        dy1 = 3.1f,
                    )
                    // a 10 10 0 0 1 -2.52 2.09
                    arcToRelative(
                        a = 10.0f,
                        b = 10.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -2.52f,
                        dy1 = 2.09f,
                    )
                    // a 18 18 0 0 1 -4.5 1.8
                    arcToRelative(
                        a = 18.0f,
                        b = 18.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -4.5f,
                        dy1 = 1.8f,
                    )
                    // a 34 34 0 0 1 -5.29 0.94
                    arcToRelative(
                        a = 34.0f,
                        b = 34.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -5.29f,
                        dy1 = 0.94f,
                    )
                    // a 60 60 0 0 1 -7.12 0.3
                    arcToRelative(
                        a = 60.0f,
                        b = 60.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -7.12f,
                        dy1 = 0.3f,
                    )
                    // q -3 -0.03 -5.98 -0.45
                    quadToRelative(
                        dx1 = -3.0f,
                        dy1 = -0.03f,
                        dx2 = -5.98f,
                        dy2 = -0.45f,
                    )
                    // a 25 25 0 0 1 -6.06 -1.57
                    arcToRelative(
                        a = 25.0f,
                        b = 25.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -6.06f,
                        dy1 = -1.57f,
                    )
                    // a 14 14 0 0 1 -3.56 -2.07
                    arcToRelative(
                        a = 14.0f,
                        b = 14.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -3.56f,
                        dy1 = -2.07f,
                    )
                    // a 8.7 8.7 0 0 1 -2.95 -4.32
                    arcToRelative(
                        a = 8.7f,
                        b = 8.7f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -2.95f,
                        dy1 = -4.32f,
                    )
                    // a 7 7 0 0 1 -0.3 -2.07
                    arcToRelative(
                        a = 7.0f,
                        b = 7.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -0.3f,
                        dy1 = -2.07f,
                    )
                    // l 0.03 -0.34
                    lineToRelative(dx = 0.03f, dy = -0.34f)
                    // h 0.47
                    horizontalLineToRelative(dx = 0.47f)
                    // a 11 11 0 0 0 2.03 0.04
                    arcToRelative(
                        a = 11.0f,
                        b = 11.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 2.03f,
                        dy1 = 0.04f,
                    )
                    // a 7.4 7.4 0 0 1 4.82 1.33
                    arcToRelative(
                        a = 7.4f,
                        b = 7.4f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 4.82f,
                        dy1 = 1.33f,
                    )
                    // a 20 20 0 0 1 2.44 2
                    arcToRelative(
                        a = 20.0f,
                        b = 20.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 2.44f,
                        dy1 = 2.0f,
                    )
                    // l 1.78 1.75
                    lineToRelative(dx = 1.78f, dy = 1.75f)
                    // a 9 9 0 0 0 2.33 1.69
                    arcToRelative(
                        a = 9.0f,
                        b = 9.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 2.33f,
                        dy1 = 1.69f,
                    )
                    // a 7 7 0 0 0 3.1 0.73
                    arcToRelative(
                        a = 7.0f,
                        b = 7.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 3.1f,
                        dy1 = 0.73f,
                    )
                    // A 7 7 0 0 0 24.91 38
                    arcTo(
                        horizontalEllipseRadius = 7.0f,
                        verticalEllipseRadius = 7.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        x1 = 24.91f,
                        y1 = 38.0f,
                    )
                    // a 10 10 0 0 0 2.02 -1.34
                    arcToRelative(
                        a = 10.0f,
                        b = 10.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 2.02f,
                        dy1 = -1.34f,
                    )
                    // l 0.67 -0.53
                    lineToRelative(dx = 0.67f, dy = -0.53f)
                    // a 4.9 4.9 0 0 1 3.75 -0.85
                    arcToRelative(
                        a = 4.9f,
                        b = 4.9f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 3.75f,
                        dy1 = -0.85f,
                    )
                    // c 0.77 0.12 1.5 0.36 2.25 0.57
                    curveToRelative(
                        dx1 = 0.77f,
                        dy1 = 0.12f,
                        dx2 = 1.5f,
                        dy2 = 0.36f,
                        dx3 = 2.25f,
                        dy3 = 0.57f,
                    )
                    // c 0.7 0.2 1.41 0.42 2.13 0.56
                    curveToRelative(
                        dx1 = 0.7f,
                        dy1 = 0.2f,
                        dx2 = 1.41f,
                        dy2 = 0.42f,
                        dx3 = 2.13f,
                        dy3 = 0.56f,
                    )
                    // a 5 5 0 0 0 2.3 -0.04
                    arcToRelative(
                        a = 5.0f,
                        b = 5.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 2.3f,
                        dy1 = -0.04f,
                    )
                    // a 2.3 2.3 0 0 0 1.67 -1.4
                    arcToRelative(
                        a = 2.3f,
                        b = 2.3f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = false,
                        dx1 = 1.67f,
                        dy1 = -1.4f,
                    )
                    // q 0.24 -0.67 0.43 -1.34
                    quadToRelative(
                        dx1 = 0.24f,
                        dy1 = -0.67f,
                        dx2 = 0.43f,
                        dy2 = -1.34f,
                    )
                    // q 0.16 -0.58 0.34 -1.14
                    quadToRelative(
                        dx1 = 0.16f,
                        dy1 = -0.58f,
                        dx2 = 0.34f,
                        dy2 = -1.14f,
                    )
                    // a 2.7 2.7 0 0 1 2.05 -1.85
                    arcToRelative(
                        a = 2.7f,
                        b = 2.7f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = 2.05f,
                        dy1 = -1.85f,
                    )
                    // l 2.5 -0.6
                    lineToRelative(dx = 2.5f, dy = -0.6f)
                    // q 0.04 0.17 0.06 0.3
                    quadToRelative(
                        dx1 = 0.04f,
                        dy1 = 0.17f,
                        dx2 = 0.06f,
                        dy2 = 0.3f,
                    )
                    // a 17 17 0 0 1 -0.24 3.43z
                    arcToRelative(
                        a = 17.0f,
                        b = 17.0f,
                        theta = 0.0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        dx1 = -0.24f,
                        dy1 = 3.43f,
                    )
                    close()
                }
            }
        }.build().also { _Feedbin = it }
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
                imageVector = Feedbin,
                contentDescription = null,
                modifier = Modifier
                    .width(32.0.dp)
                    .height(29.0.dp),
            )
        }
    }
}
