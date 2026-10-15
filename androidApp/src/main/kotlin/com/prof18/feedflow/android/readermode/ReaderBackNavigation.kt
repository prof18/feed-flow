package com.prof18.feedflow.android.readermode

import com.prof18.feedflow.core.model.ReaderModeState

internal fun navigateReaderBack(
    state: ReaderModeState,
    canGoBack: Boolean,
    navigateHistory: () -> Unit,
    leaveReader: () -> Unit,
) {
    if (state is ReaderModeState.ContentNotAvailable && state.url.isNotBlank() && canGoBack) {
        navigateHistory()
    } else {
        leaveReader()
    }
}
