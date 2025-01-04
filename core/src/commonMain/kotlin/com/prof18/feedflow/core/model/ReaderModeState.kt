package com.prof18.feedflow.core.model

sealed interface ReaderModeState {
    data object Loading : ReaderModeState
    data class Success(val readerModeData: ReaderModeData) : ReaderModeState
    data class HtmlNotAvailable(
        val url: String,
        val id: String,
        val isBookmarked: Boolean,
    ) : ReaderModeState

    val getIsBookmarked: Boolean
        get() = when (this) {
            is Loading -> false
            is Success -> readerModeData.isBookmarked
            is HtmlNotAvailable -> isBookmarked
        }
}
