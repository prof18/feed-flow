package com.prof18.feedflow.feedsync.feedbin.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RenameTagRequest(
    @SerialName("old_name")
    val oldName: String,
    @SerialName("new_name")
    val newName: String,
)
