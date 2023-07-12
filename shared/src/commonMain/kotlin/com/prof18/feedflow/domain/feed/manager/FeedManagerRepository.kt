package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.domain.model.Browser
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.opml.OPMLInput
import com.prof18.feedflow.domain.opml.OPMLOutput
import kotlinx.coroutines.flow.Flow

interface FeedManagerRepository {
    suspend fun addFeedsFromFile(opmlInput: OPMLInput)
    suspend fun getFeeds(): Flow<List<FeedSource>>
    suspend fun addFeed(url: String, name: String)
    suspend fun exportFeedsAsOpml(opmlOutput: OPMLOutput)
    suspend fun deleteFeed(feedSource: FeedSource)
    fun getFavouriteBrowserId(): String?
    fun setFavouriteBrowser(browser: Browser)
}