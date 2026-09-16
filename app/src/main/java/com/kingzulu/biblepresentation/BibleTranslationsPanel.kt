package com.kingzulu.biblepresentation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var removeCandidate by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        installed = OfflineBibleRepository.installedBibles()
        selected = OfflineBibleRepository.selectedTranslation()
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val result = runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    readModuleBytes(input, 64 * 1024 * 1024)
                } ?: error("Unable to open selected file")
                BibleTranslationManager.install(context, bytes)
            }.getOrElse { BibleTranslationManager.InstallResult(false, message = it.message ?: "Unable to import Bible module") }
            message = result.message
            refresh()
        }
    }

    Text("Bible Translations", fontSize = 30.sp, fontWeight = FontWeight.Black)
    Text("Installed Bibles work completely offline during service.", color = Color(0xFFAAAAB2))

    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OFFLINE LIBRARY", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF9B8AFF))
                    Text("${installed.size} translation${if (installed.size == 1) "" else "s"} installed", fontWeight = FontWeight.Bold)
                }
                Surface(color = Color(0xFF153324), shape = RoundedCornerShape(999.dp)) {
                    Text("OFFLINE READY", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF75D69C), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
            installed.forEach { bible ->
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(bible.abbreviation, fontSize = 19.sp, fontWeight = FontWeight.Black)
                                Text(bible.name, fontSize = 13.sp)
                                Text(if (bible.builtIn) "Built in • Available without internet" else "Installed • Available without internet", fontSize = 11.sp, color = Color(0xFFAAAAB2))
                            }
                            if (selected.equals(bible.abbreviation, true)) {
                                Surface(color = Color(0xFF30275C), shape = RoundedCornerShape(999.dp)) {
                                    Text("ACTIVE", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFFB9ADFF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            } else OutlinedButton(onClick = {
                                if (OfflineBibleRepository.selectTranslation(bible.abbreviation)) {
                                    selected = bible.abbreviation
                                    message = "${bible.abbreviation} is now the active offline Bible"
                                }
                            }) { Text("USE") }
                        }
                        if (!bible.builtIn) {
                            TextButton(onClick = { removeCandidate = bible.abbreviation }) { Text("REMOVE FROM DEVICE") }
                        }
                    }
                }
            }
        }
    }

    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ADD OFFLINE BIBLE", fontWeight = FontWeight.Black)
            Text("Import an authorised King Zulu-compatible JSON Bible module from this device. Copyrighted translations should only be imported when your source or licence permits local use.", fontSize = 13.sp, color = Color(0xFFAAAAB2))
            Button(onClick = { importer.launch(arrayOf("application/json", "text/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                Text("IMPORT BIBLE MODULE")
            }
            Text("Modules are validated before activation. If an update cannot be activated, King Zulu restores the previous installed copy.", fontSize = 12.sp, color = Color(0xFF75D69C))
        }
    }
    if (message.isNotBlank()) Text(message, color = Color(0xFF9B8AFF))
    OutlinedButton(onDone, Modifier.fillMaxWidth()) { Text("BACK TO BIBLE") }

    removeCandidate?.let { abbreviation ->
        AlertDialog(
            onDismissRequest = { removeCandidate = null },
            title = { Text("Remove $abbreviation?") },
            text = { Text("This removes the downloaded offline module from this device. The built-in KJV is never removed.") },
            confirmButton = {
                TextButton(onClick = {
                    val removed = BibleTranslationManager.remove(abbreviation)
                    message = if (removed) "$abbreviation removed from this device" else "Unable to remove $abbreviation"
                    removeCandidate = null
                    refresh()
                }) { Text("REMOVE") }
            },
            dismissButton = { TextButton(onClick = { removeCandidate = null }) { Text("CANCEL") } }
        )
    }
}

private fun readModuleBytes(input: java.io.InputStream, maxBytes: Int): ByteArray {
    val buffer = ByteArray(8192)
    val output = java.io.ByteArrayOutputStream()
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) error("Bible module is too large")
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
