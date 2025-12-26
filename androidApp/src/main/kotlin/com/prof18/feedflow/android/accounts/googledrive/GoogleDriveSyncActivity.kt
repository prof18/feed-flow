package com.prof18.feedflow.android.accounts.googledrive

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.shared.presentation.GoogleDriveSyncViewModel
import com.prof18.feedflow.shared.ui.accounts.googledrive.GoogleDriveSyncContent
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class GoogleDriveSyncActivity : BaseThemeActivity() {

    // TODO: handle error messages and stuff
    private val viewModel by viewModel<GoogleDriveSyncViewModel>()

    private val authorizationLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                val authorizationResult = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data)

                if (authorizationResult.accessToken != null) {
                    viewModel.onAuthorizationSuccess()
                } else {
                    Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show()
                    viewModel.onAuthorizationFailed()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show()
                viewModel.onAuthorizationFailed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
                        // Handled via authorization flow
                    }
                }
            }
        }

        GoogleDriveSyncContent(
            uiState = uiState,
            onBackClick = { finish() },
            onBackupClick = { viewModel.triggerBackup() },
            onDisconnectClick = { performUnlink() },
            customPlatformUI = {
                SettingItem(
                    modifier = androidx.compose.ui.Modifier
                        .padding(top = Spacing.regular),
                    title = strings.googleDriveConnectButton,
                    icon = androidx.compose.material.icons.Icons.Default.Link,
                    onClick = { startSignIn() },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        )
    }



    private fun startSignIn() {
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
            .build()

        Identity.getAuthorizationClient(this)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    val pendingIntent = authResult.pendingIntent
                    if (pendingIntent != null) {
                        authorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    }
                } else {
                    viewModel.onAuthorizationSuccess()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performUnlink() {
        lifecycleScope.launch {
            // TODO: dneed toshow a loader, it takes a bit
            // Clear credential state so user can pick a different account next time
            val credentialManager = CredentialManager.create(this@GoogleDriveSyncActivity)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            viewModel.unlink()
        }
    }
}
