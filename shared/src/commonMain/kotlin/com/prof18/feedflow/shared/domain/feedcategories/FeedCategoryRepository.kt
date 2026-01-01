package com.prof18.feedflow.shared.domain.feedcategories

import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryWithUnreadCount
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.presentation.model.SyncError
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FeedCategoryRepository(
    private val databaseHelper: DatabaseHelper,
    private val accountsRepository: AccountsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val gReaderRepository: GReaderRepository,
    private val feedStateRepository: FeedStateRepository,
    private val feedbinRepository: FeedbinRepository,
) {
    private val categoriesMutableState: MutableStateFlow<CategoriesState> = MutableStateFlow(CategoriesState())
    val categoriesState = categoriesMutableState
    private var selectedCategoryName: CategoryName? = null

    fun getSelectedCategory(): FeedSourceCategory? {
        val category = categoriesState.value.categories.firstOrNull { it.isSelected }
        if (category == null || category.id == EMPTY_CATEGORY_ID) {
            return null
        }
        return FeedSourceCategory(
            id = category.id,
            title = requireNotNull(category.name),
        )
    }

    suspend fun addNewCategory(categoryName: CategoryName) {
        selectedCategoryName = categoryName
        createCategory(categoryName)
    }

    suspend fun deleteCategory(categoryId: String) {
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX -> {
                gReaderRepository.deleteCategory(categoryId)
                    .fold(
                        onSuccess = {
                            gReaderRepository.fetchFeedSourcesAndCategories()
                                .onErrorSuspend {
                                    feedStateRepository.emitErrorState(
                                        SyncError(errorCode = FeedSyncError.FetchFeedSourcesAndCategoriesFailed),
                                    )
                                }
                        },
                        onFailure = {
                            feedStateRepository.emitErrorState(
                                SyncError(errorCode = FeedSyncError.DeleteCategoryFailed),
                            )
                        },
                    )
            }

            SyncAccounts.FEEDBIN -> {
                feedbinRepository.deleteCategory(categoryId)
                    .fold(
                        onSuccess = {
                            feedbinRepository.fetchFeedSourcesAndCategories()
                                .onErrorSuspend {
                                    feedStateRepository.emitErrorState(
                                        SyncError(errorCode = FeedSyncError.FetchFeedSourcesAndCategoriesFailed),
                                    )
                                }
                        },
                        onFailure = {
                            feedStateRepository.emitErrorState(
                                SyncError(errorCode = FeedSyncError.DeleteCategoryFailed),
                            )
                        },
                    )
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                databaseHelper.deleteCategory(categoryId)
                feedSyncRepository.deleteFeedSourceCategory(categoryId)
            }
        }
    }

    suspend fun updateCategoryName(categoryId: CategoryId, newName: CategoryName) {
        // If the category being edited is currently selected, update selectedCategoryName
        val selectedCategory = categoriesState.value.categories.firstOrNull { it.isSelected }
        if (selectedCategory?.id == categoryId.value) {
            selectedCategoryName = newName
        }

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX -> {
                gReaderRepository.editCategoryName(categoryId, newName)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.EditCategoryNameFailed))
                    }
            }

            SyncAccounts.FEEDBIN -> {
                feedbinRepository.editCategoryName(categoryId, newName)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.EditCategoryNameFailed))
                    }
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                databaseHelper.updateCategoryName(categoryId.value, newName.name)
                val category = FeedSourceCategory(
                    id = categoryId.value,
                    title = newName.name,
                )
                feedSyncRepository.updateCategory(category)
            }
        }
    }

    fun setInitialSelection(categoryName: CategoryName?) {
        this.selectedCategoryName = categoryName
    }

    suspend fun initCategories() {
        observeCategories().collect { categories ->
            val categoriesWithEmpty = listOf(getEmptyCategory()) + categories.map { feedSourceCategory ->
                feedSourceCategory.toCategoryItem()
            }
            categoriesMutableState.update {
                it.copy(
                    categories = categoriesWithEmpty.toPersistentList(),
                )
            }
        }
    }

    fun observeCategories(): Flow<List<FeedSourceCategory>> =
        databaseHelper.observeFeedSourceCategories()

    fun observeCategoriesWithUnreadCount(): Flow<List<CategoryWithUnreadCount>> =
        databaseHelper.observeCategoriesWithUnreadCount()

    suspend fun createCategory(categoryName: CategoryName) {
        val categoryId = when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX -> gReaderRepository.buildCategoryId(categoryName)
            SyncAccounts.FEEDBIN -> feedbinRepository.buildCategoryId(categoryName)
            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> categoryName.name.hashCode().toString()
        }

        val category = FeedSourceCategory(
            id = categoryId,
            title = categoryName.name,
        )
        databaseHelper.insertCategories(
            listOf(category),
        )

        feedSyncRepository.insertFeedSourceCategories(listOf(category))
    }

    fun onCategorySelected(categoryId: CategoryId) {
        val selectedCategory = categoriesMutableState.value.categories.firstOrNull { it.id == categoryId.value }
        selectedCategoryName = selectedCategory?.name?.let { CategoryName(it) }

        categoriesMutableState.update { state ->
            val updatedCategories = state.categories.map { categoryItem ->
                if (categoryId.value == categoryItem.id) {
                    categoryItem.copy(
                        isSelected = true,
                    )
                } else {
                    categoryItem.copy(
                        isSelected = false,
                    )
                }
            }
            state.copy(
                categories = updatedCategories.toPersistentList(),
            )
        }
    }

    private fun FeedSourceCategory.toCategoryItem(): CategoriesState.CategoryItem =
        CategoriesState.CategoryItem(
            id = id,
            name = title,
            isSelected = selectedCategoryName?.name == title,
        )

    private fun getEmptyCategory() = CategoriesState.CategoryItem(
        id = EMPTY_CATEGORY_ID,
        name = null,
        isSelected = selectedCategoryName?.name == null,
    )

    private companion object {
        // To maintain backward compatibility
        private const val EMPTY_CATEGORY_ID = Long.MAX_VALUE.toString()
    }
}
