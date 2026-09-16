package com.kingzulu.biblepresentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
fun BrandingPanel(onPreview: (PresentationSlide) -> Unit) {
    val context = LocalContext.current
    var branding by remember { mutableStateOf(BrandingStore.load(context)) }
    var message by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val imported = BrandingStore.import(context, uri)
            if (imported != null) { branding = imported; message = "Brand logo saved on this device." }
            else message = "That file could not be imported. Try another image or video."
        }
    }
    Text("Branding", fontSize = 32.sp, fontWeight = FontWeight.Black)
    Text("Make the congregation screen belong to your church, not to the app.", color = Color(0xFFAAAAB2))
    Surface(Modifier.fillMaxWidth(), color = Color(0xFF151518), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CHURCH LOGO", color = Color(0xFF9B8AFF), fontWeight = FontWeight.Black, fontSize = 11.sp)
            PresentationCanvas(BrandingStore.slide(context), null, Modifier.fillMaxWidth())
            Text(if (branding == null) "King Zulu is the fallback until you add your own branding." else "Custom ${branding!!.mediaType} logo is active.", color = Color(0xFFAAAAB2), fontSize = 12.sp)
            Button({ picker.launch("*/*") }, Modifier.fillMaxWidth()) { Text(if (branding == null) "CHOOSE IMAGE OR VIDEO" else "REPLACE IMAGE OR VIDEO") }
            OutlinedButton({ onPreview(BrandingStore.slide(context)) }, Modifier.fillMaxWidth()) { Text("SEND LOGO TO PREVIEW") }
            if (branding != null) TextButton({ BrandingStore.clear(context); branding = null; message = "Custom logo removed." }) { Text("Remove custom logo") }
            if (message.isNotBlank()) Text(message, color = Color(0xFFAAAAB2), fontSize = 11.sp)
            Text("Logo selection never changes Live by itself. Preview it first, then use GO LIVE.", color = Color(0xFF66D19E), fontSize = 11.sp)
        }
    }
}
