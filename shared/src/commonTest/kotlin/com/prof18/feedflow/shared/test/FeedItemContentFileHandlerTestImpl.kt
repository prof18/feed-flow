package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.StagedReaderContent

class FeedItemContentFileHandlerTestImpl : FeedItemContentFileHandler {
    private val contentMap = mutableMapOf<String, String>()
    val savedIds = mutableListOf<String>()
    val stagedIds = mutableListOf<String>()
    var failPromotion = false

    override suspend fun saveFeedItemContentToFile(feedItemId: String, content: String) {
        savedIds += feedItemId
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

    override suspend fun stageContent(feedItemId: String, content: String): StagedReaderContent {
        stagedIds += feedItemId
        return StagedReaderContent(feedItemId, content)
    }

    override suspend fun promoteStaged(staged: StagedReaderContent, commit: (() -> Unit) -> Unit): Boolean {
        if (failPromotion) return false
        var promoted = false
        return try {
            commit {
                contentMap[staged.feedItemId] = staged.token as String
                promoted = true
            }
            promoted
        } catch (_: IllegalStateException) {
            false
        }
    }

    override suspend fun discardStaged(staged: StagedReaderContent) = Unit
}
