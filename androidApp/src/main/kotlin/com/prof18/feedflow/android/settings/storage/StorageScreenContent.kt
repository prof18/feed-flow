package com.prof18.feedflow.android.settings.storage

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.prof18.feedflow.android.ui.preview.PreviewPhone
import com.prof18.feedflow.android.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.android.ui.settings.ConfirmationSettingItem
import com.prof18.feedflow.android.ui.settings.SettingDropdownOption
import com.prof18.feedflow.android.ui.settings.SettingsSectionHeader
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.AutoDeletePeriod
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun StorageScreenContent(
    autoDeletePeriod: AutoDeletePeriod,
    navigateBack: () -> Unit,
    onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    onClearDownloadedArticles: () -> Unit,
    onClearImageCache: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsStorage) },
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
                val strings = LocalFeedFlowStrings.current
                CompactSettingDropdownRow(
                    modifier = Modifier.testTag(SettingsE2eIds.SYNC_AUTO_DELETE),
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
                SettingsSectionHeader(title = LocalFeedFlowStrings.current.settingsClearDataTitle)
            }

            item {
                ConfirmationSettingItem(
                    modifier = Modifier.testTag(SettingsE2eIds.SYNC_CLEAR_DOWNLOADED),
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
                    onConfirm = onClearImageCache,
                    isDestructive = false,
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
private fun StorageScreenContentPreview() {
    FeedFlowTheme {
        StorageScreenContent(
            autoDeletePeriod = AutoDeletePeriod.ONE_WEEK,
            navigateBack = {},
            onAutoDeletePeriodSelected = {},
            onClearDownloadedArticles = {},
            onClearImageCache = {},
        )
    }
}
