package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountsViewModel(
    private val dropboxSettings: DropboxSettings,
) : BaseViewModel() {

    private val accountsMutableState = MutableStateFlow(SyncAccounts.LOCAL)

    @NativeCoroutinesState
    val accountsState = accountsMutableState.asStateFlow()

    fun restoreAccounts() {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            accountsMutableState.value = SyncAccounts.DROPBOX
        }
    }
}
