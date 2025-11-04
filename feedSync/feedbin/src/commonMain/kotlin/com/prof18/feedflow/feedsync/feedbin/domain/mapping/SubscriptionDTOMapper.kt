package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.feedsync.feedbin.data.dto.SubscriptionDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.TaggingDTO

internal fun SubscriptionDTO.toFeedSource(taggings: List<TaggingDTO>): ParsedFeedSource {
    val tagging = taggings.firstOrNull { it.feedId == feedId }
    return ParsedFeedSource(
        id = "feed/$feedId",
        url = feedUrl,
        title = title,
        category = tagging?.toFeedSourceCategory(),
        logoUrl = null,
    )
}
