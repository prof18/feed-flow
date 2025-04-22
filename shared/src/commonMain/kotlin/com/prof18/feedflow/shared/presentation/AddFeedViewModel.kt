package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFeedViewModel internal constructor(
    private val categoryRepository: FeedCategoryRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val databaseHelper: DatabaseHelper,
) : ViewModel() {

    private var feedUrl: String = ""
    private val feedAddedMutableState: MutableSharedFlow<FeedAddedState> = MutableSharedFlow()

    val feedAddedState = feedAddedMutableState.asSharedFlow()
    val categoriesState = categoryRepository.categoriesState

    private val showNotificationToggleMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showNotificationToggleState = showNotificationToggleMutableState.asStateFlow()

    private val isNotificationEnabledMutableStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isNotificationEnabledState = isNotificationEnabledMutableStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.initCategories()
        }

        viewModelScope.launch {
            showNotificationToggleMutableState.update { databaseHelper.areNotificationsEnabled() }
        }
    }

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrl = feedUrlTextFieldValue
        viewModelScope.launch {
            feedAddedMutableState.emit(FeedAddedState.FeedNotAdded)
        }
    }

    fun updateNotificationStatus(status: Boolean) {
        isNotificationEnabledMutableStateFlow.update { status }
    }

    fun addFeed() {
        viewModelScope.launch {
            feedAddedMutableState.emit(FeedAddedState.Loading)
            if (feedUrl.isNotEmpty()) {
                val categoryName = categoryRepository.getSelectedCategory()

                val feedAddedState = feedSourcesRepository.addFeedSource(
                    feedUrl = feedUrl,
                    categoryName = categoryName,
                    isNotificationEnabled = isNotificationEnabledMutableStateFlow.value,
                )
                feedAddedMutableState.emit(feedAddedState)
                if (feedAddedState is FeedAddedState.FeedAdded) {
                    isNotificationEnabledMutableStateFlow.update { false }
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
