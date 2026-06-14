package com.prof18.feedflow.shared.domain.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderModeHtmlPreprocessorTest {

    @Test
    fun `prepare removes noisy element blocks without removing similarly named tags`() {
        val html = buildString {
            append("<html><head>")
            append("<script>window.noise = '${"x".repeat(NOISE_LENGTH)}';</script>")
            append("<script type=\"application/ld+json\">{\"headline\":\"Schema title\"}</script>")
            append("<script type=\"math/tex\">x^2</script>")
            append("<style>.ad { display: none; }</style>")
            append("<base href=\"https://example.com/\">")
            append("</head><body>")
            append("<article><p>Readable article body</p></article>")
            append("<description>This should remain because it is not a script tag.</description>")
            append("<iframe src=\"https://example.com/embed\"></iframe>")
            append("<object data=\"movie.swf\"><param name=\"src\" value=\"movie.swf\"></object>")
            append("<embed src=\"movie.swf\">")
            append("<applet code=\"LegacyApplet\"></applet>")
            append("</body></html>")
        }

        val result = ReaderModeHtmlPreprocessor.prepare(html)

        assertFalse(result.html.contains("window.noise"))
        assertFalse(result.html.contains("display: none"))
        assertFalse(result.html.contains("<base"))
        assertFalse(result.html.contains("<object"))
        assertFalse(result.html.contains("<embed"))
        assertFalse(result.html.contains("<applet"))
        assertTrue(result.html.contains("application/ld+json"))
        assertTrue(result.html.contains("Schema title"))
        assertTrue(result.html.contains("math/tex"))
        assertTrue(result.html.contains("<iframe"))
        assertTrue(result.html.contains("Readable article body"))
        assertTrue(result.html.contains("<description>"))
        assertTrue(result.removedChars > NOISE_LENGTH)
        assertFalse(result.truncated)
    }

    @Test
    fun `prepare caps very large html after removing noisy blocks`() {
        val html = buildString {
            append("<html><body>")
            append("<script>${"x".repeat(NOISE_LENGTH)}</script>")
            append("<article>")
            append("Readable ".repeat(LARGE_ARTICLE_WORDS))
            append("</article></body></html>")
        }

        val result = ReaderModeHtmlPreprocessor.prepare(html)

        assertEquals(MAX_EXPECTED_HTML_LENGTH, result.html.length)
        assertTrue(result.truncated)
        assertFalse(result.html.contains("<script>"))
    }

    private companion object {
        private const val NOISE_LENGTH = 2_000
        private const val LARGE_ARTICLE_WORDS = 700_000
        private const val MAX_EXPECTED_HTML_LENGTH = 5 * 1024 * 1024
    }
}
