package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedListSettingsState
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
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
        loadSettings()
    }

    private fun loadSettings() {
        val isHideDescriptionEnabled = feedAppearanceSettingsRepository.getHideDescription()
        val isHideImagesEnabled = feedAppearanceSettingsRepository.getHideImages()
        val isHideDateEnabled = feedAppearanceSettingsRepository.getHideDate()
        val dateFormat = feedAppearanceSettingsRepository.getDateFormat()
        val timeFormat = feedAppearanceSettingsRepository.getTimeFormat()
        val feedLayout = feedAppearanceSettingsRepository.getFeedLayout()
        val isGridLayoutEnabled = feedAppearanceSettingsRepository.getGridLayoutEnabled()
        val fontScale = feedAppearanceSettingsRepository.getFeedListFontScaleFactor()
        val leftSwipeAction = feedAppearanceSettingsRepository.getSwipeAction(SwipeDirection.LEFT)
        val rightSwipeAction = feedAppearanceSettingsRepository.getSwipeAction(SwipeDirection.RIGHT)
        val isRemoveTitleFromDescriptionEnabled = feedAppearanceSettingsRepository.getRemoveTitleFromDescription()
        val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()
        val isHideUnreadDotEnabled = feedAppearanceSettingsRepository.getHideUnreadDot()
        val isHideFeedSourceEnabled = feedAppearanceSettingsRepository.getHideFeedSource()
        val descriptionLineLimit = feedAppearanceSettingsRepository.getDescriptionLineLimit()

        stateMutableFlow.update {
            FeedListSettingsState(
                isHideDescriptionEnabled = isHideDescriptionEnabled,
                isHideImagesEnabled = isHideImagesEnabled,
                isHideDateEnabled = isHideDateEnabled,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                feedLayout = feedLayout,
                isGridLayoutEnabled = isGridLayoutEnabled,
                fontScale = fontScale,
                leftSwipeActionType = leftSwipeAction,
                rightSwipeActionType = rightSwipeAction,
                isRemoveTitleFromDescriptionEnabled = isRemoveTitleFromDescriptionEnabled,
                feedOrder = feedOrder,
                isHideUnreadDotEnabled = isHideUnreadDotEnabled,
                isHideFeedSourceEnabled = isHideFeedSourceEnabled,
                descriptionLineLimit = descriptionLineLimit,
            )
        }
    }

    fun updateHideDescription(value: Boolean) {
        feedAppearanceSettingsRepository.setHideDescription(value)
        stateMutableFlow.update {
            it.copy(isHideDescriptionEnabled = value)
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideImages(value: Boolean) {
        feedAppearanceSettingsRepository.setHideImages(value)
        stateMutableFlow.update {
            it.copy(isHideImagesEnabled = value)
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideDate(value: Boolean) {
        feedAppearanceSettingsRepository.setHideDate(value)
        stateMutableFlow.update {
            it.copy(isHideDateEnabled = value)
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateDateFormat(format: DateFormat) {
        feedAppearanceSettingsRepository.setDateFormat(format)
        stateMutableFlow.update {
            it.copy(dateFormat = format)
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateTimeFormat(format: TimeFormat) {
        feedAppearanceSettingsRepository.setTimeFormat(format)
        stateMutableFlow.update {
            it.copy(timeFormat = format)
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        feedAppearanceSettingsRepository.setFeedLayout(feedLayout)
        val isGridLayoutEnabled = feedAppearanceSettingsRepository.getGridLayoutEnabled()
        stateMutableFlow.update {
            it.copy(
                feedLayout = feedAppearanceSettingsRepository.getFeedLayout(),
                isGridLayoutEnabled = isGridLayoutEnabled,
            )
        }
    }

    fun updateGridLayoutEnabled(isEnabled: Boolean) {
        feedAppearanceSettingsRepository.setGridLayoutEnabled(isEnabled)
        stateMutableFlow.update {
            it.copy(isGridLayoutEnabled = isEnabled)
        }
    }

    fun updateFontScale(value: Int) {
        fontSizeRepository.updateFontScale(value)
        stateMutableFlow.update {
            it.copy(fontScale = value)
        }
    }

    fun updateSwipeAction(direction: SwipeDirection, action: SwipeActionType) {
        feedAppearanceSettingsRepository.setSwipeAction(direction, action)
        stateMutableFlow.update {
            when (direction) {
                SwipeDirection.LEFT -> it.copy(leftSwipeActionType = action)
                SwipeDirection.RIGHT -> it.copy(rightSwipeActionType = action)
            }
        }
    }

    fun updateRemoveTitleFromDescription(value: Boolean) {
        feedAppearanceSettingsRepository.setRemoveTitleFromDescription(value)
        stateMutableFlow.update {
            it.copy(isRemoveTitleFromDescriptionEnabled = value)
        }
    }

    fun updateFeedOrder(feedOrder: com.prof18.feedflow.core.model.FeedOrder) {
        feedAppearanceSettingsRepository.setFeedOrder(feedOrder)
        stateMutableFlow.update {
            it.copy(feedOrder = feedOrder)
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideUnreadDot(value: Boolean) {
        feedAppearanceSettingsRepository.setHideUnreadDot(value)
        stateMutableFlow.update {
            it.copy(isHideUnreadDotEnabled = value)
        }
    }

    fun updateHideFeedSource(value: Boolean) {
        feedAppearanceSettingsRepository.setHideFeedSource(value)
        stateMutableFlow.update {
            it.copy(isHideFeedSourceEnabled = value)
        }
    }

    fun updateDescriptionLineLimit(limit: DescriptionLineLimit) {
        feedAppearanceSettingsRepository.setDescriptionLineLimit(limit)
        stateMutableFlow.update {
            it.copy(descriptionLineLimit = limit)
        }
    }
}
