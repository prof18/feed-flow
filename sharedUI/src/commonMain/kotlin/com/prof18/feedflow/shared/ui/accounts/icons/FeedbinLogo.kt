package com.prof18.feedflow.shared.ui.accounts.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _Feedbin: ImageVector? = null

val Feedbin: ImageVector
    get() {
        if (_Feedbin != null) {
            return _Feedbin!!
        }
        _Feedbin = ImageVector.Builder(
            name = "Feedbin",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 256f,
            viewportHeight = 256f,
        ).apply {
            // RSS-style feed icon with Feedbin blue color
            path(
                fill = SolidColor(Color(0xFF006AFF)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                // Small circle at bottom left
                moveTo(64f, 192f)
                arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 48f, 208f)
                arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 32f, 192f)
                arcTo(16f, 16f, 0f, isMoreThanHalf = false, isPositiveArc = true, 64f, 192f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF006AFF)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 24f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                // Inner arc
                moveTo(32f, 144f)
                arcTo(80f, 80f, 0f, isMoreThanHalf = false, isPositiveArc = true, 112f, 224f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF006AFF)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 24f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                // Outer arc
                moveTo(32f, 80f)
                arcTo(144f, 144f, 0f, isMoreThanHalf = false, isPositiveArc = true, 176f, 224f)
            }
        }.build()
        return _Feedbin!!
    }
