package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.feedsync.greader.data.dto.SubscriptionDTO
import io.ktor.http.Url
import io.ktor.http.hostWithPortIfSpecified

internal fun SubscriptionDTO.toFeedSource(): ParsedFeedSource =
    ParsedFeedSource(
        id = id,
        url = resolveFeedUrl(),
        title = title,
        category = categories.firstOrNull()?.toFeedSourceCategory(),
        logoUrl = iconUrl?.ifBlank { null },
        websiteUrl = getHost(),
    )

private fun SubscriptionDTO.resolveFeedUrl(): String {
    val subscriptionUrl = url?.trim().orEmpty()
    if (subscriptionUrl.isNotBlank()) {
        return subscriptionUrl
    }

    return if (id.startsWith("feed/")) {
        id.removePrefix("feed/")
    } else {
        id
    }
}

private fun SubscriptionDTO.getHost(): String? = runCatching {
    val parsedUrl = htmlUrl?.let { Url(it) }
    val host = parsedUrl?.hostWithPortIfSpecified
    "https://$host"
}.getOrNull()
