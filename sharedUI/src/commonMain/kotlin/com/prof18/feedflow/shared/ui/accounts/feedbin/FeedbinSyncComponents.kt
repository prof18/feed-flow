package com.prof18.feedflow.shared.ui.accounts.feedbin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun FeedbinSyncContent(
    uiState: AccountConnectionUiState,
    isLoginLoading: Boolean,
    onBackClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onLoginClick: (username: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
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
                    Text(text = "Feedbin")
                },
            )
        },
        snackbarHost = snackbarHost,
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
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

            Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
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
    onLoginClick: (username: String, password: String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollableState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollableState)
            .padding(Spacing.regular),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(LocalFeedFlowStrings.current.accountTextFieldUsername) },
            supportingText = {
                Text(LocalFeedFlowStrings.current.feedbinUsernameHint)
            },
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
                Text(text = LocalFeedFlowStrings.current.feedbinPasswordHint)
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
                onLoginClick(username, password)
            },
            modifier = Modifier
                .fillMaxWidth(),
            enabled = !isLoginLoading && username.isNotBlank() && password.isNotBlank(),
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
    val scrollableState = rememberScrollState()
    Column(
        Modifier.verticalScroll(scrollableState),
    ) {
        Text(
            text = LocalFeedFlowStrings.current.feedbinAccountConnected,
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
                    text = LocalFeedFlowStrings.current.noFeedbinSyncYet,
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
                        text = LocalFeedFlowStrings.current.feedbinLastSync(lastDownload),
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
