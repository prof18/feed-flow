package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRepositoryShowReadArticlesTest : KoinTestBase() {

    private val repository: SettingsRepository by inject()

    @Test
    fun `setShowReadArticlesTimeline emits new value on showReadArticlesTimelineFlow`() = runTest {
        repository.showReadArticlesTimelineFlow.test {
            assertFalse(awaitItem())

            repository.setShowReadArticlesTimeline(true)
            assertTrue(awaitItem())

            repository.setShowReadArticlesTimeline(false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `setMarkFeedAsReadWhenScrolling emits new value on markFeedAsReadWhenScrollingFlow`() = runTest {
        repository.markFeedAsReadWhenScrollingFlow.test {
            assertTrue(awaitItem())

            repository.setMarkFeedAsReadWhenScrolling(false)
            assertFalse(awaitItem())

            repository.setMarkFeedAsReadWhenScrolling(true)
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `setHideReadItems emits new value on hideReadItemsFlow`() = runTest {
        repository.hideReadItemsFlow.test {
            assertFalse(awaitItem())

            repository.setHideReadItems(true)
            assertTrue(awaitItem())

            repository.setHideReadItems(false)
            assertFalse(awaitItem())
        }
    }
}
