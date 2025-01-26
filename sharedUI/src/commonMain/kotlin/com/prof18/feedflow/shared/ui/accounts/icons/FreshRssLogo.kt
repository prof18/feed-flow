package com.prof18.feedflow.shared.ui.accounts.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _FreshRSS: ImageVector? = null

val FreshRSS: ImageVector
    get() {
        if (_FreshRSS != null) {
            return _FreshRSS!!
        }
        _FreshRSS = ImageVector.Builder(
            name = "Icon",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 256f,
            viewportHeight = 256f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF0062BE)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(161f, 128f)
                arcTo(33f, 33f, 0f, isMoreThanHalf = false, isPositiveArc = true, 128f, 161f)
                arcTo(33f, 33f, 0f, isMoreThanHalf = false, isPositiveArc = true, 95f, 128f)
                arcTo(33f, 33f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 128f)
                close()
            }
            group {
                group {
                    path(
                        fill = null,
                        fillAlpha = 1.0f,
                        stroke = SolidColor(Color(0xFF0062BE)),
                        strokeAlpha = 0.3f,
                        strokeLineWidth = 24f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                        strokeLineMiter = 1.0f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(12f, 128f)
                        arcTo(116f, 116f, 0f, isMoreThanHalf = true, isPositiveArc = true, 128f, 244f)
                    }
                    path(
                        fill = null,
                        fillAlpha = 1.0f,
                        stroke = SolidColor(Color(0xFF0062BE)),
                        strokeAlpha = 0.3f,
                        strokeLineWidth = 24f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                        strokeLineMiter = 1.0f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(54f, 128f)
                        arcTo(74f, 74f, 0f, isMoreThanHalf = true, isPositiveArc = true, 128f, 202f)
                    }
                }
                path(
                    fill = null,
                    fillAlpha = 1.0f,
                    stroke = SolidColor(Color(0xFF0062BE)),
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 24f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(128f, 12f)
                    arcTo(116f, 116f, 0f, isMoreThanHalf = false, isPositiveArc = true, 244f, 128f)
                }
                path(
                    fill = null,
                    fillAlpha = 1.0f,
                    stroke = SolidColor(Color(0xFF0062BE)),
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 24f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(128f, 54f)
                    arcTo(74f, 74f, 0f, isMoreThanHalf = false, isPositiveArc = true, 202f, 128f)
                }
            }
        }.build()
        return _FreshRSS!!
    }
