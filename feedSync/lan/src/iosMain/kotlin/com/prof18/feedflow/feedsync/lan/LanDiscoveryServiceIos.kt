package com.prof18.feedflow.feedsync.lan

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LanDiscoveryServiceIos(
    private val logger: Logger,
) : LanDiscoveryService {

    private val discoveredDevices = MutableStateFlow<List<LanDevice>>(emptyList())
    private var isDiscoveringFlag = false

    override fun startDiscovery() {
        logger.d { "iOS LAN discovery starting (TODO: implement NSNetService)" }
        isDiscoveringFlag = true
        // TODO: Implement using NSNetServiceBrowser
        // This requires Objective-C interop with NSNetService/Bonjour
    }

    override fun stopDiscovery() {
        logger.d { "iOS LAN discovery stopping" }
        isDiscoveringFlag = false
        discoveredDevices.value = emptyList()
    }

    override fun advertiseService(deviceId: String, deviceName: String, port: Int) {
        logger.d { "iOS LAN advertising service: $deviceName on port $port" }
        // TODO: Implement using NSNetService
    }

    override fun stopAdvertising() {
        logger.d { "iOS LAN stop advertising" }
        // TODO: Implement using NSNetService
    }

    override fun getDiscoveredDevices(): Flow<List<LanDevice>> =
        discoveredDevices.asStateFlow()

    override fun isDiscovering(): Boolean = isDiscoveringFlag
}
