package com.prof18.feedflow.domain.feed.manager

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.data.SettingsHelper
import com.prof18.feedflow.domain.model.Browser
import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.flow.Flow

internal class FeedManagerRepositoryImpl(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedHandler: OpmlFeedHandler,
    private val settingsHelper: SettingsHelper,
    private val rssParser: RssParser,
    private val logger: Logger,
) : FeedManagerRepository {
    override suspend fun addFeedsFromFile(opmlInput: OpmlInput) {
        val feeds = opmlFeedHandler.generateFeedSources(opmlInput)
        val categories = feeds.mapNotNull { it.category }.distinct()

        // TODO: check url and returns

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

    @Suppress("TooGenericExceptionCaught")
    override suspend fun checkIfValidRss(url: String): Boolean {
        return try {
            rssParser.getRssChannel(url)
            true
        } catch (e: Throwable) {
            logger.d { "Wrong url input: $e" }
            false
        }
    }
}
