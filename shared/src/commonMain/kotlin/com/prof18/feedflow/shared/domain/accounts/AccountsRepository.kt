package com.prof18.feedflow.shared.domain.accounts

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.shared.domain.model.CurrentOS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AccountsRepository(
    private val currentOS: CurrentOS,
    private val dropboxSettings: DropboxSettings,
) {
    private val currentAccountMutableState = MutableStateFlow(SyncAccounts.LOCAL)
    val currentAccountState = currentAccountMutableState.asStateFlow()

    private val desktopAccounts = listOf(
        SyncAccounts.LOCAL,
        SyncAccounts.DROPBOX,
    )

    private val androidAccounts = listOf(
        SyncAccounts.LOCAL,
        SyncAccounts.DROPBOX,
    )

    private val iosAccounts = listOf(
        SyncAccounts.LOCAL,
        SyncAccounts.DROPBOX,
        SyncAccounts.ICLOUD,
    )

    init {
        restoreAccounts()
    }

    fun getValidAccounts() =
        when (currentOS) {
            CurrentOS.Desktop -> desktopAccounts
            CurrentOS.Android -> androidAccounts
            CurrentOS.Ios -> iosAccounts
        }

    fun setDropboxAccount() {
        currentAccountMutableState.value = SyncAccounts.DROPBOX
    }

    fun clearAccount() {
        currentAccountMutableState.value = SyncAccounts.LOCAL
    }

    private fun restoreAccounts() {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            currentAccountMutableState.value = SyncAccounts.DROPBOX
        }
    }
}
