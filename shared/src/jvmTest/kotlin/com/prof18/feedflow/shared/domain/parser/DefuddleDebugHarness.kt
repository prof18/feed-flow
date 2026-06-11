package com.prof18.feedflow.shared.domain.parser

import kotlinx.serialization.json.Json
import org.htmlunit.BrowserVersion
import org.htmlunit.WebClient
import org.htmlunit.corejs.javascript.Undefined
import org.htmlunit.html.HtmlPage
import java.io.File
import kotlin.test.Test

/**
 * Manual debugging harness for the desktop defuddle pipeline. Not a regression test.
 * Reads the article HTML from the file in DEBUG_HTML_PATH and prints defuddle's
 * debug log plus the resulting markdown. Run with:
 * ./gradlew --quiet --console=plain :shared:jvmTest --tests "*.DefuddleDebugHarness"
 */
class DefuddleDebugHarness {

    private fun loadResource(name: String): String =
        DefuddleDebugHarness::class.java
            .getResourceAsStream("/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Could not load $name")

    @Test
    fun `debug defuddle on a real article`() {
        val htmlPath = System.getenv("DEBUG_HTML_PATH") ?: DEBUG_HTML_PATH
        val articleUrl = System.getenv("DEBUG_ARTICLE_URL") ?: DEFAULT_ARTICLE_URL
        val htmlFile = File(htmlPath)
        if (!htmlFile.exists()) {
            println("No fixture at $htmlPath, skipping")
            return
        }
        val articleHtml = htmlFile.readText()

        val defuddleJs = loadResource("defuddle-full-es5.js")
        val readerContentParserJs = loadResource("defuddle-content-parser.js")
        val htmlShell = """
        <html dir='auto'>
        <head>
          <script>
            var __logs = [];
            (function() {
              function capture(level) {
                return function() {
                  var parts = [];
                  for (var i = 0; i < arguments.length; i++) {
                    var a = arguments[i];
                    try {
                      if (a && (a.message || a.stack)) {
                        parts.push((a.name || 'Error') + ': ' + a.message + (a.stack ? '\n' + a.stack : ''));
                      } else {
                        parts.push(String(a));
                      }
                    } catch (e) { parts.push('?'); }
                  }
                  __logs.push(level + ': ' + parts.join(' '));
                };
              }
              window.console = {
                log: capture('log'),
                warn: capture('warn'),
                error: capture('error'),
                info: capture('info'),
                debug: capture('debug')
              };
            })();
          </script>
          <script>$defuddleJs</script>
          <script>$readerContentParserJs</script>
        </head>
        <body></body>
        </html>
        """.trimIndent()

        val htmlEscaped = Json.encodeToString(articleHtml)
        val urlEscaped = Json.encodeToString(articleUrl)

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
                    var doc = new DOMParser().parseFromString($htmlEscaped, 'text/html');
                    var defuddle = new Defuddle(doc, {
                        url: $urlEscaped,
                        markdown: true,
                        debug: true
                    });
                    var result = defuddle.parse();
                    parsingResult = result && result.content ? result.content : null;
                } catch(e) {
                    parsingError = e.toString() + (e.stack ? '\n' + e.stack : '');
                }
            """.trimIndent()

            page.executeJavaScript(script)

            val logs = page.executeJavaScript("__logs.join('\\n')").javaScriptResult
            println("===== CONSOLE =====")
            println(logs)

            val errorObj = page.executeJavaScript("parsingError").javaScriptResult
            if (errorObj != null && errorObj != Undefined.instance) {
                println("===== ERROR =====")
                println(errorObj)
            }

            val resultObj = page.executeJavaScript("parsingResult").javaScriptResult
            println("===== CONTENT =====")
            val content = if (resultObj != null && resultObj != Undefined.instance) resultObj.toString() else "<null>"
            println(content.take(1500))
            println("===== CONTENT TAIL =====")
            println(content.takeLast(1500))
            println("===== CONTENT LENGTH: ${content.length} =====")
        }
    }

    private companion object {
        private const val DEBUG_HTML_PATH = "/tmp/ilpost-article.html"
        private const val DEFAULT_ARTICLE_URL =
            "https://www.ilpost.it/2026/06/11/jose-mourinho-real-madrid-ritorno/"
    }
}
