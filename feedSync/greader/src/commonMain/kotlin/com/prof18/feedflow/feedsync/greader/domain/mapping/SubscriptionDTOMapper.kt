package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.feedsync.greader.data.dto.SubscriptionDTO

internal fun SubscriptionDTO.toFeedSource(): ParsedFeedSource =
    ParsedFeedSource(
        id = id,
        url = url,
        title = title,
        category = categories.firstOrNull()?.toFeedSourceCategory(),
        logoUrl = iconUrl.ifBlank { null },
    )
