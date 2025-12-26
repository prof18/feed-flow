package com.prof18.feedflow.core.model

sealed class GoogleDriveSynMessages {
    data object Error : GoogleDriveSynMessages()

    // TODO: probably not needed at all.
    data class ProceedToAuth(val authorizeUrl: String) : GoogleDriveSynMessages()
}
