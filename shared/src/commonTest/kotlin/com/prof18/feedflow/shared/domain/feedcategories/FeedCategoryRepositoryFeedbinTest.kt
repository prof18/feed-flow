package com.prof18.feedflow.shared.domain.feedcategories

import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.feedbin.configureFeedbinMocks
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedCategoryRepositoryFeedbinTest : KoinTestBase() {

    private val feedCategoryRepository: FeedCategoryRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + getFeedSyncTestModules(
            feedbinBaseURL = "https://api.feedbin.com/",
            feedbinConfig = {
                configureFeedbinMocks()
            },
        )

    fun setupFeedbinAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.FEEDBIN)
        settings.setSyncUsername("testuser")
        settings.setSyncPwd("testpassword")
    }

    @Test
    fun `deleteCategory should call feedbinRepository and delete category`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val category = FeedSourceCategory(id = "TestCategory", title = "TestCategory")
        databaseHelper.insertCategories(listOf(category))
        advanceUntilIdle()

        feedCategoryRepository.deleteCategory(category.id)
        advanceUntilIdle()

        val deletedCategory = databaseHelper.getFeedSourceCategory(category.id)
        assertNull(deletedCategory)
    }

    @Test
    fun `updateCategoryName should call feedbinRepository and update name`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val category = FeedSourceCategory(id = "TestCategory", title = "TestCategory")
        databaseHelper.insertCategories(listOf(category))
        advanceUntilIdle()

        val newName = CategoryName("UpdatedCategory")
        feedCategoryRepository.updateCategoryName(CategoryId(category.id), newName)
        advanceUntilIdle()

        val updatedCategory = databaseHelper.getFeedSourceCategory(newName.name)
        assertTrue(updatedCategory != null)
        assertEquals(updatedCategory.title, newName.name)
    }

    @Test
    fun `createCategory should create category with correct Feedbin ID format`() = runTest(testDispatcher) {
        setupFeedbinAccount()
        val categoryName = CategoryName("NewCategory")

        feedCategoryRepository.createCategory(categoryName)
        advanceUntilIdle()

        val createdCategory = databaseHelper.getFeedSourceCategory(categoryName.name)
        assertTrue(createdCategory != null)
        assertEquals(createdCategory.id, categoryName.name)
        assertEquals(createdCategory.title, categoryName.name)
    }
}
