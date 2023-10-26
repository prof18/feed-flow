package com.prof18.feedflow.domain

import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

internal class JvmHtmlRetriever(
    private val dispatcherProvider: DispatcherProvider,
) : HtmlRetriever {
    override suspend fun retrieveHtml(url: String): String? = withContext(dispatcherProvider.io) {
        return@withContext Jsoup.connect(url).get().html()
    }
}
