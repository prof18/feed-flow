package com.prof18.feedflow.shared.domain.mappers

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import io.ktor.http.parseUrl

internal class RssChannelMapper(
    private val dateFormatter: DateFormatter,
    private val htmlParser: HtmlParser,
    private val logger: Logger,
) {

    fun getFeedItems(rssChannel: RssChannel, feedSource: FeedSource): List<FeedItem> =
        rssChannel.items.mapNotNull { rssItem ->
            val url = rssItem.resolveUrl()
            val content = rssItem.content?.takeIf { it.isNotBlank() } ?: rssItem.description
            if (url == null && content.isNullOrBlank()) {
                // Without a URL there is nothing to open and without content there is nothing
                // to show, so the item is unusable — skip it.
                logger.i { "Skipping item of ${rssChannel.link}, article link and content are null" }
                return@mapNotNull null
            }

            val title = rssItem.title
                ?.let { htmlParser.getTextFromHTML(it) }
                ?.filterSpecialCharacters()

            FeedItem(
                id = rssItem.resolveId(url, content, feedSource.id),
                // feed_item.url stays NOT NULL; URL-less items use an empty string and are
                // opened straight into the reader with their feed-provided content.
                url = url.orEmpty(),
                title = title,
                subtitle = rssItem.description?.let { description ->
                    val partialDesc = if (description.isNotEmpty()) {
                        description.take(n = 500)
                    } else {
                        description
                    }
                    htmlParser.getTextFromHTML(partialDesc)
                },
                content = content,
                imageUrl = rssItem.resolveImageUrl(),
                feedSource = feedSource,
                pubDateMillis = resolveDateMillis(rssItem),
                dateString = null, // This is not saved on database, so we can skip it for this mapper
                isRead = false,
                commentsUrl = rssItem.commentsUrl,
                isBookmarked = false,
            )
        }

    private fun resolveDateMillis(rssItem: RssItem): Long {
        val parsedDateMillis: Long = rssItem.pubDate?.let {
            dateFormatter.getDateMillisFromString(it)
        } ?: dateFormatter.currentTimeMillis()

        // Normalize future dates to the current time to avoid articles with incorrect
        // future dates always appearing at the top of the feed list
        val currentTimeMillis = dateFormatter.currentTimeMillis()
        return if (parsedDateMillis > currentTimeMillis) currentTimeMillis else parsedDateMillis
    }
}

private fun RssItem.resolveUrl(): String? = link?.takeIf { it.isNotBlank() } ?: run {
    if (parseUrl(guid.orEmpty()) != null) {
        return@run guid?.takeIf { it.isNotBlank() }
    }
    // Check for URL in enclosures (e.g., podcasts, media items)
    rawEnclosure?.url?.takeIf { it.isNotBlank() }
}

private fun RssItem.resolveId(url: String?, content: String?, feedSourceId: String): String = when {
    url == null -> feedContentId(content, feedSourceId)
    !guid.isNullOrBlank() -> guid.hashCode().toString()
    else -> url.hashCode().toString()
}

/**
 * URL-less items have no natural key, so the id stays scoped to the feed source: guids in
 * content-only feeds are often sequential ("1", "2", …) and would collide across feeds.
 * The guid is still preferred over the content fingerprint because it survives the publisher
 * editing the title, date or body — a fingerprint would resurface the item as a new one.
 */
private fun RssItem.feedContentId(content: String?, feedSourceId: String): String {
    val identifier = if (!guid.isNullOrBlank()) {
        listOf(guid.orEmpty())
    } else {
        listOf(title.orEmpty(), pubDate.orEmpty(), content.hashCode().toString())
    }
    return (listOf("feed-content", feedSourceId) + identifier).joinToString(separator = ":")
}

private fun RssItem.resolveImageUrl(): String? = when {
    youtubeItemData?.thumbnailUrl != null -> youtubeItemData?.thumbnailUrl
    image?.contains("http:") == true -> image?.replace("http:", "https:")
    else -> image
}

private fun String.filterSpecialCharacters(): String =
    this.replace("â€™", "’")
        .replace("â€™", "’")
        .replace("&acirc;&#128;&#153;", "’")
        .replace("â€œ", "“")
        .replace("â&#128;&#156;", "“")
        .replace("&acirc;&#128;&#156;", "“")
        .replace("â€", "”")
        .replace("â&#128;&#157;", "”")
        .replace("&acirc;&#128;&#157;", "”")
        .replace("â€”", "—")
        .replace("&acirc;&#128;&#148;", "—")
        .replace("Â", "")
        .replace("&Acirc;&nbsp;", "")
        .replace(" &amp;hellip;", "…")
        .replace("&amp;hellip;", "…")
        .replace("&hellip;", "…")
        .replace("&#8230;", "…")
        .replace("&#8220;", "“")
        .replace("&#8221;", "”")
        .replace("&#8217;", "’")
