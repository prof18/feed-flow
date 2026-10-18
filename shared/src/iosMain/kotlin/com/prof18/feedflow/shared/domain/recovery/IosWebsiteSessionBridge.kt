package com.prof18.feedflow.shared.domain.recovery

import kotlinx.coroutines.CancellationException
import platform.Foundation.NSData

/** Implemented in Swift: sends one request with the identity saved in a WebKit website data store. */
interface IosWebsiteSessionBridge {
    @Throws(Exception::class, CancellationException::class)
    @Suppress("LongParameterList")
    suspend fun fetch(
        requestId: String,
        url: String,
        profileId: String,
        userAgent: String,
        accept: String,
        etag: String?,
        lastModified: String?,
    ): IosWebsiteSessionResult

    /** Cancels the in-flight request started with [requestId]; a no-op when it already finished. */
    fun cancel(requestId: String)
}

@Suppress("LongParameterList")
class IosWebsiteSessionResult(
    val data: NSData,
    val statusCode: Int,
    val contentType: String?,
    val etag: String?,
    val lastModified: String?,
    val cacheControl: String?,
    val expires: String?,
    val date: String?,
    val retryAfter: String?,
    val challenge: Boolean,
    // null, "no-identity", "redirect", "too-large", "transport", "timeout"
    val failure: String?,
)
