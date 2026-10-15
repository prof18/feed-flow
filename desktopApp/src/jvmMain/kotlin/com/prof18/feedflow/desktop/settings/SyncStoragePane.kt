package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Divider

@Composable
internal fun SyncStoragePane(
    isRefreshFeedsOnLaunchEnabled: Boolean,
    autoDeletePeriod: AutoDeletePeriod,
    syncPeriod: SyncPeriod,
    onRefreshFeedsOnLaunchToggled: (Boolean) -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onClearDownloadedArticles: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    SettingsScrollPane(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SettingsGroup(strings.settingsSyncAndStorage)
        SettingsCheckbox(
            strings.settingsRefreshFeedsOnLaunch,
            isRefreshFeedsOnLaunchEnabled,
            onRefreshFeedsOnLaunchToggled,
        )
        JewelSettingsChoice(
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
        JewelSettingsChoice(
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

        Divider(
            orientation = org.jetbrains.jewel.ui.Orientation.Horizontal,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        SettingsGroup(strings.settingsDangerTitle)
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
