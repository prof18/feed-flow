package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.Logger

/** Reports a handled database failure once, with diagnostic tags where the platform supports them. */
internal expect fun reportDatabaseFailure(operation: String, error: Throwable, logger: Logger)
