package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class DesktopHomeSettingsRepository(
    private val settings: Settings,
) {
    fun isDrawerVisible(): Boolean =
        settings.getBoolean(
            DesktopHomeSettingsFields.DESKTOP_HOME_DRAWER_VISIBLE.name,
            defaultValue = true,
        )

    fun setDrawerVisible(value: Boolean) =
        settings.set(DesktopHomeSettingsFields.DESKTOP_HOME_DRAWER_VISIBLE.name, value)

    fun getPaneExpansionIndex(): Int =
        settings.getInt(
            DesktopHomeSettingsFields.DESKTOP_HOME_PANE_EXPANSION_INDEX.name,
            defaultValue = DEFAULT_PANE_EXPANSION_INDEX,
        )

    fun setPaneExpansionIndex(value: Int) =
        settings.set(DesktopHomeSettingsFields.DESKTOP_HOME_PANE_EXPANSION_INDEX.name, value)

    private companion object {
        const val DEFAULT_PANE_EXPANSION_INDEX = 5
    }
}

private enum class DesktopHomeSettingsFields {
    DESKTOP_HOME_DRAWER_VISIBLE,
    DESKTOP_HOME_PANE_EXPANSION_INDEX,
}
