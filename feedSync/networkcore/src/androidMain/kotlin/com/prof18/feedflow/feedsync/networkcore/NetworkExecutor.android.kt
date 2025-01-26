package com.prof18.feedflow.feedsync.networkcore

import java.net.UnknownHostException

actual fun Throwable.isMissingConnectionError(): Boolean =
    this is UnknownHostException
