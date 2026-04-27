package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class IosHomeSettingsRepository(
    private val settings: Settings,
) {
    private val isMultiPaneLayoutEnabledMutableFlow = MutableStateFlow(isMultiPaneLayoutEnabled())
    val isMultiPaneLayoutEnabledFlow: StateFlow<Boolean> = isMultiPaneLayoutEnabledMutableFlow.asStateFlow()

    fun isMultiPaneLayoutEnabled(): Boolean =
        settings.getBoolean(
            IosHomeSettingsFields.IOS_HOME_MULTI_PANE_LAYOUT_ENABLED.name,
            defaultValue = false,
        )

    fun setMultiPaneLayoutEnabled(value: Boolean) {
        settings[IosHomeSettingsFields.IOS_HOME_MULTI_PANE_LAYOUT_ENABLED.name] = value
        isMultiPaneLayoutEnabledMutableFlow.update { value }
    }
}

private enum class IosHomeSettingsFields {
    IOS_HOME_MULTI_PANE_LAYOUT_ENABLED,
}
