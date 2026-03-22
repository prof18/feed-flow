package com.prof18.feedflow.shared.domain.opml

import java.io.InputStream

actual class OpmlInput(
    val inputStreamProvider: () -> InputStream?,
)
