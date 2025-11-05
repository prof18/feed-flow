package com.prof18.feedflow.desktop.accounts.googledrive

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
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import com.prof18.feedflow.shared.ui.accounts.googledrive.GoogleDriveSyncContent
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch

internal class GoogleDriveSyncScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<GoogleDriveSyncViewModel>() }

        val uiState by viewModel.googleDriveConnectionUiState.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        val errorMessage = LocalFeedFlowStrings.current.googleDriveSyncError

        LaunchedEffect(Unit) {
            viewModel.googleDriveSyncMessageState.collect { event ->
                when (event) {
                    GoogleDriveSynMessages.Error -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = errorMessage,
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }

                    is GoogleDriveSynMessages.ProceedToAuth -> {
                        uriHandler.openUri(event.authorizeUrl)
                    }
                }
            }
        }

        val navigator = LocalNavigator.currentOrThrow

        GoogleDriveSyncContent(
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
                        text = LocalFeedFlowStrings.current.googleDriveSyncCommonDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = Spacing.regular),
                    )

                    SettingItem(
                        modifier = Modifier.padding(top = Spacing.regular),
                        title = LocalFeedFlowStrings.current.googleDriveConnectButton,
                        icon = Icons.Default.Link,
                        onClick = {
                            viewModel.startGoogleDriveAuthFlow()
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
                            label = { Text(LocalFeedFlowStrings.current.googleDriveAuthCodeHint) },
                        )

                        Button(
                            modifier = Modifier
                                .padding(top = Spacing.xsmall)
                                .padding(end = Spacing.regular),
                            enabled = authCode.isNotBlank(),
                            onClick = {
                                viewModel.handleGoogleDriveAuthResponse(authCode)
                            },
                        ) {
                            Text(LocalFeedFlowStrings.current.googleDriveConfirmButton)
                        }
                    }
                }
            },
        )
    }
}
