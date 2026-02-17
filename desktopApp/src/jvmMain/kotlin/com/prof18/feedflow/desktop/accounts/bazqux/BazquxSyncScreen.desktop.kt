package com.prof18.feedflow.desktop.accounts.bazqux

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.core.model.BazquxLoginFailure
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.shared.presentation.BazquxSyncViewModel
import com.prof18.feedflow.shared.ui.accounts.bazqux.BazquxSyncContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun BazquxSyncScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<BazquxSyncViewModel>()

    val uiState by viewModel.uiState.collectAsState()
    val isLoginLoading by viewModel.loginLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val strings = LocalFeedFlowStrings.current
    LaunchedEffect(Unit) {
        viewModel.errorState.collect { failure ->
            val errorMessage = when (failure) {
                BazquxLoginFailure.YearSubscriptionExpired -> strings.bazquxLoginSubscriptionExpired
                BazquxLoginFailure.FreeTrialExpired -> strings.bazquxLoginFreeTrialExpired
                BazquxLoginFailure.Unknown -> strings.genericErrorMessage
                is NetworkFailure.Unauthorised -> strings.wrongCredentialsErrorMessage
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

    BazquxSyncContent(
        uiState = uiState,
        isLoginLoading = isLoginLoading,
        onBackClick = navigateBack,
        onLoginClick = { username, password ->
            viewModel.login(username, password)
        },
        onDisconnectClick = {
            viewModel.disconnect()
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    )
}
