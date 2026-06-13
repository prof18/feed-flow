package com.prof18.feedflow.core.model

data class FeedItemUrlInfo(
    val id: String,
    val url: String,
    val title: String?,
    val openOnlyOnBrowser: Boolean = false,
    val isBookmarked: Boolean,
    val linkOpeningPreference: LinkOpeningPreference,
    val commentsUrl: String?,
    val imageUrl: String? = null,
)

fun FeedItemUrlInfo.canOpenReaderMode(): Boolean =
    !openOnlyOnBrowser && ReaderModeEligibility.canOpenReaderMode(url)
