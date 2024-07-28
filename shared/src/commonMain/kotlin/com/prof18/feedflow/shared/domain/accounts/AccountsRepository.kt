package com.prof18.feedflow.shared.domain.accounts

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.model.CurrentOS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AccountsRepository(
    private val currentOS: CurrentOS,
    private val dropboxSettings: DropboxSettings,
    private val icloudSettings: ICloudSettings,
) {
    private val currentAccountMutableState = MutableStateFlow(SyncAccounts.LOCAL)
    val currentAccountState = currentAccountMutableState.asStateFlow()

    private val desktopAccounts = listOf(
        SyncAccounts.DROPBOX,
    )

    private val androidAccounts = listOf(
        SyncAccounts.DROPBOX,
    )

    private val iosAccounts = listOf(
        SyncAccounts.ICLOUD,
        SyncAccounts.DROPBOX,
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

    fun setICloudAccount() {
        currentAccountMutableState.value = SyncAccounts.ICLOUD
    }

    fun clearAccount() {
        currentAccountMutableState.value = SyncAccounts.LOCAL
    }

    private fun restoreAccounts() {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            currentAccountMutableState.value = SyncAccounts.DROPBOX
        }
        if (currentOS == CurrentOS.Ios) {
            val useICloud = icloudSettings.getUseICloud()
            if (useICloud) {
                currentAccountMutableState.value = SyncAccounts.ICLOUD
            }
        }
    }
}
