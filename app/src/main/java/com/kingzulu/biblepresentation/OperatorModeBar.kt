package com.kingzulu.biblepresentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Service-critical controls stay visible near the live workflow instead of being buried in Settings. */
@Composable
fun OperatorModeBar() {
    val context = LocalContext.current
    var routeAll by remember { mutableStateOf(OperatorPreferences.routeAllOutputs(context)) }
    var autoLive by remember { mutableStateOf(OperatorPreferences.aiAutoLive(context)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("OPERATOR MODES", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFAAAAB2))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (routeAll) Button({ routeAll = true; OperatorPreferences.setRouteAllOutputs(context, true) }, Modifier.weight(1f)) { Text("ALL SCREENS") }
                else OutlinedButton({ routeAll = true; OperatorPreferences.setRouteAllOutputs(context, true) }, Modifier.weight(1f)) { Text("ALL SCREENS") }
                if (!routeAll) Button({ routeAll = false; OperatorPreferences.setRouteAllOutputs(context, false) }, Modifier.weight(1f)) { Text("CUSTOM") }
                else OutlinedButton({ routeAll = false; OperatorPreferences.setRouteAllOutputs(context, false) }, Modifier.weight(1f)) { Text("CUSTOM") }
            }
            Text(
                if (routeAll) "Live content is intended for every active audience output."
                else "Custom routing selected. Choose individual output targets when output routing is configured.",
                fontSize = 11.sp,
                color = if (routeAll) Color(0xFF75D69C) else Color(0xFFFFC66D)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (autoLive) Button({ autoLive = true; OperatorPreferences.setAiAutoLive(context, true) }, Modifier.weight(1f)) { Text("AUTO LIVE") }
                else OutlinedButton({ autoLive = true; OperatorPreferences.setAiAutoLive(context, true) }, Modifier.weight(1f)) { Text("AUTO LIVE") }
                if (!autoLive) Button({ autoLive = false; OperatorPreferences.setAiAutoLive(context, false) }, Modifier.weight(1f)) { Text("PREVIEW FIRST") }
                else OutlinedButton({ autoLive = false; OperatorPreferences.setAiAutoLive(context, false) }, Modifier.weight(1f)) { Text("PREVIEW FIRST") }
            }
            Text(
                if (autoLive) "Only decisive Scripture recognition may replace Live; uncertain matches still wait."
                else "AI prepares Scripture without replacing the current Live slide.",
                fontSize = 11.sp,
                color = Color(0xFFAAAAB2)
            )
        }
    }
}
