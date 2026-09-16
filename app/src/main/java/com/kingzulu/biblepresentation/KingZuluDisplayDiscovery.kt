package com.kingzulu.biblepresentation

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DiscoveredDisplay(
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String = "Local Wi-Fi"
)

/** Discovers VIDAA/Hisense and other SSDP/DIAL televisions on the local LAN. */
class KingZuluDisplayDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private var job: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(onFound: (DiscoveredDisplay) -> Unit) {
        stop()
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("kingzulu-tv-discovery").apply {
            setReferenceCounted(false); acquire()
        }
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val seen = mutableSetOf<String>()
            val targets = listOf("urn:dial-multiscreen-org:service:dial:1", "ssdp:all")
            DatagramSocket().use { socket ->
                socket.soTimeout = 900
                socket.broadcast = true
                for (st in targets) {
                    val request = ("M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: $st\r\n\r\n").toByteArray()
                    socket.send(DatagramPacket(request, request.size, InetAddress.getByName("239.255.255.250"), 1900))
                    val until = System.currentTimeMillis() + 2600
                    while (isActive && System.currentTimeMillis() < until) {
                        try {
                            val buf = ByteArray(8192)
                            val packet = DatagramPacket(buf, buf.size)
                            socket.receive(packet)
                            val text = String(packet.data, 0, packet.length)
                            val host = packet.address.hostAddress ?: continue
                            val server = header(text, "SERVER")
                            val usn = header(text, "USN")
                            val location = header(text, "LOCATION")
                            val evidence = "$server $usn $location $text".lowercase()
                            val isVidaa = evidence.contains("hisense") || evidence.contains("vidaa") || evidence.contains("hisense-smart-tv")
                            val isDialTv = evidence.contains("dial") || evidence.contains("tv")
                            if (!isVidaa && !isDialTv) continue
                            val key = host
                            if (seen.add(key)) {
                                val name = when {
                                    isVidaa -> "VIDAA / Hisense TV"
                                    server.isNotBlank() -> server.substringBefore('/').take(48)
                                    else -> "Smart TV"
                                }
                                withContext(Dispatchers.Main) {
                                    onFound(DiscoveredDisplay(name, host, if (isVidaa) 36669 else 1900, if (isVidaa) "VIDAA" else "SSDP / DIAL"))
                                }
                            }
                        } catch (_: java.net.SocketTimeoutException) { }
                    }
                }
            }
        }
    }

    private fun header(response: String, name: String): String =
        response.lineSequence().firstOrNull { it.startsWith("$name:", ignoreCase = true) }
            ?.substringAfter(':')?.trim().orEmpty()

    fun stop() {
        job?.cancel(); job = null
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
    }
}
