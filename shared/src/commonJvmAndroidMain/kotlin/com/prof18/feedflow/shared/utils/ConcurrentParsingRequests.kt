package com.prof18.feedflow.shared.utils

import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY

internal actual fun getNumberOfConcurrentParsingRequests(): Int = DEFAULT_CONCURRENCY
