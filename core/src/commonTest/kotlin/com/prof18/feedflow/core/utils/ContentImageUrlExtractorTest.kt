package com.prof18.feedflow.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContentImageUrlExtractorTest {

    @Test
    fun `extracts first image url from html content`() {
        val content = """<p>Some text <img src="https://example.com/image.png"> more text</p>"""

        assertEquals(
            "https://example.com/image.png",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `keeps the query string of the image url`() {
        val content =
            """<p><a href="https://www.tagesschau.de/article.html">""" +
                """<img src="https://images.tagesschau.de/image/abc/16x9-big/schnieder-144.jpg?width=1920" /></a></p>"""

        assertEquals(
            "https://images.tagesschau.de/image/abc/16x9-big/schnieder-144.jpg?width=1920",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `decodes html entities inside the image url`() {
        val content = """<img src="https://example.com/image.jpg?w=180&amp;h=180">"""

        assertEquals(
            "https://example.com/image.jpg?w=180&h=180",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `ignores emoji images`() {
        val content = """<img src="https://s.w.org/images/core/emoji/test.png">"""

        assertNull(ContentImageUrlExtractor.extractImageUrl(content))
    }

    @Test
    fun `ignores forum smilies`() {
        val content = """<img src="https://example.com/smilies/wink.gif">"""

        assertNull(ContentImageUrlExtractor.extractImageUrl(content))
    }

    @Test
    fun `returns null when content is null`() {
        assertNull(ContentImageUrlExtractor.extractImageUrl(null))
    }

    @Test
    fun `returns null when content has no image`() {
        assertNull(ContentImageUrlExtractor.extractImageUrl("<p>Just text</p>"))
    }
}
