package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.rssparser.model.RssChannel

// todo: improve
internal class FeedSourceLogoRetrieverImpl(
    private val htmlRetriever: HtmlRetriever,
    private val htmlParser: HtmlParser,
) : FeedSourceLogoRetriever {
    override suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel): String? {
        val regex = "^.+?[^/:](?=[?/]|\$)".toRegex()
        val baseDomain = rssChannel.link?.let { link ->
            regex.find(link)?.value
        }

        var logoUrl = when {
            rssChannel.image?.url != null -> rssChannel.image?.url
            baseDomain != null -> {
                val html = htmlRetriever.retrieveHtml(baseDomain)
                if (html != null) {
                    htmlParser.getFaviconUrl(html) ?: getFaviconFromGoogle(baseDomain)
                } else {
                    getFaviconFromGoogle(baseDomain)
                }
            }

            else -> null
        }

        if (baseDomain != null && (logoUrl == null || logoUrl.startsWith("/"))) {
            logoUrl = "$baseDomain/favicon.ico"
        }
        return logoUrl
    }

    override suspend fun getFeedSourceLogoUrl(websiteLink: String?): String? {
        val regex = "^.+?[^/:](?=[?/]|\$)".toRegex()
        val baseDomain = websiteLink?.let { link ->
            regex.find(link)?.value
        }

        var logoUrl = when {
            baseDomain != null -> {
                val html = htmlRetriever.retrieveHtml(baseDomain)
                if (html != null) {
                    htmlParser.getFaviconUrl(html) ?: getFaviconFromGoogle(baseDomain)
                } else {
                    getFaviconFromGoogle(baseDomain)
                }
            }

            else -> null
        }

        if (baseDomain != null && (logoUrl == null || logoUrl.startsWith("/"))) {
            logoUrl = "$baseDomain/favicon.ico"
        }
        return logoUrl
    }

    fun getFaviconFromGoogle(websiteLink: String): String {
        val websiteDomain = websiteLink
            .replace("https://", "")
            .replace("http://", "")
            .split("/")[0]
        return "https://www.google.com/s2/favicons?domain=$websiteDomain&sz=64"
    }
}
