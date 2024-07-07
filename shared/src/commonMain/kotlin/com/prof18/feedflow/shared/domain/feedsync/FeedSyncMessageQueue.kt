package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.shared.domain.model.SyncResult
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FeedSyncMessageQueue {
    private val mutableMessageQueue = MutableSharedFlow<SyncResult>()

    @NativeCoroutines
    val messageQueue = mutableMessageQueue.asSharedFlow()

    internal suspend fun emitResult(result: SyncResult) {
        mutableMessageQueue.emit(result)
    }
}