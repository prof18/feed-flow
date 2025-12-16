package com.prof18.feedflow.desktop.accounts.dropbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.DropboxSynMessages
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.generateUniqueKey
import com.prof18.feedflow.shared.presentation.DropboxSyncViewModel
import com.prof18.feedflow.shared.ui.accounts.dropbox.DropboxSyncContent
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch

internal class DropboxSyncScreen : Screen {

    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<DropboxSyncViewModel>() }

        val uiState by viewModel.dropboxConnectionUiState.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        val errorMessage = LocalFeedFlowStrings.current.dropboxSyncError
        val codeExpiredMessage = LocalFeedFlowStrings.current.dropboxAuthCodeExpired

        LaunchedEffect(Unit) {
            viewModel.dropboxSyncMessageState.collect { event ->
                when (event) {
                    DropboxSynMessages.Error -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = errorMessage,
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }

                    DropboxSynMessages.CodeExpired -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = codeExpiredMessage,
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }

                    is DropboxSynMessages.ProceedToAuth -> {
                        uriHandler.openUri(event.authorizeUrl)
                    }
                }
            }
        }

        val navigator = LocalNavigator.currentOrThrow

        DropboxSyncContent(
            uiState = uiState,
            onBackClick = {
                navigator.pop()
            },
            onBackupClick = {
                viewModel.triggerBackup()
            },
            onDisconnectClick = {
                viewModel.disconnect()
            },
            customPlatformUI = {
                Column(
                    modifier = Modifier.padding(top = Spacing.regular),
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.dropboxSyncDesktopDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = Spacing.regular),
                    )

                    SettingItem(
                        modifier = Modifier.padding(top = Spacing.regular),
                        title = LocalFeedFlowStrings.current.accountConnectButton,
                        icon = Icons.Default.Link,
                        onClick = {
                            viewModel.startDropboxAuthFlow()
                        },
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = Spacing.small),
                    ) {
                        var authCode by remember {
                            mutableStateOf("")
                        }

                        OutlinedTextField(
                            modifier = Modifier
                                .padding(horizontal = Spacing.regular)
                                .weight(1f),
                            value = authCode,
                            onValueChange = { authCode = it },
                            label = { Text(LocalFeedFlowStrings.current.dropboxAuthCodeHint) },
                        )

                        Button(
                            modifier = Modifier
                                .padding(top = Spacing.xsmall)
                                .padding(end = Spacing.regular),
                            enabled = authCode.isNotBlank(),
                            onClick = {
                                viewModel.handleDropboxAuthResponse(authCode)
                            },
                        ) {
                            Text(LocalFeedFlowStrings.current.dropboxConfirmButton)
                        }
                    }
                }
            },
        )
    }
}
