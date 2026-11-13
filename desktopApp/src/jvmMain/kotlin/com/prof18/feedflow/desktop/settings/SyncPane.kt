package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.SyncPeriod

@Composable
internal fun SyncPane(
    currentAccountTitle: String,
    onAccounts: () -> Unit,
    isRefreshFeedsOnLaunchEnabled: Boolean,
    syncPeriod: SyncPeriod,
    onRefreshFeedsOnLaunchToggled: (Boolean) -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    SettingsPage(title = strings.settingsSyncAndRefresh) {
        SettingsSection {
            SettingsActionRow(
                title = strings.settingsAccounts,
                supportingText = currentAccountTitle,
                onClick = onAccounts,
                showChevron = true,
            )
        }
        SyncRefreshSection(
            isRefreshFeedsOnLaunchEnabled = isRefreshFeedsOnLaunchEnabled,
            syncPeriod = syncPeriod,
            onRefreshFeedsOnLaunchToggled = onRefreshFeedsOnLaunchToggled,
            onSyncPeriodSelected = onSyncPeriodSelected,
        )
    }
}
