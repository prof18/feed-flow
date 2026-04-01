package com.prof18.feedflow.android.settings.syncstorage

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import coil3.imageLoader
import com.prof18.feedflow.android.settings.components.BackgroundSyncRestrictionsSection
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.domain.model.BackgroundSyncRestrictions
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.ConfirmationSettingItem
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SyncAndStorageScreenContent(
    syncPeriod: SyncPeriod,
    backgroundSyncRestrictions: BackgroundSyncRestrictions,
    autoDeletePeriod: AutoDeletePeriod,
    refreshFeedsOnLaunch: Boolean,
    showRssParsingErrors: Boolean,
    navigateBack: () -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onSyncOnlyOnWifiToggle: (Boolean) -> Unit,
    onSyncOnlyWhenChargingToggle: (Boolean) -> Unit,
    onRefreshFeedsOnLaunchToggle: (Boolean) -> Unit,
    onShowRssParsingErrorsToggle: (Boolean) -> Unit,
    onClearDownloadedArticles: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsSyncAndStorage) },
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
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsRefreshFeedsOnLaunch,
                    isChecked = refreshFeedsOnLaunch,
                    onCheckedChange = onRefreshFeedsOnLaunchToggle,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsShowRssParsingErrors,
                    isChecked = showRssParsingErrors,
                    onCheckedChange = onShowRssParsingErrorsToggle,
                )
            }

            item {
                val strings = LocalFeedFlowStrings.current
                CompactSettingDropdownRow(
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
                    showHeader = false,
                )
            }

            item {
                val strings = LocalFeedFlowStrings.current
                CompactSettingDropdownRow(
                    title = strings.settingsAutoDelete,
                    currentValue = autoDeletePeriod,
                    options = persistentListOf(
                        SettingDropdownOption(AutoDeletePeriod.DISABLED, strings.settingsAutoDeletePeriodDisabled),
                        SettingDropdownOption(AutoDeletePeriod.ONE_DAY, strings.settingsAutoDeletePeriodOneDay),
                        SettingDropdownOption(AutoDeletePeriod.ONE_WEEK, strings.settingsAutoDeletePeriodOneWeek),
                        SettingDropdownOption(AutoDeletePeriod.TWO_WEEKS, strings.settingsAutoDeletePeriodTwoWeeks),
                        SettingDropdownOption(AutoDeletePeriod.ONE_MONTH, strings.settingsAutoDeletePeriodOneMonth),
                    ),
                    onOptionSelected = onAutoDeletePeriodSelected,
                )
            }

            item {
                Text(
                    text = LocalFeedFlowStrings.current.settingsDangerTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.padding(Spacing.regular),
                )
            }

            item {
                ConfirmationSettingItem(
                    title = LocalFeedFlowStrings.current.settingsClearDownloadedArticles,
                    dialogTitle = LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogTitle,
                    dialogMessage = LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogMessage,
                    onConfirm = onClearDownloadedArticles,
                )
            }

            item {
                ConfirmationSettingItem(
                    title = LocalFeedFlowStrings.current.settingsClearImageCache,
                    dialogTitle = LocalFeedFlowStrings.current.settingsClearImageCacheDialogTitle,
                    dialogMessage = LocalFeedFlowStrings.current.settingsClearImageCacheDialogMessage,
                    onConfirm = {
                        val imageLoader = context.applicationContext.imageLoader
                        imageLoader.memoryCache?.clear()
                        imageLoader.diskCache?.clear()
                    },
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
private fun SyncAndStorageScreenContentPreview() {
    FeedFlowTheme {
        SyncAndStorageScreenContent(
            navigateBack = {},
            syncPeriod = SyncPeriod.ONE_HOUR,
            backgroundSyncRestrictions = BackgroundSyncRestrictions(),
            autoDeletePeriod = AutoDeletePeriod.ONE_WEEK,
            refreshFeedsOnLaunch = true,
            onSyncPeriodSelected = {},
            onAutoDeletePeriodSelected = {},
            onSyncOnlyOnWifiToggle = {},
            onSyncOnlyWhenChargingToggle = {},
            onRefreshFeedsOnLaunchToggle = {},
            showRssParsingErrors = true,
            onShowRssParsingErrorsToggle = {},
            onClearDownloadedArticles = {},
        )
    }
}
