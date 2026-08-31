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

val BookmarkOffIcon: ImageVector
    get() {
        if (_BookmarkOffIcon != null) return _BookmarkOffIcon!!

        _BookmarkOffIcon = ImageVector.Builder(
            name = "BookmarkOff",
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
                moveTo(3.28034f, 2.21968f)
                curveTo(2.98745f, 1.92678f, 2.51257f, 1.92677f, 2.21968f, 2.21966f)
                curveTo(1.92678f, 2.51255f, 1.92677f, 2.98743f, 2.21966f, 3.28032f)
                lineTo(5.00752f, 6.06823f)
                curveTo(5.00423f, 6.12813f, 5.00256f, 6.18846f, 5.00256f, 6.24918f)
                verticalLineTo(21.2451f)
                curveTo(5.00256f, 21.8563f, 5.69444f, 22.2109f, 6.19058f, 21.8539f)
                lineTo(12.0018f, 17.673f)
                lineTo(17.8129f, 21.8539f)
                curveTo(18.3091f, 22.2109f, 19.001f, 21.8563f, 19.001f, 21.2451f)
                verticalLineTo(20.062f)
                lineTo(20.7194f, 21.7805f)
                curveTo(21.0123f, 22.0734f, 21.4872f, 22.0734f, 21.7801f, 21.7805f)
                curveTo(22.073f, 21.4876f, 22.073f, 21.0127f, 21.7801f, 20.7198f)
                lineTo(3.28034f, 2.21968f)
                close()
                moveTo(17.501f, 18.5619f)
                verticalLineTo(19.7816f)
                lineTo(12.4398f, 16.1402f)
                curveTo(12.1781f, 15.952f, 11.8254f, 15.952f, 11.5637f, 16.1402f)
                lineTo(6.50256f, 19.7816f)
                verticalLineTo(7.56331f)
                lineTo(17.501f, 18.5619f)
                close()
                moveTo(17.501f, 6.24918f)
                verticalLineTo(14.3192f)
                lineTo(19.001f, 15.8192f)
                verticalLineTo(6.24918f)
                curveTo(19.001f, 4.45426f, 17.5459f, 2.99918f, 15.751f, 2.99918f)
                horizontalLineTo(8.25256f)
                curveTo(7.65756f, 2.99918f, 7.0999f, 3.15908f, 6.62021f, 3.43824f)
                lineTo(7.75336f, 4.57142f)
                curveTo(7.91155f, 4.52442f, 8.07911f, 4.49918f, 8.25256f, 4.49918f)
                horizontalLineTo(15.751f)
                curveTo(16.7175f, 4.49918f, 17.501f, 5.28269f, 17.501f, 6.24918f)
                close()
            }
        }.build()

        return _BookmarkOffIcon!!
    }

private var _BookmarkOffIcon: ImageVector? = null
