package com.prof18.feedflow.shared.data

import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopHomeSettingsRepositoryTest : KoinTestBase() {
    @Test
    fun `requested widths survive restart without resetting legacy layout preferences`() {
        val settings = MapSettings()
        val repository = DesktopHomeSettingsRepository(settings)
        assertNull(repository.getRequestedSidebarWidth())
        assertNull(repository.getRequestedListWidth())
        repository.setDrawerVisible(false)
        repository.setMultiPaneLayoutEnabled(false)
        repository.setPaneExpansionIndex(9)
        repository.setRequestedSidebarWidth(280f)
        repository.setRequestedListWidth(460f)
        val restored = DesktopHomeSettingsRepository(settings)
        assertEquals(280f, restored.getRequestedSidebarWidth())
        assertEquals(460f, restored.getRequestedListWidth())
        assertEquals(false, restored.isDrawerVisible())
        assertEquals(false, restored.isMultiPaneLayoutEnabled())
        assertEquals(9, restored.getPaneExpansionIndex())
    }

    @Test
    fun `explicit collapsed groups survive restart without changing existing layout preferences`() {
        val settings = MapSettings()
        val repository = DesktopHomeSettingsRepository(settings)
        repository.setDrawerVisible(false)
        repository.setMultiPaneLayoutEnabled(false)
        repository.setPaneExpansionIndex(9)
        assertTrue(repository.getCollapsedFeedTreeSections().isEmpty())
        val collapsed = setOf("category:comma,unicode-日本", "uncategorized")
        repository.setCollapsedFeedTreeSections(collapsed)
        val restored = DesktopHomeSettingsRepository(settings)
        assertEquals(collapsed, restored.getCollapsedFeedTreeSections())
        assertEquals(false, restored.isDrawerVisible())
        assertEquals(false, restored.isMultiPaneLayoutEnabled())
        assertEquals(9, restored.getPaneExpansionIndex())
        restored.setCollapsedFeedTreeSections(emptySet())
        assertTrue(DesktopHomeSettingsRepository(settings).getCollapsedFeedTreeSections().isEmpty())
    }

    @Test
    fun `invalid collapse preference falls back to expanded`() {
        val settings = MapSettings()
        settings.set("DESKTOP_HOME_COLLAPSED_FEED_TREE_SECTIONS", "invalid-json")
        assertTrue(DesktopHomeSettingsRepository(settings).getCollapsedFeedTreeSections().isEmpty())
    }
}
