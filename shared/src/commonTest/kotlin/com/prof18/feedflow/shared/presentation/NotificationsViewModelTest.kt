package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NotificationMode
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationsViewModelTest : KoinTestBase() {

    private val viewModel: NotificationsViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

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

            // The initial database load can still land after the update; wait for the settled state.
            var state = awaitItem()
            while (state.feedSources.isEmpty() || !state.feedSources.all { it.isEnabled }) {
                state = awaitItem()
            }
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
    fun `article mode persists independently of feed switches and keeps the fresh default`() = runTest(testDispatcher) {
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource("id1", "url1", "title1", null, null, null),
                ParsedFeedSource("id2", "url2", "title2", null, null, null),
            ),
        )
        val first: NotificationsViewModel = getKoin().get()
        assertEquals(NotificationMode.FEED_SOURCE, first.notificationSettingState.value.notificationMode)
        first.updateNotificationStatus(true, "id1")
        first.updateNotificationMode(NotificationMode.ARTICLE)

        val recreated: NotificationsViewModel = getKoin().get()
        val state = recreated.notificationSettingState.value
        assertEquals(NotificationMode.ARTICLE, state.notificationMode)
        assertTrue(state.feedSources.first { it.feedSourceId == "id1" }.isEnabled)
        assertFalse(state.feedSources.first { it.feedSourceId == "id2" }.isEnabled)
        assertFalse(state.isEnabledForAll)
    }

    @Test
    fun `feed groups are sorted by category with uncategorized last`() = runTest(testDispatcher) {
        insertGroupedFeeds()
        advanceUntilIdle()

        val groups = viewModel.notificationSettingState.value.feedGroups
        assertEquals(listOf("android", "Kotlin", null), groups.map { it.categoryTitle })
    }

    @Test
    fun `feeds inside a group are sorted by title ignoring case`() = runTest(testDispatcher) {
        insertGroupedFeeds()
        advanceUntilIdle()

        val groups = viewModel.notificationSettingState.value.feedGroups
        assertEquals(listOf("Android Police", "android weekly"), groups[0].feedSources.map { it.feedSourceTitle })
        assertEquals(listOf("kotlin blog", "Kotlin Weekly"), groups[1].feedSources.map { it.feedSourceTitle })
        assertEquals(listOf("alpha news", "Zeta"), groups[2].feedSources.map { it.feedSourceTitle })
    }

    @Test
    fun `feed preferences carry their category title`() = runTest(testDispatcher) {
        insertGroupedFeeds()
        advanceUntilIdle()

        val feedSources = viewModel.notificationSettingState.value.feedSources
        assertEquals("android", feedSources.first { it.feedSourceId == "a1" }.categoryTitle)
        assertEquals("Kotlin", feedSources.first { it.feedSourceId == "k1" }.categoryTitle)
        assertNull(feedSources.first { it.feedSourceId == "u1" }.categoryTitle)
    }

    @Test
    fun `search filters feed groups by title and keeps every feed source`() = runTest(testDispatcher) {
        insertGroupedFeeds()
        advanceUntilIdle()

        viewModel.updateSearchQuery("  andr ")
        advanceUntilIdle()

        val state = viewModel.notificationSettingState.value
        assertEquals("  andr ", state.searchQuery)
        assertEquals(listOf("android"), state.feedGroups.map { it.categoryTitle })
        assertEquals(
            listOf("Android Police", "android weekly"),
            state.feedGroups.single().feedSources.map { it.feedSourceTitle },
        )
        assertEquals(6, state.feedSources.size)
    }

    @Test
    fun `an empty search brings every feed back`() = runTest(testDispatcher) {
        insertGroupedFeeds()
        advanceUntilIdle()
        viewModel.updateSearchQuery("andr")
        advanceUntilIdle()

        viewModel.updateSearchQuery("")
        advanceUntilIdle()

        val state = viewModel.notificationSettingState.value
        assertEquals(6, state.feedGroups.sumOf { it.feedSources.size })
        assertEquals(3, state.feedGroups.size)
    }

    private suspend fun insertGroupedFeeds() {
        val kotlin = FeedSourceCategory(id = "cat-kotlin", title = "Kotlin")
        val android = FeedSourceCategory(id = "cat-android", title = "android")
        databaseHelper.insertCategories(listOf(kotlin, android))
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource("k1", "url-k1", "Kotlin Weekly", kotlin, null, null),
                ParsedFeedSource("k2", "url-k2", "kotlin blog", kotlin, null, null),
                ParsedFeedSource("a1", "url-a1", "android weekly", android, null, null),
                ParsedFeedSource("a2", "url-a2", "Android Police", android, null, null),
                ParsedFeedSource("u1", "url-u1", "Zeta", null, null, null),
                ParsedFeedSource("u2", "url-u2", "alpha news", null, null, null),
            ),
        )
    }

    @Test
    fun `turning a feed on at Never turns refresh on and announces it`() = runTest(testDispatcher) {
        insertSingleFeed()
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)

        viewModel.backgroundRefreshTurnedOn.test {
            viewModel.updateNotificationStatus(true, "id1")
            advanceUntilIdle()

            assertEquals(SyncPeriod.ONE_HOUR, awaitItem())
            assertEquals(SyncPeriod.ONE_HOUR, viewModel.syncPeriodFlow.value)
        }
    }

    @Test
    fun `turning all feeds on at Never turns refresh on and announces it`() = runTest(testDispatcher) {
        insertSingleFeed()
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)

        viewModel.backgroundRefreshTurnedOn.test {
            viewModel.updateAllNotificationStatus(true)
            advanceUntilIdle()

            assertEquals(SyncPeriod.ONE_HOUR, awaitItem())
            assertEquals(SyncPeriod.ONE_HOUR, viewModel.syncPeriodFlow.value)
        }
    }

    @Test
    fun `turning a feed on while refresh is on keeps the period silently`() = runTest(testDispatcher) {
        insertSingleFeed()
        settingsRepository.setSyncPeriod(SyncPeriod.SIX_HOURS)

        viewModel.backgroundRefreshTurnedOn.test {
            viewModel.updateNotificationStatus(true, "id1")
            advanceUntilIdle()

            expectNoEvents()
            assertEquals(SyncPeriod.SIX_HOURS, viewModel.syncPeriodFlow.value)
        }
    }

    @Test
    fun `turning a feed off keeps the period`() = runTest(testDispatcher) {
        insertSingleFeed()
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)

        viewModel.backgroundRefreshTurnedOn.test {
            viewModel.updateNotificationStatus(true, "id1")
            advanceUntilIdle()
            awaitItem()

            viewModel.updateNotificationStatus(false, "id1")
            advanceUntilIdle()

            expectNoEvents()
            assertEquals(SyncPeriod.ONE_HOUR, viewModel.syncPeriodFlow.value)
        }
    }

    @Test
    fun `updateSyncPeriod saves the chosen period without an announcement`() = runTest(testDispatcher) {
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)

        viewModel.backgroundRefreshTurnedOn.test {
            viewModel.updateSyncPeriod(SyncPeriod.SIX_HOURS)
            advanceUntilIdle()

            expectNoEvents()
            assertEquals(SyncPeriod.SIX_HOURS, viewModel.syncPeriodFlow.value)
            assertEquals(SyncPeriod.SIX_HOURS, settingsRepository.getSyncPeriod())
        }
    }

    @Test
    fun `updateSyncPeriod can turn background refresh off`() = runTest(testDispatcher) {
        settingsRepository.setSyncPeriod(SyncPeriod.ONE_HOUR)

        viewModel.updateSyncPeriod(SyncPeriod.NEVER)
        advanceUntilIdle()

        assertEquals(SyncPeriod.NEVER, settingsRepository.getSyncPeriod())
    }

    private suspend fun insertSingleFeed() {
        databaseHelper.insertFeedSource(
            listOf(ParsedFeedSource("id1", "url1", "title1", null, null, null)),
        )
    }
}
