package com.prof18.feedflow.shared.test.cloudsync

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.Foundation.writeToFile
import platform.posix.getenv

internal actual fun cloudArtifactExchangeConfig(): CloudArtifactExchangeConfig? =
    cloudArtifactConfigFromEnvironment(
        root = environmentValue(CLOUD_ARTIFACT_DIRECTORY_ENV),
        role = environmentValue(CLOUD_ARTIFACT_ROLE_ENV),
        runId = environmentValue(CLOUD_ARTIFACT_RUN_ID_ENV),
        producerPlatforms = environmentValue(CLOUD_ARTIFACT_PRODUCERS_ENV),
        platform = environmentValue(CLOUD_ARTIFACT_PLATFORM_ENV) ?: "ios",
    )

internal actual fun writeCloudArtifact(path: String, bytes: ByteArray) {
    check(!NSFileManager.defaultManager.fileExistsAtPath(path)) {
        "Cloud artifact already exists: $path"
    }
    val directory = path.substringBeforeLast('/', missingDelimiterValue = "")
    if (directory.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    check(bytes.toNSData().writeToFile(path, atomically = true)) { "Unable to write cloud artifact: $path" }
}

internal actual fun readCloudArtifact(path: String): ByteArray =
    requireNotNull(NSData.create(contentsOfFile = path)) { "Missing cloud artifact: $path" }.toByteArray()

private fun environmentValue(name: String): String? = getenv(name)?.toKString()

private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> getBytes(pinned.addressOf(0), length) }
    }
    return result
}
