package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.ui.settings.ConfirmationSettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
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
            icon = Icons.Outlined.Sync,
            isChecked = isRefreshFeedsOnLaunchEnabled,
            onCheckedChange = onRefreshFeedsOnLaunchToggled,
        )

        SettingSwitchItem(
            title = strings.settingsShowRssParsingErrors,
            icon = Icons.Outlined.ErrorOutline,
            isChecked = isShowRssParsingErrorsEnabled,
            onCheckedChange = onShowRssParsingErrorsToggled,
        )

        val autoDeleteLabel = when (autoDeletePeriod) {
            AutoDeletePeriod.DISABLED -> strings.settingsAutoDeletePeriodDisabled
            AutoDeletePeriod.ONE_DAY -> strings.settingsAutoDeletePeriodOneDay
            AutoDeletePeriod.ONE_WEEK -> strings.settingsAutoDeletePeriodOneWeek
            AutoDeletePeriod.TWO_WEEKS -> strings.settingsAutoDeletePeriodTwoWeeks
            AutoDeletePeriod.ONE_MONTH -> strings.settingsAutoDeletePeriodOneMonth
        }

        var showAutoDeleteDialog by remember { mutableStateOf(false) }

        SettingSelectorItem(
            title = strings.settingsAutoDelete,
            currentValueLabel = autoDeleteLabel,
            icon = Icons.Outlined.DeleteSweep,
            onClick = { showAutoDeleteDialog = true },
        )

        if (showAutoDeleteDialog) {
            AutoDeletePeriodDialog(
                currentPeriod = autoDeletePeriod,
                onPeriodSelected = {
                    onAutoDeletePeriodSelected(it)
                    showAutoDeleteDialog = false
                },
                onDismiss = { showAutoDeleteDialog = false },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.small))

        Text(
            text = strings.settingsDangerTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.padding(Spacing.regular),
        )

        ConfirmationSettingItem(
            title = strings.settingsClearDownloadedArticles,
            icon = Icons.Outlined.DeleteSweep,
            dialogTitle = strings.settingsClearDownloadedArticlesDialogTitle,
            dialogMessage = strings.settingsClearDownloadedArticlesDialogMessage,
            onConfirm = onClearDownloadedArticles,
        )

        ConfirmationSettingItem(
            title = strings.settingsClearImageCache,
            icon = Icons.Outlined.Image,
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

@Composable
private fun AutoDeletePeriodDialog(
    currentPeriod: AutoDeletePeriod,
    onPeriodSelected: (AutoDeletePeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val periods = listOf(
        AutoDeletePeriod.DISABLED to strings.settingsAutoDeletePeriodDisabled,
        AutoDeletePeriod.ONE_DAY to strings.settingsAutoDeletePeriodOneDay,
        AutoDeletePeriod.ONE_WEEK to strings.settingsAutoDeletePeriodOneWeek,
        AutoDeletePeriod.TWO_WEEKS to strings.settingsAutoDeletePeriodTwoWeeks,
        AutoDeletePeriod.ONE_MONTH to strings.settingsAutoDeletePeriodOneMonth,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingsAutoDelete) },
        text = {
            Column {
                periods.forEach { (period, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentPeriod == period,
                                onClick = { onPeriodSelected(period) },
                            )
                            .padding(vertical = Spacing.small),
                    ) {
                        RadioButton(
                            selected = currentPeriod == period,
                            onClick = { onPeriodSelected(period) },
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = Spacing.small),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelButton)
            }
        },
    )
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
