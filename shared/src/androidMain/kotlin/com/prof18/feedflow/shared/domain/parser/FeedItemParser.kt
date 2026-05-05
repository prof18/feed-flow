package com.prof18.feedflow.shared.domain.parser

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.HtmlRetriever
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.Duration.Companion.seconds

internal class FeedItemParser(
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val webView: WebView? = null,
) {
    private val parserPageMutex = Mutex()
    private val parseMutex = Mutex()
    private var loadedWebView: WebView? = null
    private var defuddleJs: String? = null

    suspend fun warmUp() {
        ensureParserPageReady()
    }

    suspend fun parseFeedItem(url: String, onResult: (ParsingResult) -> Unit) {
        onResult(parseFeedItem(url))
    }

    suspend fun parseFeedItem(url: String): ParsingResult = coroutineScope {
        val parserPageReady = async { ensureParserPageReady() }
        val html = withContext(dispatcherProvider.io) {
            htmlRetriever.retrieveHtml(url)?.let(::cleanPlaceholderImages)
        } ?: return@coroutineScope ParsingResult.Error

        parseMutex.withLock {
            val parserWebView = parserPageReady.await() ?: return@withLock ParsingResult.Error
            executeArticleParse(
                webView = parserWebView,
                url = url,
                html = html,
            )
        }
    }

    private suspend fun ensureParserPageReady(): WebView? {
        loadedWebView?.let { return it }

        return parserPageMutex.withLock {
            loadedWebView?.let { return@withLock it }

            val providedWebView = webView
            val parserWebView = if (providedWebView != null) {
                withContext(dispatcherProvider.main) {
                    setupWebView(providedWebView)
                }
                providedWebView
            } else {
                withContext(dispatcherProvider.main) {
                    WebView(appContext).also(::setupWebView)
                }
            }
            if (!loadParserPage(parserWebView)) {
                return@withLock null
            }
            loadedWebView = parserWebView
            parserWebView
        }
    }

    private suspend fun loadParserPage(parserWebView: WebView): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val js = loadDefuddleJs()
        withContext(dispatcherProvider.main) {
            parserWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (!deferred.isCompleted) {
                        deferred.complete(true)
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame && !deferred.isCompleted) {
                        deferred.complete(false)
                    }
                }
            }

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
            parserWebView.loadDataWithBaseURL(null, htmlScript, "text/html", "UTF-8", null)
        }
        return withTimeoutOrNull(PARSER_PAGE_LOAD_TIMEOUT) {
            deferred.await()
        } == true
    }

    private suspend fun executeArticleParse(
        webView: WebView,
        url: String,
        html: String,
    ): ParsingResult =
        withContext(dispatcherProvider.main) {
            val htmlClean = JSONObject.quote(html)
            val urlClean = JSONObject.quote(url)
            val resultDeferred = CompletableDeferred<ParsingResult>()
            val injectedJS = """
                (function() {
                    console.log('Parsing content');
                    const link = $urlClean;
                    const htmlContent = $htmlClean;

                    const parser = new DOMParser();
                    const doc = parser.parseFromString(htmlContent, 'text/html');

                    const defuddle = new Defuddle(doc, {
                         url: link
                    });

                    const result = defuddle.parse();

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

                    let plainText = '';
                    if (result.content) {
                        const tempDiv2 = document.createElement('div');
                        tempDiv2.innerHTML = result.content;
                        plainText = (tempDiv2.textContent || tempDiv2.innerText || '').trim();
                    }

                    result.plainText = plainText;

                    return result;
                })();
            """.trimIndent()

            webView.evaluateJavascript(injectedJS) { result ->
                resultDeferred.complete(parseJavaScriptResult(result))
            }
            withTimeoutOrNull(PARSER_EXECUTION_TIMEOUT) {
                resultDeferred.await()
            } ?: ParsingResult.Error
        }

    private fun parseJavaScriptResult(result: String?): ParsingResult =
        try {
            val jsonResult = JSONObject(result.orEmpty())
            var content = jsonResult.getString("content")
            val title = jsonResult.optString("title")
            val site = jsonResult.optString("site")
            val plainText = jsonResult.optString("plainText", "")
            logger.d { "title: $title" }

            if (plainText.length < MIN_CONTENT_LENGTH) {
                logger.d { "Content too short (${plainText.length} chars), rejecting" }
                return ParsingResult.Error
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

            ParsingResult.Success(
                htmlContent = content.takeIf { it.isNotBlank() },
                title = title.takeIf { it.isNotBlank() },
                siteName = site.takeIf { it.isNotBlank() },
            )
        } catch (_: Throwable) {
            ParsingResult.Error
        }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.javaScriptEnabled = true
    }

    private fun cleanPlaceholderImages(html: String): String {
        if (!html.contains(PLACEHOLDER_IMAGE_MARKER)) return html

        return PLACEHOLDER_IMAGE_URL_REGEX.replace(html, "")
    }

    private suspend fun loadDefuddleJs(): String {
        val existing = defuddleJs
        if (existing != null) return existing

        val loadedJs = withContext(dispatcherProvider.io) {
            readRawResource(appContext, com.prof18.feedflow.shared.R.raw.defuddle)
        }
        defuddleJs = loadedJs
        return loadedJs
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

private const val MIN_CONTENT_LENGTH = 200
private const val PLACEHOLDER_IMAGE_MARKER = "placeholder.png"
private val PARSER_PAGE_LOAD_TIMEOUT = 5.seconds
private val PARSER_EXECUTION_TIMEOUT = 10.seconds
private val PLACEHOLDER_IMAGE_URL_REGEX = Regex("""https?://[^\s"'<>]*placeholder\.png""")
