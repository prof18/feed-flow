package com.prof18.feedflow.presentation

import com.prof18.feedflow.domain.settings.SettingsRepository
import com.prof18.feedflow.presentation.model.SettingsState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
) : BaseViewModel() {

    private val settingsMutableState = MutableStateFlow(SettingsState())

    @NativeCoroutinesState
    val settingsState: StateFlow<SettingsState> = settingsMutableState.asStateFlow()

    init {
        scope.launch {
            val isMarkReadEnabled = settingsRepository.isMarkFeedAsReadWhenScrollingEnabled()
            settingsMutableState.update {
                SettingsState(
                    isMarkReadWhenScrollingEnabled = isMarkReadEnabled,
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
}
