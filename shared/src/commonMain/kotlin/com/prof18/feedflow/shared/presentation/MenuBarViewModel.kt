package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.presentation.model.MenuBarSettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MenuBarViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val feedStateRepository: FeedStateRepository,
    private val contentPrefetchRepository: ContentPrefetchRepository,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(MenuBarSettingsState())
    val state: StateFlow<MenuBarSettingsState> = stateMutableFlow.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val themeMode = settingsRepository.getThemeMode()
        val isMarkReadWhenScrollingEnabled = settingsRepository.getMarkFeedAsReadWhenScrolling()
        val isShowReadItemsEnabled = settingsRepository.getShowReadArticlesTimeline()
        val isReaderModeEnabled = settingsRepository.isUseReaderModeEnabled()
        val isSaveReaderModeContentEnabled = settingsRepository.isSaveItemContentOnOpenEnabled()
        val isPrefetchArticleContentEnabled = settingsRepository.isPrefetchArticleContentEnabled()
        val isRefreshFeedsOnLaunchEnabled = settingsRepository.getRefreshFeedsOnLaunch()
        val isReduceMotionEnabled = settingsRepository.getReduceMotionEnabled()
        val autoDeletePeriod = settingsRepository.getAutoDeletePeriod()
        val isCrashReportingEnabled = settingsRepository.getCrashReportingEnabled()
        val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()

        stateMutableFlow.update {
            MenuBarSettingsState(
                themeMode = themeMode,
                isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
                isShowReadItemsEnabled = isShowReadItemsEnabled,
                isReaderModeEnabled = isReaderModeEnabled,
                isSaveReaderModeContentEnabled = isSaveReaderModeContentEnabled,
                isPrefetchArticleContentEnabled = isPrefetchArticleContentEnabled,
                isRefreshFeedsOnLaunchEnabled = isRefreshFeedsOnLaunchEnabled,
                isReduceMotionEnabled = isReduceMotionEnabled,
                autoDeletePeriod = autoDeletePeriod,
                isCrashReportingEnabled = isCrashReportingEnabled,
                feedOrder = feedOrder,
            )
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
        stateMutableFlow.update {
            it.copy(themeMode = mode)
        }
    }

    fun updateMarkReadWhenScrolling(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMarkFeedAsReadWhenScrolling(value)
            stateMutableFlow.update {
                it.copy(isMarkReadWhenScrollingEnabled = value)
            }
        }
    }

    fun updateShowReadItemsOnTimeline(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowReadArticlesTimeline(value)
            stateMutableFlow.update {
                it.copy(isShowReadItemsEnabled = value)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun updateReaderMode(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseReaderMode(value)
            stateMutableFlow.update {
                it.copy(isReaderModeEnabled = value)
            }
        }
    }

    fun updateSaveReaderModeContent(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSaveItemContentOnOpen(value)
            stateMutableFlow.update {
                it.copy(isSaveReaderModeContentEnabled = value)
            }
        }
    }

    fun updatePrefetchArticleContent(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrefetchArticleContent(value)
            stateMutableFlow.update {
                it.copy(isPrefetchArticleContentEnabled = value)
            }

            if (!value) {
                contentPrefetchRepository.cancelFetching()
            }
        }
    }

    fun updateRefreshFeedsOnLaunch(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRefreshFeedsOnLaunch(value)
            stateMutableFlow.update {
                it.copy(isRefreshFeedsOnLaunchEnabled = value)
            }
        }
    }

    fun updateReduceMotionEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReduceMotionEnabled(value)
            stateMutableFlow.update {
                it.copy(isReduceMotionEnabled = value)
            }
        }
    }

    fun updateAutoDeletePeriod(period: AutoDeletePeriod) {
        viewModelScope.launch {
            settingsRepository.setAutoDeletePeriod(period)
            stateMutableFlow.update {
                it.copy(autoDeletePeriod = period)
            }
        }
    }

    fun updateCrashReporting(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCrashReportingEnabled(value)
            stateMutableFlow.update {
                it.copy(isCrashReportingEnabled = value)
            }
        }
    }

    fun updateFeedOrder(feedOrder: FeedOrder) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setFeedOrder(feedOrder)
            stateMutableFlow.update {
                it.copy(feedOrder = feedOrder)
            }
            feedStateRepository.getFeeds()
        }
    }

    fun clearDownloadedArticleContent() {
        viewModelScope.launch {
            feedItemContentFileHandler.clearAllContent()
        }
    }
}
