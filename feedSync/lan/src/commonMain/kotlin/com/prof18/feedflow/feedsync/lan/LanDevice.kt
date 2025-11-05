package com.prof18.feedflow.feedsync.lan

import kotlinx.serialization.Serializable

@Serializable
data class LanDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val lastSeen: Long,
)

@Serializable
data class LanSyncMetadata(
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long,
    val databaseVersion: Int = 1,
)

@Serializable
data class LanSyncResponse(
    val success: Boolean,
    val message: String? = null,
    val metadata: LanSyncMetadata? = null,
)
