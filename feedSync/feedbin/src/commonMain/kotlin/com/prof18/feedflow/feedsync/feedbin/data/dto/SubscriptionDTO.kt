package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscriptionDTO(
    val id: Long,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("feed_id")
    val feedId: Long,
    val title: String,
    @SerialName("feed_url")
    val feedUrl: String,
    @SerialName("site_url")
    val siteUrl: String,
)
