package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationsViewModelTest : KoinTestBase() {

    private val viewModel: NotificationsViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `initial state is loaded correctly`() = runTest {
        val feedSource1 = ParsedFeedSource(
            id = "id1",
            url = "url1",
            title = "title1",
            category = null,
            logoUrl = null,
            websiteUrl = null,
        )
        val feedSource2 = ParsedFeedSource(
            id = "id2",
            url = "url2",
            title = "title2",
            category = null,
            logoUrl = null,
            websiteUrl = null,
        )
        databaseHelper.insertFeedSource(listOf(feedSource1, feedSource2))

        viewModel.notificationSettingState.test {
            val initialState = awaitItem()

            val state = if (initialState.feedSources.isEmpty()) awaitItem() else initialState

            assertEquals(2, state.feedSources.size)
            assertFalse(state.isEnabledForAll)
            assertEquals(NotificationMode.FEED_SOURCE, state.notificationMode)
        }
    }

    @Test
    fun `updateAllNotificationStatus updates state and database`() = runTest {
        val feedSource1 = ParsedFeedSource(
            id = "id1",
            url = "url1",
            title = "title1",
            category = null,
            logoUrl = null,
            websiteUrl = null,
        )
        databaseHelper.insertFeedSource(listOf(feedSource1))

        viewModel.notificationSettingState.test {
            awaitItem() // Initial or first update

            viewModel.updateAllNotificationStatus(true)

            val state = awaitItem()
            assertTrue(state.isEnabledForAll)
            assertTrue(state.feedSources.all { it.isEnabled })
        }
    }

    @Test
    fun `updateNotificationStatus updates state and database`() = runTest {
        val feedSource1 = ParsedFeedSource(
            id = "id1",
            url = "url1",
            title = "title1",
            category = null,
            logoUrl = null,
            websiteUrl = null,
        )
        val feedSource2 = ParsedFeedSource(
            id = "id2",
            url = "url2",
            title = "title2",
            category = null,
            logoUrl = null,
            websiteUrl = null,
        )
        databaseHelper.insertFeedSource(listOf(feedSource1, feedSource2))

        viewModel.notificationSettingState.test {
            awaitItem() // Initial or first update

            viewModel.updateNotificationStatus(true, "id1")

            val state = awaitItem()
            assertTrue(state.feedSources.first { it.feedSourceId == "id1" }.isEnabled)
            assertFalse(state.isEnabledForAll)
        }
    }

    @Test
    fun `updateNotificationMode updates state and repository`() = runTest {
        viewModel.notificationSettingState.test {
            awaitItem()

            viewModel.updateNotificationMode(NotificationMode.GROUPED)

            val state = awaitItem()
            assertEquals(NotificationMode.GROUPED, state.notificationMode)
        }
    }

    @Test
    fun `updateSyncPeriod updates repository`() = runTest {
        viewModel.syncPeriodFlow.test {
            assertEquals(SyncPeriod.NEVER, awaitItem())

            viewModel.updateSyncPeriod(SyncPeriod.TWELVE_HOURS)

            assertEquals(SyncPeriod.TWELVE_HOURS, awaitItem())
        }
    }
}
