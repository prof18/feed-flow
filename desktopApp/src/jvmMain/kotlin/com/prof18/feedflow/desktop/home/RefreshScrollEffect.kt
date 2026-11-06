package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Runs [onRefresh] once per new refresh. The handled trigger is saved with the screen state, so
 * coming back from the single-pane reader does not replay an old refresh and jump to the top.
 */
@Composable
internal fun RefreshScrollEffect(refreshTrigger: Int, onRefresh: suspend () -> Unit) {
    var handledRefreshTrigger by rememberSaveable { mutableIntStateOf(0) }
    val latestOnRefresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > handledRefreshTrigger) {
            handledRefreshTrigger = refreshTrigger
            latestOnRefresh()
        }
    }
}

/**
 * In the multi-pane layout an open article keeps the list where it is on refresh, so the reader
 * and the reading position survive; the new-articles pill still jumps to the top.
 */
internal fun shouldScrollToTopOnRefresh(
    isMultiPane: Boolean,
    isReaderOpen: Boolean,
    fromNewArticles: Boolean,
): Boolean = !isMultiPane || !isReaderOpen || fromNewArticles
