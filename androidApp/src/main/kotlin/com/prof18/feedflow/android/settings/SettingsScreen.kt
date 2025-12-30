package com.prof18.feedflow.android.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.settings.components.ThemeModeDialog
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.presentation.SettingsViewModel
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    navigateToFeedsAndAccounts: () -> Unit,
    navigateToFeedListSettings: () -> Unit,
    navigateToReadingBehavior: () -> Unit,
    navigateToSyncAndStorage: () -> Unit,
    navigateToAboutAndSupport: () -> Unit,
) {
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val settingState by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        themeMode = settingState.themeMode,
        navigateBack = navigateBack,
        onThemeModeSelected = { themeMode ->
            settingsViewModel.updateThemeMode(themeMode)
        },
        navigateToFeedsAndAccounts = navigateToFeedsAndAccounts,
        navigateToFeedListSettings = navigateToFeedListSettings,
        navigateToReadingBehavior = navigateToReadingBehavior,
        navigateToSyncAndStorage = navigateToSyncAndStorage,
        navigateToAboutAndSupport = navigateToAboutAndSupport,
    )
}

@Composable
private fun SettingsScreenContent(
    themeMode: ThemeMode,
    navigateBack: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    navigateToFeedsAndAccounts: () -> Unit,
    navigateToFeedListSettings: () -> Unit,
    navigateToReadingBehavior: () -> Unit,
    navigateToSyncAndStorage: () -> Unit,
    navigateToAboutAndSupport: () -> Unit,
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
                        onThemeModeSelected = onThemeModeSelected,
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
                    title = LocalFeedFlowStrings.current.settingsAboutAndSupport,
                    icon = Icons.Outlined.Info,
                    onClick = navigateToAboutAndSupport,
                )
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
            navigateBack = {},
            onThemeModeSelected = {},
            navigateToFeedsAndAccounts = {},
            navigateToFeedListSettings = {},
            navigateToReadingBehavior = {},
            navigateToSyncAndStorage = {},
            navigateToAboutAndSupport = {},
        )
    }
}
