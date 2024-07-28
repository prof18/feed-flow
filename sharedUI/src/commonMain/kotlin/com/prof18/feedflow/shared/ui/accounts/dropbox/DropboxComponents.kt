package com.prof18.feedflow.shared.ui.accounts.dropbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun DropboxSyncContent(
    uiState: AccountConnectionUiState,
    onBackClick: () -> Unit,
    onBackupClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    customPlatformUI: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = {
                    Text(text = "Dropbox")
                },
            )
        },
        snackbarHost = snackbarHost,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = Spacing.regular),
        ) {
            when (uiState) {
                is AccountConnectionUiState.Linked -> ConnectedView(
                    uiState = uiState,
                    onBackupClick = onBackupClick,
                    onDisconnectClick = onDisconnectClick,
                )

                AccountConnectionUiState.Loading -> LoadingView()
                AccountConnectionUiState.Unlinked -> DisconnectedView(
                    customPlatformUI = customPlatformUI,
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ConnectedView(
    uiState: AccountConnectionUiState.Linked,
    onBackupClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    Column {
        Text(
            text = LocalFeedFlowStrings.current.dropboxSyncSuccess,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        when (val syncState = uiState.syncState) {
            AccountSyncUIState.Loading -> {
                Row(
                    modifier = Modifier
                        .padding(
                            top = Spacing.regular,
                            start = Spacing.regular,
                            end = Spacing.regular,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = LocalFeedFlowStrings.current.accountRefreshProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = Spacing.small),
                    )
                }
            }

            AccountSyncUIState.None -> {
                Text(
                    text = LocalFeedFlowStrings.current.noDropboxSyncYet,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(horizontal = Spacing.regular)
                        .padding(top = Spacing.small),
                )
            }

            is AccountSyncUIState.Synced -> {
                val lastUpload = syncState.lastUploadDate
                if (lastUpload != null) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacing.regular)
                            .padding(top = Spacing.small),
                        text = LocalFeedFlowStrings.current.lastUpload(lastUpload),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                val lastDownload = syncState.lastDownloadDate
                if (lastDownload != null) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacing.regular)
                            .padding(top = Spacing.small),
                        text = LocalFeedFlowStrings.current.lastDownload(lastDownload),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        SettingItem(
            modifier = Modifier
                .padding(top = Spacing.regular),
            isEnabled = uiState.syncState !is AccountSyncUIState.Loading,
            title = LocalFeedFlowStrings.current.backupButton,
            icon = Icons.Default.FileUpload,
            onClick = onBackupClick,
        )

        SettingItem(
            title = LocalFeedFlowStrings.current.accountDisconnectButton,
            isEnabled = uiState.syncState !is AccountSyncUIState.Loading,
            icon = Icons.Default.LinkOff,
            onClick = onDisconnectClick,
        )
    }
}

@Composable
private fun DisconnectedView(
    customPlatformUI: @Composable () -> Unit,
) {
    Column {
        Text(
            text = LocalFeedFlowStrings.current.dropboxSyncCommonDescription,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        customPlatformUI()
    }
}
