package com.kingzulu.biblepresentation

import android.app.Activity
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
import androidx.mediarouter.app.MediaRouteChooserDialog
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.delay

private val DS=Color(0xFF151518);private val DM=Color(0xFFAAAAB2);private val DG=Color(0xFF66D19E)

@Composable fun DisplaysPanel(sharedWiredBridge:WiredDisplayBridge?=null,browserReceiver:BrowserReceiverServer?=null){
 val context=LocalContext.current;val activity=context as? Activity;val discovery=remember{KingZuluDisplayDiscovery(context)};val castBridge=remember{GoogleCastBridge(context)};val wired=sharedWiredBridge?:remember{WiredDisplayBridge(context)};val browser=browserReceiver?:remember{BrowserReceiverServer(context)}
 var castDevice by remember{mutableStateOf(castBridge.currentDeviceName())};var browserAddress by remember{mutableStateOf<String?>(null)};var diagnostics by remember{mutableStateOf(false)};var scanning by remember{mutableStateOf(false)};val devices=remember{mutableStateListOf<DiscoveredDisplay>()}
 LaunchedEffect(Unit){while(true){castDevice=castBridge.currentDeviceName();delay(1000)}}
 LaunchedEffect(scanning){if(scanning){delay(3500);discovery.stop();scanning=false}}
 fun showCastPicker(){if(activity==null)return;runCatching{val selector=CastContext.getSharedInstance(context).mergedSelector?:return@runCatching;MediaRouteChooserDialog(activity).apply{routeSelector=selector;setTitle("Choose a TV");show()}}}
 fun scan(){devices.clear();scanning=true;discovery.start{d->if(devices.none{it.host==d.host&&it.port==d.port})devices.add(d)}}
 DisposableEffect(Unit){onDispose{discovery.stop();if(browserReceiver==null)browser.stop();if(sharedWiredBridge==null)wired.dismiss()}}
 Text("Displays",fontSize=32.sp,fontWeight=FontWeight.Black);Text("Connect the congregation screen.",color=DM)
 Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("GOOGLE CAST",fontWeight=FontWeight.Black);Text(if(castDevice==null)"Chromecast, Google TV and compatible TVs" else "● $castDevice connected",color=if(castDevice==null)DM else DG);Button({showCastPicker()},Modifier.fillMaxWidth()){Text(if(castDevice==null)"FIND CAST TVs" else "CHANGE CAST TV")}}}
 Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("BROWSER DISPLAY",fontWeight=FontWeight.Black);if(browserAddress==null)Button({browserAddress=browser.start()},Modifier.fillMaxWidth()){Text("START RECEIVER")}else{Text("● Receiver ready",color=DG);Text(browserAddress!!,fontSize=22.sp,fontWeight=FontWeight.Black);OutlinedButton({browser.stop();browserAddress=null},Modifier.fillMaxWidth()){Text("STOP RECEIVER")}}}}
 Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("HDMI",fontWeight=FontWeight.Black);val displays=wired.available();if(displays.isEmpty())Text("No wired display detected",color=DM)else displays.forEach{d->Button({wired.show(d.id)},Modifier.fillMaxWidth()){Text("USE ${d.name}")}}}}
 TextButton({diagnostics=!diagnostics}){Text(if(diagnostics)"HIDE NETWORK DIAGNOSTICS" else "NETWORK DIAGNOSTICS")}
 if(diagnostics)Surface(Modifier.fillMaxWidth(),color=DS,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Nearby network devices",fontWeight=FontWeight.Bold);OutlinedButton({scan()},Modifier.fillMaxWidth(),enabled=!scanning){Text(if(scanning)"SEARCHING…" else "SCAN LOCAL NETWORK")};if(scanning)LinearProgressIndicator(Modifier.fillMaxWidth());devices.forEach{d->Text("${d.name} · ${d.protocol} · ${d.host}",fontSize=12.sp,color=DM)};if(!scanning&&devices.isEmpty())Text("No devices found",fontSize=12.sp,color=DM);Text("Network discovery does not mean a presentation connection is active.",fontSize=11.sp,color=DM)}}
}
