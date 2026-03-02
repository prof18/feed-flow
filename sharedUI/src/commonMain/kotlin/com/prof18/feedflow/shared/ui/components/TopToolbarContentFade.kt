package com.prof18.feedflow.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun TopToolbarContentFade(
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    maxAlpha: Float = 0.85f,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        FadeTopStop to color.copy(alpha = maxAlpha),
                        FadeMiddleStop to color.copy(alpha = maxAlpha * MiddleStopAlphaFactor),
                        FadeBottomStop to Color.Transparent,
                    ),
                ),
            ),
    )
}

private const val FadeTopStop = 0f
private const val FadeMiddleStop = 0.45f
private const val FadeBottomStop = 1f
private const val MiddleStopAlphaFactor = 0.4f
