package com.prof18.feedflow

class FeedManagerRepository(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedParser: OPMLFeedParser,
) {

    suspend fun addFeedsFromFile(source: String) {
        val feeds = opmlFeedParser.parse(source)
        val categories = feeds.mapNotNull { it.category }.distinct()

        databaseHelper.insertCategories(categories)
        databaseHelper.insertFeedSource(feeds)
    }

    suspend fun getFeeds(): List<FeedSource> {
        return databaseHelper.getFeedSources()
    }

    // TODO: Add category
    suspend fun addFeed(url: String, name: String) {
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
}
