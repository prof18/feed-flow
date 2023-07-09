package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.model.ParsedFeedSource
import com.prof18.feedflow.domain.opml.OPMLFeedHandler
import com.prof18.feedflow.domain.opml.OPMLInput
import com.prof18.feedflow.domain.opml.OPMLOutput

internal class FeedManagerRepositoryImpl(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedHandler: OPMLFeedHandler,
) : FeedManagerRepository {

    override suspend fun addFeedsFromFile(opmlInput: OPMLInput) {
        val feeds = opmlFeedHandler.importFeed(opmlInput)
        val categories = feeds.mapNotNull { it.category }.distinct()

        databaseHelper.insertCategories(categories)
        databaseHelper.insertFeedSource(feeds)
    }

    override suspend fun getFeeds(): List<FeedSource> {
        return databaseHelper.getFeedSources()
    }

    // TODO: Add category
    override suspend fun addFeed(url: String, name: String) {
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    url = url,
                    title = name,
                    category = null,
                )
            )
        )
    }

    override suspend fun exportFeedsAsOpml(opmlOutput: OPMLOutput) {
        val feeds = databaseHelper.getFeedSources()
        opmlFeedHandler.exportFeed(opmlOutput, feeds)
    }
}
