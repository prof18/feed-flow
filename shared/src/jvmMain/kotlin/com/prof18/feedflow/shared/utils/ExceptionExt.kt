package com.prof18.feedflow.shared.utils

import com.dropbox.core.NetworkIOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun Throwable.isTemporaryNetworkError(): Boolean = when (this) {
    is NetworkIOException -> true
    is SocketException -> true
    is SocketTimeoutException -> true
    is UnknownHostException -> true
    else -> cause?.isTemporaryNetworkError() ?: false
}
