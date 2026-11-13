package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
internal fun StoragePane(
    autoDeletePeriod: AutoDeletePeriod,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onClearDownloadedArticles: () -> Unit,
    onClearOldArticles: () -> Unit = {},
    canClearOldArticles: Boolean = true,
) {
    val strings = LocalFeedFlowStrings.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    SettingsPage(title = strings.settingsStorage) {
        SettingsSection {
            SettingsChoice(
                title = strings.settingsAutoDelete,
                selected = autoDeletePeriod,
                choices = persistentListOf(
                    AutoDeletePeriod.DISABLED to strings.settingsAutoDeletePeriodDisabled,
                    AutoDeletePeriod.ONE_DAY to strings.settingsAutoDeletePeriodOneDay,
                    AutoDeletePeriod.ONE_WEEK to strings.settingsAutoDeletePeriodOneWeek,
                    AutoDeletePeriod.TWO_WEEKS to strings.settingsAutoDeletePeriodTwoWeeks,
                    AutoDeletePeriod.ONE_MONTH to strings.settingsAutoDeletePeriodOneMonth,
                ),
                onSelected = onAutoDeletePeriodSelected,
            )
        }
        SettingsSection(title = strings.settingsDangerTitle) {
            ConfirmationSetting(
                title = strings.clearOldArticlesButton,
                dialogTitle = strings.clearOldArticlesButton,
                dialogMessage = strings.clearOldArticlesDialogMessage,
                onConfirm = onClearOldArticles,
                enabled = canClearOldArticles,
            )
            ConfirmationSetting(
                title = strings.settingsClearDownloadedArticles,
                dialogTitle = strings.settingsClearDownloadedArticlesDialogTitle,
                dialogMessage = strings.settingsClearDownloadedArticlesDialogMessage,
                onConfirm = onClearDownloadedArticles,
            )
            ConfirmationSetting(
                title = strings.settingsClearImageCache,
                dialogTitle = strings.settingsClearImageCacheDialogTitle,
                dialogMessage = strings.settingsClearImageCacheDialogMessage,
                onConfirm = {
                    scope.launch {
                        val imageLoader = SingletonImageLoader.get(context)
                        imageLoader.memoryCache?.clear()
                        imageLoader.diskCache?.clear()
                    }
                },
            )
        }
    }
}
