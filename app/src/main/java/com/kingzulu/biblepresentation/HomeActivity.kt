package com.kingzulu.biblepresentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OfflineBibleRepository.initialize(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFF7B61FF),background=Color(0xFF0A0A0B),surface=Color(0xFF151518))) {
                val bridge=remember{WiredDisplayBridge(this@HomeActivity)}
                var page by remember{mutableStateOf("Home")}
                DisposableEffect(bridge){onDispose{bridge.dismiss()}}
                Scaffold(containerColor=Color(0xFF0A0A0B)){pad->
                    Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                        when(page){
                            "Branding"->BrandingPanel{page="Home"}
                            "Outputs"->DisplaysPanel(bridge)
                            else->HomePanel(
                                OfflineBibleRepository.selectedTranslation(),null,false,
                                bridge.available().isNotEmpty(),OperatorPreferences.aiAutoLive(this@HomeActivity)
                            ){target->
                                when(target){
                                    "Branding","Outputs"->page=target
                                    else->startActivity(Intent(this@HomeActivity,MainActivity::class.java))
                                }
                            }
                        }
                        if(page!="Home") OutlinedButton({page="Home"},Modifier.fillMaxWidth()){Text("BACK TO HOME")}
                    }
                }
            }
        }
    }
}
