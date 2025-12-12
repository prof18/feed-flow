package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.ErrorCode

sealed class UIErrorState {
    data class DatabaseError(
        val errorCode: ErrorCode,
    ) : UIErrorState()
    data class FeedErrorState(
        val feedName: String,
    ) : UIErrorState()
    data class SyncError(
        val errorCode: ErrorCode,
    ) : UIErrorState()
}
