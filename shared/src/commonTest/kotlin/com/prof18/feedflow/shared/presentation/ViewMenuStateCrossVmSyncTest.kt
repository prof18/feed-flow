package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewMenuStateCrossVmSyncTest : KoinTestBase() {

    private val homeViewModel: HomeViewModel by inject()
    private val feedListSettingsViewModel: FeedListSettingsViewModel by inject()
    private val menuBarViewModel: MenuBarViewModel by inject()
    private val readingBehaviorViewModel: ReadingBehaviorSettingsViewModel by inject()

    @Test
    fun `updateFeedOrder via HomeViewModel syncs to MenuBarViewModel`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            homeViewModel.updateFeedOrder(FeedOrder.OLDEST_FIRST)
            advanceUntilIdle()

            assertEquals(FeedOrder.OLDEST_FIRST, homeViewModel.viewMenuState.value.feedOrder)
            assertEquals(FeedOrder.OLDEST_FIRST, menuBarViewModel.state.value.feedOrder)
        }

    @Test
    fun `updateShowReadArticlesTimeline via HomeViewModel syncs to MenuBarViewModel`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            homeViewModel.updateShowReadArticlesTimeline(true)
            advanceUntilIdle()

            assertTrue(homeViewModel.viewMenuState.value.showReadArticlesTimeline)
            assertTrue(menuBarViewModel.state.value.isShowReadItemsEnabled)
        }

    @Test
    fun `updateShowReadItemsOnTimeline via ReadingBehaviorSettingsViewModel syncs to other VMs`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            readingBehaviorViewModel.updateShowReadItemsOnTimeline(true)
            advanceUntilIdle()

            assertTrue(homeViewModel.viewMenuState.value.showReadArticlesTimeline)
            assertTrue(menuBarViewModel.state.value.isShowReadItemsEnabled)
        }

    @Test
    fun `updateFeedOrder via FeedListSettingsViewModel syncs to HomeViewModel and MenuBarViewModel`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            feedListSettingsViewModel.updateFeedOrder(FeedOrder.OLDEST_FIRST)
            advanceUntilIdle()

            assertEquals(FeedOrder.OLDEST_FIRST, homeViewModel.viewMenuState.value.feedOrder)
            assertEquals(FeedOrder.OLDEST_FIRST, menuBarViewModel.state.value.feedOrder)
        }
}
