package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AddFeedViewModel internal constructor(
    private val categoryRepository: FeedCategoryRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
) : ViewModel() {

    private var feedUrl: String = ""
    private val feedAddedMutableState: MutableSharedFlow<FeedAddedState> = MutableSharedFlow()

    val feedAddedState = feedAddedMutableState.asSharedFlow()
    val categoriesState = categoryRepository.categoriesState

    init {
        viewModelScope.launch {
            categoryRepository.initCategories()
        }
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
                val categoryName = categoryRepository.getSelectedCategory()

                val feedAddedState = feedSourcesRepository.addFeedSource(feedUrl, categoryName)
                feedAddedMutableState.emit(feedAddedState)
                if (feedAddedState is FeedAddedState.FeedAdded) {
                    categoryRepository.initCategories()
                }
            }
        }
    }

    fun onExpandCategoryClick() {
        categoryRepository.onExpandCategoryClick()
    }

    fun addNewCategory(categoryName: CategoryName) {
        viewModelScope.launch {
            categoryRepository.addNewCategory(categoryName)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
        }
    }

    fun editCategory(categoryId: CategoryId, newName: CategoryName) {
        viewModelScope.launch {
            categoryRepository.updateCategoryName(categoryId, newName)
        }
    }
}
