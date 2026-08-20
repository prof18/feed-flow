package com.prof18.feedflow.core.utils

import platform.Foundation.NSProcessInfo
import platform.Foundation.performExpiringActivityWithReason
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * Uses NSProcessInfo.performExpiringActivity rather than UIApplication.beginBackgroundTask
 * because it is also available in the widget and share extensions, which open the same
 * app-group container.
 *
 * If the system expires the activity before [block] finishes, the guard is released and the
 * work continues unprotected: still better than never asserting background time at all.
 */
actual suspend fun <T> withSuspensionGuard(reason: String, block: suspend () -> T): T {
    val finished = dispatch_semaphore_create(0)
    NSProcessInfo.processInfo.performExpiringActivityWithReason(reason) { expired: Boolean ->
        if (expired) {
            dispatch_semaphore_signal(finished)
        } else {
            dispatch_semaphore_wait(finished, DISPATCH_TIME_FOREVER)
        }
    }
    return try {
        block()
    } finally {
        dispatch_semaphore_signal(finished)
    }
}
