package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger

internal expect fun getDateMillisFromString(
    dateString: String,
    logger: Logger? = null,
): Long?

internal expect fun formatDate(millis: Long): String

internal expect fun currentTimeMillis(): Long
