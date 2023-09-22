package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.domain.model.NotValidFeedSources
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import kotlinx.coroutines.flow.Flow

internal interface FeedManagerRepository {
    suspend fun addFeedsFromFile(opmlInput: OpmlInput): NotValidFeedSources
    suspend fun getFeedSources(): Flow<List<FeedSource>>
    suspend fun addFeed(url: String, name: String)
    suspend fun checkIfValidRss(url: String): Boolean
    suspend fun exportFeedsAsOpml(opmlOutput: OpmlOutput)
    suspend fun deleteFeed(feedSource: FeedSource)
    fun deleteAllFeeds()
}
