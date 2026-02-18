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
        val readabilityJs = loadResource("readability-es5.js")
        val turndownJs = loadResource("turndown-es5.js")
        // language=HTML
        """
        <html dir='auto'>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <script>$readabilityJs</script>
          <script>$turndownJs</script>
        </head>
        <body></body>
        </html>
        """.trimIndent()
    }

    override suspend fun parse(feedItemId: String, url: String): ParsingResult {
        logger.d { "Triggering immediate parsing for: $url (feedItemId: $feedItemId)" }

        return withContext(dispatcherProvider.io) {
            try {
                val html = htmlRetriever.retrieveHtml(url)
                if (html == null) {
                    logger.d { "Failed to retrieve HTML for: $url" }
                    return@withContext ParsingResult.Error
                }
                val cleanedHtml = html
                    .replace(Regex("https?://.*?placeholder\\.png"), "")
                    // HtmlUnit's DOMParser crashes on <iframe>/<frame> elements because the
                    // parsed document has no parent WebWindow. Strip them before parsing.
                    .replace(
                        Regex(
                            "<iframe\\b[^>]*>.*?</iframe>",
                            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                        ),
                        "",
                    )
                    .replace(Regex("<iframe\\b[^>]*/>", RegexOption.IGNORE_CASE), "")
                    .replace(
                        Regex(
                            "<frameset\\b[^>]*>.*?</frameset>",
                            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                        ),
                        "",
                    )
                    .replace(Regex("<frame\\b[^>]*>", RegexOption.IGNORE_CASE), "")

                val parseResult = runReadability(cleanedHtml, url)
                if (parseResult == null) {
                    logger.d { "Readability returned no result for: $url" }
                    return@withContext ParsingResult.Error
                }

                if (parseResult.content.length < MIN_CONTENT_LENGTH) {
                    logger.d { "Content too short (${parseResult.content.length} chars), rejecting: $url" }
                    return@withContext ParsingResult.Error
                }

                val markdown = buildString {
                    if (!parseResult.title.isNullOrBlank()) {
                        appendLine("# ${parseResult.title}")
                    }
                    if (!parseResult.siteName.isNullOrBlank()) {
                        appendLine("**${parseResult.siteName}**")
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

    private data class ReadabilityResult(
        val content: String,
        val title: String?,
        val siteName: String?,
    )

    private fun runReadability(html: String, url: String): ReadabilityResult? {
        val htmlEscaped = Json.encodeToString(html)
        val urlEscaped = Json.encodeToString(url)

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

                try {
                    var htmlContent = $htmlEscaped;
                    var link = $urlEscaped;
                    var domParser = new DOMParser();
                    var doc = domParser.parseFromString(htmlContent, 'text/html');

                    // Inject <base> so Readability can resolve relative links and image URLs
                    var base = doc.createElement('base');
                    base.href = link;
                    doc.head.insertBefore(base, doc.head.firstChild);

                    // Normalize lazy-loaded images before Readability strips unknown attributes
                    var imgs = doc.querySelectorAll('img');
                    for (var i = 0; i < imgs.length; i++) {
                        var img = imgs[i];
                        var lazySrc = img.getAttribute('data-src')
                            || img.getAttribute('data-lazy-src')
                            || img.getAttribute('data-original')
                            || img.getAttribute('data-lazy');
                        if (lazySrc && (!img.getAttribute('src') || img.getAttribute('src').indexOf('data:') === 0)) {
                            img.setAttribute('src', lazySrc);
                        }
                    }

                    var reader = new Readability(doc);
                    var article = reader.parse();

                    if (!article) {
                        parsingError = "Readability returned null";
                    } else {
                        var turndown = new TurndownService({ headingStyle: 'atx', codeBlockStyle: 'fenced' });
                        var markdown = turndown.turndown(article.content || '');
                        parsingResult = JSON.stringify({
                            content: markdown,
                            title: article.title || null,
                            siteName: article.siteName || null
                        });
                    }
                } catch(e) {
                    parsingError = e.toString();
                }
            """.trimIndent()

            try {
                page.executeJavaScript(script)
            } catch (e: Exception) {
                logger.d(e) { "JS error in Readability parse script" }
                return null
            }

            val errorObj = page.executeJavaScript("parsingError").javaScriptResult
            if (errorObj != null && errorObj != Undefined.instance) {
                logger.e { "Readability JS error: $errorObj" }
                return null
            }

            val resultObj = page.executeJavaScript("parsingResult").javaScriptResult
            val resultJson = if (resultObj != null && resultObj != Undefined.instance) {
                resultObj.toString()
            } else {
                logger.e { "Readability returned no result" }
                return null
            }

            val jsObject = Json.parseToJsonElement(resultJson).jsonObject
            val content = jsObject["content"]?.jsonPrimitive?.content
            if (content.isNullOrBlank()) {
                logger.e { "Readability returned empty content" }
                return null
            }
            val title = jsObject["title"]?.jsonPrimitive?.takeIf { it.isString }?.content
            val siteName = jsObject["siteName"]?.jsonPrimitive?.takeIf { it.isString }?.content

            return ReadabilityResult(content, title, siteName)
        }
    }

    private companion object {
        private const val MIN_CONTENT_LENGTH = 200

        init {
            JLogger.getLogger("org.htmlunit").level = Level.OFF
            JLogger.getLogger("org.htmlunit.javascript").level = Level.OFF
            JLogger.getLogger("org.htmlunit.css").level = Level.OFF
            JLogger.getLogger("org.htmlunit.html").level = Level.OFF
        }
    }
}
