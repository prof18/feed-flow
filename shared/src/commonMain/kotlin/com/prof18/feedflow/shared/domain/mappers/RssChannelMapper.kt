package com.prof18.feedflow.shared.domain.mappers

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.rssparser.model.RssChannel
import io.ktor.http.parseUrl

internal class RssChannelMapper(
    private val dateFormatter: DateFormatter,
    private val htmlParser: HtmlParser,
    private val logger: Logger,
) {

    fun getFeedItems(rssChannel: RssChannel, feedSource: FeedSource, dateFormat: DateFormat): List<FeedItem> =
        rssChannel.items.mapNotNull { rssItem ->
            val title = rssItem.title?.filterSpecialCharacters()
            val url = rssItem.link ?: run {
                val parsedUrl = parseUrl(rssItem.guid.orEmpty())
                return@run if (parsedUrl != null) {
                    rssItem.guid
                } else {
                    null
                }
            }
            val pubDate = rssItem.pubDate

            val dateMillis = if (pubDate != null) {
                dateFormatter.getDateMillisFromString(pubDate)
            } else {
                null
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
                FeedItem(
                    id = url.hashCode().toString(),
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
                    dateString = if (dateMillis != null) {
                        dateFormatter.formatDateForFeed(dateMillis, dateFormat)
                    } else {
                        null
                    },
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
        .replace("&#8217;", "’")
