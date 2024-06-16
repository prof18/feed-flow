package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.shared.domain.model.SyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FeedSyncMessageQueue {
    private val mutableMessageQueue = MutableSharedFlow<SyncResult>()
    val messageQueue = mutableMessageQueue.asSharedFlow()

    suspend fun emitResult(result: SyncResult) {
        mutableMessageQueue.emit(result)
    }
}
