package com.prof18.feedflow.feedsync.lan

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

class LanSyncRepository(
    private val settings: LanSyncSettings,
    private val discoveryService: LanDiscoveryService,
    private val syncServer: LanSyncServer,
    private val syncClient: LanSyncClient,
    private val logger: Logger,
    private val saveDatabaseFile: suspend (ByteArray) -> Boolean,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var syncJob: Job? = null

    private val _syncState = MutableStateFlow<LanSyncState>(LanSyncState.Idle)
    val syncState = _syncState.asStateFlow()

    fun initialize() {
        if (settings.getDeviceId() == null) {
            val deviceId = generateDeviceId()
            settings.setDeviceId(deviceId)
        }

        if (settings.getDeviceName() == null) {
            settings.setDeviceName("FeedFlow Device")
        }
    }

    fun enableSync(deviceName: String? = null) {
        initialize()

        deviceName?.let {
            settings.setDeviceName(it)
        }

        settings.setLanSyncEnabled(true)
        startServices()
    }

    fun disableSync() {
        settings.setLanSyncEnabled(false)
        stopServices()
    }

    fun isEnabled(): Boolean = settings.isLanSyncEnabled()

    private fun startServices() {
        val deviceId = settings.getDeviceId() ?: return
        val deviceName = settings.getDeviceName() ?: "FeedFlow Device"
        val port = settings.getServerPort()

        syncServer.start()
        discoveryService.advertiseService(deviceId, deviceName, port)
        discoveryService.startDiscovery()

        logger.d { "LAN sync services started for device: $deviceName ($deviceId)" }
    }

    private fun stopServices() {
        discoveryService.stopDiscovery()
        discoveryService.stopAdvertising()
        syncServer.stop()
        syncJob?.cancel()

        logger.d { "LAN sync services stopped" }
    }

    fun getDiscoveredDevices(): Flow<List<LanDevice>> =
        discoveryService.getDiscoveredDevices()

    suspend fun syncWithDevice(device: LanDevice): SyncResult {
        _syncState.value = LanSyncState.Syncing(device.name)

        return try {
            val metadata = syncClient.fetchMetadata(device)
            if (metadata == null) {
                _syncState.value = LanSyncState.Error("Failed to fetch metadata from ${device.name}")
                return SyncResult.Failure("Failed to fetch metadata")
            }

            val lastLocalSync = settings.getLastSyncTimestamp() ?: 0L

            if (metadata.timestamp > lastLocalSync) {
                logger.d { "Remote device ${device.name} has newer data (${metadata.timestamp} > $lastLocalSync)" }

                val dbBytes = syncClient.downloadSyncDatabase(device)
                if (dbBytes == null) {
                    _syncState.value = LanSyncState.Error("Failed to download database from ${device.name}")
                    return SyncResult.Failure("Failed to download database")
                }

                val saved = saveDatabaseFile(dbBytes)
                if (!saved) {
                    _syncState.value = LanSyncState.Error("Failed to save database from ${device.name}")
                    return SyncResult.Failure("Failed to save database")
                }

                settings.setLastSyncTimestamp(metadata.timestamp)
                logger.d { "Successfully synced with ${device.name}" }

                _syncState.value = LanSyncState.Success(device.name)
                SyncResult.Success
            } else {
                logger.d { "Local data is up-to-date (${lastLocalSync} >= ${metadata.timestamp})" }
                _syncState.value = LanSyncState.Success(device.name, upToDate = true)
                SyncResult.UpToDate
            }
        } catch (e: Exception) {
            logger.e(e) { "Error syncing with ${device.name}" }
            _syncState.value = LanSyncState.Error("Error: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncWithAllDevices(): List<SyncResult> {
        val devices = mutableListOf<LanDevice>()
        discoveryService.getDiscoveredDevices().collect { discoveredDevices ->
            devices.clear()
            devices.addAll(discoveredDevices)
        }

        return devices.map { device ->
            syncWithDevice(device)
        }
    }

    fun updateLocalTimestamp() {
        settings.setLastSyncTimestamp(Clock.System.now().toEpochMilliseconds())
    }

    fun cleanup() {
        stopServices()
        syncClient.close()
    }

    private fun generateDeviceId(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = Random.nextInt(10000, 99999)
        return "feedflow-$timestamp-$random"
    }

    sealed class SyncResult {
        data object Success : SyncResult()
        data object UpToDate : SyncResult()
        data class Failure(val message: String) : SyncResult()
    }
}

sealed class LanSyncState {
    data object Idle : LanSyncState()
    data class Syncing(val deviceName: String) : LanSyncState()
    data class Success(val deviceName: String, val upToDate: Boolean = false) : LanSyncState()
    data class Error(val message: String) : LanSyncState()
}
