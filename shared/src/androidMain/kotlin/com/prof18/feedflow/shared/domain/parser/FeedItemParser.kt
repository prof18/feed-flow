package com.prof18.feedflow.shared.domain.parser

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.HtmlRetriever
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

private const val MIN_CONTENT_LENGTH = 200

internal class FeedItemParser(
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val webView: WebView? = null,
) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun parseFeedItem(url: String, onResult: (ParsingResult) -> Unit) {
        val js = readRawResource(appContext, com.prof18.feedflow.shared.R.raw.defuddle)
        val html = withContext(dispatcherProvider.io) {
            htmlRetriever.retrieveHtml(url).also {
                it?.replace(Regex("https?://.*?placeholder\\.png"), "")
            }
            /*
                Maybe do also:
                 val doc = Jsoup.parse(html)
                val modifiedHtml = doc.html()
             */
        }
        if (html == null) {
            onResult(ParsingResult.Error)
            return
        }
        val htmlClean = JSONObject.quote(html)
        val urlClean = JSONObject.quote(url)

        withContext(dispatcherProvider.main) {
            val webView = webView ?: WebView(appContext)
            webView.apply {
                setWebViewClient(object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val injectedJS = """
                            console.log('Parsing content');
                            const link = $urlClean;
                            const htmlContent = $htmlClean;

                            const parser = new DOMParser();
                            const doc = parser.parseFromString(htmlContent, 'text/html');

                            const defuddle = new Defuddle(doc, {
                                 url: link
                            });

                            const result = defuddle.parse();

                            // Convert relative URLs to absolute
                            if (result.content) {
                                const baseUrl = link;
                                const tempDiv = document.createElement('div');
                                tempDiv.innerHTML = result.content;

                                tempDiv.querySelectorAll('[src]').forEach(el => {
                                    const src = el.getAttribute('src');
                                    if (src && !src.startsWith('http') && !src.startsWith('data:')) {
                                        try {
                                            el.setAttribute('src', new URL(src, baseUrl).href);
                                        } catch (e) {}
                                    }
                                });

                                tempDiv.querySelectorAll('[href]').forEach(el => {
                                    const href = el.getAttribute('href');
                                    if (href && !href.startsWith('http') && !href.startsWith('#') && !href.startsWith('mailto:')) {
                                        try {
                                            el.setAttribute('href', new URL(href, baseUrl).href);
                                        } catch (e) {}
                                    }
                                });

                                result.content = tempDiv.innerHTML;
                            }

                            // Extract plain text from content
                            let plainText = '';
                            if (result.content) {
                                const tempDiv2 = document.createElement('div');
                                tempDiv2.innerHTML = result.content;
                                plainText = (tempDiv2.textContent || tempDiv2.innerText || '').trim();
                            }

                            // Add plainText to result for validation
                            result.plainText = plainText;

                            result
                        """.trimIndent()

                        evaluateJavascript(injectedJS) { result ->
                            logger.d { "Parsed article" }
                            logger.d { "result: $result" }
                            try {
                                val jsonResult = JSONObject(result)
                                var content = jsonResult.getString("content")
                                val title = jsonResult.optString("title")
                                val site = jsonResult.optString("site")
                                val plainText = jsonResult.optString("plainText", "")

                                if (plainText.length < MIN_CONTENT_LENGTH) {
                                    logger.w { "Content too short (${plainText.length} chars), rejecting" }
                                    onResult(ParsingResult.Error)
                                    return@evaluateJavascript
                                }

                                val siteHtml = if (!site.isNullOrEmpty()) {
                                    "<h4>$site</h4>"
                                } else {
                                    ""
                                }
                                if (!title.isNullOrEmpty() && !content.isNullOrEmpty()) {
                                    // Prepend title and site to content for display
                                    content = """
                                        <h1>$title</h1>
                                        $siteHtml
                                        $content
                                    """.trimIndent()
                                }

                                onResult(
                                    ParsingResult.Success(
                                        htmlContent = content.takeIf { it.isNotBlank() },
                                        title = title.takeIf { it.isNotBlank() },
                                        siteName = site.takeIf { it.isNotBlank() },
                                    ),
                                )
                            } catch (_: Exception) {
                                onResult(ParsingResult.Error)
                            }
                        }
                    }
                })

                settings.javaScriptEnabled = true

                // language=html
                val htmlScript = """
                <html dir='auto'>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                      <script>$js</script>
                    </head>
                    <body />
                </html>
                """.trimIndent()
                loadDataWithBaseURL(null, htmlScript, "text/html", "UTF-8", null)
            }
        }
    }

    private fun readRawResource(context: Context, resId: Int): String {
        return context.resources.openRawResource(resId).use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                BufferedReader(reader).use { bufferedReader ->
                    bufferedReader.readText()
                }
            }
        }
    }
}
