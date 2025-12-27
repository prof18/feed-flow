package com.prof18.feedflow.android.accounts.googledrive

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts

@Suppress("UnusedParameter", "UnusedPrivateProperty")
class GoogleDriveAuthHelper(
    private val activity: ComponentActivity,
) {
    fun createAuthorizationLauncher(
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ): ActivityResultLauncher<IntentSenderRequest> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) {
            // no-op
        }
    }

    fun startSignIn(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        // no-op
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun performUnlink() {
        // no-op
    }
}
