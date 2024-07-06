package com.prof18.feedflow.feedsync.dropbox

import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential

actual fun getDxCredentialsAsString(): String {
    val dbxCredential = Auth.getDbxCredential()
    return DbxCredential.Writer.writeToString(dbxCredential)
}
