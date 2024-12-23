package com.prof18.feedflow.shared.presentation.model

data class SettingsState(
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val isShowReadItemsEnabled: Boolean = false,
    val isReaderModeEnabled: Boolean = false,
    val isRemoveTitleFromDescriptionEnabled: Boolean = false,
)
