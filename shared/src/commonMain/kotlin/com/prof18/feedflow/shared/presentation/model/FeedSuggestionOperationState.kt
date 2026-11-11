package com.prof18.feedflow.shared.presentation.model

data class FeedSuggestionOperationState(
    val status: FeedSuggestionOperationStatus,
    // Set only for feeds added during this visit, to show where they went.
    val addedToCategory: String? = null,
)

enum class FeedSuggestionOperationStatus {
    Adding,
    Added,
    AddFailed,
}

data class FeedSuggestionUndoOffer(
    val feedName: String,
    val feedUrl: String,
    val categoryName: String,
    val generation: Long,
)
