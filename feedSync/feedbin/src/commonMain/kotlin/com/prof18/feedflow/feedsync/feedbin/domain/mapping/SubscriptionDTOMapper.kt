package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.feedsync.feedbin.data.dto.IconDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.SubscriptionDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.TaggingDTO
import com.prof18.feedflow.feedsync.feedbin.domain.feedbinFeedSourceId
import io.ktor.http.Url

internal fun SubscriptionDTO.toFeedSource(
    taggings: List<TaggingDTO>,
    icons: List<IconDTO>,
): ParsedFeedSource {
    val tagging = taggings.firstOrNull { it.feedId == feedId }
    val logoUrl = getHost()
        ?.let { host -> icons.firstOrNull { it.host == host } }
        ?.url
    return ParsedFeedSource(
        id = feedbinFeedSourceId(subscriptionId = id, feedId = feedId),
        url = feedUrl,
        title = title,
        category = tagging?.toFeedSourceCategory(),
        logoUrl = logoUrl,
    )
}

private fun SubscriptionDTO.getHost(): String? = runCatching {
    Url(feedUrl).host
}.getOrNull()
