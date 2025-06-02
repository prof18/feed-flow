package com.prof18.feedflow.feedsync.feedbin.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbinSubscriptionDTO(
    @SerialName("id") val id: Long,
    @SerialName("created_at") val createdAt: String, // ISO 8601 date string
    @SerialName("feed_id") val feedId: Long,
    @SerialName("title") val title: String,
    @SerialName("feed_url") val feedUrl: String,
    @SerialName("site_url") val siteUrl: String
)
