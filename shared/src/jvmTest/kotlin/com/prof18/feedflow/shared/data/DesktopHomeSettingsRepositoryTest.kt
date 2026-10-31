package com.prof18.feedflow.shared.data

import com.prof18.feedflow.shared.test.InMemoryPreferences
import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.set
import java.util.Base64
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

    @Test
    fun `hundreds of collapsed groups fit in java preferences and survive restart`() {
        val settings = PreferencesSettings(InMemoryPreferences())
        val repository = DesktopHomeSettingsRepository(settings)
        val collapsed = (0 until 1_000).mapTo(mutableSetOf()) { "category:${-1_000_000_000 - it}" } + "uncategorized"

        repository.setCollapsedFeedTreeSections(collapsed)
        assertEquals(collapsed, DesktopHomeSettingsRepository(settings).getCollapsedFeedTreeSections())

        val fewer = setOf("category:1")
        repository.setCollapsedFeedTreeSections(fewer)
        assertEquals(fewer, DesktopHomeSettingsRepository(settings).getCollapsedFeedTreeSections())

        repository.setCollapsedFeedTreeSections(emptySet())
        assertTrue(DesktopHomeSettingsRepository(settings).getCollapsedFeedTreeSections().isEmpty())
    }

    @Test
    fun `collapsed groups saved in the single legacy value are still read and replaced on save`() {
        val settings = PreferencesSettings(InMemoryPreferences())
        val legacy = setOf("category:42", "uncategorized")
        settings.set(
            "DESKTOP_HOME_COLLAPSED_FEED_TREE_SECTIONS",
            legacy.sorted().joinToString(",") { Base64.getUrlEncoder().encodeToString(it.toByteArray()) },
        )
        val repository = DesktopHomeSettingsRepository(settings)
        assertEquals(legacy, repository.getCollapsedFeedTreeSections())

        repository.setCollapsedFeedTreeSections(setOf("category:42"))
        assertEquals(setOf("category:42"), DesktopHomeSettingsRepository(settings).getCollapsedFeedTreeSections())
        assertNull(settings.getStringOrNull("DESKTOP_HOME_COLLAPSED_FEED_TREE_SECTIONS"))
    }
}
