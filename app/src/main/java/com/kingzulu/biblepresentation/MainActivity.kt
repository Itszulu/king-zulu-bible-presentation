@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kingzulu.biblepresentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KZPurple=Color(0xFF7B61FF);private val KZPurpleSoft=Color(0xFF9B8AFF);private val KZBlack=Color(0xFF0A0A0B);private val KZSurface=Color(0xFF151518);private val KZSurface2=Color(0xFF202025);private val KZText=Color(0xFFF7F7F8);private val KZMuted=Color(0xFFAAAAB2);private val KZLive=Color(0xFFFF5C68)
private val KingZuluDark=darkColorScheme(primary=KZPurple,onPrimary=Color.White,secondary=KZPurpleSoft,background=KZBlack,surface=KZSurface,surfaceVariant=KZSurface2,onBackground=KZText,onSurface=KZText,outline=Color(0xFF3B3B42))

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);OfflineBibleRepository.initialize(this);setContent{MaterialTheme(colorScheme=KingZuluDark){KingZuluApp()}}}}

@Composable fun KingZuluApp(){
 var tab by rememberSaveable{mutableStateOf("Bible")}
 var previewReference by rememberSaveable{mutableStateOf("")};var previewText by rememberSaveable{mutableStateOf("")};var previewTranslation by rememberSaveable{mutableStateOf("")};var previewKind by rememberSaveable{mutableStateOf("")}
 var liveReference by rememberSaveable{mutableStateOf("")};var liveText by rememberSaveable{mutableStateOf("")};var liveTranslation by rememberSaveable{mutableStateOf("")};var liveKind by rememberSaveable{mutableStateOf("")}
 var theme by remember{mutableStateOf<BackgroundTheme?>(null)}
 val preview=if(previewKind.isBlank())null else PresentationSlide(previewReference,previewText,previewTranslation,previewKind)
 val live=if(liveKind.isBlank())null else PresentationSlide(liveReference,liveText,liveTranslation,liveKind)
 fun setPreview(slide:PresentationSlide?){previewReference=slide?.reference.orEmpty();previewText=slide?.text.orEmpty();previewTranslation=slide?.translation.orEmpty();previewKind=slide?.kind.orEmpty()}
 fun setLive(slide:PresentationSlide?){liveReference=slide?.reference.orEmpty();liveText=slide?.text.orEmpty();liveTranslation=slide?.translation.orEmpty();liveKind=slide?.kind.orEmpty()}
 val context=androidx.compose.ui.platform.LocalContext.current;val gallery=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->uri?.let{theme=LocalBackgroundStore.importFromGallery(context,it)}}
 Scaffold(containerColor=KZBlack,topBar={Surface(color=KZBlack){Column{Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("KING ZULU",fontWeight=FontWeight.Black,fontSize=22.sp);Text("Worship presentation",color=KZMuted,fontSize=12.sp)};Column(horizontalAlignment=Alignment.End){Text("DISPLAY",fontSize=9.sp,color=KZMuted,fontWeight=FontWeight.Bold);Text("TV",color=KZMuted,fontWeight=FontWeight.Bold,modifier=Modifier.padding(10.dp))};Spacer(Modifier.width(8.dp));Surface(color=if(live!=null)Color(0xFF35161B)else KZSurface2,shape=RoundedCornerShape(999.dp)){Text(if(live!=null)"● LIVE" else "○ READY",Modifier.padding(horizontal=12.dp,vertical=8.dp),color=if(live!=null)KZLive else Color(0xFFB8BAC1),fontWeight=FontWeight.Black,fontSize=11.sp)}};if(live!=null)Surface(color=Color(0xFF111114),modifier=Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=9.dp)){Text("ON AIR",color=KZLive,fontWeight=FontWeight.Black,fontSize=10.sp);Spacer(Modifier.width(10.dp));Text(live.reference.ifBlank{live.kind.uppercase()},fontSize=12.sp)}}}}},bottomBar={NavigationBar(containerColor=Color(0xFF101013)){listOf("Bible" to "B","Listen" to "L","Service" to "S","Media" to "M","Present" to "P").forEach{(item,glyph)->NavigationBarItem(selected=tab==item,onClick={tab=item},icon={Text(glyph,fontWeight=FontWeight.Bold)},label={Text(item,fontSize=10.sp)})}}}){pad->Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){when(tab){"Bible"->BiblePanel{setPreview(it);tab="Present"};"Listen"->AiListenPanel({setPreview(it);tab="Present"},{slide->setPreview(slide);setLive(slide);tab="Present"});"Service"->ServicePanel(preview,{setPreview(it);tab="Present"},{slide->setPreview(slide);setLive(slide);tab="Present"});"Media"->MediaPanel{setPreview(it);tab="Present"};else->LivePanel(preview,live,theme,{setLive(it)},{gallery.launch("image/*")})}}}
}

