package com.soundless

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "SoundlessNSD"
private const val CONNECT_SERVICE = "_adb-tls-connect._tcp"
private const val PAIRING_SERVICE = "_adb-tls-pairing._tcp"

class AdbServiceDiscovery(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _connectPort = MutableStateFlow<Int?>(null)
    val connectPort: StateFlow<Int?> = _connectPort

    private val _pairingPort = MutableStateFlow<Int?>(null)
    val pairingPort: StateFlow<Int?> = _pairingPort

    private var connectListener: NsdManager.DiscoveryListener? = null
    private var pairingListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        stopDiscovery()
        discoverService(CONNECT_SERVICE) { port ->
            Log.d(TAG, "Found connect port: $port")
            _connectPort.value = port
        }.also { connectListener = it }

        discoverService(PAIRING_SERVICE) { port ->
            Log.d(TAG, "Found pairing port: $port")
            _pairingPort.value = port
        }.also { pairingListener = it }
    }

    fun stopDiscovery() {
        connectListener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        pairingListener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        connectListener = null
        pairingListener = null
    }

    private fun discoverService(
        serviceType: String,
        onPortFound: (Int) -> Unit,
    ): NsdManager.DiscoveryListener {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName} type=${service.serviceType}")
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(s: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(s: NsdServiceInfo) {
                        Log.d(TAG, "Resolved: ${s.host}:${s.port}")
                        onPortFound(s.port)
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $serviceType error=$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $serviceType error=$errorCode")
            }
        }
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        return listener
    }
}
