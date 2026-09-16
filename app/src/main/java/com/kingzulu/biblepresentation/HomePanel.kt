package com.kingzulu.biblepresentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomePanel(
    translation: String,
    live: PresentationSlide?,
    black: Boolean,
    displayDetected: Boolean,
    autoLive: Boolean,
    open: (String) -> Unit
) {
    Text("Ready for service", fontSize = 32.sp, fontWeight = FontWeight.Black)
    Text("Prepare everything first. Connect an audience display only when you need it.", color = Color(0xFFAAAAB2), fontSize = 14.sp)

    Surface(Modifier.fillMaxWidth(), color = Color(0xFF151518), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SERVICE READINESS", color = Color(0xFF9B8AFF), fontWeight = FontWeight.Black, fontSize = 11.sp)
            StatusRow("Audience output", if (displayDetected) "Display detected" else "No audience display connected")
            StatusRow("Bible", "$translation · available offline")
            StatusRow("AI Listen", if (autoLive) "Auto Live enabled" else "Preview First")
            StatusRow("Live", when { black -> "Black screen"; live != null -> live.reference.ifBlank { live.kind.uppercase() }; else -> "Nothing live" })
            OutlinedButton({ open("Outputs") }, Modifier.fillMaxWidth()) { Text(if (displayDetected) "MANAGE OUTPUTS" else "CONNECT A DISPLAY") }
        }
    }

    Text("QUICK START", color = Color(0xFFAAAAB2), fontSize = 11.sp, fontWeight = FontWeight.Black)
    listOf(
        "Bible" to "Find Scripture",
        "Listen" to "AI Scripture recognition",
        "Service" to "Build the running order",
        "Media" to "Lyrics, timer and media",
        "Present" to "Preview and Live control",
        "Branding" to "Church logo and identity"
    ).chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (target, subtitle) ->
                OutlinedButton({ open(target) }, Modifier.weight(1f).heightIn(min = 74.dp), shape = RoundedCornerShape(18.dp)) {
                    Column { Text(target.uppercase(), fontWeight = FontWeight.Black); Text(subtitle, fontSize = 10.sp, color = Color(0xFFAAAAB2)) }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFAAAAB2), fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
