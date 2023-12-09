package com.prof18.feedflow.domain.mappers

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.db.SelectFeeds
import com.prof18.feedflow.domain.DateFormatter

internal fun SelectFeeds.toFeedItem(dateFormatter: DateFormatter) = FeedItem(
    id = url_hash,
    url = url,
    title = title,
    subtitle = subtitle,
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
    ),
    isRead = is_read,
    pubDateMillis = pub_date,
    dateString = if (pub_date != null) {
        @Suppress("RedundantRequireNotNullCall")
        // It's required because the variables come from another module
        dateFormatter.formatDate(
            requireNotNull(pub_date),
        )
    } else {
        null
    },
    commentsUrl = comments_url,
)
