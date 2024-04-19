package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.SettingsState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private val settingsMutableState = MutableStateFlow(SettingsState())

    @NativeCoroutinesState
    val settingsState: StateFlow<SettingsState> = settingsMutableState.asStateFlow()

    init {
        scope.launch {
            val isMarkReadEnabled = settingsRepository.isMarkFeedAsReadWhenScrollingEnabled()
            val isShowReadItemsEnabled = settingsRepository.isShowReadArticlesTimelineEnabled()
            val isReaderModeEnabled = settingsRepository.isUseReaderModeEnabled()
            settingsMutableState.update {
                SettingsState(
                    isMarkReadWhenScrollingEnabled = isMarkReadEnabled,
                    isShowReadItemsEnabled = isShowReadItemsEnabled,
                    isReaderModeEnabled = isReaderModeEnabled,
                )
            }
        }
    }

    fun updateMarkReadWhenScrolling(value: Boolean) {
        scope.launch {
            settingsRepository.setMarkFeedAsReadWhenScrolling(value)
            settingsMutableState.update {
                it.copy(
                    isMarkReadWhenScrollingEnabled = value,
                )
            }
        }
    }

    fun updateShowReadItemsOnTimeline(value: Boolean) {
        scope.launch {
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
}
