package com.prof18.feedflow.shared.domain.recovery

import kotlin.test.Test
import kotlin.test.assertEquals

class ResponseTextDecoderTest {

    @Test
    fun `utf8 bom body decodes as utf8`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "<rss>✓</rss>".encodeToByteArray()

        val text = ResponseTextDecoder.decode(bytes, contentType = null)

        assertEquals(true, text.contains("✓"))
    }

    @Test
    fun `content type charset is honoured`() {
        val bytes = "<rss>".encodeToByteArray() + byteArrayOf(0xE9.toByte()) + "</rss>".encodeToByteArray()

        val text = ResponseTextDecoder.decode(bytes, contentType = "text/xml; charset=ISO-8859-1")

        assertEquals("<rss>é</rss>", text)
    }

    @Test
    fun `xml declaration encoding is honoured without header charset`() {
        val bytes = "<?xml version=\"1.0\" encoding=\"windows-1252\"?><rss>".encodeToByteArray() +
            byteArrayOf(0xE9.toByte()) +
            "</rss>".encodeToByteArray()

        val text = ResponseTextDecoder.decode(bytes, contentType = "text/xml")

        assertEquals(true, text.contains("<rss>é</rss>"))
    }
}
