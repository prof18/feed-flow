package com.prof18.feedflow.shared.test.cloudsync

import java.io.File

internal actual fun cloudArtifactExchangeConfig(): CloudArtifactExchangeConfig? =
    cloudArtifactConfigFromEnvironment(
        root = System.getenv(CLOUD_ARTIFACT_DIRECTORY_ENV),
        role = System.getenv(CLOUD_ARTIFACT_ROLE_ENV),
        runId = System.getenv(CLOUD_ARTIFACT_RUN_ID_ENV),
        producerPlatforms = System.getenv(CLOUD_ARTIFACT_PRODUCERS_ENV),
        platform = System.getenv(CLOUD_ARTIFACT_PLATFORM_ENV) ?: "jvm",
    )

internal actual fun writeCloudArtifact(path: String, bytes: ByteArray) {
    val file = File(path)
    file.parentFile?.mkdirs()
    check(!file.exists()) { "Cloud artifact already exists: $path" }
    file.writeBytes(bytes)
}

internal actual fun readCloudArtifact(path: String): ByteArray = File(path).readBytes()
