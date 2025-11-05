package com.prof18.feedflow.feedsync.lan

import kotlinx.coroutines.flow.Flow

interface LanDiscoveryService {

    fun startDiscovery()

    fun stopDiscovery()

    fun advertiseService(deviceId: String, deviceName: String, port: Int)

    fun stopAdvertising()

    fun getDiscoveredDevices(): Flow<List<LanDevice>>

    fun isDiscovering(): Boolean
}
