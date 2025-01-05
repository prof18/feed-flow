package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val fontSizeRepository: FeedFontSizeRepository,
) : ViewModel() {

    private val settingsMutableState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = settingsMutableState.asStateFlow()

    val feedFontSizeState: StateFlow<FeedFontSizes> = fontSizeRepository.feedFontSizeState

    init {
        viewModelScope.launch {
            val isMarkReadEnabled = settingsRepository.isMarkFeedAsReadWhenScrollingEnabled()
            val isShowReadItemsEnabled = settingsRepository.isShowReadArticlesTimelineEnabled()
            val isReaderModeEnabled = settingsRepository.isUseReaderModeEnabled()
            val isRemoveTitleFromDescriptionEnabled = settingsRepository.isRemoveTitleFromDescriptionEnabled()
            settingsMutableState.update {
                SettingsState(
                    isMarkReadWhenScrollingEnabled = isMarkReadEnabled,
                    isShowReadItemsEnabled = isShowReadItemsEnabled,
                    isReaderModeEnabled = isReaderModeEnabled,
                    isRemoveTitleFromDescriptionEnabled = isRemoveTitleFromDescriptionEnabled,
                )
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
            feedRetrieverRepository.getFeeds()
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
}
