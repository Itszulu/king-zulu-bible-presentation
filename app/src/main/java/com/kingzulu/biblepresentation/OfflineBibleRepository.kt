package com.kingzulu.biblepresentation

import android.content.Context
import org.json.JSONObject

data class Verse(val reference: BibleReference, val text: String, val translation: String)
object OfflineBibleRepository {
 private var appContext:Context?=null;private var bible:JSONObject?=null
 fun initialize(context:Context){appContext=context.applicationContext}
 private fun data():JSONObject?{bible?.let{return it};val c=appContext?:return null;return runCatching{val json=c.assets.open("bibles/kjv.json").bufferedReader().use{it.readText()};JSONObject(json).also{bible=it}}.getOrNull()}
 private fun normalizeBookName(v:String)=v.lowercase().replace(Regex("[^a-z0-9]"),"").replace("psalm","psalms")
 private fun normalizeText(v:String)=v.lowercase().replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ").trim()
 fun get(r:BibleReference,translation:String="KJV"):Verse?{if(!translation.equals("KJV",true)||r.verseStart==null)return null;val books=data()?.optJSONArray("books")?:return null;val wanted=normalizeBookName(r.book);var book:JSONObject?=null;for(i in 0 until books.length()){val c=books.optJSONObject(i)?:continue;if(normalizeBookName(c.optString("englishName"))==wanted||normalizeBookName(c.optString("book"))==wanted){book=c;break}};val chapters=book?.optJSONArray("chapters")?:return null;val chapter=(0 until chapters.length()).mapNotNull{chapters.optJSONObject(it)}.firstOrNull{it.optInt("chapter")==r.chapter}?:return null;val verses=chapter.optJSONArray("verses")?:return null;val start=r.verseStart;val end=r.verseEnd?:start;if(end<start)return null;val texts=mutableListOf<String>();for(n in start..end){val v=(0 until verses.length()).mapNotNull{verses.optJSONObject(it)}.firstOrNull{it.optInt("number",it.optInt("verse"))==n}?:return null;val t=v.optString("text").trim();if(t.isBlank())return null;texts+=t};return Verse(r,texts.joinToString(" "),"KJV")}
 fun searchQuote(spoken:String):Verse?{
  val filler=setOf("the","a","an","and","but","or","so","that","this","these","those","is","are","was","were","be","been","being","to","of","for","in","on","at","with","from","as","it","its","i","you","we","they","he","she","my","your","our","their","his","her","me","us","them","says","said","say","bible","scripture","verse","book","open","turn","please")
  val qWords=normalizeText(spoken).split(" ").filter{it.length>1&&!filler.contains(it)}
  if(qWords.size<3)return null
  val books=data()?.optJSONArray("books")?:return null;var best:Verse?=null;var bestScore=0.0
  for(bi in 0 until books.length()){val b=books.optJSONObject(bi)?:continue;val name=b.optString("englishName");val chapters=b.optJSONArray("chapters")?:continue;for(ci in 0 until chapters.length()){val c=chapters.optJSONObject(ci)?:continue;val cn=c.optInt("chapter");val verses=c.optJSONArray("verses")?:continue;for(vi in 0 until verses.length()){val v=verses.optJSONObject(vi)?:continue;val text=v.optString("text");val normalized=normalizeText(text);val words=normalized.split(" ").toSet();val hits=qWords.count{it in words};var score=hits.toDouble()/qWords.size
    val phrase=qWords.joinToString(" ");if(phrase.length>=10&&normalized.contains(phrase))score+=0.7
    // reward ordered neighbouring words even when speech recognition adds/removes filler words
    val ordered=qWords.windowed(2).count{normalized.contains(it.joinToString(" "))};score+=ordered*0.08
    if(score>bestScore){bestScore=score;best=Verse(BibleReference(name,cn,v.optInt("number",v.optInt("verse"))),text,"KJV")}
  }}}
  return if(bestScore>=0.58)best else null
 }
}
