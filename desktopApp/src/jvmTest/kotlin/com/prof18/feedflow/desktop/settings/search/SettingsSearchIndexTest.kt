package com.prof18.feedflow.desktop.settings.search

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.settings.DesktopSettingsCategory
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsSearchIndexTest {

    @Test
    fun `blank query returns no results`() {
        assertTrue(index().search("  ").isEmpty())
    }

    @Test
    fun `unread finds rows on two pages`() {
        val results = index().search("unread")

        assertTrue(
            results.any {
                it.title == EnFeedFlowStrings.settingsHideUnreadCount && it.category == DesktopSettingsCategory.GENERAL
            },
        )
        assertTrue(
            results.any {
                it.title == EnFeedFlowStrings.settingsHideUnreadDot && it.category == DesktopSettingsCategory.FEED_LIST
            },
        )
    }

    @Test
    fun `matching ignores case and accents`() {
        assertEquals("theme unread", normalizeForSearch("ThÈme Ünread"))

        val entry = SettingsSearchEntry(DesktopSettingsCategory.GENERAL, "Thème", "General")
        assertEquals(listOf(entry), listOf(entry).search("theme"))
    }

    @Test
    fun `title matches rank before section matches`() {
        val sectionOnly = SettingsSearchEntry(
            category = DesktopSettingsCategory.GENERAL,
            title = "Other",
            pageTitle = "General",
            sectionTitle = "Danger",
        )
        val titleMatch = SettingsSearchEntry(DesktopSettingsCategory.GENERAL, "Danger zone", "General")

        assertEquals(listOf(titleMatch, sectionOnly), listOf(sectionOnly, titleMatch).search("danger"))
    }

    @Test
    fun `unsupported accounts are not indexed`() {
        val entries = index(supported = listOf(SyncAccounts.DROPBOX))

        assertTrue(entries.search(EnFeedFlowStrings.desktopAccountsIcloudTitle).isEmpty())
        val dropbox = entries.search("dropbox")
        assertEquals(1, dropbox.size)
        assertEquals(DesktopSettingsCategory.ACCOUNTS, dropbox.single().category)
        assertNull(dropbox.single().targetTitle)
    }

    @Test
    fun `crash reporting only when available`() {
        assertTrue(index(crash = false).none { it.title == EnFeedFlowStrings.settingsCrashReporting })
        assertEquals(
            DesktopSettingsCategory.ABOUT,
            index(crash = true).single { it.title == EnFeedFlowStrings.settingsCrashReporting }.category,
        )
    }

    @Test
    fun `grid toggle only when visible`() {
        assertTrue(index(grid = false).none { it.title == EnFeedFlowStrings.settingsFeedLayoutGridToggle })
        assertEquals(
            DesktopSettingsCategory.FEED_LIST,
            index(grid = true).single { it.title == EnFeedFlowStrings.settingsFeedLayoutGridToggle }.category,
        )
    }

    @Test
    fun `faq is in the about category`() {
        assertEquals(
            DesktopSettingsCategory.ABOUT,
            index().single { it.title == EnFeedFlowStrings.aboutMenuFaq }.category,
        )
    }

    @Test
    fun `author row is searchable`() {
        val author = index().search("Gomiero").single()

        assertEquals(DesktopSettingsCategory.ABOUT, author.category)
        assertEquals(author.title, author.targetTitle)
    }

    @Test
    fun `every page title is an entry without target`() {
        val pageTitles = mapOf(
            DesktopSettingsCategory.GENERAL to EnFeedFlowStrings.settingsAppTitle,
            DesktopSettingsCategory.ACCOUNTS to EnFeedFlowStrings.settingsAccounts,
            DesktopSettingsCategory.FEED_LIST to EnFeedFlowStrings.settingsFeedListTitle,
            DesktopSettingsCategory.READING to EnFeedFlowStrings.settingsReadingBehavior,
            DesktopSettingsCategory.FILTERS to EnFeedFlowStrings.settingsFilters,
            DesktopSettingsCategory.SYNC_STORAGE to EnFeedFlowStrings.settingsSyncAndStorage,
            DesktopSettingsCategory.ABOUT to EnFeedFlowStrings.settingsAboutAndSupport,
        )
        val entries = index()

        DesktopSettingsCategory.entries.forEach { category ->
            val page = entries.single { it.category == category && it.title == pageTitles.getValue(category) }
            assertNull(page.targetTitle)
        }
    }

    private fun index(
        supported: List<SyncAccounts> = SyncAccounts.entries,
        crash: Boolean = true,
        grid: Boolean = true,
    ) = settingsSearchIndex(EnFeedFlowStrings, supported, crash, grid)
}
