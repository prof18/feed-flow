package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.MainSettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
) : ViewModel() {

    private val settingsMutableState = MutableStateFlow(MainSettingsState())
    val settingsState: StateFlow<MainSettingsState> = settingsMutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val themeMode = settingsRepository.getThemeMode()
            val isHideUnreadCountEnabled = feedAppearanceSettingsRepository.getHideUnreadCount()
            settingsMutableState.update {
                MainSettingsState(
                    themeMode = themeMode,
                    isHideUnreadCountEnabled = isHideUnreadCountEnabled,
                )
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
        settingsMutableState.update {
            it.copy(themeMode = mode)
        }
    }

    fun updateHideUnreadCount(value: Boolean) {
        feedAppearanceSettingsRepository.setHideUnreadCount(value)
        settingsMutableState.update {
            it.copy(isHideUnreadCountEnabled = value)
        }
    }
}
