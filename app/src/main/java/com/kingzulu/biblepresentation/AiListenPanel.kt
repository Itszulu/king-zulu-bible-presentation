package com.kingzulu.biblepresentation

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat

@Composable
fun AiListenPanel(
    onPreview: (PresentationSlide) -> Unit,
    onGoLive: (PresentationSlide) -> Unit
) {
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready to listen") }
    var detectedSlide by remember { mutableStateOf<PresentationSlide?>(null) }

    val controller = remember {
        AiListenController(
            context = context,
            onListeningChanged = { listening = it },
            onTranscript = { text ->
                transcript = text
                val reference = SpokenBibleReferenceParser.parse(text)
                val verse = reference?.let { OfflineBibleRepository.get(it) }
                detectedSlide = verse?.let { PresentationSlide(it.reference.display(), it.text, it.translation) }
                status = if (verse != null) "Scripture detected: ${verse.reference.display()}" else "Listening for a Scripture reference…"
            },
            onError = { status = it }
        )
    }

    DisposableEffect(Unit) { onDispose { controller.destroy() } }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.start() else status = "Microphone permission is required for AI Listen"
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) controller.start()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Text("AI Listen", fontSize = 32.sp, fontWeight = FontWeight.Bold)
    Text("Listen to the sermon and prepare Scripture references as they are spoken.", color = Color(0xFFA6A7AD))

    Surface(Modifier.fillMaxWidth(), color = if (listening) Color(0xFF15251F) else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (listening) "● LISTENING" else "○ NOT LISTENING", fontWeight = FontWeight.Black, color = if (listening) Color(0xFF75D69C) else Color(0xFFA6A7AD))
            Text(status, color = Color(0xFFB8BAC1))
            Button(onClick = { if (listening) controller.stop() else startListening() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Text(if (listening) "STOP LISTENING" else "START LISTENING", fontWeight = FontWeight.Bold)
            }
        }
    }

    Text("LIVE TRANSCRIPT", fontWeight = FontWeight.Bold, color = Color(0xFFA6A7AD))
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Text(if (transcript.isBlank()) "Speech will appear here…" else transcript, Modifier.padding(18.dp), fontSize = 18.sp, color = if (transcript.isBlank()) Color(0xFF777980) else Color.White)
    }

    Text("SCRIPTURE DETECTED", fontWeight = FontWeight.Bold, color = Color(0xFFA6A7AD))
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (detectedSlide == null) {
                Text("No verse detected yet", color = Color(0xFF888A91))
            } else {
                Text(detectedSlide!!.title, fontWeight = FontWeight.Black, fontSize = 21.sp)
                Text(detectedSlide!!.text, fontSize = 17.sp)
                Text(detectedSlide!!.translation, color = Color(0xFFA6A7AD), fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton({ detectedSlide?.let(onPreview) }, Modifier.weight(1f)) { Text("PREVIEW") }
                    Button({ detectedSlide?.let(onGoLive) }, Modifier.weight(1f)) { Text("GO LIVE") }
                }
            }
        }
    }
}
