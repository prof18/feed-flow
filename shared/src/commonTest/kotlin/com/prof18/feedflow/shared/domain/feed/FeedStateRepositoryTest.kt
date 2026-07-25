package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.buildFeedItem
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedStateRepositoryTest : KoinTestBase() {

    private val repository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val appearanceSettingsRepository: FeedAppearanceSettingsRepository by inject()

    @Test
    fun `pending count includes items missing from current snapshot`() = runTest(testDispatcher) {
        val source = createSource("source-1")
        insertSources(source)
        insertItem("existing", 1, source)
        repository.getFeeds()
        insertItem("new-item", 2, source)

        repository.refreshPendingNewArticlesCount()

        assertEquals(1, repository.pendingNewArticlesState.value)
    }

    @Test
    fun `pending count is zero when snapshot matches first page`() = runTest(testDispatcher) {
        val source = createSource("source-1")
        insertSources(source)
        insertItem("item-1", 1, source)
        repository.getFeeds()

        repository.refreshPendingNewArticlesCount()

        assertEquals(0, repository.pendingNewArticlesState.value)
    }

    @Test
    fun `pending count respects current source filter`() = runTest(testDispatcher) {
        val selectedSource = createSource("source-1")
        val otherSource = createSource("source-2")
        insertSources(selectedSource, otherSource)
        insertItem("selected-item", 1, selectedSource)
        repository.updateFeedSourceFilter(selectedSource.id)
        insertItem("other-item", 2, otherSource)

        repository.refreshPendingNewArticlesCount()

        assertEquals(0, repository.pendingNewArticlesState.value)
    }

    @Test
    fun `pending count includes new items for oldest first order`() = runTest(testDispatcher) {
        appearanceSettingsRepository.setFeedOrder(FeedOrder.OLDEST_FIRST)
        val source = createSource("source-1")
        insertSources(source)
        insertItem("existing", 1, source)
        repository.getFeeds()
        insertItem("new-item", 2, source)

        repository.refreshPendingNewArticlesCount()

        assertEquals(1, repository.pendingNewArticlesState.value)
    }

    @Test
    fun `publishing feeds resets pending count`() = runTest(testDispatcher) {
        val source = createSource("source-1")
        insertSources(source)
        insertItem("existing", 1, source)
        repository.getFeeds()
        insertItem("new-item", 2, source)
        repository.refreshPendingNewArticlesCount()
        assertEquals(1, repository.pendingNewArticlesState.value)

        repository.getFeeds()

        assertEquals(0, repository.pendingNewArticlesState.value)
    }

    private suspend fun insertSources(vararg sources: FeedSource) {
        databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
    }

    private suspend fun insertItem(id: String, timestamp: Long, source: FeedSource) {
        databaseHelper.insertFeedItems(
            listOf(buildFeedItem(id = id, title = id, pubDateMillis = timestamp, source = source)),
            lastSyncTimestamp = 0,
        )
    }

    private fun createSource(id: String): FeedSource = FeedSourceGenerator.feedSource(
        id = id,
        url = "https://example.com/$id.xml",
        title = id,
        category = null,
    )
}
