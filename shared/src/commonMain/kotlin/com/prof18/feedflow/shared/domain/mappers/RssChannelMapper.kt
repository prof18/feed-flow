package com.prof18.feedflow.shared.domain.mappers

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.HtmlParser
import com.prof18.rssparser.model.RssChannel

internal class RssChannelMapper(
    private val dateFormatter: DateFormatter,
    private val htmlParser: HtmlParser,
    private val logger: Logger,
) {

    @Suppress("MagicNumber")
    fun getFeedItems(rssChannel: RssChannel, feedSource: FeedSource): List<FeedItem> =
        rssChannel.items.mapNotNull { rssItem ->
            val title = rssItem.title
            val url = rssItem.link
            val pubDate = rssItem.pubDate

            val dateMillis = if (pubDate != null) {
                dateFormatter.getDateMillisFromString(pubDate)
            } else {
                null
            }

            val imageUrl = if (rssItem.image?.contains("http:") == true) {
                rssItem.image?.replace("http:", "https:")
            } else {
                rssItem.image
            }

            if (url == null) {
                logger.d { "Skipping: ${rssItem.title}" }
                null
            } else {
                FeedItem(
                    id = url.hashCode().toString(),
                    url = url,
                    title = title,
                    subtitle = rssItem.description?.let { description ->
                        val partialDesc = if (description.isNotEmpty()) {
                            description.take(500)
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
                        dateFormatter.formatDateForFeed(dateMillis)
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
