package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.test.testLogger
import com.russhwolf.settings.MapSettings
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KleadFeedItemParserWorkerTest {

    @Test
    fun `returns decorated Markdown and caches it when enabled`() = runTest {
        val fileHandler = RecordingFeedItemContentFileHandler()
        val settingsRepository = SettingsRepository(MapSettings()).apply {
            setKleadParserEnabled(true)
            setSaveItemContentOnOpen(true)
        }
        val worker = worker(
            html = articleHtml,
            fileHandler = fileHandler,
            settingsRepository = settingsRepository,
        )

        val result = worker.parse(
            feedItemId = "item-1",
            url = "https://example.com/articles/klead",
            imageUrl = "https://example.com/feed-hero.png",
        )

        val success = assertIs<ParsingResult.Success>(result)
        val content = assertNotNull(success.htmlContent)
        assertEquals("Klead Reader Mode", success.title)
        assertEquals("Example Daily", success.siteName)
        assertTrue(content.startsWith("# Klead Reader Mode"))
        assertTrue(content.contains("**Example Daily**"))
        assertTrue(content.contains("![](https://example.com/feed-hero.png)"))
        assertTrue(content.contains("Klead extracts article prose"))
        assertTrue(content.contains("**Markdown emphasis**"))
        assertEquals(content, fileHandler.savedContentById["item-1"])
    }

    @Test
    fun `does not duplicate an extracted leading image`() = runTest {
        val worker = worker(html = articleHtmlWithLeadingImage)

        val result = worker.parse(
            feedItemId = "item-2",
            url = "https://example.com/articles/klead-leading-image",
            imageUrl = "https://example.com/feed-hero.png",
        )

        val content = assertNotNull(assertIs<ParsingResult.Success>(result).htmlContent)
        assertFalse(content.contains("![](https://example.com/feed-hero.png)"))
        assertTrue(content.contains("https://example.com/article-hero.png"))
    }

    @Test
    fun `rejects short extracted content`() = runTest {
        val worker = worker(
            html = "<html><body><article><p>Too short.</p></article></body></html>",
        )

        assertIs<ParsingResult.Error>(
            worker.parse("item-3", "https://example.com/short"),
        )
    }

    @Test
    fun `returns decorated HTML for mobile readers`() = runTest {
        val worker = worker(
            html = articleHtml,
            contentFormat = KleadContentFormat.HTML,
        )

        val result = worker.parse(
            feedItemId = "item-4",
            url = "https://example.com/articles/klead-html",
            imageUrl = "https://example.com/feed-hero.png",
        )

        val content = assertNotNull(assertIs<ParsingResult.Success>(result).htmlContent)
        assertTrue(content.startsWith("<h1>Klead Reader Mode</h1>"))
        assertTrue(content.contains("<h4>Example Daily</h4>"))
        assertTrue(content.contains("<strong>Markdown emphasis</strong>"))
        assertFalse(content.contains("# Klead Reader Mode"))
        assertFalse(content.contains("feed-hero.png"))
    }

    @Test
    fun `prefetch parser leaves cache writes to its caller`() = runTest {
        val fileHandler = RecordingFeedItemContentFileHandler()
        val settingsRepository = SettingsRepository(MapSettings()).apply {
            setSaveItemContentOnOpen(true)
        }
        val worker = worker(
            html = articleHtml,
            contentFormat = KleadContentFormat.HTML,
            fileHandler = fileHandler,
            settingsRepository = settingsRepository,
            cacheResultWhenEnabled = false,
        )

        assertIs<ParsingResult.Success>(
            worker.parse("item-5", "https://example.com/articles/klead-prefetch"),
        )
        assertFalse("item-5" in fileHandler.savedContentById)
    }

    private fun worker(
        html: String,
        contentFormat: KleadContentFormat = KleadContentFormat.MARKDOWN,
        fileHandler: FeedItemContentFileHandler = RecordingFeedItemContentFileHandler(),
        settingsRepository: SettingsRepository = SettingsRepository(MapSettings()),
        cacheResultWhenEnabled: Boolean = true,
    ) = KleadFeedItemParserWorker(
        contentFormat = contentFormat,
        htmlRetriever = htmlRetriever(html),
        logger = testLogger,
        feedItemContentFileHandler = fileHandler,
        settingsRepository = settingsRepository,
        cacheResultWhenEnabled = cacheResultWhenEnabled,
    )

    private fun htmlRetriever(html: String): HtmlRetriever = HtmlRetriever(
        logger = testLogger,
        client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = html,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
                    )
                }
            }
        },
    )

    private class RecordingFeedItemContentFileHandler : FeedItemContentFileHandler {
        val savedContentById = mutableMapOf<String, String>()

        override suspend fun saveFeedItemContentToFile(feedItemId: String, content: String) {
            savedContentById[feedItemId] = content
        }

        override suspend fun loadFeedItemContent(feedItemId: String): String? = savedContentById[feedItemId]

        override suspend fun isContentAvailable(feedItemId: String): Boolean = feedItemId in savedContentById

        override suspend fun deleteFeedItemContent(feedItemId: String) {
            savedContentById.remove(feedItemId)
        }

        override suspend fun clearAllContent() {
            savedContentById.clear()
        }
    }

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

        private val articleHtmlWithLeadingImage = """
            <!doctype html>
            <html>
              <head>
                <title>Klead Leading Image - Example Daily</title>
                <meta property="og:title" content="Klead Leading Image">
                <meta property="og:site_name" content="Example Daily">
              </head>
              <body>
                <article>
                  <figure><img src="https://example.com/article-hero.png" alt=""></figure>
                  <p>$articleBody</p>
                  <p><strong>Markdown emphasis</strong> should survive conversion.</p>
                </article>
              </body>
            </html>
        """.trimIndent()
    }
}
