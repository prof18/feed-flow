package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AndroidHomeSettingsRepository(
    private val settings: Settings,
) {
    private val isMultiPaneLayoutEnabledMutableFlow = MutableStateFlow(isMultiPaneLayoutEnabled())
    val isMultiPaneLayoutEnabledFlow: StateFlow<Boolean> = isMultiPaneLayoutEnabledMutableFlow.asStateFlow()

    fun isMultiPaneLayoutEnabled(): Boolean =
        settings.getBoolean(
            AndroidHomeSettingsFields.ANDROID_HOME_MULTI_PANE_LAYOUT_ENABLED.name,
            defaultValue = false,
        )

    fun setMultiPaneLayoutEnabled(value: Boolean) {
        settings.set(AndroidHomeSettingsFields.ANDROID_HOME_MULTI_PANE_LAYOUT_ENABLED.name, value)
        isMultiPaneLayoutEnabledMutableFlow.update { value }
    }

    fun getPaneExpansionIndex(): Int =
        settings.getInt(
            AndroidHomeSettingsFields.ANDROID_HOME_PANE_EXPANSION_INDEX.name,
            defaultValue = DEFAULT_PANE_EXPANSION_INDEX,
        )

    fun setPaneExpansionIndex(value: Int) =
        settings.set(AndroidHomeSettingsFields.ANDROID_HOME_PANE_EXPANSION_INDEX.name, value)

    private companion object {
        const val DEFAULT_PANE_EXPANSION_INDEX = 5
    }
}

private enum class AndroidHomeSettingsFields {
    ANDROID_HOME_MULTI_PANE_LAYOUT_ENABLED,
    ANDROID_HOME_PANE_EXPANSION_INDEX,
}
