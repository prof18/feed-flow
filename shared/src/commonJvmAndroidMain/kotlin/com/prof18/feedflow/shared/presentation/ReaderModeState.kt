package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.shared.domain.ReaderModeData

sealed interface ReaderModeState {
    data object Loading : ReaderModeState
    data class Success(val readerModeData: ReaderModeData) : ReaderModeState
    data class HtmlNotAvailable(val url: String) : ReaderModeState
}
