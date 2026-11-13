package com.prof18.feedflow.android.settings.sync

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import com.prof18.feedflow.android.settings.SettingsE2eIds
import com.prof18.feedflow.android.settings.components.BackgroundSyncRestrictionsSection
import com.prof18.feedflow.android.ui.preview.PreviewPhone
import com.prof18.feedflow.android.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.android.ui.settings.SettingDropdownOption
import com.prof18.feedflow.android.ui.settings.SettingItem
import com.prof18.feedflow.android.ui.settings.SettingSwitchItem
import com.prof18.feedflow.android.ui.settings.SettingsSectionHeader
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.BackgroundSyncRestrictions
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SyncAndRefreshScreenContent(
    syncPeriod: SyncPeriod,
    backgroundSyncRestrictions: BackgroundSyncRestrictions,
    refreshFeedsOnLaunch: Boolean,
    navigateBack: () -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onSyncOnlyOnWifiToggle: (Boolean) -> Unit,
    onSyncOnlyWhenChargingToggle: (Boolean) -> Unit,
    onRefreshFeedsOnLaunchToggle: (Boolean) -> Unit,
    navigateToAccounts: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsSyncAndRefresh) },
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
            if (navigateToAccounts != null) {
                item {
                    SettingItem(
                        modifier = Modifier.testTag(SettingsE2eIds.ACCOUNTS_ROW),
                        title = LocalFeedFlowStrings.current.settingsAccounts,
                        icon = Icons.Outlined.AccountCircle,
                        onClick = navigateToAccounts,
                    )
                }
            }

            item {
                SettingsSectionHeader(title = LocalFeedFlowStrings.current.settingsRefreshSectionTitle)
            }

            item {
                SettingSwitchItem(
                    modifier = Modifier.testTag(SettingsE2eIds.SYNC_REFRESH_ON_LAUNCH),
                    title = LocalFeedFlowStrings.current.settingsRefreshFeedsOnLaunch,
                    isChecked = refreshFeedsOnLaunch,
                    onCheckedChange = onRefreshFeedsOnLaunchToggle,
                )
            }

            item {
                val strings = LocalFeedFlowStrings.current
                CompactSettingDropdownRow(
                    modifier = Modifier.testTag(SettingsE2eIds.SYNC_PERIOD),
                    title = strings.settingsSyncPeriod,
                    currentValue = syncPeriod,
                    options = persistentListOf(
                        SettingDropdownOption(SyncPeriod.NEVER, strings.settingsSyncPeriodNever),
                        SettingDropdownOption(SyncPeriod.FIFTEEN_MINUTES, strings.settingsSyncPeriodFifteenMinutes),
                        SettingDropdownOption(SyncPeriod.THIRTY_MINUTES, strings.settingsSyncPeriodThirtyMinutes),
                        SettingDropdownOption(SyncPeriod.ONE_HOUR, strings.settingsSyncPeriodOneHour),
                        SettingDropdownOption(SyncPeriod.TWO_HOURS, strings.settingsSyncPeriodTwoHours),
                        SettingDropdownOption(SyncPeriod.SIX_HOURS, strings.settingsSyncPeriodSixHours),
                        SettingDropdownOption(SyncPeriod.TWELVE_HOURS, strings.settingsSyncPeriodTwelveHours),
                        SettingDropdownOption(SyncPeriod.ONE_DAY, strings.settingsSyncPeriodOneDay),
                    ),
                    onOptionSelected = onSyncPeriodSelected,
                )
            }

            item {
                BackgroundSyncRestrictionsSection(
                    syncOnlyOnWifi = backgroundSyncRestrictions.syncOnlyOnWifi,
                    syncOnlyWhenCharging = backgroundSyncRestrictions.syncOnlyWhenCharging,
                    onSyncOnlyOnWifiChange = onSyncOnlyOnWifiToggle,
                    onSyncOnlyWhenChargingChange = onSyncOnlyWhenChargingToggle,
                    enabled = syncPeriod != SyncPeriod.NEVER,
                    showHeader = false,
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@PreviewPhone
@Composable
private fun SyncAndRefreshScreenContentPreview() {
    FeedFlowTheme {
        SyncAndRefreshScreenContent(
            syncPeriod = SyncPeriod.ONE_HOUR,
            backgroundSyncRestrictions = BackgroundSyncRestrictions(),
            refreshFeedsOnLaunch = true,
            navigateBack = {},
            navigateToAccounts = {},
            onSyncPeriodSelected = {},
            onSyncOnlyOnWifiToggle = {},
            onSyncOnlyWhenChargingToggle = {},
            onRefreshFeedsOnLaunchToggle = {},
        )
    }
}
