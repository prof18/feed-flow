package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.core.model.ReadArticlesVisibility
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `auto hide is effective only when scroll marking is disabled`() = runTest {
        repository.setMarkFeedAsReadWhenScrolling(true)
        repository.setHideReadItems(false)
        assertFalse(repository.getEffectiveHideReadItems())

        repository.setHideReadItems(true)
        assertFalse(repository.getEffectiveHideReadItems())
        assertTrue(repository.getHideReadItems())

        repository.setMarkFeedAsReadWhenScrolling(false)
        assertTrue(repository.getEffectiveHideReadItems())
        assertTrue(repository.getHideReadItems())

        repository.setHideReadItems(false)
        assertFalse(repository.getEffectiveHideReadItems())

        repository.setHideReadItems(true)
        repository.setMarkFeedAsReadWhenScrolling(true)
        assertFalse(repository.getEffectiveHideReadItems())
        assertTrue(repository.getHideReadItems())
    }

    @Test
    fun `show read wins over auto hide`() = runTest {
        repository.setShowReadArticlesTimeline(true)
        repository.setHideReadItems(true)
        repository.setMarkFeedAsReadWhenScrolling(false)

        assertFalse(repository.getEffectiveHideReadItems())
        assertEquals(ReadArticlesVisibility.KEEP_IN_LIST, repository.getReadArticlesVisibility())
    }

    @Test
    fun `auto hide without show read and scroll marking reads as hide right away`() = runTest {
        repository.setShowReadArticlesTimeline(false)
        repository.setHideReadItems(true)
        repository.setMarkFeedAsReadWhenScrolling(false)

        assertEquals(ReadArticlesVisibility.HIDE_RIGHT_AWAY, repository.getReadArticlesVisibility())
    }

    @Test
    fun `scroll marking turns a saved hide right away into hide on refresh and keeps the stored value`() = runTest {
        repository.setShowReadArticlesTimeline(false)
        repository.setHideReadItems(true)
        repository.setMarkFeedAsReadWhenScrolling(true)

        assertEquals(ReadArticlesVisibility.HIDE_ON_REFRESH, repository.getReadArticlesVisibility())
        assertTrue(repository.getHideReadItems())
    }

    @Test
    fun `everything off reads as hide on refresh`() = runTest {
        repository.setShowReadArticlesTimeline(false)
        repository.setHideReadItems(false)
        repository.setMarkFeedAsReadWhenScrolling(false)

        assertEquals(ReadArticlesVisibility.HIDE_ON_REFRESH, repository.getReadArticlesVisibility())
    }

    @Test
    fun `setReadArticlesVisibility writes the hide values`() = runTest {
        repository.setReadArticlesVisibility(ReadArticlesVisibility.HIDE_ON_REFRESH)
        assertFalse(repository.getShowReadArticlesTimeline())
        assertFalse(repository.getHideReadItems())

        repository.setReadArticlesVisibility(ReadArticlesVisibility.HIDE_RIGHT_AWAY)
        assertFalse(repository.getShowReadArticlesTimeline())
        assertTrue(repository.getHideReadItems())
    }

    @Test
    fun `keep in list keeps the saved hide mode for when show read is turned off`() = runTest {
        repository.setMarkFeedAsReadWhenScrolling(false)
        repository.setReadArticlesVisibility(ReadArticlesVisibility.HIDE_RIGHT_AWAY)

        repository.setReadArticlesVisibility(ReadArticlesVisibility.KEEP_IN_LIST)
        assertTrue(repository.getShowReadArticlesTimeline())
        assertTrue(repository.getHideReadItems())

        repository.setShowReadArticlesTimeline(false)
        assertEquals(ReadArticlesVisibility.HIDE_RIGHT_AWAY, repository.getReadArticlesVisibility())
    }
}
