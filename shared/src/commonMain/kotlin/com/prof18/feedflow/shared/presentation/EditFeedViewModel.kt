package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditFeedViewModel internal constructor(
    private val categoryUseCase: FeedCategoryRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val accountsRepository: AccountsRepository,
) : ViewModel() {
    val categoriesState = categoryUseCase.categoriesState
    private var originalFeedSource: FeedSource? = null

    private val feedUrlMutableState = MutableStateFlow("")
    val feedUrlState = feedUrlMutableState.asStateFlow()

    private val feedNameMutableState = MutableStateFlow("")
    val feedNameState = feedNameMutableState.asStateFlow()

    private val linkOpeningPreferenceMutableState = MutableStateFlow(LinkOpeningPreference.DEFAULT)
    val linkOpeningPreferenceState = linkOpeningPreferenceMutableState.asStateFlow()

    private val isHiddenFromTimelineMutableState = MutableStateFlow(false)
    val isHiddenFromTimelineState = isHiddenFromTimelineMutableState.asStateFlow()

    private val isPinnedMutableState = MutableStateFlow(false)
    val isPinnedState = isPinnedMutableState.asStateFlow()

    private val feedEditedMutableState: MutableSharedFlow<FeedEditedState> = MutableSharedFlow()
    val feedEditedState = feedEditedMutableState.asSharedFlow()

    fun canEditUrl(): Boolean =
        accountsRepository.getCurrentSyncAccount() != SyncAccounts.FRESH_RSS

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

    fun updateLinkOpeningPreference(preference: LinkOpeningPreference) {
        linkOpeningPreferenceMutableState.update { preference }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsHiddenFromTimeline(isHidden: Boolean) {
        isHiddenFromTimelineMutableState.update { isHidden }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsPinned(isPinned: Boolean) {
        isPinnedMutableState.update { isPinned }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun loadFeedToEdit(feedSource: FeedSource) {
        originalFeedSource = feedSource

        viewModelScope.launch {
            feedUrlMutableState.update { feedSource.url }
            feedNameMutableState.update { feedSource.title }
            linkOpeningPreferenceMutableState.update { feedSource.linkOpeningPreference }
            isHiddenFromTimelineMutableState.update { feedSource.isHiddenFromTimeline }
            isPinnedMutableState.update { feedSource.isPinned }

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
                linkOpeningPreference = linkOpeningPreferenceState.value,
                isHiddenFromTimeline = isHiddenFromTimelineState.value,
                isPinned = isPinnedState.value,
            )

            if (newFeedSource != null && newFeedSource != originalFeedSource) {
                val state = feedRetrieverRepository.editFeedSource(
                    newFeedSource = newFeedSource,
                    originalFeedSource = originalFeedSource,
                )
                feedEditedMutableState.emit(state)
            } else {
                feedEditedMutableState.emit(FeedEditedState.FeedEdited(feedNameState.value))
            }
        }
    }
}
