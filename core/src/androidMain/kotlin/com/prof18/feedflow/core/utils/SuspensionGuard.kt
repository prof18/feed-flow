package com.prof18.feedflow.core.utils

actual suspend fun <T> withSuspensionGuard(reason: String, block: suspend () -> T): T = block()
