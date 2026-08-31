/*
MIT License

Copyright (c) 2020 Microsoft Corporation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.prof18.feedflow.shared.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BookmarkIcon: ImageVector
    get() {
        if (_BookmarkIcon != null) return _BookmarkIcon!!

        _BookmarkIcon = ImageVector.Builder(
            name = "Bookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 0.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.19094f, 21.8547f)
                curveTo(5.6948f, 22.2117f, 5.00293f, 21.8571f, 5.00293f, 21.2459f)
                verticalLineTo(6.25f)
                curveTo(5.00293f, 4.45507f, 6.458f, 3f, 8.25293f, 3f)
                horizontalLineTo(15.7513f)
                curveTo(17.5462f, 3f, 19.0013f, 4.45507f, 19.0013f, 6.25f)
                verticalLineTo(21.2459f)
                curveTo(19.0013f, 21.8571f, 18.3094f, 22.2117f, 17.8133f, 21.8547f)
                lineTo(12.0021f, 17.6738f)
                lineTo(6.19094f, 21.8547f)
                close()
                moveTo(17.5013f, 6.25f)
                curveTo(17.5013f, 5.2835f, 16.7178f, 4.5f, 15.7513f, 4.5f)
                horizontalLineTo(8.25293f)
                curveTo(7.28643f, 4.5f, 6.50293f, 5.2835f, 6.50293f, 6.25f)
                verticalLineTo(19.7824f)
                lineTo(11.5641f, 16.141f)
                curveTo(11.8258f, 15.9528f, 12.1785f, 15.9528f, 12.4401f, 16.141f)
                lineTo(17.5013f, 19.7824f)
                verticalLineTo(6.25f)
                close()
            }
        }.build()

        return _BookmarkIcon!!
    }

private var _BookmarkIcon: ImageVector? = null
