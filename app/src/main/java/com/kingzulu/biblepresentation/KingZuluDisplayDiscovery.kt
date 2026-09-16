package com.kingzulu.biblepresentation

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

data class DiscoveredDisplay(
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String = "King Zulu"
)

/**
 * Discovers compatible presentation/display endpoints advertised on the local Wi-Fi.
 * This is intentionally lifecycle-safe: start/stop may be called repeatedly as the
 * user moves between pages without losing the rest of the presentation state.
 */
class KingZuluDisplayDiscovery(context: Context) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.DiscoveryListener? = null
    private val seen = mutableSetOf<String>()

    fun start(onFound: (DiscoveredDisplay) -> Unit) {
        stop()
        seen.clear()
        val discovery = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { stop() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val host = info.host?.hostAddress ?: return
                        val key = "$host:${info.port}"
                        if (seen.add(key)) {
                            onFound(DiscoveredDisplay(info.serviceName.ifBlank { "Display" }, host, info.port, "Local Wi-Fi"))
                        }
                    }
                })
            }
        }
        listener = discovery
        runCatching { nsd.discoverServices("_kingzulu._tcp.", NsdManager.PROTOCOL_DNS_SD, discovery) }
    }

    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
    }
}
