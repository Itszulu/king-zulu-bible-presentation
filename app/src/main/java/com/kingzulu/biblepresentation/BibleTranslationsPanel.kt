package com.kingzulu.biblepresentation

import android.net.Uri
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
fun BibleTranslationsPanel(onDone: () -> Unit) {
    val context = LocalContext.current
    var installed by remember { mutableStateOf(OfflineBibleRepository.installedBibles()) }
    var selected by remember { mutableStateOf(OfflineBibleRepository.selectedTranslation()) }
    var message by remember { mutableStateOf("") }

    fun refresh() {
        installed = OfflineBibleRepository.installedBibles()
        selected = OfflineBibleRepository.selectedTranslation()
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val result = runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Unable to open Bible file")
                BibleTranslationManager.install(context, bytes)
            }.getOrElse {
                BibleTranslationManager.InstallResult(false, message = it.message ?: "This Bible file could not be installed")
            }
            message = result.message
            refresh()
        }
    }

    Text("Bibles", fontSize = 30.sp, fontWeight = FontWeight.Black)
    Text("Choose an offline Bible or add one from your device.", color = Color(0xFFAAAAB2))

    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            installed.forEach { bible ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(bible.abbreviation, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text(bible.name, fontSize = 13.sp)
                    }
                    if (selected.equals(bible.abbreviation, true)) {
                        Text("ACTIVE", color = Color(0xFF9B8AFF), fontWeight = FontWeight.Black)
                    } else {
                        OutlinedButton(onClick = {
                            if (OfflineBibleRepository.selectTranslation(bible.abbreviation)) {
                                selected = bible.abbreviation
                                message = "${bible.abbreviation} selected"
                            }
                        }) { Text("USE") }
                    }
                }
            }
        }
    }

    Button(
        onClick = { importer.launch(arrayOf("application/json", "text/json", "text/plain")) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("ADD BIBLE FROM DEVICE") }

    Text("King Zulu checks the file before installing it. Unsupported Bible formats are rejected safely.", fontSize = 12.sp, color = Color(0xFFAAAAB2))
    if (message.isNotBlank()) Text(message, color = Color(0xFF9B8AFF))
    OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("BACK TO BIBLE") }
}
