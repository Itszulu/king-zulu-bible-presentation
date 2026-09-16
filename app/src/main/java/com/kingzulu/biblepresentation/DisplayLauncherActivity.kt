package com.kingzulu.biblepresentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class DisplayLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("KING ZULU · CONNECT DISPLAY") }) }
                ) { pad ->
                    Column(Modifier.padding(pad).padding(18.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DisplaysPanel()
                        Button(
                            onClick = { startActivity(Intent(this@DisplayLauncherActivity, MainActivity::class.java)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("OPEN PRESENTATION") }
                    }
                }
            }
        }
    }
}
