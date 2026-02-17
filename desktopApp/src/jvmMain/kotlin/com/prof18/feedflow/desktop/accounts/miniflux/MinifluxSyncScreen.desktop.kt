package com.prof18.feedflow.desktop.accounts.miniflux

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.shared.presentation.MinifluxSyncViewModel
import com.prof18.feedflow.shared.ui.accounts.miniflux.MinifluxSyncContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MinifluxSyncScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<MinifluxSyncViewModel>()

    val uiState by viewModel.uiState.collectAsState()
    val isLoginLoading by viewModel.loginLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val strings = LocalFeedFlowStrings.current
    LaunchedEffect(Unit) {
        viewModel.errorState.collect { failure ->
            val errorMessage = when (failure) {
                is NetworkFailure.Unauthorised -> strings.wrongCredentialsErrorMessage
                is DataNotFound -> strings.wrongUrlErrorMessage
                else -> strings.genericErrorMessage
            }

            scope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    MinifluxSyncContent(
        uiState = uiState,
        isLoginLoading = isLoginLoading,
        onBackClick = navigateBack,
        onLoginClick = { serverUrl, username, password ->
            viewModel.login(username, password, serverUrl)
        },
        onDisconnectClick = {
            viewModel.disconnect()
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    )
}
