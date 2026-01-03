package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.rssparser.model.RssChannel

internal class FeedSourceLogoRetrieverImpl(
    private val htmlRetriever: HtmlRetriever,
    private val htmlParser: HtmlParser,
) : FeedSourceLogoRetriever {
    private val baseDomainRegex = "^.+?[^/:](?=[?/]|\$)".toRegex()

    override suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel): String? {
        val baseDomain = extractBaseDomain(rssChannel.link)
        val logoUrl = rssChannel.image?.url ?: resolveLogoUrlFromWebsite(baseDomain)
        return normalizeLogoUrl(baseDomain, logoUrl)
    }

    override suspend fun getFeedSourceLogoUrl(websiteLink: String?): String? {
        val baseDomain = extractBaseDomain(websiteLink)
        val logoUrl = resolveLogoUrlFromWebsite(baseDomain)
        return normalizeLogoUrl(baseDomain, logoUrl)
    }

    private fun extractBaseDomain(link: String?): String? =
        link?.let { value -> baseDomainRegex.find(value)?.value }

    private suspend fun resolveLogoUrlFromWebsite(baseDomain: String?): String? {
        if (baseDomain == null) {
            return null
        }

        val html = htmlRetriever.retrieveHtml(baseDomain)
        return if (html != null) {
            htmlParser.getFaviconUrl(html) ?: getFaviconFromGoogle(baseDomain)
        } else {
            getFaviconFromGoogle(baseDomain)
        }
    }

    private fun normalizeLogoUrl(baseDomain: String?, logoUrl: String?): String? {
        if (baseDomain != null && (logoUrl == null || logoUrl.startsWith("/"))) {
            return "$baseDomain/favicon.ico"
        }
        return logoUrl
    }

    private fun getFaviconFromGoogle(websiteLink: String): String {
        val websiteDomain = websiteLink
            .replace("https://", "")
            .replace("http://", "")
            .split("/")[0]
        return "https://www.google.com/s2/favicons?domain=$websiteDomain&sz=64"
    }
}
