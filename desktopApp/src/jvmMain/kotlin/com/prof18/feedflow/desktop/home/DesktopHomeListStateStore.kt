package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal class DesktopHomeListStateStore(
    private val createListState: () -> LazyListState = { LazyListState() },
) {
    private val listStatesByLayout = mutableMapOf<Boolean, LazyListState>()

    fun getListState(isMultiPaneLayoutEnabled: Boolean): LazyListState =
        listStatesByLayout.getOrPut(isMultiPaneLayoutEnabled, createListState)
}

@Composable
internal fun rememberDesktopHomeListStateStore(): DesktopHomeListStateStore =
    remember { DesktopHomeListStateStore() }
