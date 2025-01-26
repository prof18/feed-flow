package com.prof18.feedflow.desktop.accounts.icloud

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.presentation.ICloudSyncViewModel
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch

internal class ICloudSyncScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<ICloudSyncViewModel>() }

        val uiState by viewModel.iCloudConnectionUiState.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val errorMessage = LocalFeedFlowStrings.current.errorAccountSync
        LaunchedEffect(Unit) {
            viewModel.syncMessageQueue.collect { event ->
                if (event is SyncResult.Error) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }

        val navigator = LocalNavigator.currentOrThrow

        IcloudSyncContent(
            uiState = uiState,
            onConnectClick = {
                viewModel.setICloudAuth()
            },
            onBackupClick = {
                viewModel.triggerBackup()
            },
            onDisconnectClick = {
                viewModel.unlink()
            },
            onBackClick = {
                navigator.pop()
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
        )
    }
}

@Composable
private fun IcloudSyncContent(
    uiState: AccountConnectionUiState,
    onConnectClick: () -> Unit,
    onBackClick: () -> Unit,
    onBackupClick: () -> Unit,
    onDisconnectClick: () -> Unit,
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
                    Text(text = "iCloud")
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
                    onConnectClick = onConnectClick,
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
private fun DisconnectedView(
    onConnectClick: () -> Unit,
) {
    Column {
        Text(
            text = LocalFeedFlowStrings.current.icloudSyncDescription,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.regular),
        )

        SettingItem(
            modifier = Modifier.padding(top = Spacing.regular),
            title = LocalFeedFlowStrings.current.accountConnectButton,
            icon = Icons.Default.Link,
            onClick = onConnectClick,
        )
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
            text = LocalFeedFlowStrings.current.icloudSyncSuccess,
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
                    text = LocalFeedFlowStrings.current.noIcloudSyncYet,
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
