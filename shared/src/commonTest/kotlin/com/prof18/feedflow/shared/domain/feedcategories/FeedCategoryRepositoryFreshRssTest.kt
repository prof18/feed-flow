package com.prof18.feedflow.shared.domain.feedcategories

import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocks
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.insertFeedSourceWithCategory
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedCategoryRepositoryFreshRssTest : KoinTestBase() {

    private val feedCategoryRepository: FeedCategoryRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val networkSettings: NetworkSettings by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            gReaderProvider = SyncAccounts.FRESH_RSS,
            gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
            gReaderConfig = {
                configureFreshRssMocks()
            },
        )

    @BeforeTest
    fun setupFreshRssAccount() {
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncUsername("testuser")
        networkSettings.setSyncPwd("testpassword")
        networkSettings.setSyncUrl("https://freshrss.example.com/api/greader.php/")
    }

    @Test
    fun `deleteCategory should call gReaderRepository and delete category`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "user/-/label/TestCategory", title = "TestCategory")
        databaseHelper.insertCategories(listOf(category))

        val feedSource = createFeedSource(
            id = "feed/242",
            title = "Test Feed",
            category = category,
        )
        databaseHelper.insertFeedSourceWithCategory(feedSource)
        advanceUntilIdle()

        feedCategoryRepository.deleteCategory(category.id)
        advanceUntilIdle()

        val deletedCategory = databaseHelper.getFeedSourceCategory(category.id)
        assertNull(deletedCategory, "Category should be deleted")
    }

    @Test
    fun `updateCategoryName should call gReaderRepository and update name`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "user/-/label/TestCategory", title = "TestCategory")
        databaseHelper.insertCategories(listOf(category))
        advanceUntilIdle()

        val newName = CategoryName("UpdatedCategory")
        feedCategoryRepository.updateCategoryName(CategoryId(category.id), newName)
        advanceUntilIdle()

        val newCategoryId = "user/-/label/UpdatedCategory"
        val updatedCategory = databaseHelper.getFeedSourceCategory(newCategoryId)
        assertTrue(updatedCategory != null, "Category should exist with new ID")
        assertEquals(updatedCategory!!.title, newName.name, "Category name should be updated")
    }

    @Test
    fun `createCategory should create category with correct GReader ID format`() = runTest(testDispatcher) {
        val categoryName = CategoryName("NewCategory")

        feedCategoryRepository.createCategory(categoryName)
        advanceUntilIdle()

        val expectedCategoryId = "user/-/label/${categoryName.name}"
        val createdCategory = databaseHelper.getFeedSourceCategory(expectedCategoryId)
        assertTrue(createdCategory != null, "Category should be created")
        assertEquals(createdCategory!!.id, expectedCategoryId, "Category ID should follow GReader format: user/-/label/{name}")
        assertEquals(createdCategory.title, categoryName.name, "Category title should match the name")
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com",
        fetchFailed = false,
        linkOpeningPreference = com.prof18.feedflow.core.model.LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
