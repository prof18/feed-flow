package com.prof18.feedflow.desktop.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.prof18.feedflow.desktop.settings.DesktopSettingsCategory
import com.prof18.feedflow.desktop.settings.DesktopSettingsSection

internal enum class DesktopDialogWindowDestination {
    AddFeed,
    EditFeed,
    FeedSuggestions,
    Settings,
}

internal class DesktopDialogWindowNavigator {
    private val openedWindows = mutableStateMapOf<DesktopDialogWindowDestination, Boolean>()
    var settingsCategory by mutableStateOf(DesktopSettingsCategory.GENERAL)
        private set
    var settingsSection by mutableStateOf<DesktopSettingsSection?>(null)
        private set
    var settingsActivationRequest by mutableIntStateOf(0)
        private set

    fun openSettings(category: DesktopSettingsCategory? = null, section: DesktopSettingsSection? = null) {
        if (category != null) selectSettingsCategory(category)
        if (section != null) settingsSection = section
        settingsActivationRequest++
        open(DesktopDialogWindowDestination.Settings)
    }

    fun selectSettingsCategory(category: DesktopSettingsCategory) {
        settingsCategory = category
        settingsSection = null
    }

    fun selectSettingsSection(section: DesktopSettingsSection?) {
        settingsSection = section
    }

    fun open(destination: DesktopDialogWindowDestination) {
        openedWindows[destination] = true
    }

    fun close(destination: DesktopDialogWindowDestination) {
        openedWindows[destination] = false
    }

    fun isOpen(destination: DesktopDialogWindowDestination): Boolean =
        openedWindows[destination] == true
}

@Composable
internal fun rememberDesktopDialogWindowNavigator(): DesktopDialogWindowNavigator = remember {
    DesktopDialogWindowNavigator()
}

// Secondary windows share the main-window navigator.
@Suppress("CompositionLocalAllowlist")
internal val LocalDesktopDialogWindowNavigator = staticCompositionLocalOf<DesktopDialogWindowNavigator> {
    error("Desktop dialog navigator is not provided")
}
