package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.ConfirmationSettingItem
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
internal fun SyncStoragePane(
    isRefreshFeedsOnLaunchEnabled: Boolean,
    isShowRssParsingErrorsEnabled: Boolean,
    autoDeletePeriod: AutoDeletePeriod,
    onRefreshFeedsOnLaunchToggled: (Boolean) -> Unit,
    onShowRssParsingErrorsToggled: (Boolean) -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onClearDownloadedArticles: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalFeedFlowStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingSwitchItem(
            title = strings.settingsRefreshFeedsOnLaunch,
            isChecked = isRefreshFeedsOnLaunchEnabled,
            onCheckedChange = onRefreshFeedsOnLaunchToggled,
        )

        SettingSwitchItem(
            title = strings.settingsShowRssParsingErrors,
            isChecked = isShowRssParsingErrorsEnabled,
            onCheckedChange = onShowRssParsingErrorsToggled,
        )

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

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.small))

        Text(
            text = strings.settingsDangerTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.padding(Spacing.regular),
        )

        ConfirmationSettingItem(
            title = strings.settingsClearDownloadedArticles,
            dialogTitle = strings.settingsClearDownloadedArticlesDialogTitle,
            dialogMessage = strings.settingsClearDownloadedArticlesDialogMessage,
            onConfirm = onClearDownloadedArticles,
        )

        ConfirmationSettingItem(
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

@Preview
@Composable
private fun SyncStoragePanePreview() {
    FeedFlowTheme {
        SyncStoragePane(
            isRefreshFeedsOnLaunchEnabled = false,
            isShowRssParsingErrorsEnabled = false,
            autoDeletePeriod = AutoDeletePeriod.DISABLED,
            onRefreshFeedsOnLaunchToggled = {},
            onShowRssParsingErrorsToggled = {},
            onAutoDeletePeriodSelected = {},
            onClearDownloadedArticles = {},
        )
    }
}
