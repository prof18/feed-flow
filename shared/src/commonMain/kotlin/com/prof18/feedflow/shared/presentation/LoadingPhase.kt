package com.prof18.feedflow.shared.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

val LOADING_INDICATOR_SHOW_AFTER: Duration = 300.milliseconds
val LOADING_INDICATOR_MIN_VISIBLE: Duration = 500.milliseconds

/**
 * Maps a loading flag to what a screen should render, so a fast load never flashes the empty state or a spinner:
 * nothing is shown for [showAfter], then a spinner that stays at least [minVisible] once it appears.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<Boolean>.toLoadingPhase(
    showAfter: Duration = LOADING_INDICATOR_SHOW_AFTER,
    minVisible: Duration = LOADING_INDICATOR_MIN_VISIBLE,
    timeSource: TimeSource = TimeSource.Monotonic,
): Flow<LoadingPhase> = flow {
    var spinnerShownAt: TimeMark? = null
    emitAll(
        distinctUntilChanged().transformLatest { isLoading ->
            if (isLoading) {
                if (spinnerShownAt == null) {
                    emit(LoadingPhase.Pending)
                    delay(showAfter)
                    spinnerShownAt = timeSource.markNow()
                }
                emit(LoadingPhase.Spinner)
            } else {
                spinnerShownAt?.let { shownAt -> delay(minVisible - shownAt.elapsedNow()) }
                spinnerShownAt = null
                emit(LoadingPhase.Ready)
            }
        },
    )
}

enum class LoadingPhase {
    /** Still loading, but not for long enough to show a spinner: render nothing. */
    Pending,
    Spinner,
    Ready,
}
