package com.prof18.feedflow.feedsync.feedbin.domain

internal data class FeedbinIds(
    val subscriptionId: Long?,
    val feedId: Long,
)

internal fun feedbinFeedSourceId(subscriptionId: Long, feedId: Long): String =
    "feedbin/$subscriptionId/$feedId"

internal fun parseFeedbinIds(feedSourceId: String): FeedbinIds? {
    val feedbinPrefix = "feedbin/"
    if (feedSourceId.startsWith(feedbinPrefix)) {
        val parts = feedSourceId.removePrefix(feedbinPrefix).split("/")
        if (parts.size != 2) {
            return null
        }
        val subscriptionId = parts[0].toLongOrNull() ?: return null
        val feedId = parts[1].toLongOrNull() ?: return null
        return FeedbinIds(subscriptionId = subscriptionId, feedId = feedId)
    }
    return null
}
