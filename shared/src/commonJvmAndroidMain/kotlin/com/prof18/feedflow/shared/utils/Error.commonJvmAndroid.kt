package com.prof18.feedflow.shared.utils

import okhttp3.internal.http2.StreamResetException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

actual fun Throwable.ignoreError(): Boolean =
    this is SocketTimeoutException || this is StreamResetException || this is UnknownHostException
