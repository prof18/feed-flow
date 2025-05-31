package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.model.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val fontSizeRepository: FeedFontSizeRepository,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val settingsMutableState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = settingsMutableState.asStateFlow()

    val feedFontSizeState: StateFlow<FeedFontSizes> = fontSizeRepository.feedFontSizeState

    init {
        viewModelScope.launch {
            settingsRepository.syncPeriodFlow.collect { syncPeriod ->
                val isMarkReadEnabled = settingsRepository.getMarkFeedAsReadWhenScrolling()
                val isShowReadItemsEnabled = settingsRepository.getShowReadArticlesTimeline()
                val isReaderModeEnabled = settingsRepository.isUseReaderModeEnabled()
                val isExperimentalParsingEnabled = settingsRepository.isExperimentalParsingEnabled()
                val isRemoveTitleFromDescriptionEnabled = settingsRepository.getRemoveTitleFromDescription()
                val isHideDescriptionEnabled = settingsRepository.getHideDescription()
                val isHideImagesEnabled = settingsRepository.getHideImages()
                val autoDeletePeriod = settingsRepository.getAutoDeletePeriod()
                val isCrashReportingEnabled = settingsRepository.getCrashReportingEnabled()
                val leftSwipeAction = settingsRepository.getSwipeAction(SwipeDirection.LEFT)
                val rightSwipeAction = settingsRepository.getSwipeAction(SwipeDirection.RIGHT)
                val dateFormat = settingsRepository.getDateFormat()
                val feedOrder = settingsRepository.getFeedOrder()
                val feedLayout = settingsRepository.getFeedLayout()
                settingsMutableState.update {
                    SettingsState(
                        feedOrder = feedOrder,
                        isMarkReadWhenScrollingEnabled = isMarkReadEnabled,
                        isShowReadItemsEnabled = isShowReadItemsEnabled,
                        isReaderModeEnabled = isReaderModeEnabled,
                        isExperimentalParsingEnabled = isExperimentalParsingEnabled,
                        isRemoveTitleFromDescriptionEnabled = isRemoveTitleFromDescriptionEnabled,
                        isHideDescriptionEnabled = isHideDescriptionEnabled,
                        isHideImagesEnabled = isHideImagesEnabled,
                        autoDeletePeriod = autoDeletePeriod,
                        isCrashReportingEnabled = isCrashReportingEnabled,
                        syncPeriod = syncPeriod,
                        leftSwipeActionType = leftSwipeAction,
                        rightSwipeActionType = rightSwipeAction,
                        dateFormat = dateFormat,
                        feedLayout = feedLayout,
                    )
                }
            }
        }
    }

    fun updateMarkReadWhenScrolling(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMarkFeedAsReadWhenScrolling(value)
            settingsMutableState.update {
                it.copy(
                    isMarkReadWhenScrollingEnabled = value,
                )
            }
        }
    }

    fun updateShowReadItemsOnTimeline(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowReadArticlesTimeline(value)
            settingsMutableState.update {
                it.copy(
                    isShowReadItemsEnabled = value,
                )
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateReaderMode(value: Boolean) {
        settingsRepository.setUseReaderMode(value)
        settingsMutableState.update {
            it.copy(
                isReaderModeEnabled = value,
            )
        }
    }

    fun updateExperimentalParsing(value: Boolean) {
        settingsRepository.setExperimentalParsing(value)
        settingsMutableState.update {
            it.copy(
                isExperimentalParsingEnabled = value,
            )
        }
    }

    fun updateRemoveTitleFromDescription(value: Boolean) {
        settingsRepository.setRemoveTitleFromDescription(value)
        settingsMutableState.update {
            it.copy(
                isRemoveTitleFromDescriptionEnabled = value,
            )
        }
    }

    fun updateFontScale(value: Int) {
        fontSizeRepository.updateFontScale(value)
    }

    fun updateAutoDeletePeriod(period: AutoDeletePeriod) {
        viewModelScope.launch {
            settingsRepository.setAutoDeletePeriod(period)
            settingsMutableState.update {
                it.copy(
                    autoDeletePeriod = period,
                )
            }
        }
    }

    fun updateHideDescription(value: Boolean) {
        settingsRepository.setHideDescription(value)
        settingsMutableState.update {
            it.copy(
                isHideDescriptionEnabled = value,
            )
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideImages(value: Boolean) {
        settingsRepository.setHideImages(value)
        settingsMutableState.update {
            it.copy(
                isHideImagesEnabled = value,
            )
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateCrashReporting(value: Boolean) {
        settingsRepository.setCrashReportingEnabled(value)
        settingsMutableState.update {
            it.copy(
                isCrashReportingEnabled = value,
            )
        }
    }

    fun updateSyncPeriod(period: SyncPeriod) {
        viewModelScope.launch {
            settingsRepository.setSyncPeriod(period)
        }
    }

    fun updateSwipeAction(direction: SwipeDirection, action: SwipeActionType) {
        viewModelScope.launch {
            settingsRepository.setSwipeAction(direction, action)
            settingsMutableState.update {
                when (direction) {
                    SwipeDirection.LEFT -> it.copy(leftSwipeActionType = action)
                    SwipeDirection.RIGHT -> it.copy(rightSwipeActionType = action)
                }
            }
        }
    }

    fun updateDateFormat(format: DateFormat) {
        viewModelScope.launch {
            settingsRepository.setDateFormat(format)
            settingsMutableState.update {
                it.copy(dateFormat = format)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateFeedOrder(feedOrder: FeedOrder) {
        viewModelScope.launch {
            settingsRepository.setFeedOrder(feedOrder)
            settingsMutableState.update {
                it.copy(feedOrder = feedOrder)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        viewModelScope.launch {
            settingsRepository.setFeedLayout(feedLayout)
            settingsMutableState.update { it.copy(feedLayout = feedLayout) }
        }
    }
}
