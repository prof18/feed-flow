package com.prof18.feedflow.shared.domain.feedcategories

import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedCategoryRepositoryLocalTest : KoinTestBase() {

    private val feedCategoryRepository: FeedCategoryRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> = TestModules.createTestModules()

    fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `getSelectedCategory should return null when no category is selected`() = runTest(testDispatcher) {
        setupLocalAccount()
        val selectedCategory = feedCategoryRepository.getSelectedCategory()
        assertNull(selectedCategory)
    }

    @Test
    fun `getSelectedCategory should return selected category when one is selected`() = runTest(testDispatcher) {
        setupLocalAccount()
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
        assertTrue(selectedCategory != null)
        assertEquals(selectedCategory.id, category.id)
        assertEquals(selectedCategory.title, category.title)

        initJob.cancel()
    }

    @Test
    fun `getSelectedCategory should return null when empty category is selected`() = runTest(testDispatcher) {
        setupLocalAccount()
        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        val emptyCategoryId = Long.MAX_VALUE.toString()
        feedCategoryRepository.onCategorySelected(CategoryId(emptyCategoryId))
        advanceUntilIdle()

        val selectedCategory = feedCategoryRepository.getSelectedCategory()
        assertNull(selectedCategory)

        initJob.cancel()
    }

    @Test
    fun `addNewCategory should set selectedCategoryName and create category`() = runTest(testDispatcher) {
        setupLocalAccount()
        val categoryName = CategoryName("NewCategory")

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        feedCategoryRepository.addNewCategory(categoryName)
        advanceUntilIdle()

        val expectedCategoryId = categoryName.name.hashCode().toString()
        val createdCategory = databaseHelper.getFeedSourceCategory(expectedCategoryId)
        assertTrue(createdCategory != null)
        assertEquals(createdCategory.id, expectedCategoryId)
        assertEquals(createdCategory.title, categoryName.name)

        advanceUntilIdle()
        val categoriesState = feedCategoryRepository.categoriesState.first()
        val categoryItem = categoriesState.categories.firstOrNull { it.id == expectedCategoryId }
        assertTrue(categoryItem != null)
        assertTrue(categoryItem.isSelected)

        initJob.cancel()
    }

    @Test
    fun `addNewCategory should not create duplicate category ignoring case and whitespace`() =
        runTest(testDispatcher) {
            setupLocalAccount()
            val existingCategory = FeedSourceCategory(id = "1", title = "Tech")
            databaseHelper.insertCategories(listOf(existingCategory))

            val initJob = launch {
                feedCategoryRepository.initCategories()
            }
            advanceUntilIdle()

            feedCategoryRepository.addNewCategory(CategoryName(" tech "))
            advanceUntilIdle()

            val categories = databaseHelper.getFeedSourceCategories()
            assertEquals(1, categories.size)
            assertEquals("Tech", categories.single().title)

            initJob.cancel()
        }

    @Test
    fun `createCategory should create category with hashCode ID for local account`() = runTest(testDispatcher) {
        setupLocalAccount()
        val categoryName = CategoryName("TestCategory")

        feedCategoryRepository.createCategory(categoryName)
        advanceUntilIdle()

        val expectedCategoryId = categoryName.name.hashCode().toString()
        val createdCategory = databaseHelper.getFeedSourceCategory(expectedCategoryId)
        assertTrue(createdCategory != null)
        assertEquals(createdCategory.id, expectedCategoryId)
        assertEquals(createdCategory.title, categoryName.name)
    }

    @Test
    fun `createCategory should not create duplicate category ignoring case and whitespace`() = runTest(testDispatcher) {
        setupLocalAccount()
        val category = FeedSourceCategory(id = "1", title = "Tech")
        databaseHelper.insertCategories(listOf(category))

        feedCategoryRepository.createCategory(CategoryName(" tech "))
        advanceUntilIdle()

        val categories = databaseHelper.getFeedSourceCategories()
        assertEquals(1, categories.size)
        assertEquals("Tech", categories.single().title)
    }

    @Test
    fun `updateCategoryName should trim category name before saving`() = runTest(testDispatcher) {
        setupLocalAccount()
        val category = FeedSourceCategory(id = "1", title = "Old")
        databaseHelper.insertCategories(listOf(category))

        feedCategoryRepository.updateCategoryName(CategoryId(category.id), CategoryName(" New "))
        advanceUntilIdle()

        val updatedCategory = databaseHelper.getFeedSourceCategory(category.id)
        assertEquals("New", updatedCategory?.title)
    }

    @Test
    fun `updateCategoryName should keep renamed selected category selected`() = runTest(testDispatcher) {
        setupLocalAccount()
        val category = FeedSourceCategory(id = "1", title = "Old")
        databaseHelper.insertCategories(listOf(category))
        feedCategoryRepository.setInitialSelection(CategoryName("Old"))

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        feedCategoryRepository.updateCategoryName(CategoryId(category.id), CategoryName(" New "))
        advanceUntilIdle()

        val updatedCategory = feedCategoryRepository.categoriesState.value.categories.first { it.id == category.id }
        assertEquals("New", updatedCategory.name)
        assertTrue(updatedCategory.isSelected)

        initJob.cancel()
    }

    @Test
    fun `updateCategoryName should not rename to duplicate category ignoring case and whitespace`() =
        runTest(testDispatcher) {
            setupLocalAccount()
            val categoryToRename = FeedSourceCategory(id = "1", title = "Old")
            val existingCategory = FeedSourceCategory(id = "2", title = "Tech")
            databaseHelper.insertCategories(listOf(categoryToRename, existingCategory))

            feedCategoryRepository.updateCategoryName(CategoryId(categoryToRename.id), CategoryName(" tech "))
            advanceUntilIdle()

            val unchangedCategory = databaseHelper.getFeedSourceCategory(categoryToRename.id)
            val duplicateTarget = databaseHelper.getFeedSourceCategory(existingCategory.id)
            assertEquals("Old", unchangedCategory?.title)
            assertEquals("Tech", duplicateTarget?.title)
        }

    @Test
    fun `category name validation should reject blank and duplicate names`() = runTest(testDispatcher) {
        setupLocalAccount()
        val categoryToRename = FeedSourceCategory(id = "1", title = "Old")
        val existingCategory = FeedSourceCategory(id = "2", title = "Tech")
        databaseHelper.insertCategories(listOf(categoryToRename, existingCategory))

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        assertEquals(
            CategoryNameValidationResult.DUPLICATE,
            feedCategoryRepository.validateCategoryName(CategoryId(categoryToRename.id), CategoryName(" tech ")),
        )
        assertEquals(
            CategoryNameValidationResult.BLANK,
            feedCategoryRepository.validateCategoryName(CategoryId(categoryToRename.id), CategoryName("  ")),
        )
        assertEquals(
            CategoryNameValidationResult.VALID,
            feedCategoryRepository.validateCategoryName(CategoryId(categoryToRename.id), CategoryName(" News ")),
        )

        initJob.cancel()
    }

    @Test
    fun `category name validation should support new category names`() = runTest(testDispatcher) {
        setupLocalAccount()
        val existingCategory = FeedSourceCategory(id = "1", title = "Tech")
        databaseHelper.insertCategories(listOf(existingCategory))

        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        assertEquals(
            CategoryNameValidationResult.DUPLICATE,
            feedCategoryRepository.validateCategoryName(null, CategoryName(" tech ")),
        )
        assertEquals(
            CategoryNameValidationResult.BLANK,
            feedCategoryRepository.validateCategoryName(null, CategoryName("  ")),
        )
        assertEquals(
            CategoryNameValidationResult.VALID,
            feedCategoryRepository.validateCategoryName(null, CategoryName(" News ")),
        )

        initJob.cancel()
    }

    @Test
    fun `setInitialSelection should set selectedCategoryName`() = runTest(testDispatcher) {
        setupLocalAccount()
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
        assertTrue(categoryItem != null)
        assertTrue(categoryItem.isSelected)

        initJob.cancel()
    }

    @Test
    fun `setInitialSelection with null should set empty category as selected`() = runTest(testDispatcher) {
        setupLocalAccount()
        feedCategoryRepository.setInitialSelection(null)
        val initJob = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        val categoriesState = feedCategoryRepository.categoriesState.first()
        val emptyCategory = categoriesState.categories.firstOrNull { it.id == Long.MAX_VALUE.toString() }
        assertTrue(emptyCategory != null)
        assertTrue(emptyCategory.isSelected)

        initJob.cancel()
    }

    @Test
    fun `initCategories should observe categories and update state with empty category`() = runTest(testDispatcher) {
        setupLocalAccount()
        val category1 = FeedSourceCategory(id = "1", title = "Category1")
        val category2 = FeedSourceCategory(id = "2", title = "Category2")
        databaseHelper.insertCategories(listOf(category1, category2))
        advanceUntilIdle()

        val job = launch {
            feedCategoryRepository.initCategories()
        }
        advanceUntilIdle()

        val categoriesState = feedCategoryRepository.categoriesState.first()
        assertEquals(3, categoriesState.categories.size)
        assertTrue(
            categoriesState.categories.any {
                it.id == Long.MAX_VALUE.toString()
            },
        )
        assertTrue(
            categoriesState.categories.any { it.name == "Category1" },
        )
        assertTrue(
            categoriesState.categories.any { it.name == "Category2" },
        )

        job.cancel()
    }

    @Test
    fun `onCategorySelected should update selectedCategoryName and mark category as selected`() =
        runTest(testDispatcher) {
            setupLocalAccount()
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
            val selectedCategoryItem = categoriesState.categories.firstOrNull {
                it.isSelected && it.id != Long.MAX_VALUE.toString()
            }
            assertTrue(selectedCategoryItem != null)
            assertEquals(selectedCategoryItem.id, category1.id)
            assertEquals(selectedCategoryItem.name, category1.title)

            val selectedCategory = feedCategoryRepository.getSelectedCategory()
            assertTrue(selectedCategory != null)
            assertEquals(selectedCategory.id, category1.id)

            initJob.cancel()
        }

    @Test
    fun `onCategorySelected should deselect other categories`() = runTest(testDispatcher) {
        setupLocalAccount()
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
        assertEquals(1, selectedCount)
        val selectedCategoryItem = categoriesState.categories.firstOrNull {
            it.isSelected && it.id != Long.MAX_VALUE.toString()
        }
        assertTrue(selectedCategoryItem != null)
        assertEquals(selectedCategoryItem.id, category2.id)

        initJob.cancel()
    }
}
