package com.prof18.feedflow.desktop.settings.search

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.settings.DesktopSettingsCategory
import com.prof18.feedflow.desktop.settings.DesktopSettingsSection
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
                it.title == EnFeedFlowStrings.settingsShowUnreadDot && it.category == DesktopSettingsCategory.FEED_LIST
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
        assertEquals(DesktopSettingsCategory.SYNC, dropbox.single().category)
        assertNull(dropbox.single().targetTitle)
        assertEquals(DesktopSettingsSection.ACCOUNTS, dropbox.single().section)
    }

    @Test
    fun refreshRowsBelongToSync() {
        val entries = index()

        assertEquals(
            DesktopSettingsCategory.SYNC,
            entries.single { it.title == EnFeedFlowStrings.settingsRefreshFeedsOnLaunch }.category,
        )
        assertEquals(
            DesktopSettingsCategory.FEEDS,
            entries.single { it.title == EnFeedFlowStrings.importExportLabel }.category,
        )
    }

    @Test
    fun feedsRowsOpenTheirSubpages() {
        val entries = index()

        val blockedWords = entries.single { it.title == EnFeedFlowStrings.settingsBlockedWords }
        assertEquals(DesktopSettingsCategory.FEEDS, blockedWords.category)
        assertEquals(DesktopSettingsSection.BLOCKED_WORDS, blockedWords.section)
        val importExport = entries.single { it.title == EnFeedFlowStrings.importExportLabel }
        assertEquals(DesktopSettingsSection.IMPORT_EXPORT, importExport.section)
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
    fun `read articles picker is on the reading page`() {
        val entry = index().single { it.title == EnFeedFlowStrings.settingsReadArticlesTitle }

        assertEquals(DesktopSettingsCategory.READING, entry.category)
        assertEquals(EnFeedFlowStrings.settingsReadingTitle, entry.pageTitle)
    }

    @Test
    fun `image is in the show in list section of the feed list page`() {
        val entry = index().single { it.title == EnFeedFlowStrings.settingsShowImages }

        assertEquals(DesktopSettingsCategory.FEED_LIST, entry.category)
        assertEquals(EnFeedFlowStrings.settingsFeedListSectionShowInList, entry.sectionTitle)
    }

    @Test
    fun `feed list has no appearance or reading behavior sections`() {
        val feedListSections = index()
            .filter { it.category == DesktopSettingsCategory.FEED_LIST }
            .map { it.sectionTitle }

        assertTrue(EnFeedFlowStrings.settingsAppearance !in feedListSections)
        assertTrue(EnFeedFlowStrings.settingsReadingBehavior !in feedListSections)
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
            DesktopSettingsCategory.FEEDS to EnFeedFlowStrings.feedsTitle,
            DesktopSettingsCategory.SYNC to EnFeedFlowStrings.settingsSyncAndRefresh,
            DesktopSettingsCategory.STORAGE to EnFeedFlowStrings.settingsStorage,
            DesktopSettingsCategory.FEED_LIST to EnFeedFlowStrings.settingsFeedListTitle,
            DesktopSettingsCategory.READING to EnFeedFlowStrings.settingsReadingTitle,
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
