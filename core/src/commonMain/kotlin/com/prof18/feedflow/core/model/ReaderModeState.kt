package com.prof18.feedflow.core.model

sealed interface ReaderModeState {
    data object Loading : ReaderModeState
    data class Success(val readerModeData: ReaderModeData) : ReaderModeState
    data class HtmlNotAvailable(val url: String) : ReaderModeState
}
