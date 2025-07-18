package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class ReaderModeExtractor internal constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val htmlRetriever: HtmlRetriever,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun extractReaderContent(urlInfo: FeedItemUrlInfo): ReaderModeData? = withContext(dispatcherProvider.io) {
        val html = htmlRetriever.retrieveHtml(urlInfo.url) ?: return@withContext null

        val doc = Jsoup.parse(html)
        val modifiedHtml = doc.html()

        return@withContext ReaderModeData(
            id = FeedItemId(urlInfo.id),
            content = modifiedHtml,
            url = urlInfo.url,
            fontSize = settingsRepository.getReaderModeFontSize(),
            isBookmarked = urlInfo.isBookmarked,
            title = null,
        )
    }
}
