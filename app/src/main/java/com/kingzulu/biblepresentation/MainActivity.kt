@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kingzulu.biblepresentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme{KingZuluApp()}}}
}

@Composable fun KingZuluApp(){
 var tab by remember{mutableStateOf("Bible")};var live by remember{mutableStateOf<PresentationSlide?>(null)};var preview by remember{mutableStateOf<PresentationSlide?>(null)};var theme by remember{mutableStateOf<BackgroundTheme?>(null)}
 val context=androidx.compose.ui.platform.LocalContext.current
 val gallery=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->uri?.let{theme=LocalBackgroundStore.importFromGallery(context,it)}}
 Scaffold(topBar={TopAppBar(title={Text("King Zulu",fontWeight=FontWeight.Bold)})}){pad->Column(Modifier.padding(pad).fillMaxSize()){
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(8.dp)){listOf("Bible","Lyrics","Timer","Live").forEachIndexed{i,x->SegmentedButton(selected=tab==x,onClick={tab=x},shape=SegmentedButtonDefaults.itemShape(i,4)){Text(x)}}}
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)){
   when(tab){
    "Bible"->BiblePanel{preview=it;tab="Live"}
    "Lyrics"->LyricsPanel{preview=it;tab="Live"}
    "Timer"->TimerPanel{preview=it;tab="Live"}
    else->{Text("PREVIEW",fontWeight=FontWeight.Bold);PresentationCanvas(preview,theme,Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp));Button({live=preview},enabled=preview!=null,modifier=Modifier.fillMaxWidth()){Text("GO LIVE")};Spacer(Modifier.height(16.dp));Text("CURRENT LIVE",fontWeight=FontWeight.Bold);PresentationCanvas(live,theme,Modifier.fillMaxWidth());Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button({live=PresentationSlide(text="",kind="black")},Modifier.weight(1f)){Text("BLACK")};OutlinedButton({live=null},Modifier.weight(1f)){Text("CLEAR")};OutlinedButton({live=PresentationSlide(text="KING ZULU",kind="logo")},Modifier.weight(1f)){Text("LOGO")}};OutlinedButton({gallery.launch("image/*")},Modifier.fillMaxWidth()){Text("BACKGROUND FROM GALLERY")}}
   }
  }
 }}
}

@Composable fun BiblePanel(onPreview:(PresentationSlide)->Unit){var q by remember{mutableStateOf("Jn 5 24")};var message by remember{mutableStateOf("")};Text("Fast Scripture Search",style=MaterialTheme.typography.titleLarge);OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),label={Text("Reference — e.g. Jn 5 24")});Button({val r=BibleReferenceParser.parse(q);val v=r?.let{OfflineBibleRepository.get(it)};if(v!=null)onPreview(PresentationSlide(v.reference.display(),v.text,v.translation)) else message="Verse not in Beta starter set"},Modifier.fillMaxWidth()){Text("PREVIEW VERSE")};if(message.isNotBlank())Text(message)}
@Composable fun LyricsPanel(onPreview:(PresentationSlide)->Unit){var text by remember{mutableStateOf("")};Text("Lyrics",style=MaterialTheme.typography.titleLarge);OutlinedTextField(text,{text=it},Modifier.fillMaxWidth().height(180.dp),label={Text("Paste or type lyrics")});Button({if(text.isNotBlank())onPreview(PresentationSlide(text=text.lines().take(4).joinToString("\n"),kind="lyrics"))},Modifier.fillMaxWidth()){Text("PREVIEW LYRICS")}}
@Composable fun TimerPanel(onPreview:(PresentationSlide)->Unit){var min by remember{mutableStateOf("5")};Text("Countdown",style=MaterialTheme.typography.titleLarge);OutlinedTextField(min,{min=it.filter(Char::isDigit).take(3)},Modifier.fillMaxWidth(),label={Text("Minutes")});Button({val s=(min.toLongOrNull()?:5)*60;onPreview(PresentationSlide(text="SERVICE BEGINS IN\n\n${formatCountdown(s)}",kind="countdown"))},Modifier.fillMaxWidth()){Text("PREVIEW COUNTDOWN")}}
