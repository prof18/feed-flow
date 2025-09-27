package com.prof18.feedflow.core.model

sealed interface SyncResult {
    data object Success : SyncResult

    sealed interface Error : SyncResult {
        val errorCode: ErrorCode
    }

    data class General(override val errorCode: ErrorCode) : Error
    data class ICloudNotAvailable(override val errorCode: ErrorCode) : Error

    fun isError(): Boolean = this is Error
    fun isSuccess(): Boolean = this is Success
}
