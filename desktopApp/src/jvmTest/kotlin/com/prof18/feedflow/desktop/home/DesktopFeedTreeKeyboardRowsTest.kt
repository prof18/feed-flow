package com.prof18.feedflow.desktop.home

import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.desktop.home.drawer.desktopTreeKeyboardRows
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopFeedTreeKeyboardRowsTest {
    @Test
    fun `keyboard skips collapsing feeds but counts their temporary lazy slots`() {
        val first = FeedSourceCategory("first", "First", position = 0)
        val second = FeedSourceCategory("second", "Second", position = 1)
        val source = DesktopLibraryFixture.load().sources.first().feedSource
        val tree = projectDesktopFeedTree(
            NavDrawerState(
                feedSourcesByCategory = persistentMapOf(
                    DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper(first) to listOf(
                        DrawerItem.DrawerFeedSource(source.copy(id = "one", category = first, isPinned = false), 0),
                        DrawerItem.DrawerFeedSource(source.copy(id = "two", category = first, isPinned = false), 0),
                    ),
                    DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper(second) to listOf(
                        DrawerItem.DrawerFeedSource(source.copy(id = "three", category = second, isPinned = false), 0),
                    ),
                ),
            ),
        )
        val state = DesktopFeedTreeState(collapsedSections = setOf("category:first"))
        val closing = desktopTreeKeyboardRows(tree, state, retainedCollapsedSections = setOf("category:first"))
        assertFalse(closing.any { it.key == "feed:one" || it.key == "feed:two" })
        val firstIndex = closing.single { it.key == "category:first" }.lazyIndex
        assertEquals(firstIndex + 3, closing.single { it.key == "category:second" }.lazyIndex)
        val settled = desktopTreeKeyboardRows(tree, state)
        assertEquals(firstIndex + 1, settled.single { it.key == "category:second" }.lazyIndex)
    }
}
