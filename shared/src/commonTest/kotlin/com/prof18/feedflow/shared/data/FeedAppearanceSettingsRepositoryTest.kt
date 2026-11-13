package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedListSettingsState
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.shared.test.KoinTestBase
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedAppearanceSettingsRepositoryTest : KoinTestBase() {

    private val repository: FeedAppearanceSettingsRepository by inject()

    @Test
    fun `setFeedOrder emits new value on feedOrder flow`() = runTest {
        repository.feedOrder.test {
            assertEquals(FeedOrder.NEWEST_FIRST, awaitItem())

            repository.setFeedOrder(FeedOrder.OLDEST_FIRST)
            assertEquals(FeedOrder.OLDEST_FIRST, awaitItem())

            repository.setFeedOrder(FeedOrder.NEWEST_FIRST)
            assertEquals(FeedOrder.NEWEST_FIRST, awaitItem())
        }
    }

    @Test
    fun `swipe actions are disabled on a fresh install`() {
        val repository = FeedAppearanceSettingsRepository(MapSettings())
        val defaultState = FeedListSettingsState()

        assertEquals(
            SwipeActions(leftSwipeAction = SwipeActionType.NONE, rightSwipeAction = SwipeActionType.NONE),
            repository.swipeActions.value,
        )
        assertEquals(repository.swipeActions.value.leftSwipeAction, defaultState.leftSwipeActionType)
        assertEquals(repository.swipeActions.value.rightSwipeAction, defaultState.rightSwipeActionType)
    }

    @Test
    fun `big image layout enables adaptive grid by default`() {
        val repository = FeedAppearanceSettingsRepository(MapSettings())

        repository.setFeedLayout(FeedLayout.BIG_IMAGE)

        assertEquals(FeedLayout.BIG_IMAGE, repository.getFeedLayout())
        assertEquals(true, repository.getGridLayoutEnabled())
    }

    @Test
    fun `legacy grid layout migrates to big image with grid enabled`() {
        val settings = MapSettings()
        settings["FEED_LAYOUT"] = FeedLayout.GRID.name
        val repository = FeedAppearanceSettingsRepository(settings)

        assertEquals(FeedLayout.BIG_IMAGE, repository.getFeedLayout())
        assertEquals(true, repository.getGridLayoutEnabled())
    }

    @Test
    fun `date format choice is persisted`() {
        val settings = MapSettings()
        val repository = FeedAppearanceSettingsRepository(settings)

        repository.setDateFormat(DateFormat.DAY_MONTH_DOT)

        assertEquals(DateFormat.DAY_MONTH_DOT, FeedAppearanceSettingsRepository(settings).getDateFormat())
    }
}
