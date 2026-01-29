package com.prof18.feedflow.android.settings.syncstorage

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import coil3.imageLoader
import com.prof18.feedflow.android.settings.components.AutoDeletePeriodDialog
import com.prof18.feedflow.android.settings.components.SyncPeriodSelector
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.settings.ConfirmationSettingItem
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun SyncAndStorageScreenContent(
    syncPeriod: SyncPeriod,
    autoDeletePeriod: AutoDeletePeriod,
    refreshFeedsOnLaunch: Boolean,
    showRssParsingErrors: Boolean,
    navigateBack: () -> Unit,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
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
                    icon = Icons.Outlined.Sync,
                    isChecked = refreshFeedsOnLaunch,
                    onCheckedChange = onRefreshFeedsOnLaunchToggle,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsShowRssParsingErrors,
                    icon = Icons.Outlined.ErrorOutline,
                    isChecked = showRssParsingErrors,
                    onCheckedChange = onShowRssParsingErrorsToggle,
                )
            }

            item {
                SyncPeriodSelector(
                    currentPeriod = syncPeriod,
                    onPeriodSelected = onSyncPeriodSelected,
                )
            }

            item {
                val autoDeleteLabel = when (autoDeletePeriod) {
                    AutoDeletePeriod.DISABLED -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodDisabled
                    AutoDeletePeriod.ONE_DAY -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneDay
                    AutoDeletePeriod.ONE_WEEK -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneWeek
                    AutoDeletePeriod.TWO_WEEKS -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodTwoWeeks
                    AutoDeletePeriod.ONE_MONTH -> LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneMonth
                }
                var showDialog by remember { mutableStateOf(false) }

                SettingSelectorItem(
                    title = LocalFeedFlowStrings.current.settingsAutoDelete,
                    currentValueLabel = autoDeleteLabel,
                    icon = Icons.Outlined.DeleteSweep,
                    onClick = { showDialog = true },
                )

                if (showDialog) {
                    AutoDeletePeriodDialog(
                        currentPeriod = autoDeletePeriod,
                        onPeriodSelected = onAutoDeletePeriodSelected,
                        dismissDialog = { showDialog = false },
                    )
                }
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
                    icon = Icons.Outlined.DeleteSweep,
                    dialogTitle = LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogTitle,
                    dialogMessage = LocalFeedFlowStrings.current.settingsClearDownloadedArticlesDialogMessage,
                    onConfirm = onClearDownloadedArticles,
                )
            }

            item {
                ConfirmationSettingItem(
                    title = LocalFeedFlowStrings.current.settingsClearImageCache,
                    icon = Icons.Outlined.Image,
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
            autoDeletePeriod = AutoDeletePeriod.ONE_WEEK,
            refreshFeedsOnLaunch = true,
            onSyncPeriodSelected = {},
            onAutoDeletePeriodSelected = {},
            onRefreshFeedsOnLaunchToggle = {},
            showRssParsingErrors = true,
            onShowRssParsingErrorsToggle = {},
            onClearDownloadedArticles = {},
        )
    }
}
