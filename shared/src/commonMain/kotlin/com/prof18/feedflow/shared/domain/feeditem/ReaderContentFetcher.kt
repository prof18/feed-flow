package com.prof18.feedflow.shared.domain.feeditem

internal class ReaderContentFetcher(
    private val articleContentParser: ArticleContentParser,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) {
    suspend fun fetch(feedItemId: String, url: String, save: Boolean = false): String? {
        val content = articleContentParser.parse(url)?.takeIf { it.isNotBlank() } ?: return null
        if (save) {
            feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
        }
        return content
    }
}
