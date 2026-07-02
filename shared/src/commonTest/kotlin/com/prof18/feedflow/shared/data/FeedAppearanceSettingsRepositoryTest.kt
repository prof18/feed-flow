package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
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
}
