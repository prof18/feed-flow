package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.testLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HtmlRetrieverTest {

    @Test
    fun `bom takes precedence over meta charset`() = runTest(testDispatcher) {
        val checkmark = "\u2713"
        val html = """
            <html>
                <head>
                    <meta charset="iso-8859-1" />
                </head>
                <body>Check $checkmark</body>
            </html>
        """.trimIndent()
        val bytes = UTF8_BOM + html.encodeToByteArray()

        val result = retrieveHtml(bytes, contentType = "text/html")
        assertNotNull(result)
        assertTrue(result.contains(checkmark))
    }

    @Test
    fun `ilpost style page uses charset from content type header`() = runTest(testDispatcher) {
        val eAcute = '\u00E9'
        val snippet =
            "<html><head><title>Il Post</title></head>" +
                "<body>Perch$eAcute la pagina funziona</body></html>"
        val bytes = snippet.encodeToByteArray()

        val result = retrieveHtml(bytes, contentType = "text/html; charset=utf-8")
        assertNotNull(result)
        assertTrue(result.contains("Perch\u00E9"))
    }

    @Test
    fun `xml declaration charset is honored`() = runTest(testDispatcher) {
        val snippet =
            "<?xml version=\"1.0\" encoding=\"windows-1252\"?>" +
                "<html><body>\u00DCber</body></html>"
        val bytes = encodeWindows1252(snippet)

        val result = retrieveHtml(bytes)
        assertNotNull(result)
        assertTrue(result.contains("\u00DCber"))
    }

    @Test
    fun `winfuture style page uses meta charset`() = runTest(testDispatcher) {
        val snippet =
            "<html><head><meta charset=\"windows-1252\" /></head>" +
                "<body>Kampfansage an Adobe: Creator Studio startet mit H\u00FCrden</body></html>"
        val bytes = encodeWindows1252(snippet)

        val result = retrieveHtml(bytes)
        assertNotNull(result)
        assertTrue(result.contains("H\u00FCrden"))
    }

    @Test
    fun `http-equiv meta charset is honored`() = runTest(testDispatcher) {
        val snippet =
            "<html><head>" +
                "<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\" />" +
                "</head><body>Pi\u00F9 contenuti</body></html>"
        val bytes = encodeWindows1252(snippet)

        val result = retrieveHtml(bytes)
        assertNotNull(result)
        assertTrue(result.contains("Pi\u00F9"))
    }

    @Test
    fun `valid utf8 without hints defaults to utf8`() = runTest(testDispatcher) {
        val snippet = "<html><body>Euro \u20AC</body></html>"
        val bytes = snippet.encodeToByteArray()

        val result = retrieveHtml(bytes)
        assertNotNull(result)
        assertTrue(result.contains("\u20AC"))
    }

    @Test
    fun `invalid utf8 without hints defaults to iso-8859-1`() = runTest(testDispatcher) {
        val prefix = "<html><body>".encodeToByteArray()
        val suffix = "</body></html>".encodeToByteArray()
        val bytes = prefix + byteArrayOf(0xE4.toByte()) + suffix

        val result = retrieveHtml(bytes)
        assertNotNull(result)
        assertTrue(result.contains("\u00E4"))
    }

    private companion object {
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }

    private fun encodeWindows1252(text: String): ByteArray {
        val bytes = ByteArray(text.length)
        text.forEachIndexed { index, char ->
            bytes[index] = when {
                char.code <= 0x7F -> char.code.toByte()
                char.code <= 0xFF -> char.code.toByte()
                else -> '?'.code.toByte()
            }
        }
        return bytes
    }

    private suspend fun retrieveHtml(
        bytes: ByteArray,
        contentType: String? = null,
    ): String? {
        val headers = if (contentType == null) {
            headersOf()
        } else {
            headersOf(HttpHeaders.ContentType, contentType)
        }
        val retriever = HtmlRetriever(
            logger = testLogger,
            client = HttpClient(MockEngine) {
                engine {
                    addHandler {
                        respond(
                            content = bytes,
                            status = HttpStatusCode.OK,
                            headers = headers,
                        )
                    }
                }
            },
        )
        return retriever.retrieveHtml("https://example.com")
    }
}
