package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.rssparser.model.RssChannel

internal class FeedSourceLogoRetriever(
    private val htmlRetriever: HtmlRetriever,
    private val htmlParser: HtmlParser,
) {
    suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel): String? {
        val regex = "^.+?[^/:](?=[?/]|\$)".toRegex()
        val baseDomain = rssChannel.link?.let { link ->
            regex.find(link)?.value
        }

        var logoUrl = when {
            rssChannel.image?.url != null -> rssChannel.image?.url
            baseDomain != null -> {
                val html = htmlRetriever.retrieveHtml(baseDomain)
                if (html != null) {
                    htmlParser.getFaviconUrl(html)
                } else {
                    null
                }
            }

            else -> null
        }

        if (baseDomain != null && (logoUrl == null || logoUrl.startsWith("/"))) {
            logoUrl = "$baseDomain/favicon.ico"
        }
        return logoUrl
    }
}
