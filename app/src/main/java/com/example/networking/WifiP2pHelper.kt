package com.example.networking

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiP2pHelper(private val context: Context) {

    private val p2pManager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private var channel: WifiP2pManager.Channel? = null

    private val _isP2pEnabled = MutableStateFlow(false)
    val isP2pEnabled: StateFlow<Boolean> = _isP2pEnabled.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<WifiP2pDevice>> = _discoveredPeers.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val _thisDevice = MutableStateFlow<WifiP2pDevice?>(null)
    val thisDevice: StateFlow<WifiP2pDevice?> = _thisDevice.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready for Direct Wi-Fi")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    _isP2pEnabled.value = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                    _statusMessage.value = if (_isP2pEnabled.value) "Wi-Fi Direct Active" else "Wi-Fi Direct Disabled"
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        requestConnectionInfo()
                        _statusMessage.value = "Direct Connection Established"
                    } else {
                        _connectionInfo.value = null
                        _statusMessage.value = "Direct Connection Disconnected"
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    _thisDevice.value = device
                }
            }
        }
    }

    private var isReceiverRegistered = false

    fun init() {
        if (channel == null && p2pManager != null) {
            channel = p2pManager?.initialize(context, Looper.getMainLooper(), null)
        }
        if (!isReceiverRegistered) {
            try {
                context.registerReceiver(receiver, intentFilter)
                isReceiverRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cleanup() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(onComplete: (Boolean) -> Unit = {}) {
        val ch = channel ?: return onComplete(false)
        _statusMessage.value = "Searching for nearby CCTV nodes..."
        p2pManager?.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _statusMessage.value = "Scanning local Wi-Fi Direct..."
                onComplete(true)
            }
            override fun onFailure(reason: Int) {
                _statusMessage.value = "Scan failed (code $reason). Use Hotspot or Wi-Fi mode."
                onComplete(false)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun createGroupAsCamera(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        val ch = channel ?: return
        _statusMessage.value = "Creating Camera P2P Access Point..."
        p2pManager?.createGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _statusMessage.value = "Camera Direct AP Created"
                requestConnectionInfo()
                onSuccess()
            }
            override fun onFailure(reason: Int) {
                _statusMessage.value = "AP Creation error (code $reason)"
                onFailure(reason)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(deviceAddress: String, onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        val ch = channel ?: return
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }
        _statusMessage.value = "Connecting to $deviceAddress..."
        p2pManager?.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _statusMessage.value = "Pairing with camera..."
                onSuccess()
            }
            override fun onFailure(reason: Int) {
                _statusMessage.value = "Connection failed ($reason)"
                onFailure(reason)
            }
        })
    }

    fun removeGroup() {
        val ch = channel ?: return
        p2pManager?.removeGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _connectionInfo.value = null
                _statusMessage.value = "Disconnected"
            }
            override fun onFailure(reason: Int) {}
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        val ch = channel ?: return
        p2pManager?.requestPeers(ch) { peers: WifiP2pDeviceList? ->
            val list = peers?.deviceList?.toList() ?: emptyList()
            _discoveredPeers.value = list
        }
    }

    private fun requestConnectionInfo() {
        val ch = channel ?: return
        p2pManager?.requestConnectionInfo(ch) { info: WifiP2pInfo? ->
            _connectionInfo.value = info
        }
    }
}
