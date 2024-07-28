package com.prof18.feedflow.core.model

sealed class AccountConnectionUiState {
    data object Unlinked : AccountConnectionUiState()
    data class Linked(
        val syncState: AccountSyncUIState,
    ) : AccountConnectionUiState()
    data object Loading : AccountConnectionUiState()
}
