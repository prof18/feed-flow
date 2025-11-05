package com.prof18.feedflow.feedsync.lan

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class LanSyncSettings(
    private val settings: Settings,
) {
    fun isLanSyncEnabled(): Boolean =
        settings.getBoolean(LAN_SYNC_ENABLED_KEY, false)

    fun setLanSyncEnabled(enabled: Boolean) {
        settings[LAN_SYNC_ENABLED_KEY] = enabled
    }

    fun getDeviceId(): String? =
        settings.getStringOrNull(DEVICE_ID_KEY)

    fun setDeviceId(deviceId: String) {
        settings[DEVICE_ID_KEY] = deviceId
    }

    fun getDeviceName(): String? =
        settings.getStringOrNull(DEVICE_NAME_KEY)

    fun setDeviceName(name: String) {
        settings[DEVICE_NAME_KEY] = name
    }

    fun getServerPort(): Int =
        settings.getInt(SERVER_PORT_KEY, DEFAULT_PORT)

    fun setServerPort(port: Int) {
        settings[SERVER_PORT_KEY] = port
    }

    fun getLastSyncTimestamp(): Long? {
        val timestamp = settings.getLong(LAST_SYNC_TIMESTAMP_KEY, -1L)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        settings[LAST_SYNC_TIMESTAMP_KEY] = timestamp
    }

    fun clearAllSettings() {
        settings.remove(LAN_SYNC_ENABLED_KEY)
        settings.remove(DEVICE_ID_KEY)
        settings.remove(DEVICE_NAME_KEY)
        settings.remove(SERVER_PORT_KEY)
        settings.remove(LAST_SYNC_TIMESTAMP_KEY)
    }

    companion object {
        private const val LAN_SYNC_ENABLED_KEY = "lan_sync_enabled"
        private const val DEVICE_ID_KEY = "lan_device_id"
        private const val DEVICE_NAME_KEY = "lan_device_name"
        private const val SERVER_PORT_KEY = "lan_server_port"
        private const val LAST_SYNC_TIMESTAMP_KEY = "lan_last_sync_timestamp"
        const val DEFAULT_PORT = 8765
    }
}
