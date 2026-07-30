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
    fun `decodes decimal numeric entities inside the image url`() {
        val content = """<img src="https://example.com/image.jpg?w=180&#038;h=180&#038;sig=abc">"""

        assertEquals(
            "https://example.com/image.jpg?w=180&h=180&sig=abc",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `decodes hexadecimal numeric entities inside the image url`() {
        val content = """<img src="https://example.com/image.jpg?w=180&#x26;h=180">"""

        assertEquals(
            "https://example.com/image.jpg?w=180&h=180",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `decodes double encoded numeric entities inside the image url`() {
        val content = """<img src="https://example.com/image.jpg?w=180&amp;#038;h=180">"""

        assertEquals(
            "https://example.com/image.jpg?w=180&h=180",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `keeps invalid numeric entities untouched`() {
        val content = """<img src="https://example.com/image.jpg?name=x&#0;y">"""

        assertEquals(
            "https://example.com/image.jpg?name=x&#0;y",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `extracts image url without a file extension`() {
        val content = """<img src="https://api.ardmediathek.de/image-service/images/""" +
            """urn:ard:image:c9c909df6f5a4b3f?w=432&amp;ch=ce51a6b849fde683" /><p>Some text</p>"""

        assertEquals(
            "https://api.ardmediathek.de/image-service/images/" +
                "urn:ard:image:c9c909df6f5a4b3f?w=432&ch=ce51a6b849fde683",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `extracts image url from single quoted src`() {
        val content = """<img alt='cover' src='https://example.com/image-service/abc'>"""

        assertEquals(
            "https://example.com/image-service/abc",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `extracts image url from unquoted src`() {
        val content = """<img src=https://example.com/image-service/abc width=100>"""

        assertEquals(
            "https://example.com/image-service/abc",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `falls back to the lazy loading source when src is a placeholder`() {
        val content = """<img src="data:image/gif;base64,R0lGOD" data-src="https://example.com/photos/42">"""

        assertEquals(
            "https://example.com/photos/42",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `skips relative image sources`() {
        val content = """<img src="/assets/local.png"><img src="https://example.com/photos/42">"""

        assertEquals(
            "https://example.com/photos/42",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `prefers the image tag source over a bare url in the text`() {
        val content = """<p>See https://example.com/other.jpg</p><img src="https://example.com/hero.png">"""

        assertEquals(
            "https://example.com/hero.png",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `falls back to a bare image url when there is no image tag`() {
        val content = """<p>Cover: https://example.com/hero.jpg?width=800</p>"""

        assertEquals(
            "https://example.com/hero.jpg?width=800",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
    }

    @Test
    fun `skips emoji images and keeps looking`() {
        val content = """<img src="https://s.w.org/images/core/emoji/test.png">""" +
            """<img src="https://example.com/photos/42">"""

        assertEquals(
            "https://example.com/photos/42",
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
    fun `handles a long url-like run without an image`() {
        // The bare-url regex has to backtrack over this: a scheme followed by a long run of
        // characters that never terminates in a file extension. Kotlin_Native's regex engine
        // used to blow its stack on shapes like this, hence the guard.
        val content = "https://" + "a".repeat(50_000)

        assertNull(ContentImageUrlExtractor.extractImageUrl(content))
    }

    @Test
    fun `extracts the image from a long document`() {
        val content = "<p>${"Lorem ipsum dolor sit amet. ".repeat(2_000)}</p>" +
            """<img src="https://example.com/image-service/abc">"""

        assertEquals(
            "https://example.com/image-service/abc",
            ContentImageUrlExtractor.extractImageUrl(content),
        )
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
