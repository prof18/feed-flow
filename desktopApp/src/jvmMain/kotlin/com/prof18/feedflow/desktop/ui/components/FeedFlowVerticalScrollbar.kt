package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.ui.component.VerticalScrollbar

@Composable
fun BoxScope.FeedFlowVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    VerticalScrollbar(
        scrollState = scrollState,
        modifier = modifier.align(Alignment.TopEnd).fillMaxHeight(),
    )
}

@Composable
fun BoxScope.FeedFlowVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val adapter = rememberScrollbarAdapter(listState)
    VerticalScrollbar(
        scrollState = listState,
        modifier = modifier.align(Alignment.TopEnd).fillMaxHeight(),
        adapter = remember(adapter, listState) { EmptyListSafeScrollbarAdapter(adapter, listState) },
    )
}

/**
 * Compose's lazy-list adapter snaps to `totalItemsCount - 1` on long thumb drags, which is `-1` when the list
 * empties mid-drag and makes `scrollToItem` throw (Sentry JAVA-J6).
 */
internal class EmptyListSafeScrollbarAdapter(
    private val delegate: ScrollbarAdapter,
    private val listState: LazyListState,
) : ScrollbarAdapter by delegate {
    override suspend fun scrollTo(scrollOffset: Double) {
        if (listState.layoutInfo.totalItemsCount > 0) {
            delegate.scrollTo(scrollOffset)
        }
    }
}
