package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
internal fun SyncStoragePane(
    isRefreshFeedsOnLaunchEnabled: Boolean,
    autoDeletePeriod: AutoDeletePeriod,
    syncPeriod: SyncPeriod,
    onRefreshFeedsOnLaunchToggled: (Boolean) -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onClearDownloadedArticles: () -> Unit,
    onImportExport: () -> Unit = {},
    onClearOldArticles: () -> Unit = {},
    canClearOldArticles: Boolean = true,
) {
    val strings = LocalFeedFlowStrings.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    SettingsPage(title = strings.settingsSyncAndStorage) {
        SettingsSection {
            SettingsSwitch(
                title = strings.settingsRefreshFeedsOnLaunch,
                checked = isRefreshFeedsOnLaunchEnabled,
                onCheckedChange = onRefreshFeedsOnLaunchToggled,
            )
            SettingsChoice(
                title = strings.settingsSyncPeriod,
                selected = syncPeriod,
                choices = persistentListOf(
                    SyncPeriod.NEVER to strings.settingsSyncPeriodNever,
                    SyncPeriod.FIFTEEN_MINUTES to strings.settingsSyncPeriodFifteenMinutes,
                    SyncPeriod.THIRTY_MINUTES to strings.settingsSyncPeriodThirtyMinutes,
                    SyncPeriod.ONE_HOUR to strings.settingsSyncPeriodOneHour,
                    SyncPeriod.TWO_HOURS to strings.settingsSyncPeriodTwoHours,
                    SyncPeriod.SIX_HOURS to strings.settingsSyncPeriodSixHours,
                    SyncPeriod.TWELVE_HOURS to strings.settingsSyncPeriodTwelveHours,
                    SyncPeriod.ONE_DAY to strings.settingsSyncPeriodOneDay,
                ),
                onSelected = onSyncPeriodSelected,
            )
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
        SettingsSection {
            SettingsActionRow(
                title = strings.importExportLabel,
                onClick = onImportExport,
                showChevron = true,
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
