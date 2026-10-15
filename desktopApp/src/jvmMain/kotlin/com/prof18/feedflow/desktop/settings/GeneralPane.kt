package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun GeneralPane(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    isMultiPaneEnabled: Boolean,
    onMultiPaneToggled: (Boolean) -> Unit,
    isReduceMotionEnabled: Boolean,
    onReduceMotionToggled: (Boolean) -> Unit,
    isForceEnglishEnabled: Boolean,
    onForceEnglishToggled: (Boolean) -> Unit,
    isHideUnreadCountEnabled: Boolean,
    onHideUnreadCountToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    SettingsPage(title = strings.settingsAppTitle) {
        SettingsSection(title = strings.settingsAppearance) {
            SettingsChoice(
                title = strings.settingsTheme,
                selected = themeMode,
                choices = persistentListOf(
                    ThemeMode.SYSTEM to strings.settingsThemeSystem,
                    ThemeMode.LIGHT to strings.settingsThemeLight,
                    ThemeMode.DARK to strings.settingsThemeDark,
                    ThemeMode.OLED to strings.settingsThemeOled,
                ),
                onSelected = onThemeModeSelected,
            )
            SettingsSwitch(
                title = strings.settingsDesktopMultiPaneLayout,
                checked = isMultiPaneEnabled,
                onCheckedChange = onMultiPaneToggled,
            )
            SettingsSwitch(
                title = strings.settingsHideUnreadCount,
                checked = isHideUnreadCountEnabled,
                onCheckedChange = onHideUnreadCountToggled,
            )
            SettingsSwitch(
                title = strings.settingsReduceMotion,
                checked = isReduceMotionEnabled,
                onCheckedChange = onReduceMotionToggled,
            )
            SettingsSwitch(
                title = strings.settingsForceEnglish,
                checked = isForceEnglishEnabled,
                onCheckedChange = onForceEnglishToggled,
                supportingText = strings.settingsForceEnglishDescription,
            )
        }
    }
}
