package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.test.KoinTestBase
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
}
