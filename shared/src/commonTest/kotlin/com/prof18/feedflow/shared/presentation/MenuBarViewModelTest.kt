package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.test.ContentPrefetchRepositoryFake
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuBarViewModelTest : KoinTestBase() {

    private val viewModel: MenuBarViewModel by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val contentPrefetchRepository: ContentPrefetchRepository by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()

    @Test
    fun `initial state is loaded correctly`() = runTest {
        val state = viewModel.state.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertTrue(state.isMarkReadWhenScrollingEnabled)
        assertFalse(state.isShowReadItemsEnabled)
        assertFalse(state.isHideReadItemsEnabled)
        assertTrue(state.isReaderModeEnabled)
        assertFalse(state.isSaveReaderModeContentEnabled)
        assertFalse(state.isPrefetchArticleContentEnabled)
        assertTrue(state.isRefreshFeedsOnLaunchEnabled)
        assertTrue(state.isShowRssParsingErrorsEnabled)
        assertFalse(state.isReduceMotionEnabled)
        assertEquals(AutoDeletePeriod.DISABLED, state.autoDeletePeriod)
        assertTrue(state.isCrashReportingEnabled)
        assertEquals(FeedOrder.NEWEST_FIRST, state.feedOrder)
    }

    @Test
    fun `updateThemeMode updates state`() = runTest {
        viewModel.updateThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
    }

    @Test
    fun `updateMarkReadWhenScrolling updates state`() = runTest {
        viewModel.updateMarkReadWhenScrolling(false)
        assertFalse(viewModel.state.value.isMarkReadWhenScrollingEnabled)
    }

    @Test
    fun `updateShowReadItemsOnTimeline updates state and triggers getFeeds`() = runTest {
        populateDatabase()

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()
            viewModel.updateShowReadItemsOnTimeline(true)
            assertTrue(viewModel.state.value.isShowReadItemsEnabled)
            awaitItem()
        }
    }

    @Test
    fun `updateReaderMode updates state`() = runTest {
        viewModel.updateReaderMode(false)
        assertFalse(viewModel.state.value.isReaderModeEnabled)
    }

    @Test
    fun `updateSaveReaderModeContent updates state`() = runTest {
        viewModel.updateSaveReaderModeContent(true)
        assertTrue(viewModel.state.value.isSaveReaderModeContentEnabled)
    }

    @Test
    fun `updatePrefetchArticleContent updates state and cancels fetching when false`() = runTest {
        val prefetchRepository = contentPrefetchRepository as ContentPrefetchRepositoryFake
        viewModel.updatePrefetchArticleContent(true)
        assertTrue(viewModel.state.value.isPrefetchArticleContentEnabled)
        assertFalse(prefetchRepository.cancelFetchingCalled)

        viewModel.updatePrefetchArticleContent(false)
        assertFalse(viewModel.state.value.isPrefetchArticleContentEnabled)
        assertTrue(prefetchRepository.cancelFetchingCalled)
    }

    @Test
    fun `updateRefreshFeedsOnLaunch updates state`() = runTest {
        viewModel.updateRefreshFeedsOnLaunch(false)
        assertFalse(viewModel.state.value.isRefreshFeedsOnLaunchEnabled)
    }

    @Test
    fun `updateShowRssParsingErrors updates state`() = runTest {
        viewModel.updateShowRssParsingErrors(false)
        assertFalse(viewModel.state.value.isShowRssParsingErrorsEnabled)
    }

    @Test
    fun `updateReduceMotionEnabled updates state`() = runTest {
        viewModel.updateReduceMotionEnabled(true)
        assertTrue(viewModel.state.value.isReduceMotionEnabled)
    }

    @Test
    fun `updateAutoDeletePeriod updates state`() = runTest {
        viewModel.updateAutoDeletePeriod(AutoDeletePeriod.ONE_WEEK)
        assertEquals(AutoDeletePeriod.ONE_WEEK, viewModel.state.value.autoDeletePeriod)
    }

    @Test
    fun `updateCrashReporting updates state`() = runTest {
        viewModel.updateCrashReporting(false)
        assertFalse(viewModel.state.value.isCrashReportingEnabled)
    }

    @Test
    fun `updateFeedOrder updates state and triggers getFeeds`() = runTest {
        populateDatabase()

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()
            viewModel.updateFeedOrder(FeedOrder.OLDEST_FIRST)
            assertEquals(FeedOrder.OLDEST_FIRST, viewModel.state.value.feedOrder)
            awaitItem()
        }
    }

    @Test
    fun `updateHideReadItems updates state`() = runTest {
        viewModel.updateHideReadItems(true)
        assertTrue(viewModel.state.value.isHideReadItemsEnabled)

        viewModel.updateHideReadItems(false)
        assertFalse(viewModel.state.value.isHideReadItemsEnabled)
    }

    @Test
    fun `clearDownloadedArticleContent calls file handler`() = runTest {
        feedItemContentFileHandler.saveFeedItemContentToFile("1", "content")
        assertTrue(feedItemContentFileHandler.isContentAvailable("1"))

        viewModel.clearDownloadedArticleContent()

        assertFalse(feedItemContentFileHandler.isContentAvailable("1"))
    }

    private suspend fun populateDatabase() {
        val feedItem = FeedItemGenerator.unreadFeedItemArb().next()
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = feedItem.feedSource.id,
                    url = feedItem.feedSource.url,
                    title = feedItem.feedSource.title,
                    category = feedItem.feedSource.category,
                    logoUrl = feedItem.feedSource.logoUrl,
                    websiteUrl = feedItem.feedSource.websiteUrl,
                ),
            ),
        )

        databaseHelper.insertFeedItems(
            listOf(feedItem),
            lastSyncTimestamp = 0,
        )
    }
}
