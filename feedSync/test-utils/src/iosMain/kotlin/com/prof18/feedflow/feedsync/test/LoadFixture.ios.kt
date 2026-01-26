package com.prof18.feedflow.feedsync.test

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun loadFixture(filename: String): String {
    val resourcesRoot = getenv("TEST_RESOURCES_ROOT")?.toKString()
    requireNotNull(resourcesRoot) {
        "TEST_RESOURCES_ROOT environment variable not set. Fixture file not found: fixtures/$filename"
    }

    val path = "$resourcesRoot/fixtures/$filename"

    require(NSFileManager.defaultManager.fileExistsAtPath(path)) {
        "Fixture file not found at path: $path"
    }

    return checkNotNull(NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)) {
        "Failed to read fixture file: $path"
    }
}
