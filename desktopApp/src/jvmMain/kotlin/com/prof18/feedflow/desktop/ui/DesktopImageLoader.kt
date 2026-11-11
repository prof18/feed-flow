package com.prof18.feedflow.desktop.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.network.httpHeaders
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

private val imageAcceptHeaderInterceptor = Interceptor { chain ->
    val request = chain.request
    if (request.httpHeaders["Accept"] != null) {
        chain.proceed()
    } else {
        val headers = request.httpHeaders.newBuilder()
            .apply { this["Accept"] = "image/*" }
            .build()
        chain.withRequest(
            request.newBuilder()
                .httpHeaders(headers)
                .build(),
        ).proceed()
    }
}

private const val IMAGE_CACHE_FOLDER = "image_cache"

/**
 * The shared temporary folder, except for a debug app started by the desktop E2E suite: its cache lives
 * in the run's own data folder, so a flow never reads or clears the cache of a development app.
 */
internal fun imageCacheDirectory(
    appEnvironment: AppEnvironment,
    read: (String) -> String? = System::getProperty,
): Path {
    val e2eDataDir = DesktopE2eProperties.debugValue(appEnvironment, DesktopE2eProperties.DATA_DIR, read)
    return (e2eDataDir?.toPath() ?: FileSystem.SYSTEM_TEMPORARY_DIRECTORY) / IMAGE_CACHE_FOLDER
}

fun coilImageLoader(
    context: PlatformContext,
    debug: Boolean,
    diskCacheDirectory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / IMAGE_CACHE_FOLDER,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(imageAcceptHeaderInterceptor)
            add(SvgDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder()
                // Set the max size to 25% of the app's available memory.
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(diskCacheDirectory)
                .maxSizeBytes(size = 512L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .apply {
            if (debug) {
                logger(DebugLogger())
            }
        }
        .build()
}
