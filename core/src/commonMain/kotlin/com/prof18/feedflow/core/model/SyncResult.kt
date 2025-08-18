package com.prof18.feedflow.core.model

sealed interface SyncResult {
    data object Success : SyncResult

    sealed interface Error : SyncResult {
        data object General : Error
        data object ICloudNotAvailable : Error
    }

    fun isError(): Boolean = this is Error
    fun isSuccess(): Boolean = this is Success
}
