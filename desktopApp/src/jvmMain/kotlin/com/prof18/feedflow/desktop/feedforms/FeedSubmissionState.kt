package com.prof18.feedflow.desktop.feedforms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester

@Composable
internal fun rememberFeedSubmissionState(loading: Boolean, completionRevision: Int): FeedSubmissionState {
    val latestLoading = rememberUpdatedState(loading)
    val submission = remember { FeedSubmissionState(latestLoading) }
    val previousRevision = remember { mutableStateOf(completionRevision) }
    LaunchedEffect(completionRevision) {
        if (previousRevision.value != completionRevision) {
            submission.pending = false
        }
        previousRevision.value = completionRevision
    }
    return submission
}

@Composable
internal fun rememberFeedFieldFocus(busy: Boolean, completionRevision: Int): FocusRequester {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(busy, completionRevision) {
        if (!busy && completionRevision > 0) focus.requestFocus()
    }
    return focus
}

internal class FeedSubmissionState(private val loading: State<Boolean>) {
    var pending by mutableStateOf(false)
    val busy: Boolean get() = pending || loading.value

    fun submit(valid: Boolean, action: () -> Unit) {
        if (busy || !valid) return
        pending = true
        action()
    }
}
