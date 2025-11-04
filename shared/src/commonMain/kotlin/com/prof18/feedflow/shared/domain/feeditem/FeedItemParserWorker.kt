package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.core.model.ParsingResult

/**
 * Interface for parsing feed item content across platforms.
 *
 * Implementations handle fetching HTML and extracting feed item content
 * using platform-specific WebView/parsing libraries.
 */
interface FeedItemParserWorker {
    /**
     * Enqueue feed item parsing in background (fire-and-forget).
     * Used for bookmarked feed items that should be cached for offline reading.
     *
     * @param feedItemId The feed item ID to use as the filename
     * @param url The URL to fetch and parse
     *
     * Android: Uses WorkManager
     * iOS: Uses CoroutineScope with background dispatcher
     * Desktop: Uses CoroutineScope
     */
    suspend fun enqueueParsing(feedItemId: String, url: String)

    /**
     * Trigger immediate parsing with result callback.
     * Used when user opens feed item and content not cached.
     *
     * @param feedItemId The feed item ID to use as the filename
     * @param url The URL to fetch and parse
     *
     * Blocks until parsing completes or fails.
     */
    suspend fun triggerImmediateParsing(feedItemId: String, url: String): ParsingResult

    /**
     * Trigger background parsing (non-blocking).
     * Used by background workers for batch processing.
     *
     * @param feedItemId The feed item ID to use as the filename
     * @param url The URL to fetch and parse
     */
    suspend fun triggerBackgroundParsing(feedItemId: String, url: String): ParsingResult
}
