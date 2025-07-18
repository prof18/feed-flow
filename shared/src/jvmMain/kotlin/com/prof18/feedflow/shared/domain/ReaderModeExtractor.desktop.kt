package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.withContext
import net.dankito.readability4j.extended.Readability4JExtended

class ReaderModeExtractor internal constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val htmlRetriever: HtmlRetriever,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun extractReaderContent(urlInfo: FeedItemUrlInfo): ReaderModeData? = withContext(dispatcherProvider.io) {
        val html = htmlRetriever.retrieveHtml(urlInfo.url) ?: return@withContext null

        val readability4J = Readability4JExtended(urlInfo.url, html)
        val article = try {
            readability4J.parse()
        } catch (_: Throwable) {
            null
        }

        val contentWithDocumentsCharsetOrUtf8 = article?.contentWithDocumentsCharsetOrUtf8
            ?.replace(Regex("https?://.*?placeholder\\.png"), "")
            ?: return@withContext null

        return@withContext ReaderModeData(
            id = FeedItemId(urlInfo.id),
            title = article.title ?: urlInfo.title,
            content = contentWithDocumentsCharsetOrUtf8,
            url = urlInfo.url,
            fontSize = settingsRepository.getReaderModeFontSize(),
            isBookmarked = urlInfo.isBookmarked,
        )
    }
}
