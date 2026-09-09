package com.prof18.ikloud

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSURL

internal interface ICloudFileCoordinator {
    fun read(url: NSURL, accessor: (NSURL) -> Unit): NSError?
    fun write(url: NSURL, accessor: (NSURL) -> Unit): NSError?
}

internal class FoundationICloudFileCoordinator : ICloudFileCoordinator {
    override fun read(url: NSURL, accessor: (NSURL) -> Unit): NSError? = memScoped {
        val error: ObjCObjectVar<NSError?> = alloc()
        error.value = null
        NSFileCoordinator(filePresenter = null).coordinateReadingItemAtURL(
            url,
            options = 0u,
            error = error.ptr,
            byAccessor = { coordinatedUrl -> coordinatedUrl?.let(accessor) },
        )
        error.value
    }

    override fun write(url: NSURL, accessor: (NSURL) -> Unit): NSError? = memScoped {
        val error: ObjCObjectVar<NSError?> = alloc()
        error.value = null
        NSFileCoordinator(filePresenter = null).coordinateWritingItemAtURL(
            url,
            options = 0u,
            error = error.ptr,
            byAccessor = { coordinatedUrl -> coordinatedUrl?.let(accessor) },
        )
        error.value
    }
}
