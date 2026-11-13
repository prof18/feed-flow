package com.prof18.feedflow.core.domain

import com.prof18.rssparser.model.RssChannel

interface FeedSourceLogoRetriever {
    /** [feedUrl] stands in for the website when the channel declares no link. */
    suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel, feedUrl: String): String?
    suspend fun getFeedSourceLogoUrl(websiteLink: String?): String?

    suspend fun isStoredLogoStillAvailable(logoUrl: String): Boolean

    /** Lookups are memoised per refresh, so a caller that outlives one has to call this. */
    suspend fun invalidateLookupCaches()
}
