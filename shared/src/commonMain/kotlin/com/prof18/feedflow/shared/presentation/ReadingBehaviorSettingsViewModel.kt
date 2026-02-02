package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.ReadingBehaviorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReadingBehaviorSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(ReadingBehaviorState())
    val state: StateFlow<ReadingBehaviorState> = stateMutableFlow.asStateFlow()

    init {
        viewModelScope.launch { loadSettings() }
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
        }
    }

    fun updateHideReadItems(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideReadItems(value)
            stateMutableFlow.update {
                it.copy(isHideReadItemsEnabled = value)
            }
        }
    }
}
