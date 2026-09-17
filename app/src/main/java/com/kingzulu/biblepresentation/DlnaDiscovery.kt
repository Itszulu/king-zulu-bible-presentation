package com.kingzulu.biblepresentation

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Standards-based UPnP/DLNA discovery. No VIDAA private credentials or proprietary SDK code. */
data class SmartTvRenderer(
    val location: String,
    val server: String = "",
    val usn: String = "",
    val st: String = "",
    val address: String = ""
)

object DlnaDiscovery {
    private const val SSDP_HOST = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val MEDIA_RENDERER = "urn:schemas-upnp-org:device:MediaRenderer:1"

    fun scan(timeoutMs: Int = 2600): List<SmartTvRenderer> {
        val found = ConcurrentHashMap<String, SmartTvRenderer>()
        val request = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $SSDP_HOST:$SSDP_PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: 2\r\n")
            append("ST: $MEDIA_RENDERER\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)

        DatagramSocket().use { socket ->
            socket.soTimeout = 350
            socket.send(DatagramPacket(request, request.size, InetAddress.getByName(SSDP_HOST), SSDP_PORT))
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                try {
                    val buffer = ByteArray(8192)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val headers = response.lineSequence().drop(1).mapNotNull { line ->
                        val i = line.indexOf(':')
                        if (i <= 0) null else line.substring(0, i).trim().lowercase() to line.substring(i + 1).trim()
                    }.toMap()
                    val location = headers["location"].orEmpty()
                    if (location.isNotBlank()) {
                        found[location] = SmartTvRenderer(
                            location = location,
                            server = headers["server"].orEmpty(),
                            usn = headers["usn"].orEmpty(),
                            st = headers["st"].orEmpty(),
                            address = packet.address.hostAddress.orEmpty()
                        )
                    }
                } catch (_: SocketTimeoutException) {
                    // Continue until the overall discovery window closes.
                }
            }
        }
        return found.values.sortedBy { it.address }
    }

    /** Lightweight friendly-name lookup from the UPnP device description. */
    fun friendlyName(renderer: SmartTvRenderer): String {
        return runCatching {
            val xml = URL(renderer.location).openConnection().run {
                connectTimeout = 1200; readTimeout = 1200
                getInputStream().bufferedReader().use { it.readText() }
            }
            Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.IGNORE_CASE)
                .find(xml)?.groupValues?.getOrNull(1)?.replace("&amp;", "&")?.trim()
        }.getOrNull().takeUnless { it.isNullOrBlank() } ?: renderer.server.ifBlank { renderer.address }
    }
}
