package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.composeunstyled.rememberScrollbarState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class DesktopScrollbarContrastTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `light scrollbar thumb has at least three to one contrast`() = assertThumbContrast(false, false)

    @Test
    fun `dark scrollbar thumb has at least three to one contrast`() = assertThumbContrast(true, false)

    @Test
    fun `OLED scrollbar thumb has at least three to one contrast`() = assertThumbContrast(true, true)

    private fun assertThumbContrast(dark: Boolean, oled: Boolean) {
        rule.setContent {
            FeedFlowTheme(darkTheme = dark, useOledTheme = oled) {
                val list = rememberLazyListState()
                Box(
                    Modifier.size(width = 120.dp, height = 200.dp)
                        .background(JewelTheme.globalColors.panelBackground),
                ) {
                    LazyColumn(state = list) { items(count = 30) { Text("Fixture row $it") } }
                    FeedFlowVerticalScrollbar(rememberScrollbarState(list), Modifier.testTag("scrollbar"))
                }
            }
        }
        val pixels = rule.onNodeWithTag("scrollbar").captureToImage().toPixelMap()
        val background = pixels[0, pixels.height / 2].luminance()
        val contrast = (0 until pixels.height).maxOf { y ->
            val thumb = pixels[pixels.width / 2, y].luminance()
            (maxOf(thumb, background) + luminanceOffset) / (minOf(thumb, background) + luminanceOffset)
        }
        println("Rendered scrollbar contrast dark=$dark OLED=$oled: $contrast")
        assertTrue(contrast >= minimumContrast, "Rendered thumb contrast must be at least 3:1; measured $contrast")
    }

    private companion object {
        const val luminanceOffset = 0.05f
        const val minimumContrast = 3f
    }
}
