package com.prof18.feedflow.shared.ui.accounts.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// TODO: get an actual logo, this is not correct.
val GoogleDriveLogo: ImageVector
    get() {
        if (_GoogleDriveLogo != null) {
            return _GoogleDriveLogo!!
        }
        _GoogleDriveLogo = ImageVector.Builder(
            name = "GoogleDriveLogo",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF4285F4)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(6.67f, 20f)
                lineTo(10.67f, 27.33f)
                lineTo(21.33f, 27.33f)
                lineTo(17.33f, 20f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F9D58)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(14.67f, 4.67f)
                lineTo(6.67f, 20f)
                lineTo(17.33f, 20f)
                lineTo(25.33f, 4.67f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFF4B400)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(25.33f, 4.67f)
                lineTo(17.33f, 20f)
                lineTo(21.33f, 27.33f)
                lineTo(29.33f, 12f)
                close()
            }
        }.build()
        return _GoogleDriveLogo!!
    }

private var _GoogleDriveLogo: ImageVector? = null
