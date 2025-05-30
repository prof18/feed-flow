package com.prof18.feedflow.shared.ui.accounts.freshrss

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun FreshRssSyncContent(
    uiState: AccountConnectionUiState,
    isLoginLoading: Boolean,
    onBackClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onLoginClick: (serverUrl: String, username: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {
    },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        onBackClick()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = {
                    Text(text = "FreshRSS")
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
                    onDisconnectClick = onDisconnectClick,
                )

                AccountConnectionUiState.Loading -> LoadingView()
                AccountConnectionUiState.Unlinked -> DisconnectedView(
                    isLoginLoading = isLoginLoading,
                    onLoginClick = onLoginClick,
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
fun DisconnectedView(
    isLoginLoading: Boolean,
    modifier: Modifier = Modifier,
    onLoginClick: (serverUrl: String, username: String, password: String) -> Unit,
) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.regular),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = serverUrl,
            supportingText = {
                Text(LocalFeedFlowStrings.current.freshRssUrlHint("https://mydomain.com/api/greader.php"))
            },
            onValueChange = { serverUrl = it },
            label = { Text(LocalFeedFlowStrings.current.accountTextFieldServerUrl) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.regular),
            singleLine = true,
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(LocalFeedFlowStrings.current.accountTextFieldUsername) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.regular),
            singleLine = true,
        )

        var passwordVisible by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(LocalFeedFlowStrings.current.accountTextFieldPassword) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.medium),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            supportingText = {
                Text(text = LocalFeedFlowStrings.current.freshRssPasswordHint)
            },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            singleLine = true,
        )

        Button(
            onClick = {
                keyboardController?.hide()
                onLoginClick(serverUrl, username, password)
            },
            modifier = Modifier
                .fillMaxWidth(),
            enabled = !isLoginLoading && serverUrl.isNotBlank() &&
                username.isNotBlank() && password.isNotBlank(),
        ) {
            if (isLoginLoading) {
                CircularProgressIndicator()
            } else {
                Text(LocalFeedFlowStrings.current.accountConnectButton)
            }
        }
    }
}

@Composable
private fun ConnectedView(
    uiState: AccountConnectionUiState.Linked,
    onDisconnectClick: () -> Unit,
) {
    Column {
        Text(
            text = LocalFeedFlowStrings.current.freshRssAccountConnected,
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
                    text = LocalFeedFlowStrings.current.noFreshRssSyncYet,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(horizontal = Spacing.regular)
                        .padding(top = Spacing.small),
                )
            }

            is AccountSyncUIState.Synced -> {
                val lastDownload = syncState.lastDownloadDate
                if (lastDownload != null) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacing.regular)
                            .padding(top = Spacing.small),
                        text = LocalFeedFlowStrings.current.freshRssLastSync(lastDownload),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        SettingItem(
            modifier = Modifier.padding(top = Spacing.regular),
            title = LocalFeedFlowStrings.current.accountDisconnectButton,
            isEnabled = uiState.syncState !is AccountSyncUIState.Loading,
            icon = Icons.Default.LinkOff,
            onClick = onDisconnectClick,
        )
    }
}
