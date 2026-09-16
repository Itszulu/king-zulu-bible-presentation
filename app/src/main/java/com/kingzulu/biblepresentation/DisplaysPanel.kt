package com.kingzulu.biblepresentation

import android.app.Activity
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
import androidx.mediarouter.app.MediaRouteChooserDialog
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.delay

private val DisplayPurple=Color(0xFF7B61FF)
private val DisplaySurface=Color(0xFF151518)
private val DisplaySurface2=Color(0xFF202025)
private val DisplayMuted=Color(0xFFAAAAB2)
private val DisplayGreen=Color(0xFF66D19E)

@Composable
fun DisplaysPanel() {
    val context=LocalContext.current; val activity=context as? Activity
    val discovery=remember{KingZuluDisplayDiscovery(context)}; val castBridge=remember{GoogleCastBridge(context)}; val wiredBridge=remember{WiredDisplayBridge(context)}
    val devices=remember{mutableStateListOf<DiscoveredDisplay>()}; var scanning by remember{mutableStateOf(false)}; var selected by remember{mutableStateOf<DiscoveredDisplay?>(null)}
    var castDevice by remember{mutableStateOf(castBridge.currentDeviceName())}; var wired by remember{mutableStateOf(wiredBridge.available())}; var wiredActive by remember{mutableStateOf<Int?>(null)}

    LaunchedEffect(scanning){if(scanning){delay(3500);discovery.stop();scanning=false}}
    fun scanLocal(){discovery.stop();devices.clear();scanning=true;discovery.start{d->if(devices.none{it.host==d.host&&it.port==d.port})devices.add(d)}}
    fun showCastPicker(){if(activity==null)return;runCatching{MediaRouteChooserDialog(activity).apply{routeSelector=CastContext.getSharedInstance(context).mergedSelector;setTitle("Choose a display");setOnDismissListener{castDevice=castBridge.currentDeviceName()};show()}}}
    DisposableEffect(Unit){onDispose{discovery.stop();wiredBridge.dismiss()}}

    Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("Displays",fontSize=32.sp,fontWeight=FontWeight.Black)
        Text("Choose how King Zulu sends LIVE content to the congregation screen.",color=DisplayMuted,fontSize=14.sp)
        DisplayStatusCard(wiredActive!=null,castDevice,selected)

        DisplayMethodCard("Wired display","USB-C → HDMI","Lowest latency · works without Wi-Fi"){
            Button({wired=wiredBridge.available()},Modifier.fillMaxWidth()){Text("DETECT HDMI DISPLAY")}
            if(wired.isEmpty())Text("Connect a supported HDMI adapter, then detect again.",color=DisplayMuted,fontSize=12.sp)
            wired.forEach{d->OutlinedButton({if(wiredBridge.show(d.id))wiredActive=d.id},Modifier.fillMaxWidth()){Text(if(wiredActive==d.id)"● LIVE OUTPUT · ${d.name}" else "USE ${d.name}")}}
            if(wiredActive!=null)TextButton({wiredBridge.dismiss();wiredActive=null}){Text("Disconnect wired display")}
        }

        DisplayMethodCard("Wireless display","Google Cast / Google TV","Chromecast and compatible Android/Google TVs"){
            Button({showCastPicker()},Modifier.fillMaxWidth()){Text("FIND CAST TVs")}
            Text(if(castDevice==null)"No Cast display connected" else "● Connected · $castDevice",color=if(castDevice==null)DisplayMuted else DisplayGreen,fontSize=12.sp)
        }

        DisplayMethodCard("VIDAA & local TV","Same Wi-Fi","Hisense VIDAA and compatible LAN displays"){
            OutlinedButton({scanLocal()},Modifier.fillMaxWidth(),enabled=!scanning){Text(if(scanning)"SEARCHING…" else "SCAN LOCAL NETWORK")}
            if(scanning)LinearProgressIndicator(Modifier.fillMaxWidth())
            devices.forEach{d->OutlinedButton({selected=d},Modifier.fillMaxWidth()){Text(if(selected==d)"● ${d.name} · ${d.protocol}" else "${d.name} · ${d.protocol}")}}
            if(!scanning&&devices.isEmpty())Text("No local TVs found in the last scan.",color=DisplayMuted,fontSize=12.sp)
        }

        DisplayMethodCard("Browser display","Universal fallback","For smart TVs with a web browser"){
            Text("A short receiver address/code will appear here when the browser transport is enabled.",color=DisplayMuted,fontSize=12.sp)
            OutlinedButton({},enabled=false,modifier=Modifier.fillMaxWidth()){Text("START BROWSER DISPLAY · IN PROGRESS")}
        }
        Text("Discovery only runs when you ask for it and stops automatically after a few seconds to reduce battery, heat and network use.",color=DisplayMuted,fontSize=11.sp)
    }
}

@Composable private fun DisplayStatusCard(wired:Boolean,cast:String?,local:DiscoveredDisplay?){
    val connected=wired||cast!=null||local!=null
    Surface(Modifier.fillMaxWidth(),color=if(connected)Color(0xFF14231D) else DisplaySurface2,shape=RoundedCornerShape(18.dp)){
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Text(if(connected)"●" else "○",color=if(connected)DisplayGreen else DisplayMuted,fontSize=18.sp);Spacer(Modifier.width(10.dp));Column{Text(if(connected)"DISPLAY READY" else "NO DISPLAY CONNECTED",fontWeight=FontWeight.Black,fontSize=13.sp);Text(when{wired->"Wired HDMI output active";cast!=null->cast;local!=null->local.name;else->"Connect a screen before going live"},color=DisplayMuted,fontSize=12.sp)}
        }
    }
}

@Composable private fun DisplayMethodCard(title:String,eyebrow:String,subtitle:String,content:@Composable ColumnScope.()->Unit){
    Surface(Modifier.fillMaxWidth(),color=DisplaySurface,shape=RoundedCornerShape(22.dp)){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text(eyebrow.uppercase(),color=DisplayPurple,fontWeight=FontWeight.Bold,fontSize=10.sp);Text(title,fontWeight=FontWeight.Black,fontSize=20.sp);Text(subtitle,color=DisplayMuted,fontSize=12.sp);content()
        }
    }
}
