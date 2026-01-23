package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.core.model.plus
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
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

class FeedListSettingsViewModelTest : KoinTestBase() {

    private val viewModel: FeedListSettingsViewModel by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `initial state is loaded correctly`() = runTest {
        val initialState = viewModel.state.value
        assertFalse(initialState.isHideDescriptionEnabled)
        assertFalse(initialState.isHideImagesEnabled)
        assertFalse(initialState.isHideDateEnabled)
        assertEquals(DateFormat.NORMAL, initialState.dateFormat)
        assertEquals(TimeFormat.HOURS_24, initialState.timeFormat)
        assertEquals(FeedLayout.LIST, initialState.feedLayout)
        assertEquals(0, initialState.fontScale)
        assertEquals(SwipeActionType.NONE, initialState.leftSwipeActionType)
        assertEquals(SwipeActionType.NONE, initialState.rightSwipeActionType)
        assertFalse(initialState.isRemoveTitleFromDescriptionEnabled)
        assertEquals(FeedOrder.NEWEST_FIRST, initialState.feedOrder)
    }

    @Test
    fun `updateHideDescription updates state and triggers getFeeds`() = runTest {
        populateDatabase()

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()
            viewModel.updateHideDescription(true)
            assertTrue(viewModel.state.value.isHideDescriptionEnabled)
            awaitItem()
        }
    }

    @Test
    fun `updateHideImages updates state and triggers getFeeds`() = runTest {
        populateDatabase()

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()
            viewModel.updateHideImages(true)
            assertTrue(viewModel.state.value.isHideImagesEnabled)
            awaitItem()
        }
    }

    @Test
    fun `updateHideDate updates state and triggers getFeeds`() = runTest {
        populateDatabase()

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()
            viewModel.updateHideDate(true)
            assertTrue(viewModel.state.value.isHideDateEnabled)
            awaitItem()
        }
    }

    @Test
    fun `updateDateFormat updates state and triggers getFeeds`() = runTest {
        populateDatabase()

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()
            viewModel.updateDateFormat(DateFormat.AMERICAN)
            assertEquals(DateFormat.AMERICAN, viewModel.state.value.dateFormat)
            awaitItem()
        }
    }

    @Test
    fun `updateTimeFormat updates state and triggers getFeeds`() = runTest {
        populateDatabase()

        feedStateRepository.feedState.test {
            awaitItem() shouldBe emptyList()
            viewModel.updateTimeFormat(TimeFormat.HOURS_12)
            assertEquals(TimeFormat.HOURS_12, viewModel.state.value.timeFormat)
            awaitItem()
        }
    }

    @Test
    fun `updateFeedLayout updates state`() = runTest {
        viewModel.updateFeedLayout(FeedLayout.CARD)
        assertEquals(FeedLayout.CARD, viewModel.state.value.feedLayout)
    }

    @Test
    fun `updateFontScale updates state and font size state`() = runTest {
        viewModel.updateFontScale(2)
        assertEquals(2, viewModel.state.value.fontScale)
        assertEquals(FeedFontSizes() + 2, viewModel.feedFontSizeState.value)
    }

    @Test
    fun `updateSwipeAction updates state for LEFT direction`() = runTest {
        viewModel.updateSwipeAction(SwipeDirection.LEFT, SwipeActionType.TOGGLE_READ_STATUS)
        assertEquals(SwipeActionType.TOGGLE_READ_STATUS, viewModel.state.value.leftSwipeActionType)
    }

    @Test
    fun `updateSwipeAction updates state for RIGHT direction`() = runTest {
        viewModel.updateSwipeAction(SwipeDirection.RIGHT, SwipeActionType.TOGGLE_BOOKMARK_STATUS)
        assertEquals(SwipeActionType.TOGGLE_BOOKMARK_STATUS, viewModel.state.value.rightSwipeActionType)
    }

    @Test
    fun `updateRemoveTitleFromDescription updates state`() = runTest {
        viewModel.updateRemoveTitleFromDescription(true)
        assertTrue(viewModel.state.value.isRemoveTitleFromDescriptionEnabled)
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
