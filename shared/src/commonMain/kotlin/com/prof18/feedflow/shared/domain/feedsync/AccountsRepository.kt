package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.utils.AppConfig
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.CurrentOS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AccountsRepository(
    private val currentOS: CurrentOS,
    private val dropboxSettings: DropboxSettings,
    private val googleDriveSettings: GoogleDriveSettings,
    private val icloudSettings: ICloudSettings,
    private val appConfig: AppConfig,
    private val gReaderRepository: GReaderRepository,
    private val networkSettings: NetworkSettings,
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
                    generateAndroidAccounts()
                }
                CurrentOS.Desktop.Linux -> {
                    generateLinuxAccounts()
                }
                CurrentOS.Desktop.Mac -> {
                    generateMacOSAccounts()
                }
                CurrentOS.Desktop.Windows -> {
                    generateWindowsAccounts()
                }
                CurrentOS.Ios -> {
                    generateIOSAccounts()
                }
            }
        }

    private fun MutableList<SyncAccounts>.generateWindowsAccounts() {
        if (appConfig.isDropboxSyncEnabled) {
            add(SyncAccounts.DROPBOX)
        }
        if (appConfig.isGoogleDriveSyncEnabled) {
            add(SyncAccounts.GOOGLE_DRIVE)
        }
        add(SyncAccounts.FRESH_RSS)
        add(SyncAccounts.MINIFLUX)
    }

    private fun MutableList<SyncAccounts>.generateMacOSAccounts() {
        if (appConfig.isIcloudSyncEnabled) {
            add(SyncAccounts.ICLOUD)
        }
        if (appConfig.isDropboxSyncEnabled) {
            add(SyncAccounts.DROPBOX)
        }
        if (appConfig.isGoogleDriveSyncEnabled) {
            add(SyncAccounts.GOOGLE_DRIVE)
        }
        add(SyncAccounts.FRESH_RSS)
        add(SyncAccounts.MINIFLUX)
    }

    private fun MutableList<SyncAccounts>.generateLinuxAccounts() {
        if (appConfig.isDropboxSyncEnabled) {
            add(SyncAccounts.DROPBOX)
        }
        if (appConfig.isGoogleDriveSyncEnabled) {
            add(SyncAccounts.GOOGLE_DRIVE)
        }
        add(SyncAccounts.FRESH_RSS)
        add(SyncAccounts.MINIFLUX)
    }

    private fun MutableList<SyncAccounts>.generateAndroidAccounts() {
        if (appConfig.isDropboxSyncEnabled) {
            add(SyncAccounts.DROPBOX)
        }
        if (appConfig.isGoogleDriveSyncEnabled) {
            add(SyncAccounts.GOOGLE_DRIVE)
        }
        add(SyncAccounts.FRESH_RSS)
        add(SyncAccounts.MINIFLUX)
    }

    private fun MutableList<SyncAccounts>.generateIOSAccounts() {
        if (appConfig.isIcloudSyncEnabled) {
            add(SyncAccounts.ICLOUD)
        }
        if (appConfig.isDropboxSyncEnabled) {
            add(SyncAccounts.DROPBOX)
        }
        if (appConfig.isGoogleDriveSyncEnabled) {
            add(SyncAccounts.GOOGLE_DRIVE)
        }
        add(SyncAccounts.FRESH_RSS)
        add(SyncAccounts.MINIFLUX)
    }

    fun setDropboxAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.DROPBOX)
        currentAccountMutableState.value = SyncAccounts.DROPBOX
    }

    fun setGoogleDriveAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.GOOGLE_DRIVE)
        currentAccountMutableState.value = SyncAccounts.GOOGLE_DRIVE
    }

    fun setICloudAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.ICLOUD)
        currentAccountMutableState.value = SyncAccounts.ICLOUD
    }

    fun setFreshRssAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.FRESH_RSS)
        networkSettings.setSyncAccountType("FRESH_RSS")
        currentAccountMutableState.value = SyncAccounts.FRESH_RSS
    }

    fun setMinifluxAccount() {
        clearOtherSyncCredentials(except = SyncAccounts.MINIFLUX)
        networkSettings.setSyncAccountType("MINIFLUX")
        currentAccountMutableState.value = SyncAccounts.MINIFLUX
    }

    fun clearAccount() {
        currentAccountMutableState.value = SyncAccounts.LOCAL
    }

    private fun clearOtherSyncCredentials(except: SyncAccounts) {
        if (except != SyncAccounts.DROPBOX) {
            dropboxSettings.clearDropboxData()
        }
        if (except != SyncAccounts.GOOGLE_DRIVE) {
            googleDriveSettings.clearAll()
        }
        if (except != SyncAccounts.ICLOUD) {
            icloudSettings.setUseICloud(false)
        }
        if (except != SyncAccounts.FRESH_RSS && except != SyncAccounts.MINIFLUX) {
            networkSettings.deleteAll()
        }
    }

    fun getCurrentSyncAccount(): SyncAccounts {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            return SyncAccounts.DROPBOX
        }
        if (googleDriveSettings.isGoogleDriveLinked()) {
            return SyncAccounts.GOOGLE_DRIVE
        }
        if (currentOS == CurrentOS.Ios || currentOS == CurrentOS.Desktop.Mac) {
            val useICloud = icloudSettings.getUseICloud()
            if (useICloud) {
                return SyncAccounts.ICLOUD
            }
        }
        if (gReaderRepository.isAccountSet()) {
            val accountType = networkSettings.getSyncAccountType()
            return when (accountType) {
                "MINIFLUX" -> SyncAccounts.MINIFLUX
                else -> SyncAccounts.FRESH_RSS
            }
        }
        return SyncAccounts.LOCAL
    }

    fun isSyncEnabled(): Boolean {
        val currentSyncAccount = getCurrentSyncAccount()
        return currentSyncAccount == SyncAccounts.ICLOUD ||
            currentSyncAccount == SyncAccounts.DROPBOX ||
            currentSyncAccount == SyncAccounts.GOOGLE_DRIVE
    }

    private fun restoreAccounts() {
        currentAccountMutableState.update {
            getCurrentSyncAccount()
        }
    }
}
