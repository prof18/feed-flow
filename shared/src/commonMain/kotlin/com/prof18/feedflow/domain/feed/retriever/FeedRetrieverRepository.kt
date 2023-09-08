package com.prof18.feedflow.domain.feed.retriever

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.domain.model.FeedItemId
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.presentation.model.ErrorState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.flow.StateFlow
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
interface FeedRetrieverRepository {
    @NativeCoroutinesIgnore
    val updateState: StateFlow<FeedUpdateStatus>

    @NativeCoroutinesIgnore
    val errorState: StateFlow<ErrorState?>

    @NativeCoroutinesIgnore
    val feedState: StateFlow<List<FeedItem>>

    fun getFeeds()

    @NativeCoroutinesIgnore
    suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>)

    @NativeCoroutinesIgnore
    suspend fun fetchFeeds(updateLoadingInfo: Boolean = true, forceRefresh: Boolean = false)

    @NativeCoroutinesIgnore
    suspend fun markAllFeedAsRead()

    @NativeCoroutinesIgnore
    suspend fun deleteOldFeeds()
}
