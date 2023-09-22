package com.prof18.feedflow.domain.feed.retriever

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.domain.model.FeedItemId
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.presentation.model.ErrorState
import kotlinx.coroutines.flow.StateFlow

internal interface FeedRetrieverRepository {
    val updateState: StateFlow<FeedUpdateStatus>

    val errorState: StateFlow<ErrorState?>

    val feedState: StateFlow<List<FeedItem>>

    fun getFeeds()
    suspend fun updateReadStatus(
        lastUpdateIndex: Int,
        lastVisibleIndex: Int,
    )

    suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>)
    suspend fun fetchFeeds(
        updateLoadingInfo: Boolean = true,
        forceRefresh: Boolean = false,
        isFirstLaunch: Boolean = false,
    )

    suspend fun markAllFeedAsRead()
    suspend fun deleteOldFeeds()
}
