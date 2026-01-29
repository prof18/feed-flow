package com.prof18.feedflow.core.model

data class FeedSource(
    val id: String,
    val url: String,
    val title: String,
    val category: FeedSourceCategory?,
    val lastSyncTimestamp: Long?,
    val logoUrl: String?,
    val websiteUrl: String?,
    val fetchFailed: Boolean,
    val linkOpeningPreference: LinkOpeningPreference,
    val isHiddenFromTimeline: Boolean,
    val isPinned: Boolean,
    val isNotificationEnabled: Boolean,
) {
    fun websiteUrlFallback(): String? =
        websiteUrl ?: url.toWebsiteBaseUrl()
}

private fun String.toWebsiteBaseUrl(): String? {
    val trimmedUrl = trim()
    if (trimmedUrl.isEmpty()) {
        return null
    }
    val normalizedUrl = if (trimmedUrl.startsWith("http://", ignoreCase = true) ||
        trimmedUrl.startsWith("https://", ignoreCase = true)
    ) {
        trimmedUrl
    } else {
        "https://$trimmedUrl"
    }
    return WEBSITE_URL_REGEX.find(normalizedUrl)?.value
}

private val WEBSITE_URL_REGEX = Regex("^https?://[^/]+", RegexOption.IGNORE_CASE)
