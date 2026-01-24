package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChangeFeedCategoryViewModelTest : KoinTestBase() {

    private val viewModel: ChangeFeedCategoryViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `saveCategory updates feed source and emits state`() = runTest(testDispatcher) {
        val categoryA = FeedSourceCategory(id = "category-a", title = "News")
        val categoryB = FeedSourceCategory(id = "category-b", title = "Tech")
        databaseHelper.insertCategories(listOf(categoryA, categoryB))

        val feedSource = createFeedSource(
            id = "source-1",
            title = "Feed One",
            category = categoryA,
        )
        databaseHelper.insertFeedSource(listOf(feedSource.toParsedFeedSource()))

        viewModel.loadFeedSource(feedSource)
        advanceUntilIdle()

        viewModel.onCategorySelected(CategoryId(categoryB.id))

        viewModel.categoryChangedState.test {
            viewModel.saveCategory()
            assertEquals(Unit, awaitItem())
        }

        advanceUntilIdle()

        val updatedFeedSource = databaseHelper.getFeedSource(feedSource.id)
        assertNotNull(updatedFeedSource)
        assertEquals(categoryB.id, updatedFeedSource.category?.id)
    }

    @Test
    fun `moveFeedSourcesToCategory updates feed sources`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "category-1", title = "Moved")
        databaseHelper.insertCategories(listOf(category))

        val feedSourceA = createFeedSource(
            id = "source-a",
            title = "Feed A",
        )
        val feedSourceB = createFeedSource(
            id = "source-b",
            title = "Feed B",
        )
        databaseHelper.insertFeedSource(
            listOf(
                feedSourceA.toParsedFeedSource(),
                feedSourceB.toParsedFeedSource(),
            ),
        )

        viewModel.moveFeedSourcesToCategory(listOf(feedSourceA, feedSourceB), category)
        advanceUntilIdle()

        val updatedSources = databaseHelper.getFeedSources().associateBy { it.id }
        assertEquals(category.id, updatedSources.getValue(feedSourceA.id).category?.id)
        assertEquals(category.id, updatedSources.getValue(feedSourceB.id).category?.id)
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSourceGenerator.feedSourceArb.next().copy(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = false,
    )
}
