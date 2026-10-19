package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import coil3.ImageLoader
import coil3.PlatformContext
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.shared.domain.readerLineHeightToTextLineHeightSp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class DesktopReaderMarkdownTest(private val theme: ThemeMode) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val files = TemporaryFolder()

    private val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()

    @Test
    fun `renders representative markdown blocks and inline extensions in every desktop theme`() {
        rule.setContent {
            FeedFlowTheme(darkTheme = theme != ThemeMode.LIGHT, useOledTheme = theme == ThemeMode.OLED) {
                DesktopReaderMarkdown(
                    markdown = REPRESENTATIVE_MARKDOWN,
                    fontSize = FONT_SIZE,
                    lineHeight = LINE_HEIGHT,
                    imageLoader = imageLoader,
                    onUrlClick = {},
                    onHoveredLinkChange = {},
                )
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Markdown heading").fetchSemanticsNodes().isNotEmpty()
        }
        listOf(
            "Markdown heading",
            "A paragraph with readable text.",
            "val answer = 42",
            "A quoted sentence.",
            "Table cell",
            "removed text",
            "https://autolink.example",
        ).forEach { text ->
            rule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `applies the selected reader font size and line height in every desktop theme`() {
        val expectedLineHeight = readerLineHeightToTextLineHeightSp(FONT_SIZE, LINE_HEIGHT)
        rule.setContent {
            FeedFlowTheme(darkTheme = theme != ThemeMode.LIGHT, useOledTheme = theme == ThemeMode.OLED) {
                DesktopReaderMarkdown(
                    markdown = TYPOGRAPHY_MARKDOWN,
                    fontSize = FONT_SIZE,
                    lineHeight = LINE_HEIGHT,
                    imageLoader = imageLoader,
                    onUrlClick = {},
                    onHoveredLinkChange = {},
                )
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Typography sample").fetchSemanticsNodes().isNotEmpty()
        }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("Typography sample")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val paragraphStyle = layouts.single().layoutInput.style
        assertEquals(FONT_SIZE.toFloat(), paragraphStyle.fontSize.value)
        assertEquals(expectedLineHeight.toFloat(), paragraphStyle.lineHeight.value)
    }

    @Test
    fun `clicking and hovering a markdown link report its destination in every desktop theme`() {
        val clickedUrls = mutableListOf<String>()
        val hoveredUrls = mutableListOf<String?>()
        rule.setContent {
            FeedFlowTheme(darkTheme = theme != ThemeMode.LIGHT, useOledTheme = theme == ThemeMode.OLED) {
                DesktopReaderMarkdown(
                    markdown = "[Open article](https://example.com/article)",
                    fontSize = FONT_SIZE,
                    lineHeight = LINE_HEIGHT,
                    imageLoader = imageLoader,
                    onUrlClick = clickedUrls::add,
                    onHoveredLinkChange = hoveredUrls::add,
                )
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Open article").fetchSemanticsNodes().isNotEmpty()
        }
        val link = rule.onNodeWithText("Open article")
        link.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        link.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val linkPosition = layouts.single().getBoundingBox(LINK_CHARACTER_OFFSET).center
        link.performMouseInput { moveTo(linkPosition) }
        rule.waitForIdle()
        assertTrue(hoveredUrls.contains("https://example.com/article"), "Hover callbacks were $hoveredUrls")

        link.performMouseInput { click(linkPosition) }
        rule.runOnIdle {
            assertEquals(listOf("https://example.com/article"), clickedUrls)
        }
    }

    @Test
    fun `small headings and heading links retain readable theme colors`() {
        var background = Color.Transparent
        rule.setContent {
            FeedFlowTheme(darkTheme = theme != ThemeMode.LIGHT, useOledTheme = theme == ThemeMode.OLED) {
                background = JewelTheme.globalColors.panelBackground
                DesktopReaderMarkdown(
                    markdown = "###### Small heading\n\n# [Heading link](https://example.com/heading)",
                    fontSize = FONT_SIZE,
                    lineHeight = LINE_HEIGHT,
                    imageLoader = imageLoader,
                    onUrlClick = {},
                    onHoveredLinkChange = {},
                )
            }
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Small heading").fetchSemanticsNodes().isNotEmpty()
        }
        val smallLayouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("Small heading")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(smallLayouts) }
        assertTrue(contrast(smallLayouts.single().layoutInput.style.color, background) >= MIN_CONTRAST)
        val linkLayouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("Heading link")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(linkLayouts) }
        val text = linkLayouts.single().layoutInput.text
        val link = text.getLinkAnnotations(0, text.length).single().item
        val linkColor = requireNotNull(link.styles?.style).color
        assertTrue(
            contrast(linkColor, background) >= MIN_CONTRAST,
            "Heading link contrast ${contrast(linkColor, background)}",
        )
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val first = foreground.compositeOver(background).luminance().toDouble()
        val second = background.luminance().toDouble()
        return (maxOf(first, second) + LUMINANCE_OFFSET) / (minOf(first, second) + LUMINANCE_OFFSET)
    }

    @Test
    fun `Coil image extension displays a local fixture without network access`() {
        val file = files.newFile("reader-image.png")
        ImageIO.write(
            BufferedImage(FIXTURE_WIDTH, FIXTURE_HEIGHT, BufferedImage.TYPE_INT_RGB),
            "png",
            file,
        )
        rule.setContent {
            FeedFlowTheme(darkTheme = theme != ThemeMode.LIGHT, useOledTheme = theme == ThemeMode.OLED) {
                DesktopReaderMarkdown(
                    markdown = "![Fixture alt text](${file.toPath().toUri()} \"Fixture image\")",
                    fontSize = FONT_SIZE,
                    lineHeight = LINE_HEIGHT,
                    imageLoader = imageLoader,
                    onUrlClick = {},
                    onHoveredLinkChange = {},
                )
            }
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithContentDescription("Fixture image").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Fixture image").assertIsDisplayed()
    }

    @After
    fun closeImageLoader() {
        imageLoader.shutdown()
    }

    companion object {
        private const val FONT_SIZE = 22
        private const val LINK_CHARACTER_OFFSET = 3
        private const val FIXTURE_WIDTH = 40
        private const val FIXTURE_HEIGHT = 24
        private const val MIN_CONTRAST = 4.5
        private const val LUMINANCE_OFFSET = 0.05
        private const val LINE_HEIGHT = 3

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED)
            .map { arrayOf(it) }

        private const val TYPOGRAPHY_MARKDOWN = "Typography sample"

        private val REPRESENTATIVE_MARKDOWN = """
            # Markdown heading

            A paragraph with readable text.

            ```kotlin
            val answer = 42
            ```

            > A quoted sentence.

            | Column | Value |
            | --- | --- |
            | Table cell | Present |

            ~~removed text~~

            https://autolink.example
        """.trimIndent()
    }
}
