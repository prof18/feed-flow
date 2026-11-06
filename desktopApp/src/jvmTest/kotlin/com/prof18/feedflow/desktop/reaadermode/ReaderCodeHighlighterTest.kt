package com.prof18.feedflow.desktop.reaadermode

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.jewel.intui.standalone.code.highlighting.SimpleCodeHighlighter
import org.jetbrains.jewel.intui.standalone.code.highlighting.SyntaxHighlightColors
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderCodeHighlighterTest {

    // A code block holding one very long string literal, like a minified JSON payload in an article.
    private val longStringLiteral = "{\"data\": \"${"a".repeat(200_000)}\"}"

    @Test
    fun `Jewel highlighter overflows the stack on a long string literal`() {
        val error = runCatching {
            runBlocking {
                SimpleCodeHighlighter(SyntaxHighlightColors.light())
                    .highlight(longStringLiteral, "json")
                    .first()
            }
        }.exceptionOrNull()

        assertTrue(error is StackOverflowError, "Expected a StackOverflowError, got $error")
    }

    @Test
    fun `long string literal falls back to plain text instead of crashing`() = runBlocking {
        val highlighted = readerCodeHighlighter(dark = false)
            .highlight(longStringLiteral, "json")
            .first()

        assertEquals(longStringLiteral, highlighted.text)
        assertTrue(highlighted.spanStyles.isEmpty())
    }

    @Test
    fun `short code keeps its syntax highlighting`() = runBlocking {
        val code = "{\"name\": \"FeedFlow\", \"count\": 3}"

        val highlighted = readerCodeHighlighter(dark = false)
            .highlight(code, "json")
            .first()

        assertEquals(code, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }
}
