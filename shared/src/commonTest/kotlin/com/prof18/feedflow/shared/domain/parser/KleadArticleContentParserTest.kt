package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.test.testLogger
import com.prof18.feedflow.shared.test.unexpectedRequestHttpClient
import com.prof18.klead.KleadOutput
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KleadArticleContentParserTest {

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
        val worker = worker(html = articleHtml, contentFormat = KleadOutput.HTML)

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
        val worker = KleadArticleContentParser(
            contentFormat = KleadOutput.MARKDOWN,
            htmlRetriever = htmlRetriever(html = "", status = HttpStatusCode.NotFound),
            logger = testLogger,
        )

        assertNull(worker.parse("https://example.com/missing"))
    }

    @Test
    fun `prepare converts feed html to markdown`() = runTest {
        val markdown = markdownWorker().prepareFeedContent(
            html = "<p>First <strong>bold</strong> paragraph with " +
                "<a href=\"https://example.com/link\">a link</a>.</p>" +
                "<h2>Section</h2><ul><li>One</li><li>Two</li></ul>",
            baseUrl = "https://example.com/post",
        )

        assertTrue("**bold**" in markdown, markdown)
        assertTrue("[a link](https://example.com/link)" in markdown, markdown)
        assertTrue("## Section" in markdown, markdown)
        assertTrue("One" in markdown && "Two" in markdown, markdown)
        assertFalse("<p>" in markdown, markdown)
    }

    @Test
    fun `prepare keeps short feed content`() = runTest {
        val markdown = markdownWorker().prepareFeedContent(
            html = "<p>Short note.</p>",
            baseUrl = "https://example.com/post",
        )

        assertTrue("Short note." in markdown, markdown)
        assertFalse("<p>" in markdown, markdown)
    }

    @Test
    fun `prepare keeps image-only feed content`() = runTest {
        val markdown = markdownWorker().prepareFeedContent(
            html = "<img src=\"https://example.com/photo.jpg\" alt=\"Photo\">",
            baseUrl = "https://example.com/post",
        )

        assertTrue("https://example.com/photo.jpg" in markdown, markdown)
        assertFalse("<img" in markdown, markdown)
    }

    @Test
    fun `prepare resolves relative urls against the base url`() = runTest {
        val markdown = markdownWorker().prepareFeedContent(
            html = "<p>Read <a href=\"/more\">more</a> of this longer paragraph about the topic.</p>" +
                "<p><img src=\"images/pic.png\" alt=\"Pic\"></p>",
            baseUrl = "https://example.com/blog/post",
        )

        assertTrue("https://example.com/more" in markdown, markdown)
        assertTrue("https://example.com/blog/images/pic.png" in markdown, markdown)
    }

    @Test
    fun `prepare uses lazy-loaded image sources`() = runTest {
        val markdown = markdownWorker().prepareFeedContent(
            html = "<p>Some text around a lazy image in the feed body.</p>" +
                "<img src=\"data:image/gif;base64,R0lGODlhAQABAAAAACw=\" data-src=\"https://example.com/lazy.jpg\">",
            baseUrl = "https://example.com/post",
        )

        assertTrue("https://example.com/lazy.jpg" in markdown, markdown)
    }

    @Test
    fun `prepare works without a base url`() = runTest {
        val markdown = markdownWorker().prepareFeedContent(
            html = "<p>No base url here.</p>",
            baseUrl = null,
        )

        assertTrue("No base url here." in markdown, markdown)
    }

    @Test
    fun `feed html is returned untouched for HTML output`() = runTest {
        val html = "<p>Some <em>feed</em> html &amp; entities</p>"

        val prepared = worker(html = "", contentFormat = KleadOutput.HTML)
            .prepareFeedContent(html = html, baseUrl = "https://example.com")

        assertEquals(html, prepared)
    }

    @Test
    fun `markdown preparation falls back to the input html when conversion yields nothing`() = runTest {
        val html = "<div></div>"

        val prepared = markdownWorker().prepareFeedContent(html = html, baseUrl = "https://example.com")

        assertEquals(html, prepared)
    }

    private fun markdownWorker() = worker(html = "", contentFormat = KleadOutput.MARKDOWN)

    private fun worker(
        html: String,
        contentFormat: KleadOutput = KleadOutput.MARKDOWN,
    ) = KleadArticleContentParser(
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
