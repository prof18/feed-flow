package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.feedsync.greader.data.dto.TagDTO

internal fun TagDTO.toFeedSourceCategory(): FeedSourceCategory? {
    label ?: return null
    return FeedSourceCategory(
        id = id,
        title = label,
    )
}
