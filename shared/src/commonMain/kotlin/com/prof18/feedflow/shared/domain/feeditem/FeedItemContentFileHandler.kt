package com.prof18.feedflow.shared.domain.feeditem

/**
 * Android and Desktop share `JvmFeedItemContentFileHandler`; iOS stores files in the App Group container.
 */
interface FeedItemContentFileHandler {
    suspend fun saveFeedItemContentToFile(feedItemId: String, content: String)
    suspend fun loadFeedItemContent(feedItemId: String): String?
    suspend fun isContentAvailable(feedItemId: String): Boolean
    suspend fun deleteFeedItemContent(feedItemId: String)
    suspend fun clearAllContent()
}
