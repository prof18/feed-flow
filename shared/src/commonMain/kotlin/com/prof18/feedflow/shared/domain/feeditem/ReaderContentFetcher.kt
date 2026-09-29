package com.prof18.feedflow.shared.domain.feeditem

internal class ReaderContentFetcher(
    private val feedItemParserWorker: FeedItemParserWorker,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) {
    suspend fun fetch(feedItemId: String, url: String, save: Boolean = true): String? {
        val content = feedItemParserWorker.parse(url)?.takeIf { it.isNotBlank() } ?: return null
        if (save) {
            feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
        }
        return content
    }
}
