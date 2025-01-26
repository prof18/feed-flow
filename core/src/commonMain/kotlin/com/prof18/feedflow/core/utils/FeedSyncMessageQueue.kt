package com.prof18.feedflow.core.utils

import com.prof18.feedflow.core.model.SyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FeedSyncMessageQueue {
    private val mutableMessageQueue = MutableSharedFlow<SyncResult>()

    val messageQueue = mutableMessageQueue.asSharedFlow()

    suspend fun emitResult(result: SyncResult) {
        mutableMessageQueue.emit(result)
    }
}
