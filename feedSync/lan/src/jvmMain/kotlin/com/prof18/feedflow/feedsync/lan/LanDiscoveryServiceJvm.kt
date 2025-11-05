package com.prof18.feedflow.feedsync.lan

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlin.time.Clock

class LanDiscoveryServiceJvm(
    private val logger: Logger,
) : LanDiscoveryService {

    private var jmdns: JmDNS? = null
    private val discoveredDevices = MutableStateFlow<List<LanDevice>>(emptyList())
    private val deviceMap = mutableMapOf<String, LanDevice>()
    private var serviceListener: ServiceListener? = null
    private var isDiscoveringFlag = false

    override fun startDiscovery() {
        if (isDiscoveringFlag) {
            logger.d { "Discovery already running" }
            return
        }

        try {
            if (jmdns == null) {
                jmdns = JmDNS.create()
            }

            serviceListener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    logger.d { "Service added: ${event.name}" }
                    jmdns?.requestServiceInfo(SERVICE_TYPE, event.name, 1000)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    logger.d { "Service removed: ${event.name}" }
                    removeDevice(event.name)
                }

                override fun serviceResolved(event: ServiceEvent) {
                    logger.d { "Service resolved: ${event.name}" }
                    val serviceInfo = event.info
                    val addresses = serviceInfo.inet4Addresses

                    if (addresses.isNotEmpty()) {
                        val device = LanDevice(
                            id = event.name,
                            name = event.name,
                            ipAddress = addresses[0].hostAddress ?: return,
                            port = serviceInfo.port,
                            lastSeen = Clock.System.now().toEpochMilliseconds(),
                        )
                        addDevice(device)
                    }
                }
            }

            jmdns?.addServiceListener(SERVICE_TYPE, serviceListener)
            isDiscoveringFlag = true
            logger.d { "JmDNS discovery started" }
        } catch (e: IOException) {
            logger.e(e) { "Failed to start JmDNS discovery" }
        }
    }

    override fun stopDiscovery() {
        serviceListener?.let {
            jmdns?.removeServiceListener(SERVICE_TYPE, it)
        }
        serviceListener = null
        isDiscoveringFlag = false
        deviceMap.clear()
        discoveredDevices.value = emptyList()
        logger.d { "JmDNS discovery stopped" }
    }

    override fun advertiseService(deviceId: String, deviceName: String, port: Int) {
        try {
            if (jmdns == null) {
                jmdns = JmDNS.create()
            }

            val serviceInfo = ServiceInfo.create(
                SERVICE_TYPE,
                deviceName,
                port,
                "FeedFlow LAN Sync",
            )

            jmdns?.registerService(serviceInfo)
            logger.d { "Service advertised: $deviceName on port $port" }
        } catch (e: IOException) {
            logger.e(e) { "Failed to advertise service" }
        }
    }

    override fun stopAdvertising() {
        jmdns?.unregisterAllServices()
        logger.d { "Service advertising stopped" }
    }

    override fun getDiscoveredDevices(): Flow<List<LanDevice>> =
        discoveredDevices.asStateFlow()

    override fun isDiscovering(): Boolean = isDiscoveringFlag

    private fun addDevice(device: LanDevice) {
        synchronized(deviceMap) {
            deviceMap[device.id] = device
            discoveredDevices.value = deviceMap.values.toList()
        }
        logger.d { "Device added: ${device.name} at ${device.ipAddress}:${device.port}" }
    }

    private fun removeDevice(serviceId: String) {
        synchronized(deviceMap) {
            deviceMap.remove(serviceId)
            discoveredDevices.value = deviceMap.values.toList()
        }
        logger.d { "Device removed: $serviceId" }
    }

    fun cleanup() {
        stopDiscovery()
        stopAdvertising()
        try {
            jmdns?.close()
        } catch (e: IOException) {
            logger.e(e) { "Error closing JmDNS" }
        }
        jmdns = null
    }

    companion object {
        private const val SERVICE_TYPE = "_feedflow._tcp.local."
    }
}
