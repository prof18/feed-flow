package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UnreadEntriesRequest(
    @SerialName("unread_entries")
    val unreadEntries: List<Long>,
)
