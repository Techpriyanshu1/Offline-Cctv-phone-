package com.example.networking

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

class NetworkServiceDiscovery(private val context: Context) {

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private val _discoveredServices = MutableStateFlow<List<DiscoveredCctvCamera>>(emptyList())
    val discoveredServices: StateFlow<List<DiscoveredCctvCamera>> = _discoveredServices.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    data class DiscoveredCctvCamera(
        val name: String,
        val host: InetAddress,
        val port: Int,
        val ipAddress: String = host.hostAddress ?: ""
    )

    fun registerCameraService(cameraName: String, port: Int = Protocol.VIDEO_PORT) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = cameraName
            serviceType = "_twinguard._tcp."
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(service: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDiscovery() {
        val currentServices = mutableMapOf<String, DiscoveredCctvCamera>()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("_twinguard._tcp")) {
                    try {
                        nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                val host = resolvedInfo.host ?: return
                                val cam = DiscoveredCctvCamera(
                                    name = resolvedInfo.serviceName,
                                    host = host,
                                    port = resolvedInfo.port
                                )
                                synchronized(currentServices) {
                                    currentServices[resolvedInfo.serviceName] = cam
                                    _discoveredServices.value = currentServices.values.toList()
                                }
                            }
                        })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                synchronized(currentServices) {
                    currentServices.remove(service.serviceName)
                    _discoveredServices.value = currentServices.values.toList()
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager?.discoverServices("_twinguard._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
            registrationListener = null
        } catch (e: Exception) {}

        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
            discoveryListener = null
        } catch (e: Exception) {}
    }
}
