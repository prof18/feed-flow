package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.TimeFormat
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

        return FeedItem(
            id = entryDTO.id.toString(),
            url = entryDTO.url,
            title = entryDTO.title,
            subtitle = entryDTO.summary?.let { htmlParser.getTextFromHTML(it) },
            content = entryDTO.content,
            imageUrl = getImageFromContent(entryDTO.content ?: entryDTO.summary),
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
            commentsUrl = null,
            isBookmarked = isBookmarked,
        )
    }

    private fun getImageFromContent(content: String?): String? {
        return try {
            val urlRegex = Regex(pattern = "https?:\\/\\/[^\\s<>\"]+\\.(?:jpg|jpeg|png|gif|bmp|webp)")
            content
                ?.let { urlRegex.find(it) }
                ?.let {
                    it.value
                        .trim()
                        .takeIf { url -> !url.contains(EMOJI_WEBSITE) }
                }
        } catch (_: Throwable) {
            null
        }
    }

    private companion object {
        const val EMOJI_WEBSITE = "https://s.w.org/images/core/emoji"
    }
}
