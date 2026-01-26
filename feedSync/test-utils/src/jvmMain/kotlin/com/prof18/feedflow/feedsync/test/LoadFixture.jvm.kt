package com.prof18.feedflow.feedsync.test

import java.io.File

actual fun loadFixture(filename: String): String {
    // Try environment variable first (for consistency with iOS)
    val resourcesRoot = System.getenv("TEST_RESOURCES_ROOT")
    if (resourcesRoot != null) {
        val file = File("$resourcesRoot/fixtures/$filename")
        if (file.exists()) {
            return file.readText()
        }
    }

    // Fallback to classloader
    val classLoader = checkNotNull(Thread.currentThread().contextClassLoader) {
        "Cannot get context class loader"
    }

    val resource = classLoader.getResource("fixtures/$filename")
        ?: throw IllegalArgumentException("Fixture file not found: fixtures/$filename")

    return resource.readText()
}
