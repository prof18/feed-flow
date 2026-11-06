package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import coil3.ImageLoader
import coil3.PlatformContext
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DesktopReaderCodeHighlighterTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a long string literal falls back to plain text instead of overflowing the regex stack`() = runBlocking {
        val highlighted = ReaderCodeHighlighter(dark = false).highlight(LONG_LITERAL_CODE, "json").first()

        assertEquals(LONG_LITERAL_CODE, highlighted.text)
    }

    @Test
    fun `short code is still highlighted`() = runBlocking {
        val code = """{"key": "value"}"""

        val highlighted = ReaderCodeHighlighter(dark = false).highlight(code, "json").first()

        assertEquals(code, highlighted.text)
        assert(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun `the reader renders a code block holding a long string literal`() {
        val markdown = "Before the code\n\n```json\n$LONG_LITERAL_CODE\n```\n\nAfter the code\n"
        rule.setContent {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                DesktopReaderMarkdown(
                    markdown = markdown,
                    fontSize = FONT_SIZE,
                    lineHeight = LINE_HEIGHT,
                    imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build(),
                    onUrlClick = {},
                    onHoveredLinkChange = {},
                )
            }
        }

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("After the code").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
    }

    private companion object {
        const val FONT_SIZE = 16
        const val LINE_HEIGHT = 3
        val LONG_LITERAL_CODE = "{\"payload\": \"${"a".repeat(200_000)}\"}"
    }
}
