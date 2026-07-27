package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.feeditem.FeedContentPreparer
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.corejs.javascript.Undefined
import org.htmlunit.html.HtmlPage

internal class DesktopFeedContentPreparer(
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
) : FeedContentPreparer {

    private fun loadResource(name: String): String =
        DesktopFeedContentPreparer::class.java
            .getResourceAsStream("/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Could not load $name")

    private val htmlShell: String by lazy {
        val turndownJs = loadResource("turndown-es5.js")
        val readerContentParserJs = loadResource("reader-content-parser.js")
        // language=HTML
        """
        <html dir='auto'>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <script>$turndownJs</script>
          <script>$readerContentParserJs</script>
        </head>
        <body></body>
        </html>
        """.trimIndent()
    }

    override suspend fun prepare(
        html: String,
        baseUrl: String?,
        title: String?,
        imageUrl: String?,
        siteName: String?,
    ): String = withContext(dispatcherProvider.io) {
        val htmlEscaped = Json.encodeToString(html)
        val baseUrlEscaped = baseUrl?.let { Json.encodeToString(it) } ?: "null"

        val convertedContent = WebClient(BrowserVersion.CHROME).use { webClient ->
            webClient.options.apply {
                isCssEnabled = false
                isDownloadImages = false
                isThrowExceptionOnFailingStatusCode = false
                isThrowExceptionOnScriptError = true
            }

            val page: HtmlPage = webClient.loadHtmlCodeIntoCurrentWindow(htmlShell)

            val script = """
                var conversionResult = null;
                var conversionError = null;
                try {
                    conversionResult = convertFeedContentToMarkdown($htmlEscaped, $baseUrlEscaped);
                    var parsed = JSON.parse(conversionResult);
                    if (parsed.error) {
                        conversionError = parsed.error;
                        conversionResult = null;
                    }
                } catch(e) {
                    conversionError = e.toString();
                }
            """.trimIndent()

            try {
                page.executeJavaScript(script)
            } catch (e: Exception) {
                logger.d(e) { "JS error converting feed content to markdown" }
                return@use html
            }

            val errorObj = page.executeJavaScript("conversionError").javaScriptResult
            if (errorObj != null && errorObj != Undefined.instance) {
                logger.d { "Feed content conversion JS error: $errorObj" }
                return@use html
            }

            val resultObj = page.executeJavaScript("conversionResult").javaScriptResult
            if (resultObj == null || resultObj == Undefined.instance) {
                return@use html
            }

            val jsObject = Json.parseToJsonElement(resultObj.toString()).jsonObject
            jsObject["content"]?.jsonPrimitive?.content ?: html
        }

        buildFeedReaderMarkdown(
            content = convertedContent,
            title = title,
            imageUrl = imageUrl,
            siteName = siteName,
        )
    }
}

internal fun buildFeedReaderMarkdown(
    content: String,
    title: String?,
    imageUrl: String?,
    siteName: String? = null,
): String = buildString {
    if (!title.isNullOrBlank()) {
        appendLine("# $title")
        appendLine()
    }
    if (!siteName.isNullOrBlank()) {
        appendLine("**$siteName**")
        appendLine()
    }
    if (!imageUrl.isNullOrBlank() && !hasLeadingImage(content)) {
        appendLine("![]($imageUrl)")
        appendLine()
    }
    append(content)
}

private const val LEADING_IMAGE_SCAN_WINDOW = 1000
private val leadingImageRegex = Regex(
    pattern = "!\\[[^]]*]\\([^)]+\\)|<img\\b",
    option = RegexOption.IGNORE_CASE,
)

private fun hasLeadingImage(content: String): Boolean =
    leadingImageRegex.containsMatchIn(content.take(LEADING_IMAGE_SCAN_WINDOW))
