package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import org.jsoup.Jsoup

internal class JvmHtmlParser(
    private val logger: Logger,
) : HtmlParser {
    override fun getTextFromHTML(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            doc.text()
        } catch (e: Throwable) {
            logger.e(e) { "Unable to get text from HTML, skipping" }
            null
        }
    }

    override fun getFaviconUrl(html: String): String? {
        val doc = Jsoup.parse(html)

        val faviconLink = doc.select("link[rel~=(?i)^(shortcut|icon)$][href]").firstOrNull()

        return faviconLink?.attr("href")
    }

    override fun getRssUrl(html: String): String? {
        val doc = Jsoup.parse(html)
        val queries = listOf(
            "link[type='application/rss+xml']",
            "link[type='application/atom+xml']",
            "link[type='application/json']",
            "link[type='application/feed+json']",
        )
        for (query in queries) {
            val rssElement = doc.select(query).firstOrNull()
            val rssUrl = rssElement?.attr("href")
            if (rssUrl != null) {
                return rssUrl
            }
        }
        return null
    }
}
