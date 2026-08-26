package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.ReaderModeDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderModeHtmlAndCssTest {

    @Test
    fun `reader mode css hides images that failed to load`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(css.contains("img.__feedflow_image_load_failed"))
        assertTrue(css.contains("display: none !important;"))
    }

    @Test
    fun `reader mode css adds space around article images`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(
            css.contains(
                """
                #__content img {
                    display: block;
                    margin: 4px auto;
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `reader mode html marks failed images as hidden`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p>Content</p><img src=\"https://example.com/missing.jpg\" width=\"1080\" height=\"1920\" />",
            fontSize = 18,
        )

        assertTrue(html.contains("image.addEventListener(\"error\""))
        assertTrue(html.contains("image.complete && image.naturalWidth === 0"))
        assertTrue(html.contains("__feedflow_image_load_failed"))
        assertTrue(html.contains("aria-hidden"))
    }

    @Test
    fun `reader mode html resizes twitter embeds from trusted widget messages`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = """<iframe src="https://platform.twitter.com/embed/Tweet.html?id=12345"></iframe>""",
            fontSize = 18,
        )

        assertTrue(html.contains("""event.origin !== "https://platform.twitter.com"""))
        assertTrue(html.contains("""payload.method !== "twttr.private.resize"""))
        assertTrue(html.contains("frame.contentWindow === event.source"))
        assertTrue(html.contains("sourceFrame.style.height = Math.ceil(height)"))
    }

    @Test
    fun `reader mode css does not force a twitter fallback height`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(
            css.contains(
                """
                iframe[src^="https://platform.twitter.com/embed/Tweet.html"] {
                    border: 0;
                }
                """.trimIndent(),
            ),
        )
        assertFalse(css.contains("height: 250px"))
    }

    @Test
    fun `reader mode css sizes video embeds responsively without a global height cap`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(
            css.contains(
                """
                iframe[src^="https://www.youtube-nocookie.com/embed/"],
                iframe[src^="https://www.youtube.com/embed/"],
                iframe[src^="https://player.vimeo.com/video/"] {
                    aspect-ratio: 16 / 9;
                    height: auto;
                    border: 0;
                }
                """.trimIndent(),
            ),
        )
        assertFalse(css.contains("max-height: 250px"))
    }

    @Test
    fun `reader mode css leaves ordinary iframe height unforced`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(
            css.contains(
                """
                iframe {
                    width: 100%;
                    max-width: 100%;
                    border-radius: 7px;
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `readerLineHeightToCss maps steps`() {
        assertEquals("1.5", readerLineHeightToCss(0))
        assertEquals("1.6", readerLineHeightToCss(ReaderModeDefaults.LINE_HEIGHT))
        assertEquals("2.5", readerLineHeightToCss(10))
        assertEquals("3.0", readerLineHeightToCss(15))
    }

    @Test
    fun `readerModeCss includes line height rule`() {
        val defaultCss = readerModeCss(null, 18, lineHeight = ReaderModeDefaults.LINE_HEIGHT)
        assertTrue(defaultCss.contains("line-height: 1.6"))

        val spacedCss = readerModeCss(null, 18, lineHeight = 5)
        assertTrue(spacedCss.contains("line-height: 2.0"))
    }

    @Test
    fun `getReaderModeStyledHtml includes line height`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p>Content</p>",
            fontSize = 18,
            lineHeight = 3,
        )

        assertTrue(html.contains("line-height: 1.8"))
    }

    @Test
    fun `getReaderModeStyledHtml includes an escaped article title`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p>Content</p>",
            fontSize = 18,
            title = "Title <with> & symbols",
            imageUrl = "https://example.com/hero.jpg",
            leadingContent = "<div id=\"spacer\"></div>",
            siteName = "Example & Site",
        )

        assertTrue(html.contains("<h1>Title &lt;with&gt; &amp; symbols</h1>"))
        assertTrue(html.contains("<img class=\"__hero\" src=\"https://example.com/hero.jpg\""))
        assertTrue(html.contains("<h4>Example &amp; Site</h4>"))
        assertTrue(html.indexOf("id=\"spacer\"") < html.indexOf("<h1>"))
    }

    @Test
    fun `feed hero is not duplicated when content has a leading image`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p><img src=\"https://example.com/hero.jpg\" style=\"float: left\">Article body</p>",
            fontSize = 18,
            imageUrl = "https://example.com/hero.jpg",
            siteName = "Example Site",
        )

        assertTrue(html.contains("<h4>Example Site</h4><p><img"))
        assertTrue(html.contains("float: left"))
        assertTrue(!html.contains("class=\"__hero\""))
    }

    @Test
    fun `feed hero is not injected when content has a different leading image`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p><img src=\"https://example.com/diagram.jpg\">Article body</p>",
            fontSize = 18,
            imageUrl = "https://example.com/hero.jpg",
        )

        assertTrue(html.contains("https://example.com/diagram.jpg"))
        assertTrue(!html.contains("class=\"__hero\""))
    }
}
