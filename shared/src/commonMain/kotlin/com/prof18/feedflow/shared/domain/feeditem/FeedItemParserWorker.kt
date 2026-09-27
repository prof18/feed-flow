package com.prof18.feedflow.shared.domain.feeditem

interface FeedItemParserWorker {
    /**
     * Returns the article body extracted from [url], or null when the page cannot be turned into
     * reader content. The reader adds title, site name and hero image when it renders it.
     */
    suspend fun parse(url: String): String?
}
