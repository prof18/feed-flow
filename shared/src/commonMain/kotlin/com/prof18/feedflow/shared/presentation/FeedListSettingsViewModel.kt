package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.model.FeedListSettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedListSettingsViewModel internal constructor(
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val fontSizeRepository: FeedFontSizeRepository,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(FeedListSettingsState())
    val state: StateFlow<FeedListSettingsState> = stateMutableFlow.asStateFlow()

    val feedFontSizeState: StateFlow<FeedFontSizes> = fontSizeRepository.feedFontSizeState

    init {
        viewModelScope.launch { loadSettings() }
    }

    private fun loadSettings() {
        val isHideDescriptionEnabled = feedAppearanceSettingsRepository.getHideDescription()
        val isHideImagesEnabled = feedAppearanceSettingsRepository.getHideImages()
        val isHideDateEnabled = feedAppearanceSettingsRepository.getHideDate()
        val dateFormat = feedAppearanceSettingsRepository.getDateFormat()
        val timeFormat = feedAppearanceSettingsRepository.getTimeFormat()
        val feedLayout = feedAppearanceSettingsRepository.getFeedLayout()
        val fontScale = feedAppearanceSettingsRepository.getFeedListFontScaleFactor()
        val leftSwipeAction = feedAppearanceSettingsRepository.getSwipeAction(SwipeDirection.LEFT)
        val rightSwipeAction = feedAppearanceSettingsRepository.getSwipeAction(SwipeDirection.RIGHT)
        val isRemoveTitleFromDescriptionEnabled = feedAppearanceSettingsRepository.getRemoveTitleFromDescription()
        val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()
        val isHideUnreadCountEnabled = feedAppearanceSettingsRepository.getHideUnreadCount()

        stateMutableFlow.update {
            FeedListSettingsState(
                isHideDescriptionEnabled = isHideDescriptionEnabled,
                isHideImagesEnabled = isHideImagesEnabled,
                isHideDateEnabled = isHideDateEnabled,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                feedLayout = feedLayout,
                fontScale = fontScale,
                leftSwipeActionType = leftSwipeAction,
                rightSwipeActionType = rightSwipeAction,
                isRemoveTitleFromDescriptionEnabled = isRemoveTitleFromDescriptionEnabled,
                feedOrder = feedOrder,
                isHideUnreadCountEnabled = isHideUnreadCountEnabled,
            )
        }
    }

    fun updateHideDescription(value: Boolean) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setHideDescription(value)
            stateMutableFlow.update {
                it.copy(isHideDescriptionEnabled = value)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideImages(value: Boolean) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setHideImages(value)
            stateMutableFlow.update {
                it.copy(isHideImagesEnabled = value)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideDate(value: Boolean) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setHideDate(value)
            stateMutableFlow.update {
                it.copy(isHideDateEnabled = value)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateDateFormat(format: DateFormat) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setDateFormat(format)
            stateMutableFlow.update {
                it.copy(dateFormat = format)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setTimeFormat(format)
            stateMutableFlow.update {
                it.copy(timeFormat = format)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setFeedLayout(feedLayout)
            stateMutableFlow.update {
                it.copy(feedLayout = feedLayout)
            }
        }
    }

    fun updateFontScale(value: Int) {
        viewModelScope.launch {
            fontSizeRepository.updateFontScale(value)
            stateMutableFlow.update {
                it.copy(fontScale = value)
            }
        }
    }

    fun updateSwipeAction(direction: SwipeDirection, action: SwipeActionType) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setSwipeAction(direction, action)
            stateMutableFlow.update {
                when (direction) {
                    SwipeDirection.LEFT -> it.copy(leftSwipeActionType = action)
                    SwipeDirection.RIGHT -> it.copy(rightSwipeActionType = action)
                }
            }
        }
    }

    fun updateRemoveTitleFromDescription(value: Boolean) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setRemoveTitleFromDescription(value)
            stateMutableFlow.update {
                it.copy(isRemoveTitleFromDescriptionEnabled = value)
            }
        }
    }

    fun updateFeedOrder(feedOrder: com.prof18.feedflow.core.model.FeedOrder) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setFeedOrder(feedOrder)
            stateMutableFlow.update {
                it.copy(feedOrder = feedOrder)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideUnreadCount(value: Boolean) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setHideUnreadCount(value)
            stateMutableFlow.update {
                it.copy(isHideUnreadCountEnabled = value)
            }
        }
    }
}
