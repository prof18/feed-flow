package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.ReadArticlesVisibility
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.BackgroundSyncScheduler
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.model.MenuBarSettingsState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MenuBarViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val feedStateRepository: FeedStateRepository,
    private val contentPrefetchRepository: ContentPrefetchRepository,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val backgroundSyncScheduler: BackgroundSyncScheduler,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(MenuBarSettingsState())
    val state: StateFlow<MenuBarSettingsState> = stateMutableFlow.asStateFlow()

    private val downloadedArticlesClearedMutable = MutableSharedFlow<Boolean>()
    val downloadedArticlesCleared: SharedFlow<Boolean> = downloadedArticlesClearedMutable.asSharedFlow()

    init {
        loadSettings()
        viewModelScope.launch {
            feedAppearanceSettingsRepository.feedOrder.collect { order ->
                stateMutableFlow.update { it.copy(feedOrder = order) }
            }
        }
        viewModelScope.launch {
            combine(
                settingsRepository.showReadArticlesTimelineFlow,
                settingsRepository.hideReadItemsFlow,
                settingsRepository.markFeedAsReadWhenScrollingFlow,
            ) { showRead, _, _ ->
                showRead to settingsRepository.getReadArticlesVisibility()
            }.collect { (showRead, visibility) ->
                stateMutableFlow.update {
                    it.copy(isShowReadItemsEnabled = showRead, readArticlesVisibility = visibility)
                }
            }
        }
    }

    private fun loadSettings() {
        val themeMode = settingsRepository.getThemeMode()
        val isMarkReadWhenScrollingEnabled = settingsRepository.getMarkFeedAsReadWhenScrolling()
        val isShowReadItemsEnabled = settingsRepository.getShowReadArticlesTimeline()
        val articleOpenMode = settingsRepository.getArticleOpenMode()
        val isSaveReaderModeContentEnabled = settingsRepository.isSaveItemContentOnOpenEnabled()
        val isPrefetchArticleContentEnabled = settingsRepository.isPrefetchArticleContentEnabled()
        val isRefreshFeedsOnLaunchEnabled = settingsRepository.getRefreshFeedsOnLaunch()
        val syncPeriod = settingsRepository.getSyncPeriod()
        val isReduceMotionEnabled = settingsRepository.getReduceMotionEnabled()
        val isHideUnreadCountEnabled = feedAppearanceSettingsRepository.getHideUnreadCount()
        val autoDeletePeriod = settingsRepository.getAutoDeletePeriod()
        val isCrashReportingEnabled = settingsRepository.getCrashReportingEnabled()
        val feedOrder = feedAppearanceSettingsRepository.getFeedOrder()

        stateMutableFlow.update {
            MenuBarSettingsState(
                themeMode = themeMode,
                isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
                isShowReadItemsEnabled = isShowReadItemsEnabled,
                readArticlesVisibility = settingsRepository.getReadArticlesVisibility(),
                articleOpenMode = articleOpenMode,
                isSaveReaderModeContentEnabled = isSaveReaderModeContentEnabled,
                isPrefetchArticleContentEnabled = isPrefetchArticleContentEnabled,
                isRefreshFeedsOnLaunchEnabled = isRefreshFeedsOnLaunchEnabled,
                syncPeriod = syncPeriod,
                isReduceMotionEnabled = isReduceMotionEnabled,
                isHideUnreadCountEnabled = isHideUnreadCountEnabled,
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
            if (!value) {
                feedStateRepository.reapplyAutoHideToCurrentList()
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

    fun updateReadArticlesVisibility(value: ReadArticlesVisibility) {
        viewModelScope.launch {
            val blocked = value == ReadArticlesVisibility.HIDE_RIGHT_AWAY &&
                settingsRepository.getMarkFeedAsReadWhenScrolling()
            if (blocked || value == settingsRepository.getReadArticlesVisibility()) return@launch
            settingsRepository.setReadArticlesVisibility(value)
            when (value) {
                ReadArticlesVisibility.KEEP_IN_LIST -> feedStateRepository.getFeeds()
                ReadArticlesVisibility.HIDE_RIGHT_AWAY -> feedStateRepository.reapplyAutoHideToCurrentList()
                ReadArticlesVisibility.HIDE_ON_REFRESH -> Unit
            }
        }
    }

    fun updateArticleOpenMode(value: ArticleOpenMode) {
        viewModelScope.launch {
            settingsRepository.setArticleOpenMode(value)
            stateMutableFlow.update {
                it.copy(articleOpenMode = value)
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

    fun updateSyncPeriod(period: SyncPeriod) {
        viewModelScope.launch {
            settingsRepository.setSyncPeriod(period)
            backgroundSyncScheduler.updateSyncPeriod(period)
            stateMutableFlow.update {
                it.copy(syncPeriod = period)
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

    fun updateHideUnreadCount(value: Boolean) {
        viewModelScope.launch {
            feedAppearanceSettingsRepository.setHideUnreadCount(value)
            stateMutableFlow.update {
                it.copy(isHideUnreadCountEnabled = value)
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
            val cleared = feedItemContentFileHandler.clearAllContent()
            downloadedArticlesClearedMutable.emit(cleared)
        }
    }
}
