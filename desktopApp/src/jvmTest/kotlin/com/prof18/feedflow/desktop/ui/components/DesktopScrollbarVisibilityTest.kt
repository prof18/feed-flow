package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.styling.macOsDark
import org.jetbrains.jewel.intui.standalone.styling.macOsLight
import org.jetbrains.jewel.intui.standalone.styling.windowsAndLinuxDark
import org.jetbrains.jewel.intui.standalone.styling.windowsAndLinuxLight
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.component.styling.ScrollbarStyle
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * FeedFlow uses Jewel's stock scrollbar styles; this guards that their thumbs stay visible on FeedFlow's light,
 * dark and OLED panel backgrounds.
 */
class DesktopScrollbarVisibilityTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `light Windows and Linux scrollbar thumb is visible at rest`() =
        assertThumbVisible(dark = false, oled = false, interact = false) { ScrollbarStyle.windowsAndLinuxLight() }

    @Test
    fun `dark Windows and Linux scrollbar thumb is visible at rest`() =
        assertThumbVisible(dark = true, oled = false, interact = false) { ScrollbarStyle.windowsAndLinuxDark() }

    @Test
    fun `OLED Windows and Linux scrollbar thumb is visible at rest`() =
        assertThumbVisible(dark = true, oled = true, interact = false) { ScrollbarStyle.windowsAndLinuxDark() }

    @Test
    fun `light macOS scrollbar thumb is visible while in use`() =
        assertThumbVisible(dark = false, oled = false, interact = true) { ScrollbarStyle.macOsLight() }

    @Test
    fun `dark macOS scrollbar thumb is visible while in use`() =
        assertThumbVisible(dark = true, oled = false, interact = true) { ScrollbarStyle.macOsDark() }

    @Test
    fun `OLED macOS scrollbar thumb is visible while in use`() =
        assertThumbVisible(dark = true, oled = true, interact = true) { ScrollbarStyle.macOsDark() }

    private fun assertThumbVisible(
        dark: Boolean,
        oled: Boolean,
        interact: Boolean,
        style: @Composable () -> ScrollbarStyle,
    ) {
        rule.setContent {
            FeedFlowTheme(darkTheme = dark, useOledTheme = oled) {
                val list = rememberLazyListState()
                Box(
                    Modifier.size(width = 120.dp, height = 200.dp)
                        .background(JewelTheme.globalColors.panelBackground),
                ) {
                    LazyColumn(state = list, modifier = Modifier.testTag("list")) {
                        items(count = 30) { Text("Fixture row $it") }
                    }
                    VerticalScrollbar(
                        scrollState = list,
                        modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().testTag("scrollbar"),
                        style = style(),
                    )
                }
            }
        }
        if (interact) {
            // The macOS style hides the thumb until the list scrolls or the pointer reaches the track.
            rule.onNodeWithTag("list").performScrollToIndex(index = 5)
            rule.onNodeWithTag("scrollbar").performMouseInput { moveTo(center) }
        }
        rule.mainClock.advanceTimeBy(milliseconds = 500)
        rule.waitForIdle()

        val pixels = rule.onNodeWithTag("scrollbar").captureToImage().toPixelMap()
        val background = pixels[0, pixels.height - 1].luminance()
        val contrast = (0 until pixels.height).maxOf { y ->
            val thumb = pixels[pixels.width / 2, y].luminance()
            (maxOf(thumb, background) + luminanceOffset) / (minOf(thumb, background) + luminanceOffset)
        }
        println("Rendered scrollbar contrast dark=$dark OLED=$oled interact=$interact: $contrast")
        assertTrue(contrast >= minimumVisibleContrast, "Scrollbar thumb must be visible; measured contrast $contrast")
    }

    private companion object {
        const val luminanceOffset = 0.05f
        const val minimumVisibleContrast = 1.25f
    }
}
