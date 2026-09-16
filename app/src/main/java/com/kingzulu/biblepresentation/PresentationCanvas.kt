package com.kingzulu.biblepresentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
@Composable fun PresentationCanvas(slide:PresentationSlide?,theme:BackgroundTheme?,modifier:Modifier=Modifier){
 Box(modifier.aspectRatio(16f/9f).background(Color.Black),contentAlignment=Alignment.Center){
  theme?.localPath?.let{AsyncImage(File(it),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=theme.overlay)))}
  if(slide==null) Text("KING ZULU",color=Color.Gray) else Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text(slide.text,color=Color.White,fontSize=28.sp,lineHeight=36.sp,textAlign=TextAlign.Center,fontWeight=FontWeight.Medium)
   if(slide.reference.isNotBlank()){Spacer(Modifier.height(16.dp));Text("${slide.reference} • ${slide.translation}",color=Color.LightGray,fontWeight=FontWeight.Bold)}
  }
 }
}
