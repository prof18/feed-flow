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
     * Android: Uses WorkManager
     * iOS: Uses CoroutineScope with background dispatcher
     * Desktop: Uses CoroutineScope
     */
    suspend fun enqueueParsing(url: String)

    /**
     * Trigger immediate parsing with result callback.
     * Used when user opens feed item and content not cached.
     *
     * Blocks until parsing completes or fails.
     */
    suspend fun triggerImmediateParsing(url: String): ParsingResult

    /**
     * Trigger background parsing (non-blocking).
     * Used by background workers for batch processing.
     */
    suspend fun triggerBackgroundParsing(url: String): ParsingResult
}
