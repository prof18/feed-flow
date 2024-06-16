package com.prof18.feedflow.core.model

sealed interface DropboxSynMessages {
    data object Error : DropboxSynMessages
    data class ProceedToAuth(val authorizeUrl: String) : DropboxSynMessages
}
