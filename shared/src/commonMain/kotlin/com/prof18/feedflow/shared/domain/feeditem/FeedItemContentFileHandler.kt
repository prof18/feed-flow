package com.prof18.feedflow.shared.domain.feeditem

/**
 * Interface for managing feed item content file storage.
 *
 * Implementations handle platform-specific file I/O:
 * - Android: context.filesDir
 * - iOS: App Group container
 * - Desktop: user.home/.feedflow/articles
 */
interface FeedItemContentFileHandler {
    /**
     * Save parsed feed item content to file.
     *
     * @param feedItemId Unique identifier for the feed item (URL hash)
     * @param content Parsed HTML content
     */
    suspend fun saveFeedItemContentToFile(feedItemId: String, content: String)

    /**
     * Load feed item content from file.
     *
     * @param feedItemId Unique identifier for the feed item
     * @return Parsed HTML content, or null if not cached
     */
    suspend fun loadFeedItemContent(feedItemId: String): String?

    /**
     * Check if feed item content is cached.
     *
     * @param feedItemId Unique identifier for the feed item
     * @return true if content file exists
     */
    suspend fun isContentAvailable(feedItemId: String): Boolean

    /**
     * Delete cached feed item content.
     *
     * @param feedItemId Unique identifier for the feed item
     */
    suspend fun deleteFeedItemContent(feedItemId: String)

    /**
     * Clear all cached feed item content.
     * Used for clearing cache or troubleshooting.
     */
    suspend fun clearAllContent()
}
