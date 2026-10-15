package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class SettingsChoiceAppearanceTest(
    private val theme: ThemeMode,
    private val width: Int,
    private val fontScale: Float,
    private val direction: LayoutDirection,
) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `choice rows keep labels readable and popup options within available width`() {
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalLayoutDirection provides direction,
            ) {
                FeedFlowTheme(darkTheme = theme == ThemeMode.DARK, useOledTheme = theme == ThemeMode.OLED) {
                    Box(
                        Modifier.requiredSize(width = width.dp, height = 440.dp)
                            .background(JewelTheme.globalColors.panelBackground).testTag("settings-choice-surface"),
                    ) {
                        SettingsChoice(
                            title = TITLE,
                            supportingText = DESCRIPTION,
                            choices = persistentListOf(
                                0 to "Every hour",
                                1 to "Every twelve hours while the application remains open",
                                2 to "Never",
                            ),
                            selected = 0,
                            onSelected = {},
                        )
                    }
                }
            }
        }
        val titleBounds = rule.onNodeWithText(TITLE).fetchSemanticsNode().boundsInRoot
        val control = rule.onNodeWithContentDescription(TITLE)
        val controlBounds = control.fetchSemanticsNode().boundsInRoot
        val inline = width == WIDE_WIDTH && fontScale == 1f
        if (inline) {
            assertTrue(titleBounds.bottom > controlBounds.top && titleBounds.top < controlBounds.bottom)
            if (direction == LayoutDirection.Ltr) {
                assertTrue(titleBounds.right < controlBounds.left)
            } else {
                assertTrue(controlBounds.right < titleBounds.left)
            }
        } else {
            assertTrue(titleBounds.bottom < controlBounds.top, "Constrained rows must stack without overlap")
        }
        assertTextFits(TITLE)
        assertTextFits(DESCRIPTION)
        saveCapture("row", "settings-choice-surface")

        control.performClick()
        val popup = rule.onNodeWithTag("Jewel.ComboBox.Popup")
        assertTrue(
            popup.fetchSemanticsNode().boundsInRoot.width <= rule.onNodeWithTag("settings-choice-surface")
                .fetchSemanticsNode().boundsInRoot.width,
            "Choice popup must fit the available settings width",
        )
        assertTextFits("Every twelve hours while the application remains open")
        saveCapture("popup", "Jewel.ComboBox.Popup")
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow, "$text must wrap without clipping")
    }

    private fun saveCapture(name: String, tag: String) {
        val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) image.setRGB(x, y, pixels[x, y].toArgb())
        }
        val output = File("build/reports/tests/settings-choices/$theme-$width-$fontScale-$direction-$name.png")
        output.parentFile.mkdirs()
        ImageIO.write(image, "png", output)
    }

    companion object {
        private const val TITLE = "Background synchronization"
        private const val DESCRIPTION = "Choose when FeedFlow checks for new articles."
        private const val WIDE_WIDTH = 600
        private const val NARROW_WIDTH = 280
        private const val LARGE_FONT_SCALE = 2f

        @JvmStatic
        @Parameterized.Parameters(name = "{0}, width {1}, font {2}, {3}")
        fun cases(): List<Array<Any>> = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).map { theme ->
            arrayOf<Any>(theme, WIDE_WIDTH, 1f, LayoutDirection.Ltr)
        } + listOf(
            arrayOf<Any>(ThemeMode.DARK, NARROW_WIDTH, 1f, LayoutDirection.Rtl),
            arrayOf<Any>(ThemeMode.DARK, NARROW_WIDTH, LARGE_FONT_SCALE, LayoutDirection.Ltr),
            arrayOf<Any>(ThemeMode.DARK, WIDE_WIDTH, LARGE_FONT_SCALE, LayoutDirection.Ltr),
        )
    }
}
