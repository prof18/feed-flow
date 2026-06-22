package com.prof18.feedflow.core.model

data class FeedSourceCacheInfo(
    val feedSourceId: String,
    val etag: String?,
    val lastModified: String?,
    val nextFetchTimestamp: Long?,
    val backoffTimestamp: Long?,
)
