package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.model.CurrentOS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AccountsRepository(
    private val currentOS: CurrentOS,
    private val dropboxSettings: DropboxSettings,
    private val icloudSettings: ICloudSettings,
    private val appConfig: AppConfig,
    private val gReaderRepository: GReaderRepository,
) {
    private val currentAccountMutableState = MutableStateFlow(SyncAccounts.LOCAL)
    val currentAccountState = currentAccountMutableState.asStateFlow()

    private val desktopAccounts = listOf(
        SyncAccounts.DROPBOX,
        SyncAccounts.FRESH_RSS,
    )

    private val macOSAccounts = listOf(
        SyncAccounts.DROPBOX,
        SyncAccounts.ICLOUD,
        SyncAccounts.FRESH_RSS,
    )

    private val androidAccounts = listOf(
        SyncAccounts.DROPBOX,
        SyncAccounts.FRESH_RSS,
    )

    private val androidAccountsNoDropbox = listOf(
        SyncAccounts.FRESH_RSS,
    )

    private val iosAccounts = listOf(
        SyncAccounts.ICLOUD,
        SyncAccounts.FRESH_RSS,
        SyncAccounts.DROPBOX,
    )

    init {
        restoreAccounts()
    }

    fun getValidAccounts() =
        when (currentOS) {
            CurrentOS.Desktop.Linux -> desktopAccounts
            CurrentOS.Desktop.Mac -> if (appConfig.isIcloudSyncEnabled) macOSAccounts else desktopAccounts
            CurrentOS.Desktop.Windows -> desktopAccounts
            CurrentOS.Android -> if (appConfig.isDropboxSyncEnabled) androidAccounts else androidAccountsNoDropbox
            CurrentOS.Ios -> iosAccounts
        }

    fun setDropboxAccount() {
        currentAccountMutableState.value = SyncAccounts.DROPBOX
    }

    fun setICloudAccount() {
        currentAccountMutableState.value = SyncAccounts.ICLOUD
    }

    fun setFreshRssAccount() {
        currentAccountMutableState.value = SyncAccounts.FRESH_RSS
    }

    fun clearAccount() {
        currentAccountMutableState.value = SyncAccounts.LOCAL
    }

    fun getCurrentSyncAccount(): SyncAccounts {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            return SyncAccounts.DROPBOX
        }
        if (currentOS == CurrentOS.Ios || currentOS == CurrentOS.Desktop.Mac) {
            val useICloud = icloudSettings.getUseICloud()
            if (useICloud) {
                return SyncAccounts.ICLOUD
            }
        }
        if (gReaderRepository.isAccountSet()) {
            return SyncAccounts.FRESH_RSS
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
