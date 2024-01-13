package com.prof18.feedflow.shared.utils

internal inline fun <T> executeWithRetry(
    maxRetries: Int = 2,
    call: () -> T,
): T {
    repeat(maxRetries - 1) {
        try {
            return call()
        } catch (_: Throwable) {
        }
    }
    return call()
}
