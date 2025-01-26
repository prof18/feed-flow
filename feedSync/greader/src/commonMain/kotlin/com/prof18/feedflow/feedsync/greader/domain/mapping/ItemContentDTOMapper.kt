package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.feedsync.greader.data.dto.ItemContentDTO

internal class ItemContentDTOMapper(
    private val htmlParser: HtmlParser,
    private val dateFormatter: DateFormatter,
) {

    fun mapToFeedItem(
        itemContentDTO: ItemContentDTO,
        feedSource: FeedSource,
    ): FeedItem? {
        val url = itemContentDTO.canonical.firstOrNull()?.href ?: return null
        return FeedItem(
            id = itemContentDTO.hexID,
            url = url,
            title = itemContentDTO.title,
            subtitle = htmlParser.getTextFromHTML(itemContentDTO.summary.content),
            content = null,
            imageUrl = itemContentDTO.image?.href ?: getImageFromContent(itemContentDTO.summary.content),
            feedSource = feedSource,
            pubDateMillis = itemContentDTO.published * 1000,
            isRead = itemContentDTO.read,
            dateString = dateFormatter.formatDateForFeed(itemContentDTO.published),
            commentsUrl = null,
            isBookmarked = itemContentDTO.starred,
        )
    }

    /**
     * Finds the first img tag and gets the src as the featured image.
     *
     * @param content The content in which to search for the tag
     * @return The url, if there is one
     */
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
            // Do nothing, on iOS it could fail for too much recursion
            null
        }
    }

    private companion object {
        const val EMOJI_WEBSITE = "https://s.w.org/images/core/emoji"
    }
}
