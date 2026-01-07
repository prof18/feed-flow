package com.prof18.feedflow.shared.domain.mappers

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.rssparser.model.RssChannel
import io.ktor.http.parseUrl

internal class RssChannelMapper(
    private val dateFormatter: DateFormatter,
    private val htmlParser: HtmlParser,
    private val logger: Logger,
) {

    fun getFeedItems(rssChannel: RssChannel, feedSource: FeedSource): List<FeedItem> =
        rssChannel.items.mapNotNull { rssItem ->
            val title = rssItem.title
                ?.let { htmlParser.getTextFromHTML(it) }
                ?.filterSpecialCharacters()
            val url = rssItem.link ?: run {
                val parsedUrl = parseUrl(rssItem.guid.orEmpty())
                if (parsedUrl != null) {
                    return@run rssItem.guid
                }

                // Check for URL in enclosures (e.g., podcasts, media items)
                rssItem.rawEnclosure?.url
            }
            val parsedDateMillis: Long = rssItem.pubDate?.let {
                dateFormatter.getDateMillisFromString(it)
            } ?: dateFormatter.currentTimeMillis()

            // Normalize future dates to the current time to avoid articles with incorrect
            // future dates always appearing at the top of the feed list
            val currentTimeMillis = dateFormatter.currentTimeMillis()
            val dateMillis = if (parsedDateMillis > currentTimeMillis) {
                currentTimeMillis
            } else {
                parsedDateMillis
            }

            val imageUrl = when {
                rssItem.youtubeItemData?.thumbnailUrl != null -> {
                    rssItem.youtubeItemData?.thumbnailUrl
                }
                rssItem.image?.contains("http:") == true -> {
                    rssItem.image?.replace("http:", "https:")
                }
                else -> {
                    rssItem.image
                }
            }

            if (url == null) {
                logger.i { "Skipping item of ${rssChannel.link}, article link is null" }
                null
            } else {
                val id = if (rssItem.guid != null) {
                    rssItem.guid.hashCode().toString()
                } else {
                    url.hashCode().toString()
                }

                FeedItem(
                    id = id,
                    url = url,
                    title = title,
                    subtitle = rssItem.description?.let { description ->
                        val partialDesc = if (description.isNotEmpty()) {
                            description.take(n = 500)
                        } else {
                            description
                        }
                        htmlParser.getTextFromHTML(partialDesc)
                    },
                    content = null,
                    imageUrl = imageUrl,
                    feedSource = feedSource,
                    pubDateMillis = dateMillis,
                    dateString = null, // This is not saved on database, so we can skip it for this mapper
                    isRead = false,
                    commentsUrl = rssItem.commentsUrl,
                    isBookmarked = false,
                )
            }
        }
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
