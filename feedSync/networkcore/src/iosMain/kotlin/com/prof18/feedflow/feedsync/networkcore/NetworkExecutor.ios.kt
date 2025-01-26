package com.prof18.feedflow.feedsync.networkcore

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorNotConnectedToInternet

actual fun Throwable.isMissingConnectionError(): Boolean =
    (this as? DarwinHttpRequestException)?.origin?.code == NSURLErrorNotConnectedToInternet
