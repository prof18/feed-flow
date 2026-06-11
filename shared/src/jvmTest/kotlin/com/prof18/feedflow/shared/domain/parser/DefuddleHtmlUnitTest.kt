package com.prof18.feedflow.shared.domain.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.corejs.javascript.Undefined
import org.htmlunit.html.HtmlPage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefuddleHtmlUnitTest {

    private fun loadResource(name: String): String =
        DefuddleHtmlUnitTest::class.java
            .getResourceAsStream("/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Could not load $name")

    private val htmlShell: String by lazy {
        val defuddleJs = loadResource("defuddle-full-es5.js")
        val readerContentParserJs = loadResource("defuddle-content-parser.js")
        // language=HTML
        """
        <html dir='auto'>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <script>$defuddleJs</script>
          <script>$readerContentParserJs</script>
        </head>
        <body></body>
        </html>
        """.trimIndent()
    }

    @Test
    fun `defuddle parses an article inside HtmlUnit`() {
        val articleHtml = buildString {
            append("<html><head><title>Test Article Title</title>")
            append("<meta property=\"og:site_name\" content=\"Test Site\">")
            append("</head><body>")
            append("<header><nav><a href=\"/\">Home</a></nav></header>")
            append("<article><h1>Test Article Title</h1>")
            repeat(10) { index ->
                append("<p>This is paragraph number $index of the article body. ")
                append("It contains enough meaningful text so the extractor keeps it around ")
                append("instead of discarding it as boilerplate content.</p>")
            }
            append("<img src=\"/images/photo.png\">")
            append("<iframe src=\"https://www.youtube.com/embed/abc123\"></iframe>")
            append("<script>localStorage.getItem('darkmode');</script>")
            append("<a href=\"/other-post\">Read more</a>")
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
                try {
                    parsingResult = parseReaderContent($htmlEscaped, $urlEscaped, null);
                    var parsed = JSON.parse(parsingResult);
                    if (parsed.error) {
                        parsingError = parsed.error;
                        parsingResult = null;
                    }
                } catch(e) {
                    parsingError = e.toString() + (e.stack ? '\n' + e.stack : '');
                }
            """.trimIndent()

            page.executeJavaScript(script)

            val errorObj = page.executeJavaScript("parsingError").javaScriptResult
            if (errorObj != null && errorObj != Undefined.instance) {
                error("Defuddle JS error: $errorObj")
            }

            val resultObj = page.executeJavaScript("parsingResult").javaScriptResult
            assertNotNull(resultObj, "parseReaderContent returned no result")

            val jsObject = Json.parseToJsonElement(resultObj.toString()).jsonObject
            val content = jsObject["content"]?.jsonPrimitive?.content
            assertNotNull(content, "content missing from result")
            assertTrue(content.contains("paragraph number"), "article body missing from content: $content")
            assertTrue(
                content.contains("https://example.com/other-post"),
                "relative link not absolutized: $content",
            )
            assertTrue(
                content.contains("[Read more](https://example.com/other-post)"),
                "content is not markdown: $content",
            )
            assertTrue(!content.contains("<p>"), "content still contains HTML: $content")
            assertTrue(!content.contains("localStorage"), "script body leaked into content: $content")
        }
    }
}
