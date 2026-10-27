package com.prof18.feedflow.database

actual fun Throwable.isDatabaseFailure(): Boolean = false
