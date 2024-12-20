package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryUseCase
import com.prof18.feedflow.shared.domain.model.AddFeedResponse
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.utils.sanitizeUrl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditFeedViewModel internal constructor(
    private val categoryUseCase: FeedCategoryUseCase,
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : ViewModel() {
    val categoriesState = categoryUseCase.categoriesState
    private var originalFeedSource: FeedSource? = null

    private val feedUrlMutableState = MutableStateFlow("")
    val feedUrlState = feedUrlMutableState.asStateFlow()

    private val feedNameMutableState = MutableStateFlow("")
    val feedNameState = feedNameMutableState.asStateFlow()

    private val feedEditedMutableState: MutableSharedFlow<FeedEditedState> = MutableSharedFlow()
    val feedEditedState = feedEditedMutableState.asSharedFlow()

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrlMutableState.update { feedUrlTextFieldValue }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateFeedNameTextFieldValue(feedNameTextFieldValue: String) {
        feedNameMutableState.update { feedNameTextFieldValue }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun loadFeedToEdit(feedSource: FeedSource) {
        originalFeedSource = feedSource

        viewModelScope.launch {
            feedUrlMutableState.update { feedSource.url }
            feedNameMutableState.update { feedSource.title }

            val categoryName = feedSource.category?.title?.let { CategoryName(it) }
            categoryUseCase.initCategories(categoryName)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryUseCase.deleteCategory(categoryId)
        }
    }

    fun onExpandCategoryClick() {
        categoryUseCase.onExpandCategoryClick()
    }

    fun addNewCategory(categoryName: CategoryName) {
        viewModelScope.launch {
            categoryUseCase.addNewCategory(categoryName)
        }
    }

    fun editFeed() {
        viewModelScope.launch {
            if (feedUrlState.value.isEmpty() || feedNameState.value.isEmpty()) {
                feedEditedMutableState.emit(FeedEditedState.Error.InvalidTitleLink)
                return@launch
            }

            feedEditedMutableState.emit(FeedEditedState.Loading)

            val selectedCategory = categoryUseCase.getSelectedCategory()
            val newFeedSource = originalFeedSource?.copy(
                url = feedUrlState.value,
                title = feedNameState.value,
                category = selectedCategory,
            )

            if (newFeedSource != null && newFeedSource != originalFeedSource) {
                val newUrl = sanitizeUrl(feedUrlState.value)
                val previousUrl = originalFeedSource?.url

                if (newUrl != previousUrl) {
                    when (val response = feedRetrieverRepository.fetchSingleFeed(newUrl, selectedCategory)) {
                        is AddFeedResponse.FeedFound -> {
                            feedManagerRepository.updateFeedSource(
                                newFeedSource.copy(
                                    url = response.parsedFeedSource.url,
                                ),
                            )
                            feedEditedMutableState.emit(FeedEditedState.FeedEdited(feedNameState.value))
                        }

                        AddFeedResponse.EmptyFeed -> {
                            feedEditedMutableState.emit(FeedEditedState.Error.InvalidTitleLink)
                        }

                        AddFeedResponse.NotRssFeed -> {
                            feedEditedMutableState.emit(FeedEditedState.Error.InvalidUrl)
                        }
                    }
                } else {
                    feedManagerRepository.updateFeedSource(newFeedSource)
                    feedEditedMutableState.emit(FeedEditedState.FeedEdited(feedNameState.value))
                }
            } else {
                feedEditedMutableState.emit(FeedEditedState.FeedEdited(feedNameState.value))
            }
        }
    }
}
