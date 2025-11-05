package com.prof18.feedflow.shared.presentation

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveConstants

fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(
            Scope("https://www.googleapis.com/auth/drive.file"),
            Scope("https://www.googleapis.com/auth/drive.appdata")
        )
        .build()

    return GoogleSignIn.getClient(activity, gso)
}

fun startGoogleDriveAuth(activity: Activity, googleSignInClient: GoogleSignInClient) {
    val signInIntent = googleSignInClient.signInIntent
    activity.startActivityForResult(signInIntent, RC_SIGN_IN)
}

const val RC_SIGN_IN = 9001
