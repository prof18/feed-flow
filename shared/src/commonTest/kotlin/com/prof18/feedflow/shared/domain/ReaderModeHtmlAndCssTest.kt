package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.ReaderModeDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderModeHtmlAndCssTest {

    @Test
    fun `reader mode css hides images that failed to load`() {
        val css = readerModeCss(colors = null, fontSize = 18, lineHeight = 0)

        assertTrue(css.contains("img.__feedflow_image_load_failed"))
        assertTrue(css.contains("display: none !important;"))
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
    fun `readerLineHeightToCss maps steps`() {
        assertEquals("1.5", readerLineHeightToCss(0))
        assertEquals("1.7", readerLineHeightToCss(ReaderModeDefaults.LINE_HEIGHT))
        assertEquals("2.5", readerLineHeightToCss(10))
        assertEquals("3.0", readerLineHeightToCss(15))
    }

    @Test
    fun `readerModeCss includes line height rule`() {
        val defaultCss = readerModeCss(null, 18, lineHeight = ReaderModeDefaults.LINE_HEIGHT)
        assertTrue(defaultCss.contains("line-height: 1.7"))

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
}
