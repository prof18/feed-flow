package com.prof18.feedflow.core.model

sealed class DropboxSynMessages {
    data object Error : DropboxSynMessages()
    data class ProceedToAuth(val authorizeUrl: String) : DropboxSynMessages()
    data object CodeExpired : DropboxSynMessages()
}
