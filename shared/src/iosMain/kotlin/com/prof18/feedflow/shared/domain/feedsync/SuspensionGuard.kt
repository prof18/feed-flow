package com.prof18.feedflow.shared.domain.feedsync

import platform.Foundation.NSProcessInfo
import platform.Foundation.performExpiringActivityWithReason
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/**
 * Defers app suspension while [block] runs, so SQLite/file locks on the app-group
 * container are not held across suspension (the system kills the process with
 * 0xdead10cc otherwise). Uses NSProcessInfo.performExpiringActivity because it is
 * also available in app extensions, unlike UIApplication.beginBackgroundTask.
 *
 * If the system expires the activity before [block] finishes, the guard is released
 * and the work continues unprotected: still a best-effort improvement over never
 * asserting background time at all.
 */
internal suspend fun <T> withSuspensionGuard(reason: String, block: suspend () -> T): T {
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
