package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncAndStorageSettingsViewModelTest : KoinTestBase() {

    private val viewModel: SyncAndStorageSettingsViewModel by inject()
    private val feedItemContentFileHandler: FeedItemContentFileHandler by inject()

    @Test
    fun `state is loaded from settings repository on init`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals(SyncPeriod.NEVER, initialState.syncPeriod)
            assertEquals(AutoDeletePeriod.DISABLED, initialState.autoDeletePeriod)
            assertTrue(initialState.refreshFeedsOnLaunch)
            assertTrue(initialState.showRssParsingErrors)
        }
    }

    @Test
    fun `updateSyncPeriod updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            val newPeriod = SyncPeriod.ONE_DAY
            viewModel.updateSyncPeriod(newPeriod)

            assertEquals(newPeriod, awaitItem().syncPeriod)
        }
    }

    @Test
    fun `updateAutoDeletePeriod updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            val newPeriod = AutoDeletePeriod.ONE_WEEK
            viewModel.updateAutoDeletePeriod(newPeriod)

            assertEquals(newPeriod, awaitItem().autoDeletePeriod)
        }
    }

    @Test
    fun `updateRefreshFeedsOnLaunch updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateRefreshFeedsOnLaunch(false)
            assertFalse(awaitItem().refreshFeedsOnLaunch)

            viewModel.updateRefreshFeedsOnLaunch(true)
            assertTrue(awaitItem().refreshFeedsOnLaunch)
        }
    }

    @Test
    fun `updateShowRssParsingErrors updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateShowRssParsingErrors(false)
            assertFalse(awaitItem().showRssParsingErrors)

            viewModel.updateShowRssParsingErrors(true)
            assertTrue(awaitItem().showRssParsingErrors)
        }
    }

    @Test
    fun `clearDownloadedArticleContent clears saved content`() = runTest {
        val feedItemId = "item-id"
        feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, "content")
        assertTrue(feedItemContentFileHandler.isContentAvailable(feedItemId))

        viewModel.clearDownloadedArticleContent()

        assertFalse(feedItemContentFileHandler.isContentAvailable(feedItemId))
    }
}
