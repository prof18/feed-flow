package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.feedsync.feedbin.data.dto.TaggingDTO

internal fun TaggingDTO.toFeedSourceCategory(): FeedSourceCategory =
    FeedSourceCategory(
        id = name,
        title = name,
    )
