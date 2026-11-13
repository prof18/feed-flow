package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FeedSourceLogoRetrieverImpl(
    private val htmlRetriever: HtmlRetriever,
    private val htmlParser: HtmlParser,
) : FeedSourceLogoRetriever {
    private val baseDomainRegex = "^.+?[^/:](?=[?/]|\$)".toRegex()

    // Feeds that share a domain share a favicon: importing 100+ YouTube channels would otherwise
    // download youtube.com's ~1 MB home page once per feed.
    private val lookupsMutex = Mutex()
    private val lookups = mutableMapOf<String, DomainLogoLookup>()

    private val availabilityMutex = Mutex()
    private val availability = mutableMapOf<String, AvailabilityCheck>()

    override suspend fun invalidateLookupCaches() {
        lookupsMutex.withLock { lookups.clear() }
        availabilityMutex.withLock { availability.clear() }
    }

    override suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel, feedUrl: String): String? {
        // A YouTube playlist feed (how shorts are hidden) links only to itself, so the parsed
        // channel has no link, and without a domain there is no favicon to look up.
        val baseDomain = extractBaseDomain(rssChannel.link) ?: extractBaseDomain(feedUrl)
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

    override suspend fun isStoredLogoStillAvailable(logoUrl: String): Boolean {
        val check = availabilityMutex.withLock { availability.getOrPut(logoUrl) { AvailabilityCheck() } }
        return check.mutex.withLock {
            check.value ?: htmlRetriever.isUrlAvailable(logoUrl).also { check.value = it }
        }
    }

    private suspend fun resolveLogoUrlFromWebsite(baseDomain: String?): String? {
        if (baseDomain == null) {
            return null
        }

        val lookup = lookupsMutex.withLock { lookups.getOrPut(baseDomain) { DomainLogoLookup() } }
        return lookup.mutex.withLock {
            if (!lookup.isResolved) {
                lookup.value = fetchLogoUrlFromWebsite(baseDomain)
                lookup.isResolved = true
            }
            lookup.value
        }
    }

    private suspend fun fetchLogoUrlFromWebsite(baseDomain: String): String? {
        val html = htmlRetriever.retrieveHtml(baseDomain)
        return if (html != null) {
            htmlParser.getFaviconUrl(html) ?: getFaviconFromGoogle(baseDomain)
        } else {
            getFaviconFromGoogle(baseDomain)
        }
    }

    private fun normalizeLogoUrl(baseDomain: String?, logoUrl: String?): String? {
        if (logoUrl.isNullOrBlank()) {
            return baseDomain?.let { "$it/favicon.ico" }
        }
        if (baseDomain == null) {
            // Nothing to resolve a relative path against, and storing one would have the refresh
            // replace it with null and write it straight back on the next pass, forever.
            return logoUrl.takeIf { isAbsolute(it) }
        }
        return when {
            isAbsolute(logoUrl) -> logoUrl

            logoUrl.startsWith("//") -> {
                val scheme = if (baseDomain.startsWith("https://", ignoreCase = true)) {
                    "https:"
                } else {
                    "http:"
                }
                "$scheme$logoUrl"
            }

            logoUrl.startsWith("/") -> "$baseDomain$logoUrl"
            else -> "$baseDomain/${logoUrl.removePrefix("./")}"
        }
    }

    private fun isAbsolute(logoUrl: String): Boolean =
        logoUrl.startsWith("http://", ignoreCase = true) ||
            logoUrl.startsWith("https://", ignoreCase = true) ||
            logoUrl.startsWith("data:", ignoreCase = true)

    private fun getFaviconFromGoogle(websiteLink: String): String {
        val websiteDomain = websiteLink
            .replace("https://", "")
            .replace("http://", "")
            .split("/")[0]
        return "https://www.google.com/s2/favicons?domain=$websiteDomain&sz=64"
    }
}

private class DomainLogoLookup {
    val mutex = Mutex()
    var isResolved = false
    var value: String? = null
}

private class AvailabilityCheck {
    val mutex = Mutex()
    var value: Boolean? = null
}
