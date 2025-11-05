package com.prof18.feedflow.feedsync.googledrive

import kotlinx.serialization.Serializable

@Serializable
data class GoogleDriveCredentials(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtMillis: Long,
    val tokenType: String = "Bearer",
)
