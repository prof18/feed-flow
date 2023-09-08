package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.domain.model.Browser
import com.prof18.feedflow.domain.model.NotValidFeedSources
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.flow.Flow

interface FeedManagerRepository {

    @NativeCoroutinesIgnore
    suspend fun addFeedsFromFile(opmlInput: OpmlInput): NotValidFeedSources

    @NativeCoroutinesIgnore
    suspend fun getFeedSources(): Flow<List<FeedSource>>

    @NativeCoroutinesIgnore
    suspend fun addFeed(url: String, name: String)

    @NativeCoroutinesIgnore
    suspend fun checkIfValidRss(url: String): Boolean

    @NativeCoroutinesIgnore
    suspend fun exportFeedsAsOpml(opmlOutput: OpmlOutput)

    @NativeCoroutinesIgnore
    suspend fun deleteFeed(feedSource: FeedSource)
    fun getFavouriteBrowserId(): String?
    fun setFavouriteBrowser(browser: Browser)
    fun deleteAllFeeds()
}
