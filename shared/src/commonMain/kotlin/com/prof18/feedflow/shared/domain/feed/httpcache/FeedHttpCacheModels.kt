package com.prof18.feedflow.shared.domain.feed.httpcache

data class FeedHttpValidators(
    val etag: String?,
    val lastModified: String?,
)

data class FeedHttpResponseInfo(
    val statusCode: Int,
    val etag: String?,
    val lastModified: String?,
    val cacheControl: String?,
    val expires: String?,
    val date: String?,
    val retryAfter: String?,
)
