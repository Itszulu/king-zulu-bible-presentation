package com.kingzulu.biblepresentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun DisplaysPanel() {
    val context = LocalContext.current
    val discovery = remember { KingZuluDisplayDiscovery(context) }
    val devices = remember { mutableStateListOf<DiscoveredDisplay>() }
    var scanning by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DiscoveredDisplay?>(null) }

    fun scan() {
        devices.clear()
        scanning = true
        discovery.start { device ->
            if (devices.none { it.host == device.host && it.port == device.port }) devices.add(device)
        }
    }

    DisposableEffect(Unit) { onDispose { discovery.stop() } }

    Text("Displays", style = MaterialTheme.typography.headlineLarge)
    Text("Connect King Zulu to congregation screens on the same Wi-Fi.", color = MaterialTheme.colorScheme.onSurfaceVariant)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (selected == null) "No display connected" else "Connected: ${selected!!.name}")
            Button(onClick = { scan() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (scanning) "RESCAN DISPLAYS" else "SCAN FOR DISPLAYS")
            }
            if (devices.isEmpty()) {
                Text("No compatible display found yet. Keep the phone and screen on the same Wi-Fi, or use Browser Display below.")
            } else devices.forEach { d ->
                OutlinedButton(onClick = { selected = d }, modifier = Modifier.fillMaxWidth()) {
                    Text("${d.name} · ${d.protocol}")
                }
            }
            if (selected != null) OutlinedButton(onClick = { selected = null }, modifier = Modifier.fillMaxWidth()) { Text("DISCONNECT") }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Browser Display", style = MaterialTheme.typography.titleMedium)
            Text("For TVs that cannot be discovered directly. This receiver-free option will show a short address/code here when the local display server is enabled.")
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("START BROWSER DISPLAY · NEXT BUILD") }
        }
    }
}
