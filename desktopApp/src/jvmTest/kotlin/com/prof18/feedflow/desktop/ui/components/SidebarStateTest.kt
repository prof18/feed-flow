package com.prof18.feedflow.desktop.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SidebarStateTest {

    @Test
    fun `keys are based on feed and category ids rather than labels`() {
        assertEquals(
            SidebarEntry.feed(id = "feed-1", title = "Old title", isPinned = false).key,
            SidebarEntry.feed(id = "feed-1", title = "Renamed title", isPinned = true).key,
        )
        assertEquals(
            SidebarEntry.category(id = "category-1", title = "Old category", childCount = 3).key,
            SidebarEntry.category(id = "category-1", title = "Renamed category", childCount = 3).key,
        )
    }

    @Test
    fun `arrow focus moves without activating a feed filter`() {
        val entries = listOf(
            SidebarEntry(SidebarKey.Timeline),
            SidebarEntry(SidebarKey.Feed("feed-1")),
            SidebarEntry(SidebarKey.Feed("feed-2")),
        )
        val state = SidebarState(entries = entries, activeKey = SidebarKey.Timeline)

        val focused = state.moveFocus(delta = 1)

        assertEquals(SidebarKey.Feed("feed-1"), focused.focusedKey)
        assertEquals(SidebarKey.Timeline, focused.activeKey)
        assertNull(focused.activation)
    }

    @Test
    fun `explicit activation selects the focused feed`() {
        val feedKey = SidebarKey.Feed("feed-1")
        val state = SidebarState(
            entries = listOf(SidebarEntry(SidebarKey.Timeline), SidebarEntry(feedKey)),
            activeKey = SidebarKey.Timeline,
            focusedKey = feedKey,
        )

        val activated = state.activateFocused()

        assertEquals(feedKey, activated.activeKey)
        assertEquals(feedKey, activated.activation)
    }

    @Test
    fun `sidebar keys cover built in filters and category scopes`() {
        assertEquals(
            setOf(
                SidebarKey.Timeline,
                SidebarKey.Read,
                SidebarKey.Bookmarks,
                SidebarKey.Uncategorized,
                SidebarKey.Category("category-1"),
                SidebarKey.Feed("feed-1"),
            ),
            setOf(
                SidebarEntry(SidebarKey.Timeline).key,
                SidebarEntry(SidebarKey.Read).key,
                SidebarEntry(SidebarKey.Bookmarks).key,
                SidebarEntry(SidebarKey.Uncategorized).key,
                SidebarEntry.category("category-1", "News", childCount = 0).key,
                SidebarEntry.feed("feed-1", "Morning Paper").key,
            ),
        )
    }

    @Test
    fun `modified keyboard activation changes selection without opening a feed`() {
        val feedKey = SidebarKey.Feed("feed-1")
        val state = SidebarState(
            entries = listOf(SidebarEntry(SidebarKey.Timeline), SidebarEntry(feedKey)),
            activeKey = SidebarKey.Timeline,
            focusedKey = feedKey,
        )

        val modified = state.activateFocused(isMultiSelect = true)

        assertEquals(SidebarKey.Timeline, modified.activeKey)
        assertNull(modified.activation)
        assertEquals(setOf("feed-1"), modified.selectedFeedIds)
    }

    @Test
    fun `modified pointer activation changes selection without opening a feed`() {
        val feedKey = SidebarKey.Feed("feed-1")
        val state = SidebarState(
            entries = listOf(SidebarEntry(SidebarKey.Timeline), SidebarEntry(feedKey)),
            activeKey = SidebarKey.Timeline,
        )

        val modified = state.activatePointer(feedKey, isMultiSelect = true)

        assertEquals(SidebarKey.Timeline, modified.activeKey)
        assertNull(modified.activation)
        assertEquals(setOf("feed-1"), modified.selectedFeedIds)
    }

    @Test
    fun `modified activation on non-feed entry does not change filter`() {
        val categoryKey = SidebarKey.Category("category-1")
        val state = SidebarState(
            entries = listOf(SidebarEntry(SidebarKey.Timeline), SidebarEntry(categoryKey)),
            activeKey = SidebarKey.Timeline,
        )

        val modified = state.activatePointer(categoryKey, isMultiSelect = true)

        assertEquals(SidebarKey.Timeline, modified.activeKey)
        assertNull(modified.activation)
    }

    @Test
    fun `blank legacy labels are retained without breaking entry creation`() {
        assertEquals("", SidebarEntry.feed("feed-1", "").label)
        assertEquals("", SidebarEntry.category("category-1", "", childCount = 0).label)
    }

    @Test
    fun `empty categories remain in the entry model`() {
        val emptyCategoryKey = SidebarKey.Category("category-empty")
        val state = SidebarState(
            entries = listOf(
                SidebarEntry(SidebarKey.Timeline),
                SidebarEntry(emptyCategoryKey, childCount = 0),
            ),
            activeKey = SidebarKey.Timeline,
        )

        assertEquals(
            listOf(emptyCategoryKey),
            state.entries
                .filter { entry -> entry.key is SidebarKey.Category }
                .map { entry -> entry.key },
        )
    }

    @Test
    fun `removed active feed falls back to timeline and repairs focus`() {
        val timelineKey = SidebarKey.Timeline
        val remainingFeedKey = SidebarKey.Feed("feed-2")
        val state = SidebarState.reconcile(
            entries = listOf(
                SidebarEntry(timelineKey),
                SidebarEntry(remainingFeedKey),
            ),
            activeKey = SidebarKey.Feed("removed-feed"),
            focusedKey = SidebarKey.Feed("removed-feed"),
        )

        assertEquals(timelineKey, state.activeKey)
        assertEquals(timelineKey, state.focusedKey)
    }
}
