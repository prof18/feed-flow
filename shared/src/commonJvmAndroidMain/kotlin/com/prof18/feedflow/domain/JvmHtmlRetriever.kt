package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

internal class JvmHtmlRetriever(
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
) : HtmlRetriever {
    override suspend fun retrieveHtml(url: String): String? = withContext(dispatcherProvider.io) {
        try {
            return@withContext Jsoup.connect(url).get().html()
        } catch (e: Exception) {
            logger.e(e) { "Unable to retrieve HTML, skipping" }
            return@withContext null
        }
    }
}
