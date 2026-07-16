package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ReadingBehaviorState
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReadingBehaviorSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(ReadingBehaviorState())
    val state: StateFlow<ReadingBehaviorState> = stateMutableFlow.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val isReaderModeEnabled = settingsRepository.isUseReaderModeEnabled()
        val isSaveReaderModeContentEnabled = settingsRepository.isSaveItemContentOnOpenEnabled()
        val isPrefetchArticleContentEnabled = settingsRepository.isPrefetchArticleContentEnabled()
        val isMarkReadWhenScrollingEnabled = settingsRepository.getMarkFeedAsReadWhenScrolling()
        val isShowReadItemsEnabled = settingsRepository.getShowReadArticlesTimeline()
        val isHideReadItemsEnabled = settingsRepository.getHideReadItems()

        stateMutableFlow.update {
            ReadingBehaviorState(
                isReaderModeEnabled = isReaderModeEnabled,
                isSaveReaderModeContentEnabled = isSaveReaderModeContentEnabled,
                isPrefetchArticleContentEnabled = isPrefetchArticleContentEnabled,
                isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
                isShowReadItemsEnabled = isShowReadItemsEnabled,
                isHideReadItemsEnabled = isHideReadItemsEnabled,
            )
        }
    }

    fun updateReaderMode(value: Boolean) {
        settingsRepository.setUseReaderMode(value)
        stateMutableFlow.update {
            it.copy(isReaderModeEnabled = value)
        }
    }

    fun updateSaveReaderModeContent(value: Boolean) {
        settingsRepository.setSaveItemContentOnOpen(value)
        stateMutableFlow.update {
            it.copy(isSaveReaderModeContentEnabled = value)
        }
    }

    fun updatePrefetchArticleContent(value: Boolean) {
        settingsRepository.setPrefetchArticleContent(value)
        stateMutableFlow.update {
            it.copy(isPrefetchArticleContentEnabled = value)
        }
    }

    fun updateMarkReadWhenScrolling(value: Boolean) {
        settingsRepository.setMarkFeedAsReadWhenScrolling(value)
        stateMutableFlow.update {
            it.copy(isMarkReadWhenScrollingEnabled = value)
        }
    }

    fun updateShowReadItemsOnTimeline(value: Boolean) {
        settingsRepository.setShowReadArticlesTimeline(value)
        stateMutableFlow.update {
            it.copy(isShowReadItemsEnabled = value)
        }
        viewModelScope.launch {
            feedStateRepository.getFeeds()
        }
    }

    fun updateHideReadItems(value: Boolean) {
        settingsRepository.setHideReadItems(value)
        stateMutableFlow.update {
            it.copy(isHideReadItemsEnabled = value)
        }
    }
}
