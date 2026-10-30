package com.prof18.feedflow.core.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ReaderModeState {
    data object Loading : ReaderModeState
    data class Success(val readerModeData: ReaderModeData) : ReaderModeState
    data class ContentNotAvailable(
        val url: String,
        val id: String,
        val isBookmarked: Boolean,
    ) : ReaderModeState

    val getIsBookmarked: Boolean
        get() = when (this) {
            is Loading -> false
            is Success -> readerModeData.isBookmarked
            is ContentNotAvailable -> isBookmarked
        }

    val getUrl: String?
        get() = when (this) {
            is Loading -> null
            is Success -> readerModeData.url
            is ContentNotAvailable -> url
        }

    val getId: String?
        get() = when (this) {
            is Loading -> null
            is Success -> readerModeData.id.id
            is ContentNotAvailable -> id
        }
}
