package com.prof18.feedflow.desktop.accounts.googledrive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import com.prof18.feedflow.shared.ui.accounts.googledrive.GoogleDriveSyncContent
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun GoogleDriveSyncScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<GoogleDriveSyncViewModel>()

    val uiState by viewModel.googleDriveConnectionUiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            }
        }
    }

    val strings = LocalFeedFlowStrings.current

    GoogleDriveSyncContent(
        uiState = uiState,
        onBackClick = navigateBack,
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
                    text = strings.googleDriveDesktopSyncDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = Spacing.regular),
                )

                SettingItem(
                    modifier = Modifier.padding(top = Spacing.regular),
                    title = strings.googleDriveConnectButton,
                    icon = Icons.Default.Link,
                    onClick = {
                        viewModel.startGoogleDriveAuthFlow()
                    },
                )
            }
        },
    )
}
