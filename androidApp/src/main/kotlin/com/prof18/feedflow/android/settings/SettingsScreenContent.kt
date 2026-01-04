package com.prof18.feedflow.android.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.settings.components.ThemeModeDialog
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun SettingsScreenContent(
    themeMode: ThemeMode,
    appVersion: String,
    navigateBack: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    navigateToFeedsAndAccounts: () -> Unit,
    navigateToFeedListSettings: () -> Unit,
    navigateToReadingBehavior: () -> Unit,
    navigateToSyncAndStorage: () -> Unit,
    navigateToWidgetSettings: () -> Unit,
    navigateToExtras: () -> Unit,
    navigateToAboutAndSupport: () -> Unit,
    showWidgetSettings: Boolean,
) {
    Scaffold(
        topBar = {
            SettingsNavBar(navigateBack)
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
                val themeModeLabel = when (themeMode) {
                    ThemeMode.LIGHT -> LocalFeedFlowStrings.current.settingsThemeLight
                    ThemeMode.DARK -> LocalFeedFlowStrings.current.settingsThemeDark
                    ThemeMode.OLED -> LocalFeedFlowStrings.current.settingsThemeOled
                    ThemeMode.SYSTEM -> LocalFeedFlowStrings.current.settingsThemeSystem
                }
                var showDialog by remember { mutableStateOf(false) }

                SettingSelectorItem(
                    title = LocalFeedFlowStrings.current.settingsTheme,
                    currentValueLabel = themeModeLabel,
                    icon = Icons.Outlined.DarkMode,
                    onClick = { showDialog = true },
                )

                if (showDialog) {
                    ThemeModeDialog(
                        currentThemeMode = themeMode,
                        onThemeModeSelected = { selectedThemeMode ->
                            onThemeModeSelected(selectedThemeMode)
                            showDialog = false
                        },
                        dismissDialog = { showDialog = false },
                    )
                }
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsFeedsAndAccounts,
                    icon = Icons.Outlined.Sync,
                    onClick = navigateToFeedsAndAccounts,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsFeedListTitle,
                    icon = Icons.Outlined.Layers,
                    onClick = navigateToFeedListSettings,
                )
            }

            if (showWidgetSettings) {
                item {
                    SettingItem(
                        title = LocalFeedFlowStrings.current.widgetConfigurationTitle,
                        icon = Icons.Outlined.Widgets,
                        onClick = navigateToWidgetSettings,
                    )
                }
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsReadingBehavior,
                    icon = Icons.Outlined.LocalLibrary,
                    onClick = navigateToReadingBehavior,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsSyncAndStorage,
                    icon = Icons.Outlined.Storage,
                    onClick = navigateToSyncAndStorage,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsExtras,
                    icon = Icons.Outlined.Extension,
                    onClick = navigateToExtras,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsAboutAndSupport,
                    icon = Icons.Outlined.Info,
                    onClick = navigateToAboutAndSupport,
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.aboutAppVersion(appVersion),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun SettingsNavBar(navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(LocalFeedFlowStrings.current.settingsTitle)
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navigateBack()
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}

@PreviewPhone
@Composable
private fun SettingsScreenPreview() {
    FeedFlowTheme {
        SettingsScreenContent(
            themeMode = ThemeMode.SYSTEM,
            appVersion = "1.0.0",
            navigateBack = {},
            onThemeModeSelected = {},
            navigateToFeedsAndAccounts = {},
            navigateToFeedListSettings = {},
            navigateToReadingBehavior = {},
            navigateToSyncAndStorage = {},
            navigateToWidgetSettings = {},
            navigateToExtras = {},
            navigateToAboutAndSupport = {},
            showWidgetSettings = true,
        )
    }
}
