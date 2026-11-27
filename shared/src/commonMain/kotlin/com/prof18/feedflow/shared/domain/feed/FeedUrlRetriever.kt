package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetrievalResult
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
        return getDeclaredFeeds(url).feedUrls.firstOrNull()
    }

    /** The feeds the page declares, resolved against where the page ended up after redirects. */
    suspend fun getDeclaredFeeds(pageUrl: String): DeclaredFeeds {
        val content = htmlRetriever.retrieveResult(pageUrl) as? HtmlRetrievalResult.Content
            ?: return DeclaredFeeds(pageUrl = pageUrl, feedUrls = emptyList())
        return DeclaredFeeds(
            pageUrl = content.effectiveUrl,
            feedUrls = htmlParser.getRssUrls(content.html, content.effectiveUrl),
        )
    }
}

internal data class DeclaredFeeds(
    val pageUrl: String,
    val feedUrls: List<String>,
)
