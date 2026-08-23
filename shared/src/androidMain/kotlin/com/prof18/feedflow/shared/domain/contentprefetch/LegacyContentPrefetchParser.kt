package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import android.webkit.WebView
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.parser.FeedItemParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal class LegacyContentPrefetchParser(
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
) {
    private var webView: WebView? = null

    suspend fun parse(url: String): ParsingResult {
        val deferredResult = CompletableDeferred<ParsingResult>()
        FeedItemParser(
            htmlRetriever = htmlRetriever,
            appContext = appContext,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
            webView = getOrCreateWebView(),
        ).parseFeedItem(url) { result ->
            deferredResult.complete(result)
        }
        return deferredResult.await()
    }

    suspend fun close() {
        val parserWebView = webView ?: return
        webView = null
        withContext(NonCancellable + dispatcherProvider.main) {
            parserWebView.destroy()
        }
    }

    private suspend fun getOrCreateWebView(): WebView {
        webView?.let { return it }
        return withContext(dispatcherProvider.main) {
            WebView(appContext)
        }.also { webView = it }
    }
}
