package com.prof18.feedflow.shared

import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import kotlinx.coroutines.test.runTest
import org.koin.core.component.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedFlowDatabaseTest : KoinTestBase() {
    private val databaseHelper by inject<DatabaseHelper>()

    @Test
    fun `insertCategories stores categories in database`() = runTest {
        val categories = listOf(
            FeedSourceCategory(id = "1", title = "Category 1"),
            FeedSourceCategory(id = "2", title = "Category 2"),
        )

        databaseHelper.insertCategories(categories)

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(2, savedCategories.size)
        assertEquals("Category 1", savedCategories.find { it.id == "1" }?.title)
        assertEquals("Category 2", savedCategories.find { it.id == "2" }?.title)
    }

    @Test
    fun `categories default to alphabetical order when positions match`() = runTest {
        databaseHelper.insertCategories(
            listOf(
                FeedSourceCategory(id = "beta", title = "Beta"),
                FeedSourceCategory(id = "alpha", title = "Alpha"),
            ),
        )

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(listOf("Alpha", "Beta"), savedCategories.map { it.title })
    }

    @Test
    fun `updateCategoryPositions persists category order`() = runTest {
        databaseHelper.insertCategories(
            listOf(
                FeedSourceCategory(id = "alpha", title = "Alpha"),
                FeedSourceCategory(id = "beta", title = "Beta"),
                FeedSourceCategory(id = "gamma", title = "Gamma"),
            ),
        )

        databaseHelper.updateCategoryPositions(listOf("gamma" to 0, "alpha" to 1, "beta" to 2))

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(listOf("Gamma", "Alpha", "Beta"), savedCategories.map { it.title })
        assertEquals(listOf(0, 1, 2), savedCategories.map { it.position })
    }

    @Test
    fun `updateFeedSourcePositions persists feed source order`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
                createParsedFeedSource(id = "gamma", title = "Gamma"),
            ),
        )

        databaseHelper.updateFeedSourcePositions(listOf("gamma", "alpha", "beta"))

        val savedFeedSources = databaseHelper.getFeedSources()
        assertEquals(listOf("Gamma", "Alpha", "Beta"), savedFeedSources.map { it.title })
        assertEquals(listOf(0, 1, 2), savedFeedSources.map { it.position })
    }

    @Test
    fun `insertFeedSource preserves existing feed source position`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
            ),
        )
        databaseHelper.updateFeedSourcePositions(listOf("beta", "alpha"))

        databaseHelper.insertFeedSource(listOf(createParsedFeedSource(id = "beta", title = "Beta Updated")))

        val savedFeedSources = databaseHelper.getFeedSources()
        assertEquals(listOf("Beta Updated", "Alpha"), savedFeedSources.map { it.title })
        assertEquals(0, savedFeedSources.first { it.id == "beta" }.position)
    }

    @Test
    fun `insertFeedItems stores feed item content`() = runTest {
        val feedItem = FeedItemGenerator.feedItem(
            id = "content-item",
            content = "<article>Stored feed content</article>",
        )
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(
                    id = feedItem.feedSource.id,
                    title = feedItem.feedSource.title,
                ),
            ),
        )

        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        assertEquals("<article>Stored feed content</article>", databaseHelper.getFeedItemContent("content-item"))
    }

    @Test
    fun `insertCategories preserves existing category position`() = runTest {
        databaseHelper.insertCategories(
            listOf(
                FeedSourceCategory(id = "alpha", title = "Alpha"),
                FeedSourceCategory(id = "beta", title = "Beta"),
            ),
        )
        databaseHelper.updateCategoryPositions(listOf("beta" to 0, "alpha" to 1))

        databaseHelper.insertCategories(listOf(FeedSourceCategory(id = "beta", title = "Beta Updated")))

        val savedCategories = databaseHelper.getFeedSourceCategories()
        assertEquals(listOf("Beta Updated", "Alpha"), savedCategories.map { it.title })
        assertEquals(0, savedCategories.first { it.id == "beta" }.position)
    }

    @Test
    fun `getPinnedFeedSourceIds returns pinned ids sorted by position`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
                createParsedFeedSource(id = "gamma", title = "Gamma"),
            ),
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            preference = LinkOpeningPreference.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "beta",
            preference = LinkOpeningPreference.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "gamma",
            preference = LinkOpeningPreference.DEFAULT,
            isHidden = false,
            isPinned = false,
            isNotificationEnabled = false,
        )
        databaseHelper.updatePinnedPositions(listOf("beta", "alpha"))

        val pinnedIds = databaseHelper.getPinnedFeedSourceIds()

        assertEquals(listOf("beta", "alpha"), pinnedIds)
    }

    @Test
    fun `insertFeedSourcePreference preserves pinned position`() = runTest {
        databaseHelper.insertFeedSource(
            listOf(
                createParsedFeedSource(id = "alpha", title = "Alpha"),
                createParsedFeedSource(id = "beta", title = "Beta"),
            ),
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            preference = LinkOpeningPreference.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
        )
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "beta",
            preference = LinkOpeningPreference.DEFAULT,
            isHidden = false,
            isPinned = true,
            isNotificationEnabled = false,
        )
        databaseHelper.updatePinnedPositions(listOf("beta", "alpha"))

        databaseHelper.insertFeedSourcePreference(
            feedSourceId = "alpha",
            preference = LinkOpeningPreference.DEFAULT,
            isHidden = true,
            isPinned = true,
            isNotificationEnabled = true,
        )

        val updatedSource = databaseHelper.getFeedSource("alpha")
        assertEquals(1, updatedSource?.pinnedPosition)
        assertEquals(true, updatedSource?.isHiddenFromTimeline)
        assertEquals(true, updatedSource?.isNotificationEnabled)
    }

    private fun createParsedFeedSource(id: String, title: String) = ParsedFeedSource(
        id = id,
        url = "https://example.com/$id.xml",
        title = title,
        category = null,
        logoUrl = null,
        websiteUrl = null,
    )
}
