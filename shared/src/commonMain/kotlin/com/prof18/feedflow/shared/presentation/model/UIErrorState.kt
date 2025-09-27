package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.ErrorCode

sealed class UIErrorState {

    abstract val errorCode: ErrorCode

    data class DatabaseError(
        override val errorCode: ErrorCode,
    ) : UIErrorState()
    data class FeedErrorState(
        val feedName: String,
        override val errorCode: ErrorCode,
    ) : UIErrorState()
    data class SyncError(
        override val errorCode: ErrorCode,
    ) : UIErrorState()
}
