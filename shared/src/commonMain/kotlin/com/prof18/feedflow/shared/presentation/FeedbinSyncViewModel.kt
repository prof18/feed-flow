package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.Failure
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedbinSyncViewModel internal constructor(
    private val feedbinRepository: FeedbinRepository,
    private val accountsRepository: AccountsRepository,
    private val dateFormatter: DateFormatter,
    private val feedStateRepository: FeedStateRepository,
) : ViewModel() {

    private val uiMutableState: MutableStateFlow<AccountConnectionUiState> = MutableStateFlow(
        AccountConnectionUiState.Unlinked,
    )
    val uiState = uiMutableState.asStateFlow()

    private val errorMutableState: MutableSharedFlow<Failure> = MutableSharedFlow()
    val errorState = errorMutableState.asSharedFlow()

    private val loginLoadingMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loginLoading = loginLoadingMutableState.asStateFlow()

    init {
        uiMutableState.update { AccountConnectionUiState.Loading }
        if (feedbinRepository.isAccountSet()) {
            uiMutableState.update {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
        } else {
            uiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    fun login(
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
            loginLoadingMutableState.update { true }
            feedbinRepository.login(
                username,
                password,
            ).fold(
                onFailure = {
                    errorMutableState.emit(it)
                    loginLoadingMutableState.update { false }
                },
                onSuccess = {
                    accountsRepository.setFeedbinAccount()
                    feedbinRepository.sync()
                        .fold(
                            onFailure = {
                                loginLoadingMutableState.update { false }
                                errorMutableState.emit(it)
                                uiMutableState.update {
                                    AccountConnectionUiState.Linked(
                                        syncState = getSyncState(),
                                    )
                                }
                            },
                            onSuccess = {
                                loginLoadingMutableState.update { false }
                                feedStateRepository.getFeeds()
                                uiMutableState.update {
                                    AccountConnectionUiState.Linked(
                                        syncState = getSyncState(),
                                    )
                                }
                            },
                        )
                },
            )
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            feedbinRepository.getLastSyncDate() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = null,
                )
            }

            else -> AccountSyncUIState.None
        }
    }

    @Suppress("MagicNumber")
    private fun getLastDownloadDate(): String? =
        feedbinRepository.getLastSyncDate()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp * 1000)
        }

    fun disconnect() {
        viewModelScope.launch {
            uiMutableState.update { AccountConnectionUiState.Loading }
            feedbinRepository.disconnect()
            accountsRepository.clearAccount()
            feedStateRepository.getFeeds()
            uiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }
}
