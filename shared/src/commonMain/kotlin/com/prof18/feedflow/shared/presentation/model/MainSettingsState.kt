package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.ThemeMode

data class MainSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
