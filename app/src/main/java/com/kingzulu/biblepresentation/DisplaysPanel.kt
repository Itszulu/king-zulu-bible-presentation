package com.kingzulu.biblepresentation

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.mediarouter.app.MediaRouteChooserDialog
import com.google.android.gms.cast.framework.CastContext

@Composable
fun DisplaysPanel() {
    val context = LocalContext.current
    val activity = context as? Activity
    val discovery = remember { KingZuluDisplayDiscovery(context) }
    val castBridge = remember { GoogleCastBridge(context) }
    val wiredBridge = remember { WiredDisplayBridge(context) }
    val devices = remember { mutableStateListOf<DiscoveredDisplay>() }
    var scanning by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DiscoveredDisplay?>(null) }
    var castDevice by remember { mutableStateOf(castBridge.currentDeviceName()) }
    var wired by remember { mutableStateOf(wiredBridge.available()) }
    var wiredActive by remember { mutableStateOf<Int?>(null) }

    fun scanLocal() {
        devices.clear(); scanning = true
        discovery.start { device -> if (devices.none { it.host == device.host && it.port == device.port }) devices.add(device) }
    }
    fun showCastPicker() {
        if (activity == null) return
        runCatching {
            MediaRouteChooserDialog(activity).apply {
                routeSelector = CastContext.getSharedInstance(context).mergedSelector
                setTitle("Choose a TV")
                setOnDismissListener { castDevice = castBridge.currentDeviceName() }
                show()
            }
        }
    }
    DisposableEffect(Unit) { onDispose { discovery.stop(); wiredBridge.dismiss() } }

    Text("Displays", style = MaterialTheme.typography.headlineLarge)
    Text("Connect by HDMI or wirelessly. Your phone remains the controller while the audience screen receives presentation output.", color = MaterialTheme.colorScheme.onSurfaceVariant)

    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Wired HDMI / External Display", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { wired = wiredBridge.available() }, modifier = Modifier.fillMaxWidth()) { Text("DETECT HDMI DISPLAY") }
        if (wired.isEmpty()) Text("No secondary presentation display detected. Connect a supported USB-C to HDMI adapter, then detect again.")
        wired.forEach { d -> OutlinedButton(onClick = { if (wiredBridge.show(d.id)) wiredActive = d.id }, modifier = Modifier.fillMaxWidth()) { Text(if (wiredActive == d.id) "CONNECTED · ${d.name}" else "USE ${d.name}") } }
        if (wiredActive != null) OutlinedButton(onClick = { wiredBridge.dismiss(); wiredActive = null }, modifier = Modifier.fillMaxWidth()) { Text("DISCONNECT HDMI") }
    } }

    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Google Cast / Google TV", style = MaterialTheme.typography.titleMedium)
        Text(if (castDevice == null) "No Cast TV connected" else "Connected: $castDevice")
        Button(onClick = { showCastPicker() }, modifier = Modifier.fillMaxWidth()) { Text("FIND CAST TVs") }
    } }

    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("VIDAA / Local Network", style = MaterialTheme.typography.titleMedium)
        Text(if (selected == null) "No VIDAA/local display selected" else "Selected: ${selected!!.name}")
        OutlinedButton(onClick = { scanLocal() }, modifier = Modifier.fillMaxWidth()) { Text(if (scanning) "RESCAN LOCAL NETWORK" else "SCAN LOCAL NETWORK") }
        devices.forEach { d -> OutlinedButton(onClick = { selected = d }, modifier = Modifier.fillMaxWidth()) { Text("${d.name} · ${d.protocol}") } }
        if (selected != null) OutlinedButton(onClick = { selected = null }, modifier = Modifier.fillMaxWidth()) { Text("DISCONNECT") }
    } }

    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Browser Display", style = MaterialTheme.typography.titleMedium)
        Text("Universal fallback for smart TVs with a browser. Short receiver address/code is the next display transport being wired in.")
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("START BROWSER DISPLAY · IN PROGRESS") }
    } }
}
