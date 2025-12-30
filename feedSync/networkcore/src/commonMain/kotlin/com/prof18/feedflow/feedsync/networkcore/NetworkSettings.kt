package com.prof18.feedflow.feedsync.networkcore

import com.prof18.feedflow.core.model.SyncAccounts
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class NetworkSettings(
    private val settings: Settings,
) {
    fun getSyncPwd(): String =
        settings.getString(SettingsField.SYNC_PWD.name, "")

    fun setSyncPwd(value: String) =
        settings.set(SettingsField.SYNC_PWD.name, value)

    fun setSyncUsername(value: String) =
        settings.set(SettingsField.SYNC_USERNAME.name, value)

    fun getSyncUrl(): String =
        settings.getString(SettingsField.SYNC_URL.name, "")

    fun setSyncUrl(value: String) =
        settings.set(SettingsField.SYNC_URL.name, value)

    fun getLastSyncDate(): Long? =
        settings.getLongOrNull(SettingsField.LAST_SYNC_DATE.name)

    fun setLastSyncDate(value: Long) =
        settings.set(SettingsField.LAST_SYNC_DATE.name, value)

    fun clearLastSyncDate() {
        settings.remove(SettingsField.LAST_SYNC_DATE.name)
    }

    fun getSyncAccountType(): SyncAccounts? =
        settings.getStringOrNull(SettingsField.SYNC_ACCOUNT_TYPE.name)?.let { SyncAccounts.valueOf(it) }

    fun setSyncAccountType(value: SyncAccounts) =
        settings.set(SettingsField.SYNC_ACCOUNT_TYPE.name, value.name)

    fun deleteAll() {
        SettingsField.entries.forEach {
            settings.remove(it.name)
        }
    }
}

internal enum class SettingsField {
    SYNC_PWD,
    SYNC_USERNAME,
    SYNC_URL,
    LAST_SYNC_DATE,
    SYNC_ACCOUNT_TYPE,
}
