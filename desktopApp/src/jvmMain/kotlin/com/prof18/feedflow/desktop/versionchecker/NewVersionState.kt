package com.prof18.feedflow.desktop.versionchecker

internal sealed interface NewVersionState {
    data class NewVersion(
        val downloadLink: String,
    ) : NewVersionState

    data object NoNewVersion : NewVersionState
}
