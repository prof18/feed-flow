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
        val url = itemContentDTO.canonical?.firstOrNull()?.href ?: return null
        val content = itemContentDTO.content?.content ?: itemContentDTO.summary?.content
        val parsedContent = content?.let { htmlParser.parseFeedContent(html = it, baseUrl = url) }
        return FeedItem(
            id = itemContentDTO.hexID,
            url = url,
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
