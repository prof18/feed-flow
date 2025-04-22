package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
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
    private val accountsRepository: AccountsRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val databaseHelper: DatabaseHelper,
) : ViewModel() {
    val categoriesState = categoryUseCase.categoriesState
    private var originalFeedSource: FeedSource? = null

    private val feedUrlMutableState = MutableStateFlow("")
    val feedUrlState = feedUrlMutableState.asStateFlow()

    private val feedNameMutableState = MutableStateFlow("")
    val feedNameState = feedNameMutableState.asStateFlow()

    private val feedSourceSettingsMutableState = MutableStateFlow(FeedSourceSettings())
    val feedSourceSettingsState = feedSourceSettingsMutableState.asStateFlow()

    private val feedEditedMutableState: MutableSharedFlow<FeedEditedState> = MutableSharedFlow()
    val feedEditedState = feedEditedMutableState.asSharedFlow()

    private val showNotificationToggleMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showNotificationToggleState = showNotificationToggleMutableState.asStateFlow()

    init {
        viewModelScope.launch {
            showNotificationToggleMutableState.update { databaseHelper.areNotificationsEnabled() }
        }
    }

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
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(linkOpeningPreference = preference)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsHiddenFromTimeline(isHidden: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isHiddenFromTimeline = isHidden)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsPinned(isPinned: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isPinned = isPinned)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateIsNotificationEnabled(isNotificationEnabled: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isNotificationEnabled = isNotificationEnabled)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun loadFeedToEdit(feedSource: FeedSource) {
        originalFeedSource = feedSource

        viewModelScope.launch {
            feedUrlMutableState.update { feedSource.url }
            feedNameMutableState.update { feedSource.title }
            feedSourceSettingsMutableState.update {
                FeedSourceSettings(
                    linkOpeningPreference = feedSource.linkOpeningPreference,
                    isHiddenFromTimeline = feedSource.isHiddenFromTimeline,
                    isPinned = feedSource.isPinned,
                    isNotificationEnabled = feedSource.isNotificationEnabled,
                )
            }

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

    fun editCategory(categoryId: CategoryId, newName: CategoryName) {
        viewModelScope.launch {
            categoryUseCase.updateCategoryName(categoryId, newName)
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
                linkOpeningPreference = feedSourceSettingsState.value.linkOpeningPreference,
                isHiddenFromTimeline = feedSourceSettingsState.value.isHiddenFromTimeline,
                isPinned = feedSourceSettingsState.value.isPinned,
                isNotificationEnabled = feedSourceSettingsState.value.isNotificationEnabled,
            )

            if (newFeedSource != null && newFeedSource != originalFeedSource) {
                val state = feedSourcesRepository.editFeedSource(
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
