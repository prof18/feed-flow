package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ThemeMode
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.SelectableIconButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class ButtonHoverShapeTest(private val theme: ThemeMode) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `hovered buttons leave rounded corners clear in every theme`() {
        val sources = List(BUTTON_COUNT) { MutableInteractionSource() }
        var panel = Color.Transparent
        rule.setContent {
            FeedFlowTheme(darkTheme = theme == ThemeMode.DARK, useOledTheme = theme == ThemeMode.OLED) {
                panel = JewelTheme.globalColors.panelBackground
                Row(Modifier.background(panel)) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(40.dp).testTag("icon"),
                        interactionSource = sources[0],
                    ) {}
                    SelectableIconButton(
                        selected = true,
                        onClick = {},
                        modifier = Modifier.size(40.dp).testTag("selected"),
                        interactionSource = sources[1],
                    ) {}
                    DefaultButton(
                        onClick = {},
                        modifier = Modifier.size(width = 80.dp, height = 40.dp).testTag("default"),
                        interactionSource = sources[2],
                    ) { Text("Action") }
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.size(width = 80.dp, height = 40.dp).testTag("outlined"),
                        interactionSource = sources[3],
                    ) { Text("Action") }
                }
            }
        }
        rule.runOnIdle { sources.forEach { assertTrue(it.tryEmit(HoverInteraction.Enter())) } }
        rule.waitForIdle()
        val captures = listOf("icon", "selected", "default", "outlined").associateWith { tag ->
            val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
            val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) image.setRGB(x, y, pixels[x, y].toArgb())
            }
            val output = File("build/reports/tests/button-shapes/$theme-$tag.png")
            output.parentFile.mkdirs()
            ImageIO.write(image, "png", output)
            pixels
        }
        captures.forEach { (tag, pixels) ->
            val cornerRatio = if (tag == "icon" || tag == "selected") ICON_CORNER_X else TEXT_CORNER_X
            val cornerX = (pixels.width * cornerRatio).toInt()
            val cornerY = (pixels.height * CORNER_Y).toInt()
            assertTrue(
                matches(pixels[cornerX, cornerY], panel),
                "$tag/$theme corner ${pixels[cornerX, cornerY]} at $cornerX,$cornerY differs from $panel",
            )
            assertTrue(
                (0 until pixels.height / EDGE_Y_DIVISOR).any { y -> !matches(pixels[pixels.width / 2, y], panel) },
                "$tag/$theme must retain its visible hover/selection background or border",
            )
        }
    }

    private fun matches(first: Color, second: Color): Boolean =
        abs(first.red - second.red) < CHANNEL_TOLERANCE &&
            abs(first.green - second.green) < CHANNEL_TOLERANCE &&
            abs(first.blue - second.blue) < CHANNEL_TOLERANCE

    companion object {
        private const val BUTTON_COUNT = 4
        private const val ICON_CORNER_X = 0.05f
        private const val TEXT_CORNER_X = 0.0125f
        private const val CORNER_Y = 0.025f
        private const val EDGE_Y_DIVISOR = 4
        private const val CHANNEL_TOLERANCE = 0.02f

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun themes(): List<Array<ThemeMode>> = listOf(
            ThemeMode.LIGHT,
            ThemeMode.DARK,
            ThemeMode.OLED,
        ).map { arrayOf(it) }
    }
}
