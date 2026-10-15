package com.prof18.feedflow.desktop.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopFeedSelectionTest {
    private val visibleIds = listOf("pinned", "first", "second", "last")

    @Test
    fun `shift range uses visible tree order and retains the anchor`() {
        val state = DesktopFeedSelectionState().select("first", visibleIds, toggle = true)
        val ranged = state.select("last", visibleIds, range = true)
        assertEquals(setOf("first", "second", "last"), ranged.selectedIds)
        assertEquals("first", ranged.anchorId)
        assertEquals(setOf("first", "second"), ranged.select("second", visibleIds, range = true).selectedIds)
    }

    @Test
    fun `platform toggle adds and removes without clearing unrelated selection`() {
        val state = DesktopFeedSelectionState().select("last", visibleIds, toggle = true, activeId = "first")
        assertEquals(setOf("first", "last"), state.selectedIds)
        assertEquals(setOf("first"), state.select("last", visibleIds, toggle = true).selectedIds)
    }

    @Test
    fun `combined shift toggle adds the range to selection`() {
        val state = DesktopFeedSelectionState(setOf("pinned", "first"), anchorId = "first")
        assertEquals(
            visibleIds.toSet(),
            state.select("last", visibleIds, toggle = true, range = true).selectedIds,
        )
    }

    @Test
    fun `context preserves a selected batch or targets only the clicked row`() {
        val state = DesktopFeedSelectionState(setOf("first", "last"), anchorId = "first")
        assertEquals(state, state.contextTarget("last"))
        assertEquals(setOf("second"), state.contextTarget("second").selectedIds)
    }

    @Test
    fun `hidden anchor falls back to clicked row and deleted identities are pruned`() {
        val state = DesktopFeedSelectionState(setOf("pinned", "first"), anchorId = "deleted")
        assertEquals(setOf("last"), state.select("last", visibleIds, range = true).selectedIds)
        val reconciled = state.reconcile(setOf("pinned"))
        assertEquals(setOf("pinned"), reconciled.selectedIds)
        assertNull(reconciled.anchorId)
        assertEquals(state, state.select("absent", visibleIds, range = true))
    }
}
