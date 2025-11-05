package com.prof18.feedflow.android.accounts.googledrive

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import com.prof18.feedflow.shared.presentation.getAccessTokenFromAccount
import com.prof18.feedflow.shared.presentation.getFreshAccessToken
import com.prof18.feedflow.shared.presentation.getGoogleSignInClient
import com.prof18.feedflow.shared.presentation.startGoogleDriveAuth
import com.prof18.feedflow.shared.ui.accounts.googledrive.GoogleDriveSyncContent
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class GoogleDriveSyncActivity : BaseThemeActivity() {

    private val viewModel by viewModel<GoogleDriveSyncViewModel>()
    private lateinit var googleSignInClient: GoogleSignInClient

    @Composable
    override fun Content() {
        val uiState by viewModel.googleDriveConnectionUiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val strings = LocalFeedFlowStrings.current

        LaunchedEffect(Unit) {
            viewModel.googleDriveSyncMessageState.collect { message ->
                when (message) {
                    is GoogleDriveSynMessages.Error -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = strings.googleDriveSyncError,
                            )
                        }
                    }
                    is GoogleDriveSynMessages.ProceedToAuth -> {
                        // For Android, we handle this through Google Sign-In intent
                    }
                }
            }
        }

        GoogleDriveSyncContent(
            uiState = uiState,
            onBackClick = { finish() },
            onBackupClick = {
                lifecycleScope.launch {
                    val freshToken = getFreshAccessToken(this@GoogleDriveSyncActivity)
                    if (freshToken != null) {
                        viewModel.updateAccessToken(freshToken)
                    }
                    viewModel.triggerBackup()
                }
            },
            onDisconnectClick = { viewModel.unlink() },
            customPlatformUI = {
                SettingItem(
                    modifier = androidx.compose.ui.Modifier
                        .padding(top = Spacing.regular),
                    title = strings.googleDriveConnectButton,
                    // TODO: add icon
                    icon = androidx.compose.material.icons.Icons.Default.Link,
                    onClick = {
                        startGoogleDriveAuth(this@GoogleDriveSyncActivity, googleSignInClient)
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleSignInClient = getGoogleSignInClient(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == com.prof18.feedflow.shared.presentation.RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    // Get the actual OAuth access token for Google Drive API
                    lifecycleScope.launch {
                        val accessToken = getAccessTokenFromAccount(this@GoogleDriveSyncActivity, account)
                        if (accessToken != null) {
                            viewModel.saveGoogleDriveAuth(accessToken)
                        }
                    }
                }
            } catch (e: ApiException) {
                // Sign-in failed
            }
        }
    }
}
