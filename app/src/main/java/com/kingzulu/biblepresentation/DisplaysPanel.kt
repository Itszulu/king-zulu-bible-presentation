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
    val devices = remember { mutableStateListOf<DiscoveredDisplay>() }
    var scanning by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DiscoveredDisplay?>(null) }
    var castDevice by remember { mutableStateOf(castBridge.currentDeviceName()) }

    fun scanLegacy() {
        devices.clear(); scanning = true
        discovery.start { device -> if (devices.none { it.host == device.host && it.port == device.port }) devices.add(device) }
    }

    fun showCastPicker() {
        if (activity == null) return
        runCatching {
            val selector = CastContext.getSharedInstance(context).mergedSelector
            MediaRouteChooserDialog(activity).apply {
                routeSelector = selector
                setTitle("Choose a TV")
                setOnDismissListener { castDevice = castBridge.currentDeviceName() }
                show()
            }
        }
    }

    DisposableEffect(Unit) { onDispose { discovery.stop() } }

    Text("Displays", style = MaterialTheme.typography.headlineLarge)
    Text("Connect King Zulu to congregation screens on the same Wi-Fi.", color = MaterialTheme.colorScheme.onSurfaceVariant)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Google Cast / Google TV", style = MaterialTheme.typography.titleMedium)
            Text(if (castDevice == null) "No Cast TV connected" else "Connected: $castDevice")
            Button(onClick = { showCastPicker() }, modifier = Modifier.fillMaxWidth()) { Text("FIND CAST TVs") }
            Text("Use this for Chromecast, Google TV and compatible TCL/Android TVs. The TV picker is provided by Android's Cast framework.")
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Other local displays", style = MaterialTheme.typography.titleMedium)
            Text(if (selected == null) "No local display connected" else "Connected: ${selected!!.name}")
            OutlinedButton(onClick = { scanLegacy() }, modifier = Modifier.fillMaxWidth()) { Text(if (scanning) "RESCAN LOCAL NETWORK" else "SCAN LOCAL NETWORK") }
            devices.forEach { d -> OutlinedButton(onClick = { selected = d }, modifier = Modifier.fillMaxWidth()) { Text("${d.name} · ${d.protocol}") } }
            if (selected != null) OutlinedButton(onClick = { selected = null }, modifier = Modifier.fillMaxWidth()) { Text("DISCONNECT") }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("VIDAA / Browser Display", style = MaterialTheme.typography.titleMedium)
            Text("VIDAA discovery and the short browser receiver are being added here so Hisense and other TVs do not depend on Google Cast.")
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("VIDAA + BROWSER RECEIVER · IN PROGRESS") }
        }
    }
}
