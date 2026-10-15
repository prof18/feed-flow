package com.prof18.feedflow.shared.presentation.model

data class FeedSuggestionOperationState(
    val generation: Long,
    val status: FeedSuggestionOperationStatus,
)

enum class FeedSuggestionOperationStatus {
    Adding,
    Refreshing,
    Added,
    AddFailed,
    UpdateFailed,
}
