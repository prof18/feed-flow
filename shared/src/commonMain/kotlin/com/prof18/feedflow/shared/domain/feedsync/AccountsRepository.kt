package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.feedsync.nextcloud.NextcloudSettings
import com.prof18.feedflow.shared.domain.model.CurrentOS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AccountsRepository(
    private val currentOS: CurrentOS,
    private val dropboxSettings: DropboxSettings,
    private val icloudSettings: ICloudSettings,
    private val nextcloudSettings: NextcloudSettings,
    private val appConfig: AppConfig,
    private val gReaderRepository: GReaderRepository,
) {
    private val currentAccountMutableState = MutableStateFlow(SyncAccounts.LOCAL)
    val currentAccountState = currentAccountMutableState.asStateFlow()

    init {
        restoreAccounts()
    }

    fun getValidAccounts(): List<SyncAccounts> =
        buildList {
            when (currentOS) {
                CurrentOS.Android -> {
                    if (appConfig.isDropboxSyncEnabled) {
                        add(SyncAccounts.DROPBOX)
                    }
                    add(SyncAccounts.NEXTCLOUD)
                    add(SyncAccounts.FRESH_RSS)
                }
                CurrentOS.Desktop.Linux -> {
                    if (appConfig.isDropboxSyncEnabled) {
                        add(SyncAccounts.DROPBOX)
                    }
                    add(SyncAccounts.NEXTCLOUD)
                    add(SyncAccounts.FRESH_RSS)
                }
                CurrentOS.Desktop.Mac -> {
                    if (appConfig.isIcloudSyncEnabled) {
                        add(SyncAccounts.ICLOUD)
                    }
                    if (appConfig.isDropboxSyncEnabled) {
                        add(SyncAccounts.DROPBOX)
                    }
                    add(SyncAccounts.NEXTCLOUD)
                    add(SyncAccounts.FRESH_RSS)
                }
                CurrentOS.Desktop.Windows -> {
                    if (appConfig.isDropboxSyncEnabled) {
                        add(SyncAccounts.DROPBOX)
                    }
                    add(SyncAccounts.NEXTCLOUD)
                    add(SyncAccounts.FRESH_RSS)
                }
                CurrentOS.Ios -> {
                    if (appConfig.isIcloudSyncEnabled) {
                        add(SyncAccounts.ICLOUD)
                    }
                    if (appConfig.isDropboxSyncEnabled) {
                        add(SyncAccounts.DROPBOX)
                    }
                    add(SyncAccounts.NEXTCLOUD)
                    add(SyncAccounts.FRESH_RSS)
                }
            }
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

    fun setNextcloudAccount() {
        currentAccountMutableState.value = SyncAccounts.NEXTCLOUD
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
        if (nextcloudSettings.hasCredentials()) {
            return SyncAccounts.NEXTCLOUD
        }
        if (gReaderRepository.isAccountSet()) {
            return SyncAccounts.FRESH_RSS
        }
        return SyncAccounts.LOCAL
    }

    fun isSyncEnabled(): Boolean {
        val currentSyncAccount = getCurrentSyncAccount()
        return currentSyncAccount == SyncAccounts.ICLOUD ||
            currentSyncAccount == SyncAccounts.DROPBOX ||
            currentSyncAccount == SyncAccounts.NEXTCLOUD
    }

    private fun restoreAccounts() {
        currentAccountMutableState.update {
            getCurrentSyncAccount()
        }
    }
}
