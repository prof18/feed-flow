package com.prof18.feedflow.shared.presentation

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val SCOPES = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.appdata"

fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(
            Scope("https://www.googleapis.com/auth/drive.file"),
            Scope("https://www.googleapis.com/auth/drive.appdata"),
        )
        .build()

    return GoogleSignIn.getClient(activity, gso)
}

fun startGoogleDriveAuth(activity: Activity, googleSignInClient: GoogleSignInClient) {
    val signInIntent = googleSignInClient.signInIntent
    activity.startActivityForResult(signInIntent, RC_SIGN_IN)
}

suspend fun getAccessTokenFromAccount(activity: Activity, account: GoogleSignInAccount): String? {
    return withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.getToken(activity, account.account!!, SCOPES)
        } catch (e: Exception) {
            null
        }
    }
}

suspend fun getFreshAccessToken(context: Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(
                    Scope("https://www.googleapis.com/auth/drive.file"),
                    Scope("https://www.googleapis.com/auth/drive.appdata"),
                )
                .build()

            val signInResult = GoogleSignIn.getClient(context, gso).silentSignIn().await()

            GoogleAuthUtil.getToken(context, signInResult.account!!, SCOPES)
        } catch (e: Exception) {
            null
        }
    }
}

const val RC_SIGN_IN = 9001
