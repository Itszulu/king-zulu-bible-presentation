package com.kingzulu.biblepresentation

import android.content.Context
import android.net.wifi.WifiManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

/**
 * Lightweight LAN receiver hosted by the phone. A TV browser opens the shown address;
 * the operator phone remains private and only presentation state is exposed.
 */
class BrowserReceiverServer(private val context: Context) {
    private val running = AtomicBoolean(false)
    private val pool = Executors.newCachedThreadPool()
    private var server: ServerSocket? = null
    @Volatile private var snapshot = ReceiverSnapshot()

    data class ReceiverSnapshot(
        val reference: String = "",
        val text: String = "",
        val translation: String = "",
        val kind: String = "",
        val black: Boolean = false,
        val revision: Long = 0
    )

    fun start(port: Int = 8787): String? {
        if (running.get()) return address(port)
        return runCatching {
            server = ServerSocket(port).also { socket ->
                running.set(true)
                pool.execute {
                    while (running.get()) runCatching { socket.accept() }.getOrNull()?.let { client -> pool.execute { serve(client) } }
                }
            }
            address(port)
        }.getOrNull()
    }

    fun publish(slide: PresentationSlide?, black: Boolean) {
        val old = snapshot
        snapshot = ReceiverSnapshot(
            reference = slide?.reference.orEmpty(),
            text = slide?.text.orEmpty(),
            translation = slide?.translation.orEmpty(),
            kind = slide?.kind.orEmpty(),
            black = black,
            revision = old.revision + 1
        )
    }

    fun stop() {
        running.set(false)
        runCatching { server?.close() }
        server = null
    }

    private fun serve(socket: Socket) = socket.use { s ->
        s.soTimeout = 2500
        val reader = BufferedReader(InputStreamReader(s.getInputStream()))
        val first = reader.readLine().orEmpty()
        var line: String?
        do { line = reader.readLine() } while (!line.isNullOrEmpty())
        val path = first.split(' ').getOrNull(1)?.let { URLDecoder.decode(it, "UTF-8") } ?: "/"
        val (type, body) = when {
            path.startsWith("/state") -> "application/json; charset=utf-8" to stateJson()
            else -> "text/html; charset=utf-8" to receiverHtml()
        }
        val bytes = body.toByteArray(Charsets.UTF_8)
        val out = s.getOutputStream()
        out.write("HTTP/1.1 200 OK\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes); out.flush()
    }

    private fun stateJson(): String = snapshot.let { s -> JSONObject().apply {
        put("reference", s.reference); put("text", s.text); put("translation", s.translation)
        put("kind", s.kind); put("black", s.black); put("revision", s.revision)
    }.toString() }

    private fun receiverHtml() = """<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><style>
html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#000;color:#fff;font-family:Arial,sans-serif}#stage{box-sizing:border-box;width:100%;height:100%;display:flex;flex-direction:column;justify-content:center;align-items:center;text-align:center;padding:6vw}.ref{font-size:3vw;color:#b9adff;font-weight:700;margin-bottom:2vw}.body{font-size:5vw;line-height:1.16;font-weight:600;max-width:92vw;white-space:pre-wrap}.tr{font-size:2vw;color:#ccc;margin-top:2vw;font-weight:700}.hint{font-size:2.2vw;color:#888}
</style></head><body><div id='stage'><div class='hint'>KING ZULU RECEIVER<br>Waiting for Live…</div></div><script>
let rev=-1;async function tick(){try{let r=await fetch('/state?'+Date.now(),{cache:'no-store'}),s=await r.json();if(s.revision===rev)return;rev=s.revision;let st=document.getElementById('stage');if(s.black){st.innerHTML='';document.body.style.background='#000';return;}if(!s.text&&!s.reference){st.innerHTML='<div class="hint">KING ZULU RECEIVER<br>Ready</div>';return;}st.innerHTML='<div class="ref"></div><div class="body"></div><div class="tr"></div>';st.querySelector('.ref').textContent=s.reference||'';st.querySelector('.body').textContent=s.text||'';st.querySelector('.tr').textContent=s.translation||'';}catch(e){}}setInterval(tick,350);tick();
</script></body></html>"""

    private fun address(port: Int): String? = localIpv4()?.let { "http://$it:$port" }

    private fun localIpv4(): String? {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        return interfaces.asSequence().flatMap { Collections.list(it.inetAddresses).asSequence() }
            .filterIsInstance<Inet4Address>().firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }?.hostAddress
            ?: runCatching {
                val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ip = wifi.connectionInfo.ipAddress
                if (ip == 0) null else "%d.%d.%d.%d".format(ip and 255, ip shr 8 and 255, ip shr 16 and 255, ip shr 24 and 255)
            }.getOrNull()
    }
}
