package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.corejs.javascript.Undefined
import org.htmlunit.html.HtmlPage
import java.util.logging.Level
import java.util.logging.Logger as JLogger

internal class DesktopFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val settingsRepository: SettingsRepository,
) : FeedItemParserWorker {

    private fun loadResource(name: String): String =
        DesktopFeedItemParserWorker::class.java
            .getResourceAsStream("/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Could not load $name")

    private val htmlShell: String by lazy {
        val mercuryJs = loadResource("mercury.web.js")
        val turndownJs = loadResource("turndown-es5.js")
        val readerContentParserJs = loadResource("mercury-content-parser.js")
        // language=HTML
        """
        <html dir='auto'>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <script>$mercuryJs</script>
          <script>$turndownJs</script>
          <script>$readerContentParserJs</script>
        </head>
        <body></body>
        </html>
        """.trimIndent()
    }

    override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
        logger.d { "Triggering immediate parsing for: $url (feedItemId: $feedItemId)" }

        return withContext(dispatcherProvider.io) {
            try {
                val html = htmlRetriever.retrieveHtml(url)
                if (html == null) {
                    logger.d { "Failed to retrieve HTML for: $url" }
                    return@withContext ParsingResult.Error
                }
                val cleanedHtml = html.replace(Regex("https?://.*?placeholder\\.png"), "")

                val parseResult = runMercury(cleanedHtml, url, imageUrl)
                if (parseResult == null) {
                    logger.d { "Mercury returned no result for: $url" }
                    return@withContext ParsingResult.Error
                }
                logger.d {
                    "Mercury parsed \"${parseResult.title}\" (${parseResult.content.length} chars): " +
                        parseResult.content.take(CONTENT_LOG_SNIPPET_LENGTH)
                }

                if (parseResult.content.length < MIN_CONTENT_LENGTH) {
                    logger.d { "Content too short (${parseResult.content.length} chars), rejecting: $url" }
                    return@withContext ParsingResult.Error
                }

                val markdown = buildString {
                    if (!parseResult.title.isNullOrBlank()) {
                        appendLine("# ${parseResult.title}")
                        appendLine()
                    }
                    if (!parseResult.siteName.isNullOrBlank()) {
                        appendLine("**${parseResult.siteName}**")
                        appendLine()
                    }
                    if (!imageUrl.isNullOrBlank()) {
                        appendLine()
                        appendLine("![ ]($imageUrl)")
                        appendLine()
                    }
                    if (!parseResult.title.isNullOrBlank() || !parseResult.siteName.isNullOrBlank()) {
                        appendLine()
                    }
                    append(parseResult.content)
                }

                if (settingsRepository.isSaveItemContentOnOpenEnabled()) {
                    feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, markdown)
                    logger.d { "Successfully parsed and cached: $url" }
                }

                ParsingResult.Success(
                    htmlContent = markdown,
                    title = parseResult.title,
                    siteName = parseResult.siteName,
                )
            } catch (e: Throwable) {
                logger.d(e) { "Error parsing content for: $url" }
                ParsingResult.Error
            }
        }
    }

    private data class MercuryResult(
        val content: String,
        val title: String?,
        val siteName: String?,
    )

    private fun runMercury(html: String, url: String, imageUrl: String?): MercuryResult? {
        val htmlEscaped = Json.encodeToString(html)
        val urlEscaped = Json.encodeToString(url)
        val imageUrlEscaped = imageUrl?.let { Json.encodeToString(it) } ?: "null"

        WebClient(BrowserVersion.CHROME).use { webClient ->
            webClient.options.apply {
                isCssEnabled = false
                isDownloadImages = false
                isThrowExceptionOnFailingStatusCode = false
                isThrowExceptionOnScriptError = true
            }

            val page: HtmlPage = webClient.loadHtmlCodeIntoCurrentWindow(htmlShell)

            val script = """
                var parsingResult = null;
                var parsingError = null;
                var parsingDone = false;

                try {
                    var htmlContent = $htmlEscaped;
                    var link = $urlEscaped;
                    var bannerImage = $imageUrlEscaped;
                    parseReaderContent(htmlContent, link, bannerImage).then(function(result) {
                        parsingResult = result;
                        var parsed = JSON.parse(parsingResult);
                        if (parsed.error) {
                            parsingError = parsed.error;
                            parsingResult = null;
                        }
                        parsingDone = true;
                    }).catch(function(e) {
                        parsingError = e.toString();
                        parsingDone = true;
                    });
                } catch(e) {
                    parsingError = e.toString();
                    parsingDone = true;
                }
            """.trimIndent()

            try {
                page.executeJavaScript(script)
            } catch (e: Exception) {
                logger.d(e) { "JS error in Mercury parse script" }
                return null
            }

            if (!waitForParsing(webClient, page)) {
                logger.d { "Mercury timed out after ${PARSING_TIMEOUT_MS}ms" }
                return null
            }

            val errorObj = page.executeJavaScript("parsingError").javaScriptResult
            if (errorObj != null && errorObj != Undefined.instance) {
                logger.d { "Mercury JS error: $errorObj" }
                return null
            }

            val resultObj = page.executeJavaScript("parsingResult").javaScriptResult
            val resultJson = if (resultObj != null && resultObj != Undefined.instance) {
                resultObj.toString()
            } else {
                logger.d { "Mercury returned no result" }
                return null
            }

            val jsObject = Json.parseToJsonElement(resultJson).jsonObject
            val content = jsObject["content"]?.jsonPrimitive?.content
            if (content.isNullOrBlank()) {
                logger.d { "Mercury returned empty content" }
                return null
            }
            val title = jsObject["title"]?.jsonPrimitive?.takeIf { it.isString }?.content
            val siteName = jsObject["siteName"]?.jsonPrimitive?.takeIf { it.isString }?.content

            return MercuryResult(content, title, siteName)
        }
    }

    private fun waitForParsing(webClient: WebClient, page: HtmlPage): Boolean {
        val deadline = System.nanoTime() + PARSING_TIMEOUT_MS * NANOS_PER_MILLI
        while (System.nanoTime() < deadline) {
            webClient.waitForBackgroundJavaScript(BACKGROUND_JS_POLL_MS)
            val done = page.executeJavaScript("parsingDone === true").javaScriptResult
            if (done == true) {
                return true
            }
            Thread.sleep(BACKGROUND_JS_POLL_MS)
        }
        return false
    }

    private companion object {
        private const val MIN_CONTENT_LENGTH = 200
        private const val CONTENT_LOG_SNIPPET_LENGTH = 500
        private const val PARSING_TIMEOUT_MS = 20_000L
        private const val BACKGROUND_JS_POLL_MS = 50L
        private const val NANOS_PER_MILLI = 1_000_000L

        init {
            JLogger.getLogger("org.htmlunit").level = Level.OFF
            JLogger.getLogger("org.htmlunit.javascript").level = Level.OFF
            JLogger.getLogger("org.htmlunit.css").level = Level.OFF
            JLogger.getLogger("org.htmlunit.html").level = Level.OFF
        }
    }
}
