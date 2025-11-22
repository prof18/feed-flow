package com.prof18.feedflow.shared.domain.feeditem

/**
 *
 * Implementations handle platform-specific file I/O:
 * - Android: context.filesDir
 * - iOS: App Group container
 * - Desktop: user.home/.feedflow/articles
 */
interface FeedItemContentFileHandler {
    suspend fun saveFeedItemContentToFile(feedItemId: String, content: String)
    suspend fun loadFeedItemContent(feedItemId: String): String?
    suspend fun isContentAvailable(feedItemId: String): Boolean
    suspend fun deleteFeedItemContent(feedItemId: String)
    suspend fun clearAllContent()
}
