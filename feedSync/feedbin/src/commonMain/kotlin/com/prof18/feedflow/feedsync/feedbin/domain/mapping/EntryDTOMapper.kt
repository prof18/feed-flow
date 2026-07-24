package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.core.utils.ContentImageUrlExtractor
import com.prof18.feedflow.feedsync.feedbin.data.dto.EntryDTO
import kotlin.time.Instant

internal class EntryDTOMapper(
    private val htmlParser: HtmlParser,
    private val dateFormatter: DateFormatter,
) {

    fun mapToFeedItem(
        entryDTO: EntryDTO,
        feedSource: FeedSource,
        isRead: Boolean,
        isBookmarked: Boolean,
    ): FeedItem {
        val pubDateMillis = try {
            Instant.parse(entryDTO.published).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }

        // The contains check avoids parsing the HTML of items that can't have a comments link
        val commentsUrl = (entryDTO.content ?: entryDTO.summary)
            ?.takeIf { it.contains("comments", ignoreCase = true) }
            ?.let { htmlParser.parseFeedContent(html = it, baseUrl = entryDTO.url).commentsUrl }

        return FeedItem(
            id = entryDTO.id.toString(),
            url = entryDTO.url,
            title = entryDTO.title,
            subtitle = entryDTO.summary?.let { htmlParser.getTextFromHTML(it) },
            content = entryDTO.content,
            imageUrl = ContentImageUrlExtractor.extractImageUrl(entryDTO.content ?: entryDTO.summary),
            feedSource = feedSource,
            pubDateMillis = pubDateMillis,
            isRead = isRead,
            dateString = pubDateMillis?.let {
                dateFormatter.formatDateForFeed(
                    millis = it / 1000,
                    dateFormat = DateFormat.NORMAL,
                    timeFormat = TimeFormat.HOURS_24,
                )
            },
            commentsUrl = commentsUrl,
            isBookmarked = isBookmarked,
        )
    }
}
