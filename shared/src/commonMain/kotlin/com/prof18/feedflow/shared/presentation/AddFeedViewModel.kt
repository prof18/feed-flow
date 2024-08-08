package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.shared.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.model.AddFeedResponse
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.utils.sanitizeUrl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFeedViewModel internal constructor(
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val feedManagerRepository: FeedManagerRepository,
) : ViewModel() {

    private var feedUrl: String = ""
    private var newCategoryName: CategoryName? = null
    private val feedAddedMutableState: MutableSharedFlow<FeedAddedState> = MutableSharedFlow()
    private val categoriesMutableState: MutableStateFlow<CategoriesState> = MutableStateFlow(CategoriesState())

    val feedAddedState = feedAddedMutableState.asSharedFlow()
    val categoriesState = categoriesMutableState.asStateFlow()

    init {
        initCategories()
    }

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrl = feedUrlTextFieldValue
        viewModelScope.launch {
            feedAddedMutableState.emit(FeedAddedState.FeedNotAdded)
        }
    }

    fun addFeed() {
        viewModelScope.launch {
            feedAddedMutableState.emit(FeedAddedState.Loading)
            if (feedUrl.isNotEmpty()) {
                val url = sanitizeUrl(feedUrl)
                val categoryName = getSelectedCategory()

                when (val feedResponse = feedRetrieverRepository.fetchSingleFeed(url, categoryName)) {
                    is AddFeedResponse.FeedFound -> {
                        feedRetrieverRepository.addFeedSource(feedResponse)
                        feedAddedMutableState.emit(
                            FeedAddedState.FeedAdded(
                                feedResponse.parsedFeedSource.title,
                            ),
                        )
                        initCategories()
                    }

                    AddFeedResponse.EmptyFeed -> {
                        feedAddedMutableState.emit(FeedAddedState.Error.InvalidTitleLink)
                    }

                    AddFeedResponse.NotRssFeed -> {
                        feedAddedMutableState.emit(FeedAddedState.Error.InvalidUrl)
                    }
                }
            }
        }
    }

    fun onExpandCategoryClick() {
        categoriesMutableState.update { state ->
            state.copy(isExpanded = state.isExpanded.not())
        }
    }

    fun addNewCategory(categoryName: CategoryName) {
        viewModelScope.launch {
            newCategoryName = categoryName
            feedManagerRepository.createCategory(categoryName)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            feedManagerRepository.deleteCategory(categoryId)
        }
    }

    private fun getSelectedCategory(): FeedSourceCategory? {
        val category = categoriesState.value.categories.firstOrNull { it.isSelected }
        if (category == null || category.id == EMPTY_CATEGORY_ID) {
            return null
        }
        return FeedSourceCategory(
            id = category.id,
            title = requireNotNull(category.name),
        )
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
            isSelected = newCategoryName?.name == title,
            onClick = { categoryId ->
                onCategorySelected(categoryId)
            },
        )

    private fun initCategories() {
        viewModelScope.launch {
            feedManagerRepository.observeCategories().collect { categories ->
                val categoriesWithEmpty = listOf(getEmptyCategory()) + categories.map { feedSourceCategory ->
                    feedSourceCategory.toCategoryItem()
                }
                categoriesMutableState.update {
                    it.copy(
                        header = newCategoryName?.name,
                        categories = categoriesWithEmpty,
                    )
                }
                newCategoryName = null
            }
        }
    }

    private fun getEmptyCategory() = CategoriesState.CategoryItem(
        id = EMPTY_CATEGORY_ID,
        name = null,
        isSelected = newCategoryName?.name == null,
        onClick = { categoryId ->
            onCategorySelected(categoryId)
        },
    )

    private companion object {
        // To maintain backward compatibility
        private const val EMPTY_CATEGORY_ID = Long.MAX_VALUE.toString()
    }
}
