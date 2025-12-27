package com.prof18.feedflow.core.model

sealed class GoogleDriveSynMessages {
    data object Error : GoogleDriveSynMessages()
}
