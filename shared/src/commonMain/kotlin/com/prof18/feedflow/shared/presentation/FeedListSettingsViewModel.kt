package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.presentation.model.FeedListSettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedListSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val fontSizeRepository: FeedFontSizeRepository,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(FeedListSettingsState())
    val state: StateFlow<FeedListSettingsState> = stateMutableFlow.asStateFlow()

    val feedFontSizeState: StateFlow<FeedFontSizes> = fontSizeRepository.feedFontSizeState

    init {
        viewModelScope.launch { loadSettings() }
    }

    private fun loadSettings() {
        val isHideDescriptionEnabled = settingsRepository.getHideDescription()
        val isHideImagesEnabled = settingsRepository.getHideImages()
        val isHideDateEnabled = settingsRepository.getHideDate()
        val dateFormat = settingsRepository.getDateFormat()
        val timeFormat = settingsRepository.getTimeFormat()
        val feedLayout = settingsRepository.getFeedLayout()
        val fontScale = settingsRepository.getFeedListFontScaleFactor()
        val leftSwipeAction = settingsRepository.getSwipeAction(SwipeDirection.LEFT)
        val rightSwipeAction = settingsRepository.getSwipeAction(SwipeDirection.RIGHT)
        val isRemoveTitleFromDescriptionEnabled = settingsRepository.getRemoveTitleFromDescription()
        val feedOrder = settingsRepository.getFeedOrder()

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
            )
        }
    }

    fun updateHideDescription(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideDescription(value)
            stateMutableFlow.update {
                it.copy(isHideDescriptionEnabled = value)
            }
        }
    }

    fun updateHideImages(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideImages(value)
            stateMutableFlow.update {
                it.copy(isHideImagesEnabled = value)
            }
        }
    }

    fun updateHideDate(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideDate(value)
            stateMutableFlow.update {
                it.copy(isHideDateEnabled = value)
            }
        }
    }

    fun updateDateFormat(format: DateFormat) {
        viewModelScope.launch {
            settingsRepository.setDateFormat(format)
            stateMutableFlow.update {
                it.copy(dateFormat = format)
            }
        }
    }

    fun updateTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            settingsRepository.setTimeFormat(format)
            stateMutableFlow.update {
                it.copy(timeFormat = format)
            }
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        viewModelScope.launch {
            settingsRepository.setFeedLayout(feedLayout)
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
            settingsRepository.setSwipeAction(direction, action)
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
            settingsRepository.setRemoveTitleFromDescription(value)
            stateMutableFlow.update {
                it.copy(isRemoveTitleFromDescriptionEnabled = value)
            }
        }
    }

    fun updateFeedOrder(feedOrder: com.prof18.feedflow.core.model.FeedOrder) {
        viewModelScope.launch {
            settingsRepository.setFeedOrder(feedOrder)
            stateMutableFlow.update {
                it.copy(feedOrder = feedOrder)
            }
        }
    }
}
