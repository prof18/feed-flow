package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.data.SettingsHelper
import com.prof18.feedflow.domain.model.Browser
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import kotlinx.coroutines.flow.Flow

internal class FeedManagerRepositoryImpl(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedHandler: OpmlFeedHandler,
    private val settingsHelper: SettingsHelper,
) : FeedManagerRepository {
    override suspend fun addFeedsFromFile(opmlInput: OpmlInput) {
        val feeds = opmlFeedHandler.importFeed(opmlInput)
        val categories = feeds.mapNotNull { it.category }.distinct()

        databaseHelper.insertCategories(categories)
        databaseHelper.insertFeedSource(feeds)
    }

    override suspend fun getFeeds(): Flow<List<FeedSource>> =
        databaseHelper.getFeedSourcesFlow()

    // TODO: Add category?
    override suspend fun addFeed(url: String, name: String) {
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    url = url,
                    title = name,
                    category = null,
                ),
            ),
        )
    }

    override suspend fun exportFeedsAsOpml(opmlOutput: OpmlOutput) {
        val feeds = databaseHelper.getFeedSources()
        opmlFeedHandler.exportFeed(opmlOutput, feeds)
    }

    override suspend fun deleteFeed(feedSource: FeedSource) {
        databaseHelper.deleteFeedSource(feedSource)
    }

    override fun getFavouriteBrowserId(): String? =
        settingsHelper.getFavouriteBrowserId()

    override fun setFavouriteBrowser(browser: Browser) {
        settingsHelper.saveFavouriteBrowserId(browser.id)
    }
}
