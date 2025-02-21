package com.prof18.feedflow.shared.domain.feedcategories

import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryWithUnreadCount
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.greader.GReaderRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FeedCategoryRepository(
    private val databaseHelper: DatabaseHelper,
    private val accountsRepository: AccountsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val gReaderRepository: GReaderRepository,
) {
    private val categoriesMutableState: MutableStateFlow<CategoriesState> = MutableStateFlow(CategoriesState())
    val categoriesState = categoriesMutableState
    private var selectedCategoryName: CategoryName? = null

//    private val errorMutableState: MutableSharedFlow<ErrorState> = MutableSharedFlow()
//    val errorState = errorMutableState.asSharedFlow()

    fun onExpandCategoryClick() {
        categoriesMutableState.update { state ->
            state.copy(isExpanded = state.isExpanded.not())
        }
    }

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
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.deleteCategory(categoryId)
                    .fold(
                        onSuccess = {
                            gReaderRepository.fetchFeedSourcesAndCategories()
                                .onErrorSuspend {
                                    // TODO: handle error?
//                                    errorMutableState.emit(SyncError)
                                }
                        },
                        onFailure = {
                            // TODO: handle error?
//                            errorMutableState.emit(SyncError)
                        },
                    )
            }

            else -> {
                databaseHelper.deleteCategory(categoryId)
                feedSyncRepository.deleteFeedSourceCategory(categoryId)
            }
        }
    }


    suspend fun initCategories(selectedCategoryName: CategoryName? = null) {
        this.selectedCategoryName = selectedCategoryName
        observeCategories().collect { categories ->
            val categoriesWithEmpty = listOf(getEmptyCategory()) + categories.map { feedSourceCategory ->
                feedSourceCategory.toCategoryItem()
            }
            categoriesMutableState.update {
                it.copy(
                    header = this.selectedCategoryName?.name,
                    categories = categoriesWithEmpty,
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
            SyncAccounts.FRESH_RSS -> {
                "user/-/label/${categoryName.name}"
            }

            else -> categoryName.name.hashCode().toString()
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

    private fun onCategorySelected(categoryId: CategoryId) {
        categoriesMutableState.update { state ->
            var selectedCategoryName: String? = null
            val updatedCategories = state.categories.map { categoryItem ->
                if (categoryId.value == categoryItem.id) {
                    selectedCategoryName = categoryItem.name
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
                header = selectedCategoryName,
                isExpanded = false,
                categories = updatedCategories,
            )
        }
    }

    private fun FeedSourceCategory.toCategoryItem(): CategoriesState.CategoryItem =
        CategoriesState.CategoryItem(
            id = id,
            name = title,
            isSelected = selectedCategoryName?.name == title,
            onClick = { categoryId ->
                onCategorySelected(categoryId)
            },
        )

    private fun getEmptyCategory() = CategoriesState.CategoryItem(
        id = EMPTY_CATEGORY_ID,
        name = null,
        isSelected = selectedCategoryName?.name == null,
        onClick = { categoryId ->
            onCategorySelected(categoryId)
        },
    )

    private companion object {
        // To maintain backward compatibility
        private const val EMPTY_CATEGORY_ID = Long.MAX_VALUE.toString()
    }
}
