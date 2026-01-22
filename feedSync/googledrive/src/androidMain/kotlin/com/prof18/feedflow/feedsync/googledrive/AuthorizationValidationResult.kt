package com.prof18.feedflow.feedsync.googledrive

import android.app.PendingIntent

sealed class AuthorizationValidationResult {
    data object Valid : AuthorizationValidationResult()
    data class NeedsReAuth(val pendingIntent: PendingIntent) : AuthorizationValidationResult()
    data object Failed : AuthorizationValidationResult()
}
