package com.prof18.feedflow.shared.ui.utils

import coil3.PlatformContext
import coil3.disk.DiskCache
import okio.Path.Companion.toPath

internal actual fun newDiskCache(context: PlatformContext): DiskCache {
    return DiskCache.Builder()
        .directory(context.cacheDir.absolutePath.toPath().resolve("images_cache"))
        .build()
}
