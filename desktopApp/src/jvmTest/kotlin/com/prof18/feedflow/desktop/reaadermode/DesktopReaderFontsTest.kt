package com.prof18.feedflow.desktop.reaadermode

import com.prof18.feedflow.core.model.ReaderFontFamily
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopReaderFontsTest {

    @Test
    fun `system font uses the default family`() {
        assertNull(readerComposeFontFamily(ReaderFontFamily.SYSTEM))
    }

    @Test
    fun `bundled fonts build a family from files on the classpath`() {
        val classLoader = Thread.currentThread().contextClassLoader
        ReaderFontFamily.entries.filter { it.isBundled }.forEach { family ->
            assertNotNull(readerComposeFontFamily(family), "$family has no FontFamily")
            listOfNotNull(family.fileName, family.italicFileName).forEach { fileName ->
                assertNotNull(classLoader.getResource("reader-fonts/$fileName"), "$fileName is not on the classpath")
            }
        }
    }
}
