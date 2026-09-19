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
 val context=LocalContext.current;val scope=rememberCoroutineScope();var sessionTick by remember{mutableIntStateOf(0)};var candidates by remember{mutableStateOf<List<ScriptureMatch>>(emptyList())};var explicitSlide by remember{mutableStateOf<PresentationSlide?>(null)};var sermonQueue by remember{mutableStateOf<List<Verse>>(AiSermonContext.queue())};var analysisId by remember{mutableIntStateOf(0)};var autoLive by remember{mutableStateOf(OperatorPreferences.aiAutoLive(context))}
 val listening=AiListenSession.listening.also{sessionTick};val transcript=AiListenSession.transcript.also{sessionTick};var localStatus by remember{mutableStateOf("")};val status=if(localStatus.isBlank())AiListenSession.status.also{sessionTick}else localStatus
 val currentAutoLive by rememberUpdatedState(autoLive);val currentGoLive by rememberUpdatedState(onGoLive);val currentPreview by rememberUpdatedState(onPreview)
 fun setAutoLive(enabled:Boolean){autoLive=enabled;OperatorPreferences.setAiAutoLive(context,enabled)}
 fun slide(v:Verse)=PresentationSlide(v.reference.display(),v.text,v.translation)
 val analyzeCandidates by rememberUpdatedState<(List<String>)->Unit>({alternatives->
   if(alternatives.isEmpty())return@rememberUpdatedState
   val id=++analysisId
   // Fast path: explicit references and contextual navigation are deterministic
   // and must never wait behind whole-Bible quotation search.
   val immediate=AiSermonContext.decide(alternatives)
   sermonQueue=immediate.queue;candidates=emptyList();explicitSlide=immediate.activate?.let(::slide)
   immediate.activate?.let{v->val s=slide(v);if(currentAutoLive){currentGoLive(s);localStatus="LIVE • ${s.reference}"}else{currentPreview(s);localStatus="Ready • ${s.reference}"};return@rememberUpdatedState}
   if(immediate.intent==ScriptureSpeechIntent.LISTING){localStatus="${immediate.queue.size} Scriptures prepared — waiting for the preacher to choose one";return@rememberUpdatedState}
   if(immediate.intent==ScriptureSpeechIntent.MENTION){localStatus="Scripture mentioned — not projected";return@rememberUpdatedState}
   localStatus="Checking Scripture wording…"
   scope.launch{
     val result=withContext(Dispatchers.Default){
       val discourse=immediate
       // Do not run a whole-Bible search for every recognizer alternative.
       // The primary transcript is normally the best candidate; try it first,
       // then one fallback only when the first is not confident.
       val primary=alternatives.first()
       var ranked=OfflineBibleRepository.searchQuoteMatches(primary,5)
       if((ranked.firstOrNull()?.score?:0.0)<0.72&&alternatives.size>1){
         ranked=(ranked+OfflineBibleRepository.searchQuoteMatches(alternatives[1],5))
           .groupBy{it.verse.reference.display()+"|"+it.verse.translation}
           .mapNotNull{(_,same)->same.maxByOrNull{it.score}}
       }
       ranked=ranked.sortedByDescending{it.score}.take(5)
       discourse to ranked
     }
     if(id!=analysisId)return@launch
     val(discourse,matches)=result;sermonQueue=discourse.queue;candidates=matches;explicitSlide=discourse.activate?.let(::slide)
     discourse.activate?.let{v->val s=slide(v);if(currentAutoLive){currentGoLive(s);localStatus="LIVE • ${s.reference}"}else{currentPreview(s);localStatus="Ready • ${s.reference}"};return@launch}
     if(discourse.intent==ScriptureSpeechIntent.LISTING){explicitSlide=null;candidates=emptyList();localStatus="${discourse.queue.size} Scriptures prepared — waiting for the preacher to choose one";return@launch}
     if(discourse.intent==ScriptureSpeechIntent.MENTION){explicitSlide=null;localStatus="Scripture mentioned — not projected";return@launch}
     val best=matches.firstOrNull();val second=matches.getOrNull(1);val decisive=best!=null&&best.score>=0.82&&(second==null||best.score-second.score>=0.18)
     if(best==null)localStatus="No confident Scripture yet — keep speaking" else if(currentAutoLive&&decisive){val s=slide(best.verse);AiSermonContext.decide(listOf(s.reference));currentGoLive(s);localStatus="LIVE • ${s.reference}"}else if(decisive){val s=slide(best.verse);currentPreview(s);localStatus="Possible quotation match • ${s.reference}"}else if(matches.size>1)localStatus="${matches.size} possible matches — choose the intended Scripture" else localStatus="Possible Scripture match found"
   }
 })
 val controller=remember(context){AiListenSession.ensure(context,{},{alts->analyzeCandidates(alts)})}
 DisposableEffect(Unit){val remove=AiListenSession.observe{sessionTick++};onDispose{remove()}}
 val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)AiListenSession.start(controller)else localStatus="Microphone permission is required for AI Listen"}
 fun startListening(){localStatus="";if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)AiListenSession.start(controller)else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)}
 Text("AI Listen",fontSize=32.sp,fontWeight=FontWeight.Bold);Text("Follows sermon context, Scripture wording and spoken references.",color=Color(0xFFA6A7AD))
 Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("AI PROJECTION",fontWeight=FontWeight.Black,fontSize=12.sp,color=Color(0xFFA6A7AD));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){if(autoLive)Button({setAutoLive(true)},Modifier.weight(1f)){Text("AUTO LIVE")}else OutlinedButton({setAutoLive(true)},Modifier.weight(1f)){Text("AUTO LIVE")};if(!autoLive)Button({setAutoLive(false)},Modifier.weight(1f)){Text("PREVIEW FIRST")}else OutlinedButton({setAutoLive(false)},Modifier.weight(1f)){Text("PREVIEW FIRST")}};Text(if(autoLive)"Direct reading commands can go live immediately. Lists and incidental mentions are held safely." else "AI prepares the intended Scripture for operator approval.",fontSize=12.sp,color=if(autoLive)Color(0xFF75D69C)else Color(0xFFA6A7AD))}}
 Surface(Modifier.fillMaxWidth(),color=if(listening)Color(0xFF15251F)else MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(if(listening)"● LISTENING" else "○ NOT LISTENING",fontWeight=FontWeight.Black,color=if(listening)Color(0xFF75D69C)else Color(0xFFA6A7AD));Text(status,color=Color(0xFFB8BAC1));Button({if(listening){AiListenSession.stop();localStatus=""}else startListening()},Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text(if(listening)"STOP LISTENING" else "START LISTENING",fontWeight=FontWeight.Bold)}}}
 Text("LIVE TRANSCRIPT",fontWeight=FontWeight.Bold,color=Color(0xFFA6A7AD));Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp)){Text(if(transcript.isBlank())"Speech will appear here…" else transcript,Modifier.padding(18.dp),fontSize=18.sp,color=if(transcript.isBlank())Color(0xFF777980)else Color.White)}
 if(sermonQueue.isNotEmpty()){Text("SERMON SCRIPTURE QUEUE",fontWeight=FontWeight.Bold,color=Color(0xFFA6A7AD));Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){sermonQueue.forEachIndexed{i,v->Text("${i+1}. ${v.reference.display()}",fontWeight=if(AiSermonContext.activeVerse()?.reference?.display()==v.reference.display())FontWeight.Black else FontWeight.Normal,color=if(AiSermonContext.activeVerse()?.reference?.display()==v.reference.display())Color(0xFF75D69C)else Color.White)}}}}
 Text(if(candidates.size>1)"POSSIBLE SCRIPTURE MATCHES" else "SCRIPTURE DETECTED",fontWeight=FontWeight.Bold,color=Color(0xFFA6A7AD));explicitSlide?.let{s->MatchCard(s,"CONTEXT SELECTED",onPreview,onGoLive)};if(explicitSlide==null&&candidates.isEmpty()&&sermonQueue.isEmpty())Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp)){Text("No verse detected yet",Modifier.padding(18.dp),color=Color(0xFF888A91))};candidates.forEachIndexed{i,m->MatchCard(slide(m.verse),if(i==0)"BEST MATCH" else "OPTION ${i+1}",onPreview,onGoLive)}
}

@Composable private fun MatchCard(s:PresentationSlide,label:String,onPreview:(PresentationSlide)->Unit,onGoLive:(PresentationSlide)->Unit){Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(label,fontSize=11.sp,fontWeight=FontWeight.Black,color=Color(0xFF9C7CFF));Text(s.reference,fontWeight=FontWeight.Black,fontSize=20.sp);Text(s.text,fontSize=16.sp,maxLines=3);Text(s.translation,color=Color(0xFFA6A7AD),fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({onPreview(s)},Modifier.weight(1f)){Text("PREVIEW")};Button({onGoLive(s)},Modifier.weight(1f)){Text("GO LIVE")}}}}}
