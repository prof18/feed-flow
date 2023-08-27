package com.prof18.feedflow.domain.feed.retriever

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.domain.model.FeedItemId
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.presentation.model.ErrorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FeedRetrieverRepository {
    val updateState: StateFlow<FeedUpdateStatus>
    val errorState: StateFlow<ErrorState?>
    fun getFeeds(): Flow<List<FeedItem>>
    suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>)
    suspend fun fetchFeeds(updateLoadingInfo: Boolean = true, forceRefresh: Boolean = false)
    suspend fun markAllFeedAsRead()
    suspend fun deleteOldFeeds()
}
