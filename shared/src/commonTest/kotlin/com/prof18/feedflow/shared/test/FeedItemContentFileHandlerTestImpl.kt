package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler

class FeedItemContentFileHandlerTestImpl : FeedItemContentFileHandler {
    private val contentMap = mutableMapOf<String, String>()

    override suspend fun saveFeedItemContentToFile(feedItemId: String, content: String) {
        contentMap[feedItemId] = content
    }

    override suspend fun loadFeedItemContent(feedItemId: String): String? {
        return contentMap[feedItemId]
    }

    override suspend fun isContentAvailable(feedItemId: String): Boolean {
        return contentMap.containsKey(feedItemId)
    }

    override suspend fun deleteFeedItemContent(feedItemId: String) {
        contentMap.remove(feedItemId)
    }

    override suspend fun clearAllContent() {
        contentMap.clear()
    }
}
