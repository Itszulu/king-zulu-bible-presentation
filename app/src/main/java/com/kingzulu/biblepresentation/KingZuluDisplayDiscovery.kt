package com.kingzulu.biblepresentation

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

data class DiscoveredDisplay(
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String = "Local Wi-Fi",
    val manufacturer: String = "",
    val model: String = ""
)

/** Discovery only. A discovered device is never considered connected until a transport establishes a session. */
class KingZuluDisplayDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private var job: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(onFound: (DiscoveredDisplay) -> Unit) {
        stop()
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("kingzulu-tv-discovery").apply { setReferenceCounted(false); acquire() }
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val seen = mutableSetOf<String>()
            val targets = listOf("urn:dial-multiscreen-org:service:dial:1", "urn:schemas-upnp-org:device:MediaRenderer:1", "ssdp:all")
            DatagramSocket().use { socket ->
                socket.soTimeout = 700
                socket.broadcast = true
                for (st in targets) {
                    val request = ("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $st\r\n\r\n").toByteArray()
                    socket.send(DatagramPacket(request, request.size, InetAddress.getByName("239.255.255.250"), 1900))
                    val until = System.currentTimeMillis() + 2200
                    while (isActive && System.currentTimeMillis() < until) {
                        try {
                            val buf = ByteArray(8192); val packet = DatagramPacket(buf, buf.size); socket.receive(packet)
                            val text = String(packet.data, 0, packet.length)
                            val host = packet.address.hostAddress ?: continue
                            val server = header(text, "SERVER"); val usn = header(text, "USN"); val location = header(text, "LOCATION")
                            val evidence = "$server $usn $location $text".lowercase()
                            val looksLikeDisplay = evidence.contains("dial") || evidence.contains("mediarenderer") || evidence.contains("tv") || evidence.contains("hisense") || evidence.contains("vidaa") || evidence.contains("tcl")
                            if (!looksLikeDisplay || !seen.add(host)) continue

                            val description = if (location.startsWith("http", true)) readDescription(location) else null
                            val friendly = description?.friendlyName.orEmpty()
                            val manufacturer = description?.manufacturer.orEmpty()
                            val model = description?.modelName.orEmpty()
                            val combined = "$friendly $manufacturer $model $evidence".lowercase()
                            val protocol = when {
                                combined.contains("vidaa") || combined.contains("hisense") -> "VIDAA / SSDP"
                                evidence.contains("dial") -> "DIAL"
                                evidence.contains("mediarenderer") -> "UPnP MediaRenderer"
                                else -> "SSDP"
                            }
                            val name = friendly.ifBlank {
                                when {
                                    manufacturer.isNotBlank() && model.isNotBlank() -> "$manufacturer $model"
                                    manufacturer.isNotBlank() -> "$manufacturer TV"
                                    combined.contains("tcl") -> "TCL TV"
                                    combined.contains("hisense") || combined.contains("vidaa") -> "Hisense / VIDAA TV"
                                    else -> "Network display at $host"
                                }
                            }.take(64)
                            withContext(Dispatchers.Main) { onFound(DiscoveredDisplay(name, host, 1900, protocol, manufacturer, model)) }
                        } catch (_: java.net.SocketTimeoutException) { }
                    }
                }
            }
        }
    }

    private data class DeviceDescription(val friendlyName:String,val manufacturer:String,val modelName:String)

    private fun readDescription(location:String):DeviceDescription? = runCatching {
        val connection=(URL(location).openConnection() as HttpURLConnection).apply { connectTimeout=800; readTimeout=800; instanceFollowRedirects=true }
        connection.inputStream.use { input ->
            val factory=DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isXIncludeAware=false; isExpandEntityReferences=false
            }
            val doc=factory.newDocumentBuilder().parse(input)
            fun value(tag:String)=doc.getElementsByTagName(tag).item(0)?.textContent?.trim().orEmpty()
            DeviceDescription(value("friendlyName"),value("manufacturer"),value("modelName"))
        }
    }.getOrNull()

    private fun header(response: String, name: String): String = response.lineSequence().firstOrNull { it.startsWith("$name:", ignoreCase = true) }?.substringAfter(':')?.trim().orEmpty()

    fun stop() { job?.cancel(); job=null; multicastLock?.let { if(it.isHeld) it.release() }; multicastLock=null }
}
