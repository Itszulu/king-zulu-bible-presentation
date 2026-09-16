package com.kingzulu.biblepresentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BibleTranslationsPanel(onDone: () -> Unit) {
    var installed by remember { mutableStateOf(OfflineBibleRepository.installedBibles()) }
    var selected by remember { mutableStateOf(OfflineBibleRepository.selectedTranslation()) }
    var message by remember { mutableStateOf("") }

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
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(bible.abbreviation, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Text(bible.name, fontSize = 13.sp)
                            Text(if (bible.builtIn) "Built in • Available without internet" else "Installed • Available without internet", fontSize = 11.sp, color = Color(0xFFAAAAB2))
                        }
                        if (selected.equals(bible.abbreviation, true)) Button(onClick = {}) { Text("ACTIVE") }
                        else OutlinedButton(onClick = {
                            if (OfflineBibleRepository.selectTranslation(bible.abbreviation)) {
                                selected = bible.abbreviation
                                message = "${bible.abbreviation} is now the active offline Bible"
                            }
                        }) { Text("USE") }
                    }
                }
            }
        }
    }

    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DOWNLOAD MORE BIBLES", fontWeight = FontWeight.Black)
            Text("Authorised downloadable translations will appear here as sources are connected. King Zulu will download once, validate the module, then use it locally.", fontSize = 13.sp, color = Color(0xFFAAAAB2))
            Text("A failed download will never remove or replace your working Bible.", fontSize = 12.sp, color = Color(0xFF75D69C))
        }
    }
    if (message.isNotBlank()) Text(message, color = Color(0xFF9B8AFF))
    OutlinedButton(onDone, Modifier.fillMaxWidth()) { Text("BACK TO BIBLE") }
}
