package com.prof18.feedflow.shared.utils

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY

@OptIn(FlowPreview::class)
internal actual fun getNumberOfConcurrentParsingRequests(): Int = DEFAULT_CONCURRENCY
