package com.prof18.feedflow.shared.domain.mappers

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.shared.utils.sanitizeUrl

internal fun Search.toFeedItem(
    dateFormatter: DateFormatter,
    removeTitleFromDesc: Boolean,
    hideDate: Boolean,
    dateFormat: DateFormat,
) = FeedItem(
    id = url_hash,
    url = sanitizeUrl(url),
    title = title,
    subtitle = subtitle?.let { desc ->
        val title = title
        if (removeTitleFromDesc && title != null) {
            desc.replace(title, "").replace("  ", "").trim()
        } else {
            desc
        }
    },
    content = null,
    imageUrl = image_url,
    feedSource = FeedSource(
        id = feed_source_id,
        url = feed_source_url,
        title = feed_source_title,
        category = if (feed_source_category_title != null && feed_source_category_id != null) {
            @Suppress("RedundantRequireNotNullCall")
            // It's required because the variables come from another module
            FeedSourceCategory(
                id = requireNotNull(feed_source_category_id),
                title = requireNotNull(feed_source_category_title),
            )
        } else {
            null
        },
        lastSyncTimestamp = feed_source_last_sync_timestamp,
        logoUrl = feed_source_logo_url,
        websiteUrl = null,
        linkOpeningPreference = feed_source_link_opening_preference ?: LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        fetchFailed = feed_source_fetch_failed,
    ),
    pubDateMillis = pub_date,
    dateString = if (pub_date != null && !hideDate) {
        @Suppress("RedundantRequireNotNullCall")
        // It's required because the variables come from another module
        dateFormatter.formatDateForFeed(
            requireNotNull(pub_date),
            dateFormat = dateFormat,
        )
    } else {
        null
    },
    isRead = is_read,
    commentsUrl = comments_url?.let { sanitizeUrl(it) },
    isBookmarked = is_bookmarked,
)
