package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StarredEntriesRequest(
    @SerialName("starred_entries")
    val starredEntries: List<Long>,
)
