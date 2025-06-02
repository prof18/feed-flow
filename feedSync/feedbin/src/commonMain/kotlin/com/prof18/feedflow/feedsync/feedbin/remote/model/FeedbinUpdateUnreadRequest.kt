package com.prof18.feedflow.feedsync.feedbin.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbinUpdateUnreadRequest(
    @SerialName("unread_entries") val unreadEntries: List<Long>
)
