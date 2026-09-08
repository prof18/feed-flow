package com.prof18.feedflow.core.utils

import com.prof18.feedflow.core.model.SyncICloudError
import com.prof18.feedflow.core.model.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class FeedSyncMessageQueue {
    private val mutableMessageQueue = MutableSharedFlow<SyncResult>()

    val messageQueue = mutableMessageQueue.asSharedFlow()

    val userMessages: Flow<SyncResult> = flow {
        var reauthAlreadyReported = false

        messageQueue.collect { result ->
            when (result) {
                SyncResult.Success -> reauthAlreadyReported = false
                is SyncResult.GoogleDriveNeedReAuth -> {
                    if (!reauthAlreadyReported) {
                        reauthAlreadyReported = true
                        emit(result)
                    }
                }
                is SyncResult.ICloudNotAvailable -> {
                    // ServiceNotAvailable is setup-only feedback from an explicit iOS account attempt.
                    if (result.errorCode == SyncICloudError.ServiceNotAvailable) {
                        emit(result)
                    }
                }
                is SyncResult.General,
                is SyncResult.BackupNotFound,
                -> Unit
            }
        }
    }

    suspend fun emitResult(result: SyncResult) {
        mutableMessageQueue.emit(result)
    }
}
