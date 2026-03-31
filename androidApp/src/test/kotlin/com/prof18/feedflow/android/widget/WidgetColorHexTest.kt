package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetColorHexTest {

    @Test
    fun `formatWidgetColorHex drops alpha and keeps uppercase rgb`() {
        assertEquals("#3366CC", formatWidgetColorHex(0x803366CC.toInt()))
    }

    @Test
    fun `parseWidgetColorHex accepts hash prefix and lowercase`() {
        val color = parseWidgetColorHex("#1a2b3c")

        assertEquals(0xFF1A2B3C.toInt(), color?.toArgb())
    }

    @Test
    fun `parseWidgetColorHex accepts value without hash prefix`() {
        val color = parseWidgetColorHex("ABCDEF")

        assertEquals(0xFFABCDEF.toInt(), color?.toArgb())
    }

    @Test
    fun `parseWidgetColorHex rejects invalid values`() {
        assertNull(parseWidgetColorHex(""))
        assertNull(parseWidgetColorHex("#12345"))
        assertNull(parseWidgetColorHex("#12345678"))
        assertNull(parseWidgetColorHex("#12GG45"))
    }
}
