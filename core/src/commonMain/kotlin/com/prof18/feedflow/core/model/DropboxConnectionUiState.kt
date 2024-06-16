package com.prof18.feedflow.core.model

sealed interface DropboxConnectionUiState {
    data object Unlinked : DropboxConnectionUiState
    data class Linked(
        val syncState: DropboxSyncUIState,
    ) : DropboxConnectionUiState
    data object Loading : DropboxConnectionUiState
}

sealed interface DropboxSyncUIState {
    data object Loading : DropboxSyncUIState
    data object None : DropboxSyncUIState
    data class Synced(
        val lastDownloadDate: String?,
        val lastUploadDate: String?,
    ) : DropboxSyncUIState
}
