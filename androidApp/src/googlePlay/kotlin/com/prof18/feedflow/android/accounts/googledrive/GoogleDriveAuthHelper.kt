package com.prof18.feedflow.android.accounts.googledrive

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class GoogleDriveAuthHelper(
    private val activity: ComponentActivity,
) {
    fun createAuthorizationLauncher(
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ): ActivityResultLauncher<IntentSenderRequest> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            try {
                val authorizationResult = Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(result.data)

                if (authorizationResult.accessToken != null) {
                    onSuccess()
                } else {
                    onFailure()
                }
            } catch (_: Exception) {
                onFailure()
            }
        }
    }

    fun startSignIn(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.appdata")))
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    val pendingIntent = authResult.pendingIntent
                    if (pendingIntent != null) {
                        launcher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                        )
                    }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    suspend fun performUnlink() {
        val credentialManager = CredentialManager.create(activity)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
