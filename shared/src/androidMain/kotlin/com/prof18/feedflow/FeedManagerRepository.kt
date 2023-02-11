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
}
