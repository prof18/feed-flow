package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class AndroidHomeSettingsRepository(
    private val settings: Settings,
) {
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
    ANDROID_HOME_PANE_EXPANSION_INDEX,
}
