package com.prof18.feedflow.android.settings.feedsandaccounts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedsAndAccountsScreen(
    navigateBack: () -> Unit,
    onFeedListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
    navigateToImportExport: () -> Unit,
    navigateToAccounts: () -> Unit,
    navigateToNotifications: () -> Unit,
    navigateToBlockedWords: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsFeedsAndAccounts) },
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
                SettingItem(
                    title = LocalFeedFlowStrings.current.feedsTitle,
                    icon = Icons.AutoMirrored.Default.Feed,
                    onClick = onFeedListClick,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.addFeed,
                    icon = Icons.Outlined.AddCircleOutline,
                    onClick = onAddFeedClick,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.importExportLabel,
                    icon = Icons.Outlined.SwapVert,
                    onClick = navigateToImportExport,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsAccounts,
                    icon = Icons.Outlined.Sync,
                    onClick = navigateToAccounts,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsNotificationsTitle,
                    icon = Icons.Outlined.Notifications,
                    onClick = navigateToNotifications,
                )
            }

            item {
                SettingItem(
                    title = LocalFeedFlowStrings.current.settingsBlockedWords,
                    icon = Icons.Outlined.Report,
                    onClick = navigateToBlockedWords,
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
private fun FeedsAndAccountsScreenContentPreview() {
    FeedFlowTheme {
        FeedsAndAccountsScreen(
            navigateBack = {},
            onFeedListClick = {},
            onAddFeedClick = {},
            navigateToImportExport = {},
            navigateToAccounts = {},
            navigateToNotifications = {},
            navigateToBlockedWords = {},
        )
    }
}
