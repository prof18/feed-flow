package com.prof18.feedflow.shared.domain.feedcategories

import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.shared.domain.feed.manager.FeedManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FeedCategoryUseCase(
    private val feedManagerRepository: FeedManagerRepository,
) {
    private val categoriesMutableState: MutableStateFlow<CategoriesState> = MutableStateFlow(CategoriesState())
    val categoriesState = categoriesMutableState
    private var selectedCategoryName: CategoryName? = null

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
        feedManagerRepository.createCategory(categoryName)
    }

    suspend fun deleteCategory(categoryId: String) {
        feedManagerRepository.deleteCategory(categoryId)
    }

    suspend fun initCategories(selectedCategoryName: CategoryName? = null) {
        this.selectedCategoryName = selectedCategoryName
        feedManagerRepository.observeCategories().collect { categories ->
            val categoriesWithEmpty = listOf(getEmptyCategory()) + categories.map { feedSourceCategory ->
                feedSourceCategory.toCategoryItem()
            }
            categoriesMutableState.update {
                it.copy(
                    header = selectedCategoryName?.name,
                    categories = categoriesWithEmpty,
                )
            }
        }
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
