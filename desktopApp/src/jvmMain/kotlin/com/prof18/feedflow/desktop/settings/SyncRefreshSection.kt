package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SyncRefreshSection(
    isRefreshFeedsOnLaunchEnabled: Boolean,
    syncPeriod: SyncPeriod,
    onRefreshFeedsOnLaunchToggled: (Boolean) -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    SettingsSection(title = strings.settingsRefreshSectionTitle) {
        SettingsSwitch(
            title = strings.settingsRefreshFeedsOnLaunch,
            checked = isRefreshFeedsOnLaunchEnabled,
            onCheckedChange = onRefreshFeedsOnLaunchToggled,
        )
        SettingsChoice(
            title = strings.settingsDesktopRefreshWhileOpen,
            supportingText = strings.settingsDesktopRefreshWhileOpenDescription,
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
    }
}
