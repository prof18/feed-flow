package com.prof18.feedflow.shared.domain.opml

import java.io.OutputStream

actual class OpmlOutput(
    val outputStreamProvider: () -> OutputStream?,
)
