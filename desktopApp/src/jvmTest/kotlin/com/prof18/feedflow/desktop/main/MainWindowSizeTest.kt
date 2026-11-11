package com.prof18.feedflow.desktop.main

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class MainWindowSizeTest {

    private val screen = DpSize(2060.dp, 1260.dp)

    @Test
    fun `a saved size that fits the screen is restored as is`() {
        assertEquals(DpSize(1280.dp, 800.dp), restoredMainWindowSize(1280, 800, screen))
    }

    @Test
    fun `a saved size larger than the screen shrinks to the screen`() {
        assertEquals(screen, restoredMainWindowSize(4000, 3000, screen))
    }

    @Test
    fun `a tiny saved size grows to the minimum window size`() {
        assertEquals(MAIN_WINDOW_MINIMUM_SIZE, restoredMainWindowSize(80, 28, screen))
    }

    @Test
    fun `a screen that reports no size does not shrink the window below the minimum`() {
        assertEquals(MAIN_WINDOW_MINIMUM_SIZE, restoredMainWindowSize(1280, 800, DpSize(0.dp, 0.dp)))
    }
}
