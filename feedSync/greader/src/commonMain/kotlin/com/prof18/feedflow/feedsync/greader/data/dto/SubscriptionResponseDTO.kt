package com.prof18.feedflow.feedsync.greader.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SubscriptionListDTO(
    val subscriptions: List<SubscriptionDTO>,
)

@Serializable
internal data class SubscriptionDTO(
    val id: String,
    val title: String,
    val categories: List<TagDTO>,
    val url: String,
    val htmlUrl: String,
    val iconUrl: String,
)

@Serializable
internal data class TagDTO(
    val id: String,
    val label: String?,
)
