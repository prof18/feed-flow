package com.prof18.feedflow.feedsync.feedbin.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbinUpdateStarredRequest(
    @SerialName("starred_entries") val starredEntries: List<Long>
)
