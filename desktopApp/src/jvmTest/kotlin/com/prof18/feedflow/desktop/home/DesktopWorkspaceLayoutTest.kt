package com.prof18.feedflow.desktop.home

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopWorkspaceLayoutTest {
    @Test
    fun `width matrix uses available pane minimums without overlap`() {
        val state = DesktopWorkspaceState()
        for (width in listOf(500.dp, 741.dp, 1024.dp, 1463.dp)) {
            val layout = state.layout(width, multiPane = true)
            assertEquals(width >= 1024.dp, layout.isSidebarDocked)
            assertEquals(width >= 741.dp, layout.canSplit)
            val separators = (if (layout.isSidebarDocked) 6.dp else 0.dp) + (if (layout.canSplit) 6.dp else 0.dp)
            assertEquals(width, layout.sidebarWidth + layout.listWidth + layout.readerWidth + separators)
            assertTrue(layout.listWidth >= 300.dp)
            if (layout.canSplit) assertTrue(layout.readerWidth >= 360.dp)
        }
    }

    @Test
    fun `compact resize retains reader selection and requested split`() {
        val state = DesktopWorkspaceState(listWidth = 510.dp)
        val compact = state.layout(500.dp, multiPane = true, readerSelected = true)
        assertEquals(0.dp, compact.listWidth)
        assertEquals(500.dp, compact.readerWidth)
        assertEquals(510.dp, state.requestedListWidth)
        val wide = state.layout(1463.dp, multiPane = true, readerSelected = true)
        assertEquals(510.dp, wide.listWidth)
        assertTrue(wide.readerWidth >= 360.dp)
    }

    @Test
    fun `legacy index seeds once after a measured two pane region is available`() {
        for ((index, proportion) in legacyCases) {
            val writes = mutableListOf<Float>()
            val state = DesktopWorkspaceState(legacySplitIndex = index, onListWidthChanged = { writes.add(it.value) })
            state.seedListWidth(500.dp, multiPane = true)
            assertNull(state.requestedListWidth)
            assertTrue(writes.isEmpty())
            state.seedListWidth(1463.dp, multiPane = true)
            val expected = (1463.dp - 248.dp - 6.dp - 6.dp).value * proportion
            assertEquals(expected, state.requestedListWidth?.value)
            state.seedListWidth(1024.dp, multiPane = true)
            assertEquals(listOf(expected), writes)
        }
    }

    @Test
    fun `hidden drawer and disabled multipane preserve old profile choices`() {
        val state = DesktopWorkspaceState(drawerVisible = false, legacySplitIndex = 1)
        state.seedListWidth(1463.dp, multiPane = false)
        assertNull(state.requestedListWidth)
        val single = state.layout(1463.dp, multiPane = false)
        assertFalse(single.isSidebarDocked)
        assertFalse(single.canSplit)
        assertEquals(1463.dp, single.listWidth)
        state.seedListWidth(1463.dp, multiPane = true)
        assertEquals((1463.dp - 6.dp).value * firstLegacyProportion, state.requestedListWidth?.value)
    }

    @Test
    fun `explicit widths take precedence and resize clamps rendering without rewriting preferences`() {
        val writes = mutableListOf<Float>()
        val state = DesktopWorkspaceState(
            sidebarWidth = 340.dp,
            listWidth = 600.dp,
            legacySplitIndex = 1,
            onListWidthChanged = { writes.add(it.value) },
        )
        state.seedListWidth(1463.dp, multiPane = true)
        val narrow = state.layout(1024.dp, multiPane = true)
        assertTrue(narrow.sidebarWidth >= 200.dp)
        assertTrue(narrow.readerWidth >= 360.dp)
        assertTrue(narrow.listWidth >= 300.dp)
        assertEquals(600.dp, state.requestedListWidth)
        assertTrue(writes.isEmpty())
        assertEquals(600.dp, state.layout(1463.dp, multiPane = true).listWidth)
    }

    @Test
    fun `user divider adjustment persists and restart never reseeds`() {
        var sidebar = 248.dp
        var list = 380.dp
        val state = DesktopWorkspaceState(
            listWidth = list,
            onSidebarWidthChanged = { sidebar = it },
            onListWidthChanged = { list = it },
        )
        state.resizeSidebar(600.dp)
        state.resizeList(9999.dp, availableWidth = 1463.dp, multiPane = true)
        assertEquals(360.dp, sidebar)
        assertEquals(731.dp, list)
        val restored = DesktopWorkspaceState(sidebarWidth = sidebar, listWidth = list, legacySplitIndex = 1)
        restored.seedListWidth(1463.dp, multiPane = true)
        assertEquals(sidebar, restored.requestedSidebarWidth)
        assertEquals(list, restored.requestedListWidth)
    }

    @Test
    fun `reader fullscreen restores the requested split without persisting a fullscreen width`() {
        val state = DesktopWorkspaceState(listWidth = 420.dp)
        state.toggleReaderFullscreen()
        val fullscreen = state.layout(1024.dp, multiPane = true, readerSelected = true)
        assertEquals(0.dp, fullscreen.listWidth)
        assertEquals(770.dp, fullscreen.readerWidth)
        state.toggleReaderFullscreen()
        assertEquals(420.dp.coerceAtMost(404.dp), state.layout(1024.dp, multiPane = true).listWidth)
        assertEquals(420.dp, state.requestedListWidth)
    }

    private companion object {
        const val firstLegacyProportion = 0.3f
        val legacyCases = listOf(1 to 0.3f, 5 to 0.5f, 9 to 0.7f, 0 to 0.5f, 10 to 0.5f, -1 to 0.5f)
    }
}
