package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRequest
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryService
import com.prof18.feedflow.shared.domain.recovery.RecoveryTicket
import com.prof18.feedflow.shared.domain.recovery.WebsiteVerificationFailure
import com.prof18.feedflow.shared.utils.sanitizeUrl
import kotlinx.coroutines.CancellationException
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
    private val feedStateRepository: FeedStateRepository,
    private val recoveryService: FeedRecoveryService,
    private val recoveryRepository: FeedRecoveryRepository,
    private val coordinator: FeedRecoveryCoordinator,
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

    private val feedDeletedMutableState: MutableSharedFlow<Unit> = MutableSharedFlow()
    val feedDeletedState = feedDeletedMutableState.asSharedFlow()

    private val showNotificationToggleMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showNotificationToggleState = showNotificationToggleMutableState.asStateFlow()

    val verificationAvailable: Boolean = coordinator.available
    private val verificationRequiredMutableState = MutableStateFlow(false)
    val verificationRequiredState = verificationRequiredMutableState.asStateFlow()
    private var pendingVerification: FeedRecoveryRequest.Edit? = null
    private var verificationTicket: RecoveryTicket? = null

    init {
        viewModelScope.launch {
            showNotificationToggleMutableState.update { databaseHelper.areNotificationsEnabled() }
        }
    }

    fun canEditUrl(): Boolean =
        accountsRepository.getCurrentSyncAccount() != SyncAccounts.FRESH_RSS &&
            accountsRepository.getCurrentSyncAccount() != SyncAccounts.MINIFLUX &&
            accountsRepository.getCurrentSyncAccount() != SyncAccounts.FEEDBIN &&
            accountsRepository.getCurrentSyncAccount() != SyncAccounts.BAZQUX

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        if (feedUrlTextFieldValue != feedUrlMutableState.value) clearPendingVerification()
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

    fun updateArticleOpenMode(articleOpenMode: ArticleOpenMode) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(articleOpenMode = articleOpenMode)
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

    fun updateIsHideImagesEnabled(isHideImagesEnabled: Boolean) {
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isHideImagesEnabled = isHideImagesEnabled)
        }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun loadFeedToEdit(feedSource: FeedSource) {
        originalFeedSource = feedSource

        viewModelScope.launch {
            val currentFeedSource = databaseHelper.getFeedSource(feedSource.id) ?: feedSource
            originalFeedSource = currentFeedSource

            feedUrlMutableState.update { currentFeedSource.url }
            feedNameMutableState.update { currentFeedSource.title }
            feedSourceSettingsMutableState.update {
                FeedSourceSettings(
                    articleOpenMode = currentFeedSource.articleOpenMode,
                    isHiddenFromTimeline = currentFeedSource.isHiddenFromTimeline,
                    isPinned = currentFeedSource.isPinned,
                    isNotificationEnabled = currentFeedSource.isNotificationEnabled,
                    isHideImagesEnabled = currentFeedSource.isHideImagesEnabled,
                )
            }

            val categoryName = currentFeedSource.category?.title?.let { CategoryName(it) }
            categoryUseCase.setInitialSelection(categoryName)
            categoryUseCase.initCategories()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryUseCase.deleteCategory(categoryId)
        }
    }

    fun onCategorySelected(categoryId: CategoryId) {
        categoryUseCase.onCategorySelected(categoryId)
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

    fun validateCategoryName(categoryId: CategoryId?, newName: CategoryName): CategoryNameValidationResult =
        categoryUseCase.validateCategoryName(categoryId, newName)

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
                articleOpenMode = feedSourceSettingsState.value.articleOpenMode,
                isHiddenFromTimeline = feedSourceSettingsState.value.isHiddenFromTimeline,
                isPinned = feedSourceSettingsState.value.isPinned,
                isNotificationEnabled = feedSourceSettingsState.value.isNotificationEnabled,
                isHideImagesEnabled = feedSourceSettingsState.value.isHideImagesEnabled,
            )

            if (newFeedSource != null && newFeedSource != originalFeedSource) {
                val original = originalFeedSource
                val state = feedSourcesRepository.editFeedSource(
                    newFeedSource = newFeedSource,
                    originalFeedSource = original,
                )
                val verificationUrl = (state as? FeedEditedState.Error.FetchFailed)?.verificationUrl
                if (verificationUrl != null && original != null && newFeedSource.url == feedUrlState.value) {
                    pendingVerification = FeedRecoveryRequest.Edit(
                        original = original,
                        replacement = newFeedSource.copy(url = state.challengedUrl ?: sanitizeUrl(newFeedSource.url)),
                        verificationUrl = verificationUrl,
                        retryAfter = state.retryAfter,
                    )
                    verificationRequiredMutableState.value = true
                }
                feedEditedMutableState.emit(state)
            } else {
                feedEditedMutableState.emit(FeedEditedState.FeedEdited(feedNameState.value))
            }
        }
    }

    fun verifyWebsite() {
        val request = pendingVerification ?: return
        verificationTicket?.revoke()
        val ticket = recoveryRepository.issueTicket()
        verificationTicket = ticket
        viewModelScope.launch {
            try {
                val source = recoveryService.recover(request, ticket)
                pendingVerification = null
                verificationRequiredMutableState.value = false
                if (source.title != request.original.title) feedStateRepository.updateCurrentFilterName(source.title)
                originalFeedSource = source
                feedEditedMutableState.emit(FeedEditedState.FeedEdited(source.title))
            } catch (e: CancellationException) {
                throw e
            } catch (e: WebsiteVerificationFailure) {
                coordinator.reportFailure(e.reason)
                feedEditedMutableState.emit(FeedEditedState.Error.InvalidUrl)
            } catch (_: Exception) {
                feedEditedMutableState.emit(FeedEditedState.Error.InvalidUrl)
            }
        }
    }

    fun cancelRecovery() {
        verificationTicket?.revoke()
        verificationTicket = null
        verificationRequiredMutableState.value = false
    }

    private fun clearPendingVerification() {
        pendingVerification = null
        cancelRecovery()
    }

    fun deleteFeed() {
        val feedToDelete = originalFeedSource ?: return
        viewModelScope.launch {
            feedSourcesRepository.deleteFeed(feedToDelete)
            feedStateRepository.getFeeds()
            feedDeletedMutableState.emit(Unit)
        }
    }
}
