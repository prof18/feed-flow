package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import com.prof18.feedflow.shared.data.SettingsRepository

class ThemeViewModel internal constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val themeState = settingsRepository.themeModeFlow
}
