package com.prof18.feedflow.core.model

data class FeedItemUrlInfo(
    val id: String,
    val url: String,
    val title: String?,
    val openOnlyOnBrowser: Boolean = false,
    val isBookmarked: Boolean,
    val linkOpeningPreference: LinkOpeningPreference = LinkOpeningPreference.DEFAULT,
)

fun FeedItemUrlInfo.shouldOpenInBrowser(): Boolean =
    openOnlyOnBrowser || url.contains("type=pdf") || url.contains("youtube.com") ||
    linkOpeningPreference == LinkOpeningPreference.PREFERRED_BROWSER
