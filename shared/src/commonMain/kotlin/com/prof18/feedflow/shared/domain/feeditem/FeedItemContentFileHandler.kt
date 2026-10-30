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

    /** Writes [content] next to the article file without replacing it; null when the write failed. */
    suspend fun stageContent(feedItemId: String, content: String): StagedReaderContent?

    /** Atomically replaces the article file inside [commit]; false when stale or failed. */
    suspend fun promoteStaged(staged: StagedReaderContent, commit: (() -> Unit) -> Unit): Boolean

    suspend fun discardStaged(staged: StagedReaderContent)
}

class StagedReaderContent internal constructor(val feedItemId: String, internal val token: Any)
