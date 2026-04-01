package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun GeneralPane(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    isMultiPaneEnabled: Boolean,
    onMultiPaneToggled: (Boolean) -> Unit,
    isReduceMotionEnabled: Boolean,
    onReduceMotionToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CompactSettingDropdownRow(
            title = strings.settingsTheme,
            currentValue = themeMode,
            options = persistentListOf(
                SettingDropdownOption(ThemeMode.SYSTEM, strings.settingsThemeSystem),
                SettingDropdownOption(ThemeMode.LIGHT, strings.settingsThemeLight),
                SettingDropdownOption(ThemeMode.DARK, strings.settingsThemeDark),
                SettingDropdownOption(ThemeMode.OLED, strings.settingsThemeOled),
            ),
            onOptionSelected = onThemeModeSelected,
        )

        SettingSwitchItem(
            title = strings.settingsDesktopMultiPaneLayout,
            isChecked = isMultiPaneEnabled,
            onCheckedChange = onMultiPaneToggled,
        )

        SettingSwitchItem(
            title = strings.settingsReduceMotion,
            isChecked = isReduceMotionEnabled,
            onCheckedChange = onReduceMotionToggled,
        )
    }
}

@Preview
@Composable
private fun GeneralPanePreview() {
    FeedFlowTheme {
        GeneralPane(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeSelected = {},
            isMultiPaneEnabled = false,
            onMultiPaneToggled = {},
            isReduceMotionEnabled = false,
            onReduceMotionToggled = {},
        )
    }
}
