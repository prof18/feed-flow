@file:Suppress("MagicNumber")

package com.prof18.feedflow.shared.e2e

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class E2eSeedRunnerTest : KoinTestBase() {

    private val seedRunner: E2eSeedRunner by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()
    private val accountsRepository: AccountsRepository by inject()
    private val networkSettings: NetworkSettings by inject()

    @Test
    fun `content-rich profile seeds deterministic content and settings`() = runTest {
        seedRunner.resetAndSeed(E2eSeedProfile.CONTENT_RICH)

        assertEquals(3, databaseHelper.getFeedSourceCategories().size)
        assertEquals(7, databaseHelper.getFeedSources().size)

        val timelineItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 50,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val timelineTitles = timelineItems.mapNotNull { it.title }

        assertTrue(timelineTitles.first().startsWith("E2E Newest Unread Article"))
        assertFalse("E2E Hidden Feed Article" in timelineTitles)
        assertFalse("E2E blockedword Article" in timelineTitles)

        val bookmarkedItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Bookmarks,
            pageSize = 50,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        assertEquals(2, bookmarkedItems.size)

        val fetchFailedFeed = databaseHelper.getFeedSources()
            .first { it.title == "VeneziaToday" }
        assertTrue(fetchFailedFeed.fetchFailed)

        assertFalse(settingsRepository.getRefreshFeedsOnLaunch())
        assertEquals(ArticleOpenMode.FULL_ARTICLE, settingsRepository.getArticleOpenMode())
        assertEquals(FeedLayout.LIST, feedAppearanceSettingsRepository.getFeedLayout())
        assertEquals(DescriptionLineLimit.THREE, feedAppearanceSettingsRepository.getDescriptionLineLimit())
        assertTrue(
            feedItemContentFileHandler.isContentAvailable(E2eSeedRunner.READER_SUCCESS_ARTICLE_ID),
        )
    }

    @Test
    fun `compact-list profile applies dense feed settings`() = runTest {
        seedRunner.resetAndSeed(E2eSeedProfile.COMPACT_LIST)

        assertEquals(FeedLayout.LIST, feedAppearanceSettingsRepository.getFeedLayout())
        assertTrue(feedAppearanceSettingsRepository.getHideImages())
        assertTrue(feedAppearanceSettingsRepository.getHideDate())
        assertTrue(feedAppearanceSettingsRepository.getHideFeedSource())
        assertTrue(feedAppearanceSettingsRepository.getHideUnreadDot())
        assertTrue(feedAppearanceSettingsRepository.getHideDescription())
        assertTrue(feedAppearanceSettingsRepository.getRemoveTitleFromDescription())
    }

    @Test
    fun `reader-mode profile seeds cached reader content and font size`() = runTest {
        seedRunner.resetAndSeed(E2eSeedProfile.READER_MODE)

        assertTrue(settingsRepository.isSaveItemContentOnOpenEnabled())
        assertFalse(settingsRepository.isPrefetchArticleContentEnabled())
        assertEquals(20, settingsRepository.getReaderModeFontSize())
        assertNotNull(
            databaseHelper.getFeedItemUrlInfo(E2eSeedRunner.READER_FALLBACK_ARTICLE_ID),
        )
        assertTrue(
            feedItemContentFileHandler.isContentAvailable(E2eSeedRunner.READER_FALLBACK_ARTICLE_ID),
        )
    }

    @Test
    fun `pagination-scroll-read profile seeds enough unread items and scroll-read settings`() = runTest {
        seedRunner.resetAndSeed(E2eSeedProfile.PAGINATION_SCROLL_READ)

        val timelineItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 200,
            showReadItems = false,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val scrollReadTitles = timelineItems
            .mapNotNull { it.title }
            .filter { it.startsWith("E2E Scroll Read Article") }

        assertEquals(90, scrollReadTitles.size)
        assertEquals("E2E Scroll Read Article 001", scrollReadTitles.first())
        assertTrue("E2E Scroll Read Article 050: Skip Needle" in scrollReadTitles)
        assertTrue("E2E Scroll Read Article 090" in scrollReadTitles)

        assertTrue(settingsRepository.getMarkFeedAsReadWhenScrolling())
        assertFalse(settingsRepository.getHideReadItems())
        assertFalse(settingsRepository.getShowReadArticlesTimeline())
    }

    @Test
    fun `sync-linked-mock profile seeds selected network account`() = runTest {
        seedRunner.resetAndSeed(E2eSeedProfile.SYNC_LINKED_MOCK, E2eSeedAccount.FRESH_RSS)

        assertEquals(SyncAccounts.FRESH_RSS, accountsRepository.getCurrentSyncAccount())
        assertEquals(SyncAccounts.FRESH_RSS, networkSettings.getSyncAccountType())
        assertEquals("https://e2e.feedflow.local/greader", networkSettings.getSyncUrl())
        assertEquals("e2e-user", networkSettings.getSyncUsername())
        assertEquals("e2e-pass", networkSettings.getSyncPwd())
        assertNotNull(networkSettings.getLastSyncDate())
    }

    @Test
    fun `reset clears seeded content settings and cached files`() = runTest {
        seedRunner.resetAndSeed(E2eSeedProfile.CONTENT_RICH)

        seedRunner.reset()

        assertEquals(emptyList(), databaseHelper.getFeedSourceCategories())
        assertEquals(emptyList(), databaseHelper.getFeedSources())
        assertEquals(emptyList(), databaseHelper.observeBlockedWords().first())
        assertFalse(
            feedItemContentFileHandler.isContentAvailable(E2eSeedRunner.READER_SUCCESS_ARTICLE_ID),
        )
        assertEquals(FeedOrder.NEWEST_FIRST, feedAppearanceSettingsRepository.getFeedOrder())
        assertFalse(settingsRepository.getShowReadArticlesTimeline())
    }
}
