package com.prof18.feedflow.core.model

sealed class AccountSyncUIState {
    data object Loading : AccountSyncUIState()
    data object None : AccountSyncUIState()
    data class Synced(
        val lastDownloadDate: String?,
        val lastUploadDate: String?,
    ) : AccountSyncUIState()
}
