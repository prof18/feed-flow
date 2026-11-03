package com.prof18.feedflow.core.domain

import com.prof18.rssparser.model.RssChannel

interface FeedSourceLogoRetriever {
    suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel): String?
    suspend fun getFeedSourceLogoUrl(websiteLink: String?): String?

    suspend fun isStoredLogoStillAvailable(logoUrl: String): Boolean

    /** Lookups are memoised per refresh, so a caller that outlives one has to call this. */
    suspend fun invalidateLookupCaches()
}
