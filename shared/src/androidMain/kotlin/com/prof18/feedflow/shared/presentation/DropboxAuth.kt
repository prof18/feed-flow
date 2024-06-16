package com.prof18.feedflow.shared.presentation

import android.app.Activity
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.prof18.feedflow.feedsync.dropbox.DropboxConstants

fun startDropboxAuth(activity: Activity, apiKey: String) {
    val requestConfig = DbxRequestConfig(DropboxConstants.DROPBOX_CLIENT_IDENTIFIER)
    Auth.startOAuth2PKCE(activity, apiKey, requestConfig, DropboxConstants.DROPBOX_SCOPES)
}