@Composable private fun SectionCard(content:@Composable ColumnScope.()->Unit){Surface(Modifier.fillMaxWidth(),color=KZSurface,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}}
@Composable private fun PageHeading(title:String,subtitle:String){Text(title,fontSize=32.sp,fontWeight=FontWeight.Black);Text(subtitle,color=KZMuted,fontSize=14.sp)}
@Composable fun BiblePanel(onPreview:(PresentationSlide)->Unit){var q by rememberSaveable{mutableStateOf("")};var message by rememberSaveable{mutableStateOf("")};PageHeading("Bible","Find Scripture fast and prepare it for the congregation screen.");SectionCard{OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),label={Text("Reference")},placeholder={Text("Jn 5 24 • Rom 8 28")});Button({val r=BibleReferenceParser.parse(q);val v=r?.let{OfflineBibleRepository.get(it)};if(v!=null){message="";onPreview(PresentationSlide(v.reference.display(),v.text,v.translation))}else message="Verse not found — check the reference"},Modifier.fillMaxWidth()){Text("PREVIEW VERSE")};if(message.isNotBlank())Text(message,color=KZPurpleSoft)}}
@Composable fun MediaPanel(onPreview:(PresentationSlide)->Unit){var mode by rememberSaveable{mutableStateOf("Lyrics")};PageHeading("Media","Lyrics and countdowns live together here.");Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Lyrics","Timer").forEach{item->if(mode==item)Button({mode=item},Modifier.weight(1f)){Text(item)}else OutlinedButton({mode=item},Modifier.weight(1f)){Text(item)}}};if(mode=="Lyrics")LyricsEditor(onPreview)else TimerEditor(onPreview)}
@Composable private fun LyricsEditor(onPreview:(PresentationSlide)->Unit){var text by rememberSaveable{mutableStateOf("")};SectionCard{OutlinedTextField(text,{text=it},Modifier.fillMaxWidth().height(220.dp),label={Text("Paste or type lyrics")});Button({if(text.isNotBlank())onPreview(PresentationSlide(text=text.lines().take(4).joinToString("\n"),kind="lyrics"))},Modifier.fillMaxWidth()){Text("PREVIEW LYRICS")}}}
@Composable private fun TimerEditor(onPreview:(PresentationSlide)->Unit){var min by rememberSaveable{mutableStateOf("5")};SectionCard{OutlinedTextField(min,{min=it.filter(Char::isDigit).take(3)},Modifier.fillMaxWidth(),label={Text("Minutes")});Button({val s=(min.toLongOrNull()?:5)*60;onPreview(PresentationSlide(text="SERVICE BEGINS IN\n\n${formatCountdown(s)}",kind="countdown"))},Modifier.fillMaxWidth()){Text("PREVIEW COUNTDOWN")}}}
@Composable fun LivePanel(preview:PresentationSlide?,live:PresentationSlide?,theme:BackgroundTheme?,setLive:(PresentationSlide?)->Unit,chooseBackground:()->Unit){PageHeading("Present","Preview first, then send confidently to the congregation display.");Text("PREVIEW",color=KZMuted);Surface(shape=RoundedCornerShape(20.dp),color=Color.Black){PresentationCanvas(preview,theme,Modifier.fillMaxWidth())};Button({setLive(preview)},enabled=preview!=null,modifier=Modifier.fillMaxWidth()){Text("GO LIVE")};Text("CURRENT LIVE",color=KZMuted);Surface(shape=RoundedCornerShape(20.dp),color=Color.Black){PresentationCanvas(live,theme,Modifier.fillMaxWidth())};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({setLive(PresentationSlide(text="",kind="black"))},Modifier.weight(1f)){Text("BLACK")};OutlinedButton({setLive(null)},Modifier.weight(1f)){Text("CLEAR")};OutlinedButton({setLive(PresentationSlide(text="KING ZULU",kind="logo"))},Modifier.weight(1f)){Text("LOGO")}};OutlinedButton(chooseBackground,Modifier.fillMaxWidth()){Text("BACKGROUND FROM GALLERY")}}
