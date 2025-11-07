package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ChangeFeedCategoryViewModel internal constructor(
    private val categoryRepository: FeedCategoryRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    val categoriesState = categoryRepository.categoriesState

    private val categoryChangedMutableState: MutableSharedFlow<Unit> = MutableSharedFlow()
    val categoryChangedState = categoryChangedMutableState.asSharedFlow()

    private var feedSource: FeedSource? = null

    fun loadFeedSource(feedSource: FeedSource) {
        this.feedSource = feedSource
        viewModelScope.launch {
            val categoryName = feedSource.category?.title?.let { CategoryName(it) }
            categoryRepository.setInitialSelection(categoryName)
            categoryRepository.initCategories()
        }
    }

    fun onCategorySelected(categoryId: CategoryId) {
        categoryRepository.onCategorySelected(categoryId)
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

    fun saveCategory() {
        val currentFeedSource = feedSource ?: return
        viewModelScope.launch {
            val selectedCategory = categoryRepository.getSelectedCategory()
            val updatedFeedSource = currentFeedSource.copy(category = selectedCategory)

            feedSourcesRepository.editFeedSource(
                newFeedSource = updatedFeedSource,
                originalFeedSource = currentFeedSource,
            )
            feedStateRepository.getFeeds()
            categoryChangedMutableState.emit(Unit)
        }
    }
}
