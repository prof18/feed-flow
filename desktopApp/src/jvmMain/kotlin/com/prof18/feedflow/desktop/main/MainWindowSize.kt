package com.prof18.feedflow.desktop.main

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import java.awt.Window
import kotlin.math.roundToInt

internal val MAIN_WINDOW_MINIMUM_SIZE = DpSize(width = 360.dp, height = 294.dp)

internal fun Window.applyMainWindowMinimumSize() {
    minimumSize = Dimension(
        MAIN_WINDOW_MINIMUM_SIZE.width.value.roundToInt(),
        MAIN_WINDOW_MINIMUM_SIZE.height.value.roundToInt(),
    )
}
