package com.prof18.feedflow.feedsync.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

class LanDiscoveryServiceAndroid(
    private val context: Context,
    private val logger: Logger,
) : LanDiscoveryService {

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val discoveredDevices = MutableStateFlow<List<LanDevice>>(emptyList())
    private val deviceMap = mutableMapOf<String, LanDevice>()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isDiscoveringFlag = false

    override fun startDiscovery() {
        if (isDiscoveringFlag) {
            logger.d { "Discovery already running" }
            return
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                logger.d { "Service discovery started: $serviceType" }
                isDiscoveringFlag = true
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                logger.d { "Service found: ${serviceInfo.serviceName}" }
                resolveService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                logger.d { "Service lost: ${serviceInfo.serviceName}" }
                removeDevice(serviceInfo.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                logger.d { "Discovery stopped: $serviceType" }
                isDiscoveringFlag = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                logger.e { "Discovery start failed: $serviceType, error: $errorCode" }
                isDiscoveringFlag = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                logger.e { "Discovery stop failed: $serviceType, error: $errorCode" }
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            logger.e(e) { "Failed to start discovery" }
            isDiscoveringFlag = false
        }
    }

    override fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                logger.e(e) { "Failed to stop discovery" }
            }
        }
        discoveryListener = null
        isDiscoveringFlag = false
        deviceMap.clear()
        discoveredDevices.value = emptyList()
    }

    override fun advertiseService(deviceId: String, deviceName: String, port: Int) {
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                logger.d { "Service registered: ${serviceInfo.serviceName}" }
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                logger.e { "Registration failed: ${serviceInfo.serviceName}, error: $errorCode" }
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                logger.d { "Service unregistered: ${serviceInfo.serviceName}" }
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                logger.e { "Unregistration failed: ${serviceInfo.serviceName}, error: $errorCode" }
            }
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            logger.e(e) { "Failed to advertise service" }
        }
    }

    override fun stopAdvertising() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                logger.e(e) { "Failed to stop advertising" }
            }
        }
        registrationListener = null
    }

    override fun getDiscoveredDevices(): Flow<List<LanDevice>> =
        discoveredDevices.asStateFlow()

    override fun isDiscovering(): Boolean = isDiscoveringFlag

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                logger.e { "Resolve failed: ${serviceInfo.serviceName}, error: $errorCode" }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                logger.d { "Service resolved: ${serviceInfo.serviceName}" }

                val host = serviceInfo.host
                val port = serviceInfo.port

                if (host != null) {
                    val device = LanDevice(
                        id = serviceInfo.serviceName,
                        name = serviceInfo.serviceName,
                        ipAddress = host.hostAddress ?: return,
                        port = port,
                        lastSeen = Clock.System.now().toEpochMilliseconds(),
                    )
                    addDevice(device)
                }
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            logger.e(e) { "Failed to resolve service: ${serviceInfo.serviceName}" }
        }
    }

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

    companion object {
        private const val SERVICE_TYPE = "_feedflow._tcp"
    }
}
