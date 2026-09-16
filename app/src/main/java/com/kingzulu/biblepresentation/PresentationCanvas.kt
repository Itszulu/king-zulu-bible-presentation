package com.kingzulu.biblepresentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.io.File

data class PresentationSlide(val reference:String="",val text:String,val translation:String="",val kind:String="scripture")

private fun autoTextSize(text:String):Int = when {
    text.length <= 90 -> 32
    text.length <= 150 -> 28
    text.length <= 230 -> 24
    text.length <= 330 -> 20
    else -> 17
}

@Composable fun PresentationCanvas(slide:PresentationSlide?,theme:BackgroundTheme?,modifier:Modifier=Modifier,textSize:Int?=null,autoFit:Boolean=true){
 Box(modifier.aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
  theme?.localPath?.let{AsyncImage(File(it),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=theme.overlay)))}
  if(slide==null) Text("KING ZULU",color=Color.Gray) else {
   val size=if(autoFit) minOf(textSize?:40,autoTextSize(slide.text)) else (textSize?:28)
   Column(Modifier.fillMaxSize().padding(horizontal=30.dp,vertical=22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
    Text(slide.text,color=Color.White,fontSize=size.sp,lineHeight=(size*1.22f).sp,textAlign=TextAlign.Center,fontWeight=FontWeight.Medium,maxLines=14)
    if(slide.reference.isNotBlank()){Spacer(Modifier.height(12.dp));Text("${slide.reference} • ${slide.translation}",color=Color.LightGray,fontSize=(size*.52f).coerceAtLeast(11f).sp,fontWeight=FontWeight.Bold)}
   }
  }
 }
}
