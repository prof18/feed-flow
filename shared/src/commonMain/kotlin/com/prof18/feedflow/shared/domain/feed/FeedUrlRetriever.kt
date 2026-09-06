package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever

internal class FeedUrlRetriever(
    private val htmlParser: HtmlParser,
    private val htmlRetriever: HtmlRetriever,
) {
    suspend fun getFeedUrl(url: String): String? {
        val channelUrl = YouTubeChannelUrl.parse(url)
        channelUrl?.channelId?.let { return YouTubeChannelUrl.feedUrl(it) }
        if (channelUrl != null) {
            val html = htmlRetriever.retrieveHtml(channelUrl.pageUrl) ?: return null
            htmlParser.getRssUrl(html)?.let { YouTubeChannelUrl.normalizeFeedUrl(it) }?.let { return it }
            val canonicalUrl = htmlParser.getCanonicalUrl(html) ?: return null
            val channelId = YouTubeChannelUrl.parse(canonicalUrl)?.channelId ?: return null
            return YouTubeChannelUrl.feedUrl(channelId)
        }
        val html = htmlRetriever.retrieveHtml(url) ?: return null
        return htmlParser.getRssUrl(html)
    }
}
