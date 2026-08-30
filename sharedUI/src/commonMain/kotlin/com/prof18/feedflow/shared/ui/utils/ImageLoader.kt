package com.prof18.feedflow.shared.ui.utils

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.network.httpHeaders
import coil3.request.crossfade
import coil3.util.DebugLogger

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

fun coilImageLoader(
    context: PlatformContext,
    debug: Boolean,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(imageAcceptHeaderInterceptor)
        }
        .memoryCache {
            MemoryCache.Builder()
                // Set the max size to 25% of the app's available memory.
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .diskCache {
            newDiskCache(context)
        }
        .crossfade(true)
        .apply {
            if (debug) {
                logger(DebugLogger())
            }
        }
        .build()
}

internal expect fun newDiskCache(context: PlatformContext): DiskCache
