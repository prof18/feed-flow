package com.prof18.feedflow.shared.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class DesktopWindowSettingsRepository(
    private val settings: Settings,
    private val flush: () -> Unit = {},
) {
    fun hasSavedWindowState(): Boolean =
        DesktopWindowSettingsFields.entries.any { settings.hasKey(it.name) }

    fun getDesktopWindowWidthDp(): Int =
        settings.getInt(
            DesktopWindowSettingsFields.DESKTOP_WINDOW_WIDTH_DP.name,
            defaultValue = DEFAULT_DESKTOP_WINDOW_WIDTH_DP,
        )

    fun setDesktopWindowWidthDp(value: Int) =
        settings.set(DesktopWindowSettingsFields.DESKTOP_WINDOW_WIDTH_DP.name, value)

    fun getDesktopWindowHeightDp(): Int =
        settings.getInt(
            DesktopWindowSettingsFields.DESKTOP_WINDOW_HEIGHT_DP.name,
            defaultValue = DEFAULT_DESKTOP_WINDOW_HEIGHT_DP,
        )

    fun setDesktopWindowHeightDp(value: Int) =
        settings.set(DesktopWindowSettingsFields.DESKTOP_WINDOW_HEIGHT_DP.name, value)

    fun getDesktopWindowXPositionDp(): Float? =
        settings.getFloatOrNull(DesktopWindowSettingsFields.DESKTOP_WINDOW_X_POSITION_DP.name)

    fun setDesktopWindowXPositionDp(value: Float) =
        settings.set(DesktopWindowSettingsFields.DESKTOP_WINDOW_X_POSITION_DP.name, value)

    fun removeDesktopWindowPosition() {
        settings.remove(DesktopWindowSettingsFields.DESKTOP_WINDOW_X_POSITION_DP.name)
        settings.remove(DesktopWindowSettingsFields.DESKTOP_WINDOW_Y_POSITION_DP.name)
    }

    fun getDesktopWindowYPositionDp(): Float? =
        settings.getFloatOrNull(DesktopWindowSettingsFields.DESKTOP_WINDOW_Y_POSITION_DP.name)

    fun setDesktopWindowYPositionDp(value: Float) =
        settings.set(DesktopWindowSettingsFields.DESKTOP_WINDOW_Y_POSITION_DP.name, value)

    fun getDesktopWindowPlacement(): String? =
        settings.getStringOrNull(DesktopWindowSettingsFields.DESKTOP_WINDOW_PLACEMENT.name)

    fun setDesktopWindowPlacement(value: String) =
        settings.set(DesktopWindowSettingsFields.DESKTOP_WINDOW_PLACEMENT.name, value)

    fun clearDesktopWindowState() {
        DesktopWindowSettingsFields.entries.forEach { settings.remove(it.name) }
    }

    /** Blocks until the window values written so far are on disk. Throws when the backing store fails. */
    fun flushDesktopWindowState() = flush()

    private companion object {
        const val DEFAULT_DESKTOP_WINDOW_WIDTH_DP = 800
        const val DEFAULT_DESKTOP_WINDOW_HEIGHT_DP = 600
    }
}

private enum class DesktopWindowSettingsFields {
    DESKTOP_WINDOW_WIDTH_DP,
    DESKTOP_WINDOW_HEIGHT_DP,
    DESKTOP_WINDOW_X_POSITION_DP,
    DESKTOP_WINDOW_Y_POSITION_DP,
    DESKTOP_WINDOW_PLACEMENT,
}
