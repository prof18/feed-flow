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
    val resourceRoot = resolveResourcesRoot()
    val path = "$resourceRoot/fixtures/$filename"

    require(NSFileManager.defaultManager.fileExistsAtPath(path)) {
        "Fixture file not found at path: $path"
    }

    return checkNotNull(NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)) {
        "Failed to read fixture file: $path"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun resolveResourcesRoot(): String {
    val envRoot = getenv("TEST_RESOURCES_ROOT")?.toKString()
        ?: getenv("SIMCTL_CHILD_TEST_RESOURCES_ROOT")?.toKString()
    if (envRoot != null && NSFileManager.defaultManager.fileExistsAtPath(envRoot)) {
        return envRoot
    }

    val fileManager = NSFileManager.defaultManager
    val candidates = mutableListOf<String>()
    var currentPath = fileManager.currentDirectoryPath
    repeat(8) {
        val candidate = "$currentPath/feedSync/test-utils/src/commonMain/resources"
        candidates.add(candidate)
        if (fileManager.fileExistsAtPath(candidate)) {
            return candidate
        }
        val parent = currentPath.substringBeforeLast("/", missingDelimiterValue = currentPath)
        if (parent == currentPath) return@repeat
        currentPath = parent
    }

    val tried = listOfNotNull(envRoot) + candidates
    error(
        "TEST_RESOURCES_ROOT environment variable not set or invalid. " +
            "Tried: ${tried.joinToString()}",
    )
}
