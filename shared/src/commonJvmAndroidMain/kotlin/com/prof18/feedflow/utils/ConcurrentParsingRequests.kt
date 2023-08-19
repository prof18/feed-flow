package com.prof18.feedflow.utils

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY

@OptIn(FlowPreview::class)
actual fun getNumberOfConcurrentParsingRequests(): Int = DEFAULT_CONCURRENCY
