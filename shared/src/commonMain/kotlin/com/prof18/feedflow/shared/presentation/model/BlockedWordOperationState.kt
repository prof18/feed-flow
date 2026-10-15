package com.prof18.feedflow.shared.presentation.model

data class BlockedWordOperationState(
    val generation: Long,
    val word: String,
    val action: BlockedWordAction,
    val status: BlockedWordOperationStatus,
)

enum class BlockedWordAction { Add, Remove }

enum class BlockedWordOperationStatus {
    Writing,
    Refreshing,
    Applied,
    Failed,
    RefreshFailed,
}
