package com.prof18.feedflow.feedsync.googledrive

import kotlinx.serialization.Serializable

@Serializable
data class GoogleDriveCredentials(
    val accessToken: String,
    // TODO: is it only used on desktop?? to verify that
    val refreshToken: String?,
    val expiresAtMillis: Long,
    val tokenType: String = "Bearer",
)
