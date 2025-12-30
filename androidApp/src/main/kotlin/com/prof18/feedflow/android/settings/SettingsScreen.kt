package com.prof18.feedflow.android.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.presentation.MainSettingsViewModel
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
    val settingsViewModel = koinViewModel<MainSettingsViewModel>()
    val settingState by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val appVersion = packageInfo.versionName ?: ""

    SettingsScreenContent(
        themeMode = settingState.themeMode,
        appVersion = appVersion,
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
