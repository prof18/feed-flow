package com.prof18.feedflow.desktop.main

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import java.awt.Window
import kotlin.math.roundToInt

internal val MAIN_WINDOW_MINIMUM_SIZE = DpSize(width = 360.dp, height = 294.dp)

/**
 * The size the main window opens at: the saved size, kept on the screen but never below the minimum.
 * A screen that reports a tiny size while displays reconfigure must not shrink the window to its title bar.
 */
internal fun restoredMainWindowSize(savedWidthDp: Int, savedHeightDp: Int, screenSize: DpSize): DpSize {
    val maxWidth = screenSize.width.coerceAtLeast(MAIN_WINDOW_MINIMUM_SIZE.width)
    val maxHeight = screenSize.height.coerceAtLeast(MAIN_WINDOW_MINIMUM_SIZE.height)
    return DpSize(
        width = savedWidthDp.dp.coerceIn(MAIN_WINDOW_MINIMUM_SIZE.width, maxWidth),
        height = savedHeightDp.dp.coerceIn(MAIN_WINDOW_MINIMUM_SIZE.height, maxHeight),
    )
}

internal fun Window.applyMainWindowMinimumSize() {
    minimumSize = Dimension(
        MAIN_WINDOW_MINIMUM_SIZE.width.value.roundToInt(),
        MAIN_WINDOW_MINIMUM_SIZE.height.value.roundToInt(),
    )
}
