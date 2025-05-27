package com.prof18.feedflow.shared.ui.utils

import coil3.PlatformContext
import coil3.disk.DiskCache
import okio.FileSystem

internal actual fun newDiskCache(context: PlatformContext): DiskCache {
    return DiskCache.Builder()
        .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
        .maxSizeBytes(size = 512L * 1024 * 1024) // 512MB
        .build()
}
