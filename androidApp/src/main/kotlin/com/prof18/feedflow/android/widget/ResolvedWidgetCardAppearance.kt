package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.Color

internal data class ResolvedWidgetCardAppearance(
    val slabFillColor: Color?,
    val effectiveOuterColor: Color,
    val effectiveCardColor: Color,
    val textColors: WidgetTextColors,
    val dividerColor: Color,
)
