package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever

internal class FeedUrlRetriever(
    private val htmlParser: HtmlParser,
    private val htmlRetriever: HtmlRetriever,
) {
    suspend fun getFeedUrl(url: String): String? {
        val html = htmlRetriever.retrieveHtml(url) ?: return null
        return htmlParser.getRssUrl(html)
    }
}
