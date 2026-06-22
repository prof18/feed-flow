package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.lazy.LazyListState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DesktopHomeListStateStoreTest {

    @Test
    fun `single-pane list state survives leaving and returning to home`() {
        var createdStateCount = 0
        val store = DesktopHomeListStateStore(
            createListState = {
                createdStateCount += 1
                LazyListState(
                    firstVisibleItemIndex = if (createdStateCount == 1) 12 else 0,
                    firstVisibleItemScrollOffset = if (createdStateCount == 1) 34 else 0,
                )
            },
        )
        val initialState = store.getListState(isMultiPaneLayoutEnabled = false)

        val restoredState = store.getListState(isMultiPaneLayoutEnabled = false)

        assertSame(initialState, restoredState)
        assertEquals(12, restoredState.firstVisibleItemIndex)
        assertEquals(34, restoredState.firstVisibleItemScrollOffset)
    }

    @Test
    fun `single-pane and multi-pane list states are independent`() {
        val store = DesktopHomeListStateStore()

        val singlePaneState = store.getListState(isMultiPaneLayoutEnabled = false)
        val multiPaneState = store.getListState(isMultiPaneLayoutEnabled = true)

        assertNotSame(singlePaneState, multiPaneState)
        assertSame(singlePaneState, store.getListState(isMultiPaneLayoutEnabled = false))
        assertSame(multiPaneState, store.getListState(isMultiPaneLayoutEnabled = true))
    }
}
