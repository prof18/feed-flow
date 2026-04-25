package com.prof18.feedflow.android.settings.appearance

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun AppearanceScreenContent(
    navigateBack: () -> Unit,
    themeMode: ThemeMode,
    isMultiPaneEnabled: Boolean,
    isReduceMotionEnabled: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onMultiPaneToggled: (Boolean) -> Unit,
    onReduceMotionToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsAppearance) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
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
            }

            item {
                SettingSwitchItem(
                    title = strings.settingsThreePaneLayout,
                    isChecked = isMultiPaneEnabled,
                    onCheckedChange = onMultiPaneToggled,
                )
            }

            item {
                SettingSwitchItem(
                    title = strings.settingsReduceMotion,
                    isChecked = isReduceMotionEnabled,
                    onCheckedChange = onReduceMotionToggled,
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Preview
@Composable
private fun AppearanceScreenContentPreview() {
    FeedFlowTheme {
        AppearanceScreenContent(
            navigateBack = {},
            themeMode = ThemeMode.SYSTEM,
            isMultiPaneEnabled = true,
            isReduceMotionEnabled = false,
            onThemeModeSelected = {},
            onMultiPaneToggled = {},
            onReduceMotionToggled = {},
        )
    }
}
