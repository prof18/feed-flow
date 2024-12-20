package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryUseCase
import com.prof18.feedflow.shared.domain.model.AddFeedResponse
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.utils.sanitizeUrl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AddFeedViewModel internal constructor(
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val categoryUseCase: FeedCategoryUseCase,
) : ViewModel() {

    private var feedUrl: String = ""
    private val feedAddedMutableState: MutableSharedFlow<FeedAddedState> = MutableSharedFlow()

    val feedAddedState = feedAddedMutableState.asSharedFlow()
    val categoriesState = categoryUseCase.categoriesState

    init {
        viewModelScope.launch {
            categoryUseCase.initCategories()
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
                val url = sanitizeUrl(feedUrl)
                val categoryName = categoryUseCase.getSelectedCategory()

                when (val feedResponse = feedRetrieverRepository.fetchSingleFeed(url, categoryName)) {
                    is AddFeedResponse.FeedFound -> {
                        feedRetrieverRepository.addFeedSource(feedResponse)
                        feedAddedMutableState.emit(
                            FeedAddedState.FeedAdded(
                                feedResponse.parsedFeedSource.title,
                            ),
                        )
                        categoryUseCase.initCategories()
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
        categoryUseCase.onExpandCategoryClick()
    }

    fun addNewCategory(categoryName: CategoryName) {
        viewModelScope.launch {
            categoryUseCase.addNewCategory(categoryName)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryUseCase.deleteCategory(categoryId)
        }
    }
}
