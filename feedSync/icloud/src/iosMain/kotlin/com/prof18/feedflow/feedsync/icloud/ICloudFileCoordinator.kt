package com.prof18.feedflow.feedsync.icloud

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSURL

interface ICloudFileCoordinator {
    fun read(url: NSURL, accessor: (NSURL) -> Unit): NSError?
    fun write(url: NSURL, accessor: (NSURL) -> Unit): NSError?
}

class FoundationICloudFileCoordinator : ICloudFileCoordinator {
    override fun read(url: NSURL, accessor: (NSURL) -> Unit): NSError? = memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()
        errorPtr.value = null
        NSFileCoordinator(filePresenter = null).coordinateReadingItemAtURL(
            url = url,
            options = 0u,
            error = errorPtr.ptr,
            byAccessor = { coordinatedUrl -> coordinatedUrl?.let(accessor) },
        )
        errorPtr.value
    }

    override fun write(url: NSURL, accessor: (NSURL) -> Unit): NSError? = memScoped {
        val errorPtr: ObjCObjectVar<NSError?> = alloc()
        errorPtr.value = null
        NSFileCoordinator(filePresenter = null).coordinateWritingItemAtURL(
            url = url,
            options = 0u,
            error = errorPtr.ptr,
            byAccessor = { coordinatedUrl -> coordinatedUrl?.let(accessor) },
        )
        errorPtr.value
    }
}
