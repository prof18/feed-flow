package com.prof18.feedflow.utils

import kotlinx.coroutines.CoroutineDispatcher

internal interface DispatcherProvider {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}
