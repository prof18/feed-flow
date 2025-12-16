package com.prof18.feedflow.shared.utils

import okhttp3.internal.http2.StreamResetException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

actual fun Throwable.ignoreError(): Boolean =
    this is SocketTimeoutException ||
        this is StreamResetException ||
        this is UnknownHostException ||
        this is SSLHandshakeException ||
        this is SSLException ||
        this is ConnectException
