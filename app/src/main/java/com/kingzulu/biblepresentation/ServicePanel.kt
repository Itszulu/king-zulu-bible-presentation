package com.kingzulu.biblepresentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServicePanel(
    preview: PresentationSlide?,
    onPreview: (PresentationSlide) -> Unit,
    onGoLive: (PresentationSlide) -> Unit
) {
    var items by remember { mutableStateOf(listOf<PresentationSlide>()) }

    Text("Service", fontSize = 32.sp, fontWeight = FontWeight.Bold)
    Text("Build the running order for your church service.", color = Color(0xFF9699A3))

    Surface(Modifier.fillMaxWidth(), color = Color(0xFF14161C), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ADD TO SERVICE", color = Color(0xFF9699A3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (preview == null) {
                Text("Preview a Bible verse, lyric or countdown first, then add it here.", color = Color(0xFF777B86))
            } else {
                Text(preview.reference.ifBlank { preview.kind.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(preview.text.take(150), color = Color(0xFFB9BBC3), maxLines = 3)
                Button(
                    onClick = { items = items + preview },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("+ ADD CURRENT PREVIEW", fontWeight = FontWeight.Bold) }
            }
        }
    }

    Text("RUNNING ORDER", color = Color(0xFF9699A3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    if (items.isEmpty()) {
        Surface(Modifier.fillMaxWidth(), color = Color(0xFF101217), shape = RoundedCornerShape(20.dp)) {
            Text("Your service is empty. Add Scripture, lyrics and countdowns as you prepare.", Modifier.padding(20.dp), color = Color(0xFF777B86))
        }
    } else {
        items.forEachIndexed { index, slide ->
            Surface(Modifier.fillMaxWidth(), color = Color(0xFF14161C), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${index + 1}. ${slide.reference.ifBlank { slide.kind.uppercase() }}", fontWeight = FontWeight.Bold)
                        Text(slide.translation, color = Color(0xFF9699A3), fontSize = 12.sp)
                    }
                    Text(slide.text.take(120), color = Color(0xFFB9BBC3), maxLines = 2)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ onPreview(slide) }, Modifier.weight(1f)) { Text("PREVIEW") }
                        Button({ onGoLive(slide) }, Modifier.weight(1f)) { Text("GO LIVE") }
                    }
                    TextButton({ items = items.filterIndexed { i, _ -> i != index } }) { Text("Remove") }
                }
            }
        }
    }
}
