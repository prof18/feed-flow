package com.prof18.feedflow.shared.domain.opml

open class InvalidOpmlImportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
