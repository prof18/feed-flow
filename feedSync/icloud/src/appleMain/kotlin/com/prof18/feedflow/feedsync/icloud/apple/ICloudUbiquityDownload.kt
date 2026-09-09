package com.prof18.feedflow.feedsync.icloud.apple

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLUbiquitousItemDownloadingStatusCurrent
import platform.Foundation.NSURLUbiquitousItemDownloadingStatusKey

internal interface ICloudUbiquityDownload {
    fun status(url: NSURL): ICloudUbiquityDownloadStatus
    fun requestDownload(url: NSURL): String?
}

internal class FoundationICloudUbiquityDownload : ICloudUbiquityDownload {
    override fun status(url: NSURL): ICloudUbiquityDownloadStatus = memScoped {
        url.removeCachedResourceValueForKey(NSURLUbiquitousItemDownloadingStatusKey)
        val value: ObjCObjectVar<Any?> = alloc()
        val error: ObjCObjectVar<NSError?> = alloc()
        value.value = null
        error.value = null
        if (!url.getResourceValue(value.ptr, NSURLUbiquitousItemDownloadingStatusKey, error.ptr)) {
            return@memScoped ICloudUbiquityDownloadStatus.Failure(error.value.toString())
        }
        if (value.value == NSURLUbiquitousItemDownloadingStatusCurrent) {
            ICloudUbiquityDownloadStatus.Current
        } else {
            ICloudUbiquityDownloadStatus.Pending
        }
    }

    override fun requestDownload(url: NSURL): String? = memScoped {
        val error: ObjCObjectVar<NSError?> = alloc()
        error.value = null
        val started = NSFileManager.defaultManager.startDownloadingUbiquitousItemAtURL(url, error.ptr)
        if (started) null else error.value?.toString() ?: "The system rejected the download request"
    }
}

internal sealed class ICloudUbiquityDownloadStatus {
    data object Current : ICloudUbiquityDownloadStatus()
    data object Pending : ICloudUbiquityDownloadStatus()
    data class Failure(val message: String) : ICloudUbiquityDownloadStatus()
}
