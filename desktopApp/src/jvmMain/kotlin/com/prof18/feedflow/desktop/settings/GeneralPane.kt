package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    SettingsScrollPane(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        SettingsGroup(strings.settingsAppTitle)
        JewelSettingsChoice(
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
        SettingsCheckbox(
            title = strings.settingsDesktopMultiPaneLayout,
            checked = isMultiPaneEnabled,
            onCheckedChange = onMultiPaneToggled,
        )
        SettingsCheckbox(
            title = strings.settingsHideUnreadCount,
            checked = isHideUnreadCountEnabled,
            onCheckedChange = onHideUnreadCountToggled,
        )
        SettingsCheckbox(
            title = strings.settingsReduceMotion,
            checked = isReduceMotionEnabled,
            onCheckedChange = onReduceMotionToggled,
        )
        SettingsCheckbox(
            title = strings.settingsForceEnglish,
            checked = isForceEnglishEnabled,
            onCheckedChange = onForceEnglishToggled,
            supportingText = strings.settingsForceEnglishDescription,
        )
    }
}
