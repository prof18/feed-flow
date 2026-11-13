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
import com.prof18.feedflow.core.model.YouTubeOptionsState
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feed.YouTubeChannelUrl
import com.prof18.feedflow.shared.domain.feed.runUserWrite
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class EditFeedViewModel internal constructor(
    private val categoryUseCase: FeedCategoryRepository,
    private val accountsRepository: AccountsRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedStateRepository: FeedStateRepository,
    private val recoveryService: FeedRecoveryService,
    private val recoveryRepository: FeedRecoveryRepository,
    private val coordinator: FeedRecoveryCoordinator,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val categoriesState = categoryUseCase.categoriesState
    private val originalFeedSourceState = MutableStateFlow<FeedSource?>(null)
    private var originalFeedSource: FeedSource?
        get() = originalFeedSourceState.value
        set(value) {
            originalFeedSourceState.value = value
        }
    private val preferenceWriteMutex = Mutex()

    private val feedUrlMutableState = MutableStateFlow("")
    val feedUrlState = feedUrlMutableState.asStateFlow()

    private val feedNameMutableState = MutableStateFlow("")
    val feedNameState = feedNameMutableState.asStateFlow()

    private val feedSourceSettingsMutableState = MutableStateFlow(FeedSourceSettings())
    val feedSourceSettingsState = feedSourceSettingsMutableState.asStateFlow()

    /** True while the name, the URL or the category differs from the stored feed: the values Save covers. */
    val hasUnsavedChangesState: StateFlow<Boolean> = combine(
        feedUrlState,
        feedNameState,
        categoriesState,
        originalFeedSourceState,
    ) { url, name, categories, original ->
        if (original == null) return@combine false
        val categoryChanged = categories.categories.isNotEmpty() &&
            categoryUseCase.getSelectedCategory()?.id != original.category?.id
        url != original.url || name != original.title || categoryChanged
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val feedEditedMutableState: MutableSharedFlow<FeedEditedState> = MutableSharedFlow()
    val feedEditedState = feedEditedMutableState.asSharedFlow()

    private val feedDeletedMutableState: MutableSharedFlow<Unit> = MutableSharedFlow()
    val feedDeletedState = feedDeletedMutableState.asSharedFlow()

    private val youTubeOptionsMutableState = MutableStateFlow(YouTubeOptionsState())
    val youTubeOptionsState = youTubeOptionsMutableState.asStateFlow()

    private val showNotificationToggleMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showNotificationToggleState = showNotificationToggleMutableState.asStateFlow()

    val markReadWhenScrollingEnabledState: StateFlow<Boolean> = settingsRepository.markFeedAsReadWhenScrollingFlow

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
        refreshYouTubeOptions(feedUrlTextFieldValue)
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateHideShortsStatus(status: Boolean) {
        if (status == youTubeOptionsMutableState.value.hideShorts) return
        val channelId = youTubeChannelId(feedUrlMutableState.value) ?: return
        val url = if (status) YouTubeChannelUrl.noShortsFeedUrl(channelId) else YouTubeChannelUrl.feedUrl(channelId)
        updateFeedUrlTextFieldValue(url)
    }

    fun updateFeedNameTextFieldValue(feedNameTextFieldValue: String) {
        feedNameMutableState.update { feedNameTextFieldValue }
        viewModelScope.launch {
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun updateArticleOpenMode(articleOpenMode: ArticleOpenMode) {
        if (articleOpenMode == feedSourceSettingsState.value.articleOpenMode) return
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(articleOpenMode = articleOpenMode)
        }
        launchPreferenceWrite()
    }

    fun updateIsHiddenFromTimeline(isHidden: Boolean) {
        if (isHidden == feedSourceSettingsState.value.isHiddenFromTimeline) return
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isHiddenFromTimeline = isHidden)
        }
        launchPreferenceWrite()
    }

    fun updateIsPinned(isPinned: Boolean) {
        if (isPinned == feedSourceSettingsState.value.isPinned) return
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isPinned = isPinned)
        }
        launchPreferenceWrite()
    }

    fun updateIsNotificationEnabled(isNotificationEnabled: Boolean) {
        if (isNotificationEnabled == feedSourceSettingsState.value.isNotificationEnabled) return
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isNotificationEnabled = isNotificationEnabled)
        }
        launchPreferenceWrite()
    }

    fun updateIsHideImagesEnabled(isHideImagesEnabled: Boolean) {
        if (isHideImagesEnabled == feedSourceSettingsState.value.isHideImagesEnabled) return
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isHideImagesEnabled = isHideImagesEnabled)
        }
        launchPreferenceWrite()
    }

    fun updateIsKeepUnreadOnScrollEnabled(isKeepUnreadOnScrollEnabled: Boolean) {
        if (isKeepUnreadOnScrollEnabled == feedSourceSettingsState.value.isKeepUnreadOnScrollEnabled) return
        feedSourceSettingsMutableState.update { oldValue ->
            oldValue.copy(isKeepUnreadOnScrollEnabled = isKeepUnreadOnScrollEnabled)
        }
        val feedSourceId = originalFeedSource?.id
        val enabled = feedSourceSettingsState.value.isKeepUnreadOnScrollEnabled
        viewModelScope.launch {
            if (feedSourceId != null) persistKeepUnreadOnScroll(feedSourceId, enabled)
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    fun loadFeedToEdit(feedSource: FeedSource) {
        originalFeedSource = feedSource

        viewModelScope.launch {
            val currentFeedSource = databaseHelper.getFeedSource(feedSource.id) ?: feedSource
            originalFeedSource = currentFeedSource
            val keepUnreadOnScroll = databaseHelper.isKeepUnreadOnScrollEnabled(currentFeedSource.id)

            feedUrlMutableState.update { currentFeedSource.url }
            refreshYouTubeOptions(currentFeedSource.url)
            feedNameMutableState.update { currentFeedSource.title }
            feedSourceSettingsMutableState.update {
                FeedSourceSettings(
                    articleOpenMode = currentFeedSource.articleOpenMode,
                    isHiddenFromTimeline = currentFeedSource.isHiddenFromTimeline,
                    isPinned = currentFeedSource.isPinned,
                    isNotificationEnabled = currentFeedSource.isNotificationEnabled,
                    isHideImagesEnabled = currentFeedSource.isHideImagesEnabled,
                    isKeepUnreadOnScrollEnabled = keepUnreadOnScroll,
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
                // The save wrote the switches it captured before the fetch; write the current ones again,
                // unless another feed was loaded into this reused view model meanwhile
                if (originalFeedSource?.id == newFeedSource.id) {
                    persistPreferences(newFeedSource.id, feedSourceSettingsState.value)
                }
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
                if (state is FeedEditedState.FeedEdited && state.shortsFilterFallback && original != null) {
                    // Set directly: updateFeedUrlTextFieldValue emits Idle, which would hide the fallback notice
                    originalFeedSource = newFeedSource.copy(url = original.url)
                    feedUrlMutableState.value = original.url
                    refreshYouTubeOptions(original.url)
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
                persistPreferences(source.id, feedSourceSettingsState.value)
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

    /** Captures the feed and the switches now, so a later load of another feed cannot redirect the write. */
    private fun launchPreferenceWrite() {
        val feedSourceId = originalFeedSource?.id
        val settings = feedSourceSettingsState.value
        viewModelScope.launch {
            if (feedSourceId != null) persistPreferences(feedSourceId, settings)
            feedEditedMutableState.emit(FeedEditedState.Idle)
        }
    }

    /**
     * Writes run one at a time in call order, so the last change wins. Not cancellable: the user has
     * already seen the switch change, so it must reach the database even if the screen closes.
     */
    private suspend fun persistPreferences(
        feedSourceId: String,
        settings: FeedSourceSettings,
    ) = withContext(NonCancellable) {
        preferenceWriteMutex.withLock {
            val saved = feedStateRepository.runUserWrite(onDatabaseFailure = false) {
                feedSourcesRepository.insertFeedSourcePreference(
                    feedSourceId = feedSourceId,
                    articleOpenMode = settings.articleOpenMode,
                    isHidden = settings.isHiddenFromTimeline,
                    isPinned = settings.isPinned,
                    isNotificationEnabled = settings.isNotificationEnabled,
                    isHideImagesEnabled = settings.isHideImagesEnabled,
                )
                true
            }
            // Another feed can have been loaded into this reused view model while the write ran
            val original = originalFeedSource?.takeIf { it.id == feedSourceId } ?: return@withLock
            if (saved) {
                originalFeedSource = original.withPreferences(settings)
                feedStateRepository.getFeeds()
            } else {
                // Roll back only when no newer switch change is waiting for the lock
                feedSourceSettingsMutableState.update {
                    val unchanged = it.copy(
                        isKeepUnreadOnScrollEnabled = settings.isKeepUnreadOnScrollEnabled,
                    ) == settings
                    if (unchanged) it.withPreferencesOf(original) else it
                }
            }
        }
    }

    private suspend fun persistKeepUnreadOnScroll(
        feedSourceId: String,
        enabled: Boolean,
    ) = withContext(NonCancellable) {
        preferenceWriteMutex.withLock {
            val saved = feedStateRepository.runUserWrite(onDatabaseFailure = false) {
                databaseHelper.setKeepUnreadOnScroll(feedSourceId, enabled)
                true
            }
            if (!saved && originalFeedSource?.id == feedSourceId) {
                feedSourceSettingsMutableState.update {
                    if (it.isKeepUnreadOnScrollEnabled == enabled) {
                        it.copy(isKeepUnreadOnScrollEnabled = !enabled)
                    } else {
                        it
                    }
                }
            }
        }
    }

    private fun refreshYouTubeOptions(url: String) {
        val sanitized = sanitizeUrl(url.trim())
        val noShortsChannelId = YouTubeChannelUrl.channelIdFromNoShortsFeedUrl(sanitized)
        val visible = canEditUrl() && youTubeChannelId(sanitized) != null
        youTubeOptionsMutableState.value = YouTubeOptionsState(
            showHideShortsToggle = visible,
            hideShorts = visible && noShortsChannelId != null,
        )
    }

    private fun youTubeChannelId(url: String): String? {
        val sanitized = sanitizeUrl(url.trim())
        return YouTubeChannelUrl.channelIdFromNoShortsFeedUrl(sanitized)
            ?: YouTubeChannelUrl.channelIdFromFeedUrl(sanitized)
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

private fun FeedSource.withPreferences(settings: FeedSourceSettings) = copy(
    articleOpenMode = settings.articleOpenMode,
    isHiddenFromTimeline = settings.isHiddenFromTimeline,
    isPinned = settings.isPinned,
    isNotificationEnabled = settings.isNotificationEnabled,
    isHideImagesEnabled = settings.isHideImagesEnabled,
)

private fun FeedSourceSettings.withPreferencesOf(source: FeedSource) = copy(
    articleOpenMode = source.articleOpenMode,
    isHiddenFromTimeline = source.isHiddenFromTimeline,
    isPinned = source.isPinned,
    isNotificationEnabled = source.isNotificationEnabled,
    isHideImagesEnabled = source.isHideImagesEnabled,
)
