package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.YouTubeOptionsState
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feed.YouTubeChannelUrl
import com.prof18.feedflow.shared.domain.feedcategories.FeedCategoryRepository
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.utils.sanitizeUrl
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
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
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private var feedUrl: String = ""
    private var channelIdentity: String? = null
    private val isAddingFeedMutableState = MutableStateFlow(false)
    val isAddingFeedState = isAddingFeedMutableState.asStateFlow()
    private val youTubeOptionsMutableState = MutableStateFlow(YouTubeOptionsState())
    val youTubeOptionsState = youTubeOptionsMutableState.asStateFlow()
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
        val url = sanitizeUrl(feedUrlTextFieldValue.trim())
        val channel = YouTubeChannelUrl.parse(url)
        val id = channel?.channelId ?: YouTubeChannelUrl.channelIdFromFeedUrl(url)
        val identity = id?.let(YouTubeChannelUrl::feedUrl) ?: channel?.pageUrl
        youTubeOptionsMutableState.update {
            YouTubeOptionsState(
                showHideShortsToggle = identity != null,
                hideShorts = identity != null && identity == channelIdentity && it.hideShorts,
            )
        }
        channelIdentity = identity
        viewModelScope.launch {
            if (!isAddingFeedMutableState.value) {
                feedAddedMutableState.emit(FeedAddedState.FeedNotAdded)
            }
        }
    }

    fun updateNotificationStatus(status: Boolean) {
        isNotificationEnabledMutableStateFlow.update { status }
    }

    fun updateHideShortsStatus(status: Boolean) {
        if (!isAddingFeedMutableState.value) {
            youTubeOptionsMutableState.update { it.copy(hideShorts = status && it.showHideShortsToggle) }
        }
    }

    fun addFeed() {
        if (isAddingFeedMutableState.value) return
        isAddingFeedMutableState.value = true
        val submittedUrl = feedUrl
        val categoryName = categoryRepository.getSelectedCategory()
        val notifications = isNotificationEnabledMutableStateFlow.value
        val hideShorts = youTubeOptionsMutableState.value.hideShorts
        viewModelScope.launch {
            try {
                feedAddedMutableState.emit(FeedAddedState.Loading)
                if (submittedUrl.isEmpty()) return@launch
                val feedAddedState = feedSourcesRepository.addFeedSource(
                    feedUrl = submittedUrl,
                    categoryName = categoryName,
                    isNotificationEnabled = notifications,
                    hideYouTubeShorts = hideShorts,
                )
                feedAddedMutableState.emit(feedAddedState)
                if (feedAddedState is FeedAddedState.FeedAdded || feedAddedState is FeedAddedState.FeedAlreadyExists) {
                    isNotificationEnabledMutableStateFlow.update { false }
                    youTubeOptionsMutableState.value = YouTubeOptionsState()
                    channelIdentity = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                feedAddedMutableState.emit(FeedAddedState.Error.GenericError(canForceAdd = false))
            } finally {
                isAddingFeedMutableState.value = false
            }
        }
    }

    fun forceAddFeed() {
        if (isAddingFeedMutableState.value) return
        isAddingFeedMutableState.value = true
        val submittedUrl = sanitizeUrl(feedUrl.trim())
        val categoryName = categoryRepository.getSelectedCategory()
        val notifications = isNotificationEnabledMutableStateFlow.value
        viewModelScope.launch {
            feedAddedMutableState.emit(FeedAddedState.Loading)
            try {
                val feedTitle = runCatching { Url(submittedUrl).host }.getOrNull() ?: submittedUrl
                feedSourcesRepository.addFeedSourceWithoutFetching(
                    feedUrl = submittedUrl,
                    feedTitle = feedTitle,
                    category = categoryName,
                    logoUrl = null,
                    isNotificationEnabled = notifications,
                )
                feedStateRepository.getFeeds()
                isNotificationEnabledMutableStateFlow.update { false }
                youTubeOptionsMutableState.value = YouTubeOptionsState()
                channelIdentity = null
                feedAddedMutableState.emit(FeedAddedState.FeedAdded(feedTitle))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                feedAddedMutableState.emit(FeedAddedState.Error.GenericError(canForceAdd = false))
            } finally {
                isAddingFeedMutableState.value = false
            }
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

    fun validateCategoryName(categoryId: CategoryId?, newName: CategoryName): CategoryNameValidationResult =
        categoryRepository.validateCategoryName(categoryId, newName)
}
