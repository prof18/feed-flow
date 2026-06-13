package com.prof18.feedflow.shared.domain.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.corejs.javascript.Undefined
import org.htmlunit.html.HtmlPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MercuryHtmlUnitTest {

    private fun loadResource(name: String): String =
        MercuryHtmlUnitTest::class.java
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

    @Test
    fun `mercury parses an article inside HtmlUnit`() {
        val articleHtml = buildString {
            append("<html><head><title>Test Article Title</title>")
            append("<meta property=\"og:site_name\" content=\"Test Site\">")
            append("</head><body>")
            append("<header><nav><a href=\"/\">Home</a></nav></header>")
            append("<article><h1>Test Article Title</h1>")
            repeat(10) { index ->
                append("<p>This is paragraph number $index of the article body. ")
                append("It contains enough meaningful text so the extractor keeps it around ")
                append("instead of discarding it as boilerplate content")
                if (index == 5) {
                    append(" with a <a href=\"/other-post\">related story</a> and <strong>bold text</strong>")
                }
                append(".</p>")
                if (index == 6) {
                    append("<div id=\"attachment_123\" class=\"wp-caption alignnone\">")
                    append("<img loading=\"lazy\" decoding=\"async\" class=\"wp-image-123 size-full\" ")
                    append("src=\"https://example.com/images/mid-article-photo.jpg\" alt=\"\" ")
                    append("width=\"980\" height=\"653\">")
                    append("<p class=\"wp-caption-text\">A caption for the image</p>")
                    append("</div>")
                }
            }
            append("<iframe src=\"https://www.youtube.com/embed/abc123\"></iframe>")
            append("<script>localStorage.getItem('darkmode');</script>")
            append("</article>")
            append("<footer>Copyright footer junk</footer>")
            append("</body></html>")
        }

        val htmlEscaped = Json.encodeToString(articleHtml)
        val urlEscaped = Json.encodeToString("https://example.com/posts/test-article")

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
                    parseReaderContent($htmlEscaped, $urlEscaped, null).then(function(result) {
                        parsingResult = result;
                        var parsed = JSON.parse(parsingResult);
                        if (parsed.error) {
                            parsingError = parsed.error;
                            parsingResult = null;
                        }
                        parsingDone = true;
                    }).catch(function(e) {
                        parsingError = e.toString() + (e.stack ? '\n' + e.stack : '');
                        parsingDone = true;
                    });
                } catch(e) {
                    parsingError = e.toString() + (e.stack ? '\n' + e.stack : '');
                    parsingDone = true;
                }
            """.trimIndent()

            page.executeJavaScript(script)
            assertTrue(waitForParsing(webClient, page), "Mercury parsing did not finish")

            val errorObj = page.executeJavaScript("parsingError").javaScriptResult
            if (errorObj != null && errorObj != Undefined.instance) {
                error("Mercury JS error: $errorObj")
            }

            val resultObj = page.executeJavaScript("parsingResult").javaScriptResult
            assertNotNull(resultObj, "parseReaderContent returned no result")

            val jsObject = Json.parseToJsonElement(resultObj.toString()).jsonObject
            val content = jsObject["content"]?.jsonPrimitive?.content
            assertNotNull(content, "content missing from result")
            assertTrue(content.contains("paragraph number"), "article body missing from content: $content")
            assertTrue(!content.contains("<p>"), "content still contains HTML: $content")
            assertTrue(!content.contains("localStorage"), "script body leaked into content: $content")
            assertTrue(
                content.contains("[related story](https://example.com/other-post)"),
                "link missing from markdown: $content",
            )
            assertTrue(content.contains("**bold text**"), "bold text missing from markdown: $content")
            assertTrue(
                content.contains("![ ](https://example.com/images/mid-article-photo.jpg)"),
                "image missing from markdown: $content",
            )
            assertEquals(
                1,
                Regex.escape("A caption for the image").toRegex().findAll(content).count(),
                "caption duplicated in markdown: $content",
            )
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
        private const val PARSING_TIMEOUT_MS = 20_000L
        private const val BACKGROUND_JS_POLL_MS = 50L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
