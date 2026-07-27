package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.core.utils.ContentImageUrlExtractor
import com.prof18.feedflow.feedsync.greader.data.dto.ItemContentDTO

internal class ItemContentDTOMapper(
    private val htmlParser: HtmlParser,
    private val dateFormatter: DateFormatter,
) {

    fun mapToFeedItem(
        itemContentDTO: ItemContentDTO,
        feedSource: FeedSource,
    ): FeedItem? {
        val url = itemContentDTO.canonical
            ?.firstNotNullOfOrNull { canonical -> canonical.href?.takeIf { it.isNotBlank() } }
        val content = itemContentDTO.content?.content?.takeIf { it.isNotBlank() } ?: itemContentDTO.summary?.content
        if (url == null && content.isNullOrBlank()) {
            // No URL to open and no content to show — the item is unusable.
            return null
        }
        val parsedContent = content?.let { htmlParser.parseFeedContent(html = it, baseUrl = url.orEmpty()) }
        return FeedItem(
            id = itemContentDTO.hexID,
            // feed_item.url stays NOT NULL; URL-less items use an empty string and open straight
            // into the reader with their feed-provided content.
            url = url.orEmpty(),
            title = itemContentDTO.title,
            subtitle = parsedContent?.text,
            content = content,
            imageUrl = itemContentDTO.image?.href ?: ContentImageUrlExtractor.extractImageUrl(content),
            feedSource = feedSource,
            pubDateMillis = itemContentDTO.published * 1000,
            isRead = itemContentDTO.read,
            dateString = dateFormatter.formatDateForFeed(
                millis = itemContentDTO.published,
                // The object here is just used to save on db, and the display date is not saved
                dateFormat = DateFormat.NORMAL,
                timeFormat = TimeFormat.HOURS_24,
            ),
            commentsUrl = parsedContent?.commentsUrl,
            isBookmarked = itemContentDTO.starred,
        )
    }
}
