package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.Logger

// Not reached: isDatabaseFailure() is false on this platform, so no caller reports through here.
internal actual fun reportDatabaseFailure(operation: String, error: Throwable, logger: Logger) {
    logger.e(error) { "Database failure in $operation" }
}
