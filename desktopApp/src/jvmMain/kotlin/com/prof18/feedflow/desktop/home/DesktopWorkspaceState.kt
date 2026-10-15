package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class DesktopWorkspaceState(
    drawerVisible: Boolean = true,
    sidebarWidth: Dp? = null,
    listWidth: Dp? = null,
    private val legacySplitIndex: Int = 5,
    private val onDrawerVisibilityChanged: (Boolean) -> Unit = {},
    private val onSidebarWidthChanged: (Dp) -> Unit = {},
    private val onListWidthChanged: (Dp) -> Unit = {},
) {
    var isDrawerVisible by mutableStateOf(drawerVisible)
        private set
    var requestedSidebarWidth by mutableStateOf(sidebarWidth ?: 248.dp)
        private set
    var requestedListWidth by mutableStateOf(listWidth)
        private set
    var isReaderFullscreen by mutableStateOf(false)
        private set

    fun layout(width: Dp, multiPane: Boolean, readerSelected: Boolean = false): DesktopWorkspaceLayout {
        val available = width.coerceAtLeast(0.dp)
        val paneMinimum = if (multiPane) listMinimum + readerMinimum + workspaceDividerWidth else listMinimum
        val canDockSidebar = available >= sidebarMinimum + workspaceDividerWidth + paneMinimum
        val sidebar = if (isDrawerVisible && canDockSidebar) {
            requestedSidebarWidth.coerceIn(
                sidebarMinimum,
                sidebarMaximum.coerceAtMost(available - workspaceDividerWidth - paneMinimum),
            )
        } else {
            0.dp
        }
        val region = available - sidebar - if (sidebar > 0.dp) workspaceDividerWidth else 0.dp
        val canSplit = multiPane && region >= listMinimum + readerMinimum + workspaceDividerWidth
        val showReaderOnly = readerSelected && (!canSplit || isReaderFullscreen)
        val list = when {
            showReaderOnly -> 0.dp
            canSplit -> (requestedListWidth ?: listPreferred).coerceIn(
                listMinimum,
                region - workspaceDividerWidth - readerMinimum,
            )
            else -> region
        }
        val reader = when {
            showReaderOnly -> region
            canSplit -> region - list - workspaceDividerWidth
            else -> 0.dp
        }
        return DesktopWorkspaceLayout(sidebar, list, reader, canSplit, canDockSidebar)
    }

    fun seedListWidth(width: Dp, multiPane: Boolean) {
        if (requestedListWidth != null) return
        val layout = layout(width, multiPane)
        if (!layout.canSplit) return
        val index = legacySplitIndex.takeIf { it in 1..9 } ?: DEFAULT_SPLIT_INDEX
        val proportion = (LEGACY_BASE_PERCENT + index * LEGACY_PERCENT_STEP) / PERCENT_SCALE
        requestedListWidth = (layout.listWidth + layout.readerWidth) * proportion
        onListWidthChanged(requireNotNull(requestedListWidth))
    }

    fun resizeSidebar(width: Dp, persist: Boolean = true) {
        val requested = width.coerceIn(sidebarMinimum, sidebarMaximum)
        requestedSidebarWidth = requested
        if (persist) onSidebarWidthChanged(requested)
    }

    fun resizeList(width: Dp, availableWidth: Dp, multiPane: Boolean, persist: Boolean = true) {
        val layout = layout(availableWidth, multiPane)
        if (!layout.canSplit) return
        val maximum = layout.listWidth + layout.readerWidth - readerMinimum
        val requested = width.coerceIn(listMinimum, maximum)
        requestedListWidth = requested
        if (persist) onListWidthChanged(requested)
    }

    fun toggleDrawer() {
        isDrawerVisible = !isDrawerVisible
        onDrawerVisibilityChanged(isDrawerVisible)
    }

    fun toggleReaderFullscreen() {
        isReaderFullscreen = !isReaderFullscreen
    }

    private companion object {
        val sidebarMinimum = 200.dp
        val sidebarMaximum = 360.dp
        val listMinimum = 300.dp
        val listPreferred = 380.dp
        val readerMinimum = 360.dp
        const val DEFAULT_SPLIT_INDEX = 5
        const val LEGACY_BASE_PERCENT = 25
        const val LEGACY_PERCENT_STEP = 5
        const val PERCENT_SCALE = 100f
    }
}

internal data class DesktopWorkspaceLayout(
    val sidebarWidth: Dp,
    val listWidth: Dp,
    val readerWidth: Dp,
    val canSplit: Boolean,
    val canDockSidebar: Boolean = false,
) {
    val isSidebarDocked: Boolean get() = sidebarWidth > 0.dp
}

internal val workspaceDividerWidth = 6.dp
