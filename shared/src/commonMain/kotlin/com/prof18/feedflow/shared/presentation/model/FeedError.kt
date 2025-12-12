package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.ErrorCode
import com.prof18.feedflow.core.model.NoCode

internal sealed interface ErrorState {
    val errorCode: ErrorCode
}

internal data class FeedErrorState(
    val failingSourceName: String,
    override val errorCode: ErrorCode = NoCode,
) : ErrorState

internal data class DatabaseError(override val errorCode: ErrorCode) : ErrorState

internal data class SyncError(override val errorCode: ErrorCode) : ErrorState
