package com.prof18.feedflow.shared.domain.readerfont

import com.prof18.feedflow.core.model.ReaderFontFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderFontFaceCssJvmTest {

    @Test
    fun `bundled font declares upright and italic faces with its weight range`() {
        val css = readerFontFaceCss(ReaderFontFamily.LITERATA)

        assertTrue(css.contains("font-weight: 200 900"))
        assertTrue(css.contains("font-style: normal"))
        assertTrue(css.contains("font-style: italic"))
        assertTrue(css.contains("data:font/ttf;base64,"))
        assertEquals(2, css.fontFaceCount())
    }

    @Test
    fun `repeated family is declared once`() {
        val css = readerFontFaceCss(ReaderFontFamily.IBM_PLEX_SANS, ReaderFontFamily.IBM_PLEX_SANS)

        assertTrue(css.contains("font-weight: 100 700"))
        assertEquals(2, css.fontFaceCount())
    }

    @Test
    fun `system font declares no face`() {
        assertEquals("", readerFontFaceCss(ReaderFontFamily.SYSTEM))
    }

    @Test
    fun `every bundled font file is on the classpath`() {
        ReaderFontFamily.entries.filter { it.isBundled }.forEach { family ->
            listOfNotNull(family.fileName, family.italicFileName).forEach { fileName ->
                val bytes = readReaderFontBytes(fileName)
                assertTrue(bytes != null && bytes.isNotEmpty(), "$fileName is missing")
            }
        }
    }

    private fun String.fontFaceCount(): Int = Regex("@font-face").findAll(this).count()
}
