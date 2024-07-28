package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.model.CurrentOS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    fun getCurrentSyncAccount(): SyncAccounts {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            return SyncAccounts.DROPBOX
        }
        if (currentOS == CurrentOS.Ios) {
            val useICloud = icloudSettings.getUseICloud()
            if (useICloud) {
                return SyncAccounts.ICLOUD
            }
        }
        return SyncAccounts.LOCAL
    }

    fun isSyncEnabled(): Boolean =
        getCurrentSyncAccount() != SyncAccounts.LOCAL

    private fun restoreAccounts() {
        currentAccountMutableState.update {
            getCurrentSyncAccount()
        }
    }
}
