package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings

internal class FeedSyncAccountsRepository(
    private val dropboxSettings: DropboxSettings,
) {
    fun getCurrentAccount(): SyncAccounts {
        val dropboxSettings = dropboxSettings.getDropboxData()
        if (dropboxSettings != null) {
            return SyncAccounts.DROPBOX
        }
        return SyncAccounts.LOCAL
    }

    fun isSyncEnabled(): Boolean =
        getCurrentAccount() != SyncAccounts.LOCAL
}
