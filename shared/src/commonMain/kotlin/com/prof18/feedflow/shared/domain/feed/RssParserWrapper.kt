package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFetchTier
import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel

internal interface RssParserWrapper {
    suspend fun getRssChannel(url: String, allowBrowserTier: Boolean = true): RssChannel
}

internal class RssParserWrapperImpl(
    private val feedHttpCacheStore: FeedHttpCacheStore,
    private val parsers: Map<FeedFetchTier, suspend (String) -> RssChannel>,
) : RssParserWrapper {
    override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
        val permitted = FeedFetchTier.entries.filter { it != FeedFetchTier.BROWSER || allowBrowserTier }
        val remembered = feedHttpCacheStore.tierFor(url)?.takeIf { it in permitted }
        val tiers = listOfNotNull(remembered) + permitted.filter { it != remembered }
        var lastForbidden: HttpException? = null
        for (tier in tiers) {
            val parser = parsers[tier] ?: continue
            try {
                val channel = parser(url)
                feedHttpCacheStore.recordSuccessfulTier(url, tier)
                return channel
            } catch (error: HttpException) {
                if (error.code != HTTP_FORBIDDEN) {
                    throw error
                }
                lastForbidden = error
            }
        }
        throw checkNotNull(lastForbidden) { "No fetch tier ran for $url" }
    }

    private companion object {
        const val HTTP_FORBIDDEN = 403
    }
}
