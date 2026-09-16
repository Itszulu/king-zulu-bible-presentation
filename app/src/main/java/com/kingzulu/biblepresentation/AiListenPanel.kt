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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun AiListenPanel(onPreview:(PresentationSlide)->Unit,onGoLive:(PresentationSlide)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var listening by remember{mutableStateOf(false)};var transcript by remember{mutableStateOf("")};var status by remember{mutableStateOf("Ready to listen")};var candidates by remember{mutableStateOf<List<ScriptureMatch>>(emptyList())};var explicitSlide by remember{mutableStateOf<PresentationSlide?>(null)};var analysisId by remember{mutableIntStateOf(0)};var autoLive by remember{mutableStateOf(OperatorPreferences.aiAutoLive(context))}
 fun setAutoLive(enabled:Boolean){autoLive=enabled;OperatorPreferences.setAiAutoLive(context,enabled)}
 fun slide(v:Verse)=PresentationSlide(v.reference.display(),v.text,v.translation)
 fun analyze(text:String){val id=++analysisId;status="Searching Scripture…";scope.launch{val result=withContext(Dispatchers.Default){val explicit=SpokenBibleReferenceParser.parse(text)?.let{OfflineBibleRepository.get(it)};val matches=if(explicit==null)OfflineBibleRepository.searchQuoteMatches(text,5)else emptyList();Pair(explicit,matches)};if(id!=analysisId)return@launch;val(explicit,matches)=result;explicitSlide=explicit?.let(::slide);candidates=matches
   if(explicit!=null){val s=slide(explicit);if(autoLive){onPreview(s);onGoLive(s);status="LIVE • ${s.reference}"}else status="Scripture reference detected: ${s.reference}";return@launch}
   val best=matches.firstOrNull();val second=matches.getOrNull(1);val decisive=best!=null&&best.score>=0.78&&(second==null||best.score-second.score>=0.16)
   if(best==null){status="No confident match yet — keep speaking"}
   else if(autoLive&&decisive){val s=slide(best.verse);onPreview(s);onGoLive(s);status="LIVE • ${s.reference}"}
   else if(matches.size>1){status="${matches.size} possible matches — choose the intended Scripture"}
   else status="Possible Scripture match found"
 }}
 val controller=remember{AiListenController(context,{listening=it},{partial->transcript=partial;status="Listening…"},{finalText->transcript=finalText;analyze(finalText)},{status=it})}
 DisposableEffect(Unit){onDispose{controller.destroy()}}
 val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)controller.start()else status="Microphone permission is required for AI Listen"}
 fun startListening(){if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)controller.start()else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)}
 Text("AI Listen",fontSize=32.sp,fontWeight=FontWeight.Bold);Text("Listen for Scripture wording first, or speak a Bible reference.",color=Color(0xFFA6A7AD))
 Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("AI PROJECTION",fontWeight=FontWeight.Black,fontSize=12.sp,color=Color(0xFFA6A7AD));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){if(autoLive)Button({setAutoLive(true)},Modifier.weight(1f)){Text("AUTO LIVE")}else OutlinedButton({setAutoLive(true)},Modifier.weight(1f)){Text("AUTO LIVE")};if(!autoLive)Button({setAutoLive(false)},Modifier.weight(1f)){Text("PREVIEW FIRST")}else OutlinedButton({setAutoLive(false)},Modifier.weight(1f)){Text("PREVIEW FIRST")}};Text(if(autoLive)"A decisive match can go live immediately. Close or uncertain matches wait for your choice." else "Recognised Scripture waits until you choose GO LIVE.",fontSize=12.sp,color=if(autoLive)Color(0xFF75D69C)else Color(0xFFA6A7AD))}}
 Surface(Modifier.fillMaxWidth(),color=if(listening)Color(0xFF15251F)else MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(if(listening)"● LISTENING" else "○ NOT LISTENING",fontWeight=FontWeight.Black,color=if(listening)Color(0xFF75D69C)else Color(0xFFA6A7AD));Text(status,color=Color(0xFFB8BAC1));Button({if(listening)controller.stop()else startListening()},Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text(if(listening)"STOP LISTENING" else "START LISTENING",fontWeight=FontWeight.Bold)}}}
 Text("LIVE TRANSCRIPT",fontWeight=FontWeight.Bold,color=Color(0xFFA6A7AD));Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp)){Text(if(transcript.isBlank())"Speech will appear here…" else transcript,Modifier.padding(18.dp),fontSize=18.sp,color=if(transcript.isBlank())Color(0xFF777980)else Color.White)}
 Text(if(candidates.size>1)"POSSIBLE SCRIPTURE MATCHES" else "SCRIPTURE DETECTED",fontWeight=FontWeight.Bold,color=Color(0xFFA6A7AD))
 explicitSlide?.let{s->MatchCard(s,"EXACT REFERENCE",onPreview,onGoLive)}
 if(explicitSlide==null&&candidates.isEmpty())Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp)){Text("No verse detected yet",Modifier.padding(18.dp),color=Color(0xFF888A91))}
 candidates.forEachIndexed{i,m->MatchCard(slide(m.verse),if(i==0)"BEST MATCH" else "OPTION ${i+1}",onPreview,onGoLive)}
}

@Composable private fun MatchCard(s:PresentationSlide,label:String,onPreview:(PresentationSlide)->Unit,onGoLive:(PresentationSlide)->Unit){Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(label,fontSize=11.sp,fontWeight=FontWeight.Black,color=Color(0xFF9C7CFF));Text(s.reference,fontWeight=FontWeight.Black,fontSize=20.sp);Text(s.text,fontSize=16.sp,maxLines=3);Text(s.translation,color=Color(0xFFA6A7AD),fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({onPreview(s)},Modifier.weight(1f)){Text("PREVIEW")};Button({onGoLive(s)},Modifier.weight(1f)){Text("GO LIVE")}}}}}
