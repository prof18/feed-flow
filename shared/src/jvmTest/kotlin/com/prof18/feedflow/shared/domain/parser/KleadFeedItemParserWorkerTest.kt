package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.test.testLogger
import com.prof18.feedflow.shared.test.unexpectedRequestHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KleadFeedItemParserWorkerTest {

    @Test
    fun `returns the article Markdown without reader decorations`() = runTest {
        val worker = worker(html = articleHtml)

        val content = assertNotNull(worker.parse("https://example.com/articles/klead"))

        assertFalse(content.startsWith("# Klead Reader Mode"), content)
        assertFalse(content.contains("**Example Daily**"), content)
        assertTrue(content.contains("Klead extracts article prose"), content)
        assertTrue(content.contains("**Markdown emphasis**"), content)
    }

    @Test
    fun `returns the article HTML without reader decorations`() = runTest {
        val worker = worker(html = articleHtml, contentFormat = KleadContentFormat.HTML)

        val content = assertNotNull(worker.parse("https://example.com/articles/klead-html"))

        assertFalse(content.contains("<h1>Klead Reader Mode</h1>"), content)
        assertFalse(content.contains("<h4>Example Daily</h4>"), content)
        assertTrue(content.contains("<strong>Markdown emphasis</strong>"), content)
    }

    @Test
    fun `rejects short extracted content`() = runTest {
        val worker = worker(
            html = "<html><body><article><p>Too short.</p></article></body></html>",
        )

        assertNull(worker.parse("https://example.com/short"))
    }

    @Test
    fun `link and image urls do not count towards the Markdown minimum length`() = runTest {
        val longUrl = "https://example.com/" + "a".repeat(300)
        val worker = worker(
            html = "<html><body><article><p><a href=\"$longUrl\">Tiny link</a></p>" +
                "<p><img src=\"$longUrl.png\" alt=\"\"></p></article></body></html>",
        )

        assertNull(worker.parse("https://example.com/links-only"))
    }

    @Test
    fun `returns null when the page cannot be fetched`() = runTest {
        val worker = KleadFeedItemParserWorker(
            contentFormat = KleadContentFormat.MARKDOWN,
            htmlRetriever = htmlRetriever(html = "", status = HttpStatusCode.NotFound),
            logger = testLogger,
        )

        assertNull(worker.parse("https://example.com/missing"))
    }

    private fun worker(
        html: String,
        contentFormat: KleadContentFormat = KleadContentFormat.MARKDOWN,
    ) = KleadFeedItemParserWorker(
        contentFormat = contentFormat,
        htmlRetriever = htmlRetriever(html),
        logger = testLogger,
    )

    private fun htmlRetriever(
        html: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HtmlRetriever = HtmlRetriever(
        logger = testLogger,
        client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = html,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
                    )
                }
            }
        },
        forbiddenFallbackClient = unexpectedRequestHttpClient(),
    )

    private companion object {
        private val articleBody = List(12) { index ->
            "Klead extracts article prose into clean Markdown for desktop reader mode paragraph $index."
        }.joinToString(" ")

        private val articleHtml = """
            <!doctype html>
            <html>
              <head>
                <title>Klead Reader Mode - Example Daily</title>
                <meta property="og:title" content="Klead Reader Mode">
                <meta property="og:site_name" content="Example Daily">
              </head>
              <body>
                <nav>Navigation should not appear.</nav>
                <article>
                  <h1>Klead Reader Mode</h1>
                  <p>$articleBody</p>
                  <p><strong>Markdown emphasis</strong> should survive conversion.</p>
                </article>
              </body>
            </html>
        """.trimIndent()
    }
}
