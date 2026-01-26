package com.prof18.feedflow.shared.domain.feedcategories

import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedCategoryRepositoryLocalTest : KoinTestBase() {

    private val feedCategoryRepository: FeedCategoryRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val networkSettings: NetworkSettings by inject()

    override fun getTestModules(): List<Module> = TestModules.createTestModules()

    @BeforeTest
    fun setupLocalAccount() {
        networkSettings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `getSelectedCategory should return null when no category is selected`() = runTest(testDispatcher) {
        val selectedCategory = feedCategoryRepository.getSelectedCategory()
        assertNull(selectedCategory, "Should return null when no category is selected")
    }

    @Test
    fun `getSelectedCategory should return selected category when one is selected`() = runTest(testDispatcher) {
        val category = FeedSourceCategory(id = "123", title = "TestCategory")
        databaseHelper.insertCategories(listOf(category))
        advanceUntilIdle()

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        feedCategoryRepository.onCategorySelected(CategoryId(category.id))
        advanceUntilIdle()

        val selectedCategory = feedCategoryRepository.getSelectedCategory()
        assertTrue(selectedCategory != null, "Should return selected category")
        assertEquals(selectedCategory!!.id, category.id, "Selected category ID should match")
        assertEquals(selectedCategory.title, category.title, "Selected category title should match")

        initJob.cancel()
    }

    @Test
    fun `getSelectedCategory should return null when empty category is selected`() = runTest(testDispatcher) {
        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        val emptyCategoryId = Long.MAX_VALUE.toString()
        feedCategoryRepository.onCategorySelected(CategoryId(emptyCategoryId))
        advanceUntilIdle()

        val selectedCategory = feedCategoryRepository.getSelectedCategory()
        assertNull(selectedCategory, "Should return null when empty category is selected")

        initJob.cancel()
    }

    @Test
    fun `addNewCategory should set selectedCategoryName and create category`() = runTest(testDispatcher) {
        val categoryName = CategoryName("NewCategory")

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        feedCategoryRepository.addNewCategory(categoryName)
        advanceUntilIdle()

        val expectedCategoryId = categoryName.name.hashCode().toString()
        val createdCategory = databaseHelper.getFeedSourceCategory(expectedCategoryId)
        assertTrue(createdCategory != null, "Category should be created")
        assertEquals(createdCategory!!.id, expectedCategoryId, "Category ID should be hashCode for local account")
        assertEquals(createdCategory.title, categoryName.name, "Category title should match the name")

        advanceUntilIdle()
        val categoriesState = feedCategoryRepository.categoriesState.first()
        val categoryItem = categoriesState.categories.firstOrNull { it.id == expectedCategoryId }
        assertTrue(categoryItem != null, "Category should be in state")
        assertTrue(categoryItem!!.isSelected, "Category should be selected after addNewCategory")

        initJob.cancel()
    }

    @Test
    fun `createCategory should create category with hashCode ID for local account`() = runTest(testDispatcher) {
        val categoryName = CategoryName("TestCategory")

        feedCategoryRepository.createCategory(categoryName)
        advanceUntilIdle()

        val expectedCategoryId = categoryName.name.hashCode().toString()
        val createdCategory = databaseHelper.getFeedSourceCategory(expectedCategoryId)
        assertTrue(createdCategory != null, "Category should be created")
        assertEquals(createdCategory!!.id, expectedCategoryId, "Category ID should be hashCode for local account")
        assertEquals(createdCategory.title, categoryName.name, "Category title should match the name")
    }

    @Test
    fun `setInitialSelection should set selectedCategoryName`() = runTest(testDispatcher) {
        val categoryName = CategoryName("TestCategory")
        val category = FeedSourceCategory(
            id = categoryName.name.hashCode().toString(),
            title = categoryName.name,
        )
        databaseHelper.insertCategories(listOf(category))
        advanceUntilIdle()

        feedCategoryRepository.setInitialSelection(categoryName)
        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        val categoriesState = feedCategoryRepository.categoriesState.first()
        val categoryItem = categoriesState.categories.firstOrNull { it.name == categoryName.name }
        assertTrue(categoryItem != null, "Category should exist in state")
        assertTrue(categoryItem!!.isSelected, "Category should be selected when setInitialSelection is called")

        initJob.cancel()
    }

    @Test
    fun `setInitialSelection with null should set empty category as selected`() = runTest(testDispatcher) {
        feedCategoryRepository.setInitialSelection(null)
        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        val categoriesState = feedCategoryRepository.categoriesState.first()
        val emptyCategory = categoriesState.categories.firstOrNull { it.id == Long.MAX_VALUE.toString() }
        assertTrue(emptyCategory != null, "Empty category should exist")
        assertTrue(emptyCategory!!.isSelected, "Empty category should be selected when setInitialSelection is null")

        initJob.cancel()
    }

    @Test
    fun `initCategories should observe categories and update state with empty category`() = runTest(testDispatcher) {
        val category1 = FeedSourceCategory(id = "1", title = "Category1")
        val category2 = FeedSourceCategory(id = "2", title = "Category2")
        databaseHelper.insertCategories(listOf(category1, category2))
        advanceUntilIdle()

        val job = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        val categoriesState = feedCategoryRepository.categoriesState.first()
        assertEquals(3, categoriesState.categories.size, "Should have empty category + 2 categories")
        assertTrue(
            categoriesState.categories.any { it.id == Long.MAX_VALUE.toString() },
            "Should include empty category",
        )
        assertTrue(
            categoriesState.categories.any { it.name == "Category1" },
            "Should include Category1",
        )
        assertTrue(
            categoriesState.categories.any { it.name == "Category2" },
            "Should include Category2",
        )

        job.cancel()
    }

    @Test
    fun `onCategorySelected should update selectedCategoryName and mark category as selected`() = runTest(testDispatcher) {
        val category1 = FeedSourceCategory(id = "1", title = "Category1")
        val category2 = FeedSourceCategory(id = "2", title = "Category2")
        databaseHelper.insertCategories(listOf(category1, category2))
        advanceUntilIdle()

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        feedCategoryRepository.onCategorySelected(CategoryId(category1.id))
        advanceUntilIdle()

        val categoriesState = feedCategoryRepository.categoriesState.first()
        val selectedCategoryItem = categoriesState.categories.firstOrNull { it.isSelected && it.id != Long.MAX_VALUE.toString() }
        assertTrue(selectedCategoryItem != null, "A category should be selected")
        assertEquals(selectedCategoryItem!!.id, category1.id, "Category1 should be selected")
        assertEquals(selectedCategoryItem.name, category1.title, "Selected category name should match")

        val selectedCategory = feedCategoryRepository.getSelectedCategory()
        assertTrue(selectedCategory != null, "getSelectedCategory should return the selected category")
        assertEquals(selectedCategory!!.id, category1.id, "Selected category ID should match")

        initJob.cancel()
    }

    @Test
    fun `onCategorySelected should deselect other categories`() = runTest(testDispatcher) {
        val category1 = FeedSourceCategory(id = "1", title = "Category1")
        val category2 = FeedSourceCategory(id = "2", title = "Category2")
        databaseHelper.insertCategories(listOf(category1, category2))
        advanceUntilIdle()

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        feedCategoryRepository.onCategorySelected(CategoryId(category1.id))
        advanceUntilIdle()

        feedCategoryRepository.onCategorySelected(CategoryId(category2.id))
        advanceUntilIdle()

        val categoriesState = feedCategoryRepository.categoriesState.first()
        val selectedCount = categoriesState.categories.count { it.isSelected }
        assertEquals(1, selectedCount, "Only one category should be selected")
        val selectedCategoryItem = categoriesState.categories.firstOrNull { it.isSelected && it.id != Long.MAX_VALUE.toString() }
        assertTrue(selectedCategoryItem != null, "A category should be selected")
        assertEquals(selectedCategoryItem!!.id, category2.id, "Category2 should be selected")

        initJob.cancel()
    }
}
