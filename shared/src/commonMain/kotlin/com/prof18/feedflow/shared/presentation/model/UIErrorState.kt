package com.prof18.feedflow.shared.presentation.model

sealed class UIErrorState {
    data object DatabaseError : UIErrorState()
    data class FeedErrorState(
        val feedName: String,
    ) : UIErrorState()
    data object SyncError : UIErrorState()
}
