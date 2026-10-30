package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.fleeksoft.ksoup.Ksoup
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent

internal class KsoupHtmlParser(
    private val logger: Logger,
) : HtmlParser {
    override fun getTextFromHTML(html: String): String? {
        return try {
            val doc = Ksoup.parse(html, "")
            doc.text()
        } catch (e: Throwable) {
            logger.d(e) { "Unable to get text from HTML, skipping" }
            null
        }
    }

    override fun getFaviconUrl(html: String): String? {
        val doc = Ksoup.parse(html, "")

        val faviconLink = doc.select("link[rel~=(?i)^(shortcut|icon)$][href]").firstOrNull()

        return faviconLink?.attr("href")
    }

    override fun getRssUrl(html: String): String? {
        val doc = Ksoup.parse(html, "")
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

    override fun getCanonicalUrl(html: String): String? {
        return Ksoup.parse(html, "")
            .select("link[rel=canonical][href]")
            .firstOrNull()
            ?.attr("href")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    override fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent {
        return try {
            val doc = Ksoup.parse(html, baseUrl.orEmpty())
            val commentsUrl = doc.select("a")
                .firstOrNull { it.text().trim().equals("comments", ignoreCase = true) }
                ?.absUrl("href")
                ?.takeIf { it.startsWith("http") }
            ParsedFeedContent(
                text = doc.text(),
                commentsUrl = commentsUrl,
            )
        } catch (e: Throwable) {
            logger.d(e) { "Unable to parse feed content, skipping" }
            ParsedFeedContent(text = null, commentsUrl = null)
        }
    }
}
