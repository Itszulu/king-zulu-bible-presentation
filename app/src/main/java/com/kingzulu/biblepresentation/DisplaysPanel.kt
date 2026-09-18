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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DS=Color(0xFF151518);private val DM=Color(0xFFAAAAB2);private val DG=Color(0xFF66D19E)

@Composable fun DisplaysPanel(sharedWiredBridge:WiredDisplayBridge?=null,browserReceiver:BrowserReceiverServer?=null){
 val context=LocalContext.current;val activity=context as? Activity;val scope=rememberCoroutineScope();val discovery=remember{KingZuluDisplayDiscovery(context)};val castBridge=remember{GoogleCastBridge(context)};val wired=sharedWiredBridge?:remember{WiredDisplayBridge(context)};val browser=browserReceiver?:remember{BrowserReceiverServer(context)}
 var castDevice by remember{mutableStateOf(castBridge.currentDeviceName())};var browserAddress by remember{mutableStateOf(browser.currentAddress())};var browserClients by remember{mutableIntStateOf(browser.connectedClients())};var diagnostics by remember{mutableStateOf(false)};var scanning by remember{mutableStateOf(false)};var smartScanning by remember{mutableStateOf(false)};var smartMessage by remember{mutableStateOf("VIDAA, Hisense and compatible DLNA TVs")};val devices=remember{mutableStateListOf<DiscoveredDisplay>()};val smartTvs=remember{mutableStateListOf<Pair<SmartTvRenderer,String>>()}
 LaunchedEffect(Unit){while(true){castDevice=castBridge.currentDeviceName();browserAddress=browser.currentAddress();browserClients=browser.connectedClients();delay(1000)}}
 LaunchedEffect(scanning){if(scanning){delay(3500);discovery.stop();scanning=false}}
 fun showCastPicker(){if(activity==null)return;runCatching{val selector=CastContext.getSharedInstance(context).mergedSelector?:return@runCatching;MediaRouteChooserDialog(activity).apply{routeSelector=selector;setTitle("Choose a Cast TV");show()}}}
 fun scan(){devices.clear();scanning=true;discovery.start{d->if(devices.none{it.host==d.host&&it.port==d.port})devices.add(d)}}
 fun scanSmartTvs(){if(smartScanning)return;smartScanning=true;smartTvs.clear();smartMessage="Searching your local network…";scope.launch{val found=withContext(Dispatchers.IO){DlnaDiscovery.scan()};found.forEach{r->val name=withContext(Dispatchers.IO){DlnaDiscovery.friendlyName(r)};smartTvs.add(r to name)};smartMessage=if(found.isEmpty())"No compatible Smart TVs found" else "${found.size} compatible Smart TV${if(found.size==1)"" else "s"} found";smartScanning=false}}
 DisposableEffect(Unit){onDispose{discovery.stop();if(browserReceiver==null)browser.stop();if(sharedWiredBridge==null)wired.dismiss()}}
 Text("Displays",fontSize=32.sp,fontWeight=FontWeight.Black);Text("Connect the congregation screen.",color=DM)
 Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("GOOGLE CAST",fontWeight=FontWeight.Black);Text(if(castDevice==null)"Chromecast, Google TV and compatible Cast TVs" else "● $castDevice connected",color=if(castDevice==null)DM else DG);Button({showCastPicker()},Modifier.fillMaxWidth()){Text(if(castDevice==null)"FIND CAST TVs" else "CHANGE CAST TV")};Text("The Android Cast chooser opens immediately so a tap never appears unresponsive.",fontSize=11.sp,color=DM)}}
 Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("SMART TV / DLNA",fontWeight=FontWeight.Black);Text(smartMessage,color=if(smartTvs.isEmpty())DM else DG);Button({scanSmartTvs()},Modifier.fillMaxWidth(),enabled=!smartScanning){Text(if(smartScanning)"SEARCHING…" else "FIND SMART TVs")};if(smartScanning)LinearProgressIndicator(Modifier.fillMaxWidth());smartTvs.forEach{(tv,name)->Surface(Modifier.fillMaxWidth(),color=DS,shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(12.dp)){Text(name,fontWeight=FontWeight.Bold);Text("${tv.address} · DLNA MediaRenderer",fontSize=11.sp,color=DM)}}};if(smartTvs.isNotEmpty())Text("Discovery confirmed. Presentation connection will only be enabled for capabilities verified on the TV.",fontSize=11.sp,color=DM)}}
 Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("BROWSER DISPLAY",fontWeight=FontWeight.Black);if(browserAddress==null)Button({browserAddress=browser.start()},Modifier.fillMaxWidth()){Text("START RECEIVER")}else{Text("● Receiver active",color=DG);Text(browserAddress!!,fontSize=22.sp,fontWeight=FontWeight.Black);Text("$browserClients browser display${if(browserClients==1)"" else "s"} connected",color=if(browserClients>0)DG else DM,fontWeight=FontWeight.Bold);OutlinedButton({browser.stop();browserAddress=null;browserClients=0},Modifier.fillMaxWidth()){Text("STOP RECEIVER")}}}}
 Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("HDMI",fontWeight=FontWeight.Black);val displays=wired.available();if(displays.isEmpty())Text("No wired display detected",color=DM)else displays.forEach{d->Button({wired.show(d.id)},Modifier.fillMaxWidth()){Text("USE ${d.name}")}}}}
 TextButton({diagnostics=!diagnostics}){Text(if(diagnostics)"HIDE NETWORK DIAGNOSTICS" else "NETWORK DIAGNOSTICS")}
 if(diagnostics)Surface(Modifier.fillMaxWidth(),color=DS,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Nearby network devices",fontWeight=FontWeight.Bold);OutlinedButton({scan()},Modifier.fillMaxWidth(),enabled=!scanning){Text(if(scanning)"SEARCHING…" else "SCAN LOCAL NETWORK")};if(scanning)LinearProgressIndicator(Modifier.fillMaxWidth());devices.forEach{d->Text("${d.name} · ${d.protocol} · ${d.host}",fontSize=12.sp,color=DM)};if(!scanning&&devices.isEmpty())Text("No devices found",fontSize=12.sp,color=DM);Text("Network discovery does not mean a presentation connection is active.",fontSize=11.sp,color=DM)}}
}
