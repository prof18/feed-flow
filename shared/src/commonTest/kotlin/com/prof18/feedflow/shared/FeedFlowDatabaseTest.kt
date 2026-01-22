package com.prof18.feedflow.shared

import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
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
}
